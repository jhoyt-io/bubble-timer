package io.jhoyt.bubbletimer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

// required interactions with repository
//   * insert new timers when notified of new timers by the websocket
//   * update timers when notified by websocket
//   * Delete timers when notified by websocket
//
// required interactions with websocket
//   * insert new timers when notified of new timers by the repository
//   * update timers when notified by repository
//   * Delete timers when notified by repository

public class WebsocketManager {
    private final OkHttpClient okHttpClient;
    private final WebsocketMessageListener messageListener;
    private WebSocket webSocket;

    private ActiveTimerRepository activeTimerRepository;
    public interface WebsocketMessageListener {
        void onFailure(String reason);
    }

    public WebsocketManager(
            WebsocketMessageListener messageListener,
            ActiveTimerRepository activeTimerRepository
    ) {
        this.okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        this.messageListener = messageListener;
        this.webSocket = null;
        this.activeTimerRepository = activeTimerRepository;
    }

    private void upsertLocalTimerList(Timer timer) {
        List<Timer> timers = this.activeTimerRepository.getAllActiveTimers().getValue();

        int i=0;
        for (; i<timers.size(); i++) {
            Timer t = timers.get(i);
            if (t.getId().equals(timer.getId())) {
                this.activeTimerRepository.update(timer.copy());
                break;
            }
        }

        // if we got to the end and didn't find it, add it
        if (i == timers.size()) {
            this.activeTimerRepository.insert(timer.copy());
        }
    }

    private void removeTimerFromLocalTimerList(String timerId) {
        this.activeTimerRepository.deleteById(timerId);
    }

    public void initialize(String authToken, String deviceId) {
        Request webSocketRequest = new Request.Builder()
                .header("Authorization", authToken)
                .header("DeviceId", deviceId)
                .url("wss://zc4ahryh1l.execute-api.us-east-1.amazonaws.com/prod/")
                .build();

        WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                Log.i("WebsocketManager", "[Websocket] Failure: " + (response != null ? response.body() : ""));
                Log.i("WebsocketManager", "[Websocket] Caused by: ", t);

                messageListener.onFailure(t.getMessage());
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
                Log.i("WebsocketManager", "[Websocket] Closed: " + reason);
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
                Log.i("WebsocketManager", "[Websocket] Closing: " + reason);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.i("WebsocketManager", "[Websocket] Received: " + text);

                try {
                    JSONObject jsonData = new JSONObject(text);
                    String type = jsonData.getString("type");
                    Log.i("WebsocketManager", "Type: " + type);
                    switch (type) {
                        case "activeTimerList":
                            JSONArray timerList = jsonData.getJSONArray("timerList");
                            for (int i = 0; i < timerList.length(); i++) {
                                upsertLocalTimerList(Timer.timerFromJson(timerList.getJSONObject(i)));
                            }

                            return;

                        case "updateTimer":
                            JSONObject timer = jsonData.getJSONObject("timer");
                            upsertLocalTimerList(Timer.timerFromJson(timer));

                            return;

                        case "stopTimer":
                            String timerId = jsonData.getString("timerId");
                            removeTimerFromLocalTimerList(timerId);

                            return;

                        default:
                            Log.e("WebsocketManager", "Unsupported type: " + type);
                    }
                } catch (JSONException e) {
                    Log.i("WebsocketManager", "Could not parse: ", e);
                }
            }
        };

        webSocket = okHttpClient.newWebSocket(webSocketRequest, webSocketListener);
    }

    private JSONObject fixFrigginTimer(Timer timer) throws JSONException {
        JSONObject result = Timer.timerToJson(timer);

        if (!result.has("userId")) {
            Log.i("fixFrigginTimer", "userId is not set");
            result.put("userId", "whattheheck");
        }
        return result;
    }

    public void sendUpdateTimerToWebsocket(Timer timer, String updateReason) {
        JSONArray shareWithArray = new JSONArray();
        timer.getSharedWith().forEach(shareWithArray::put);

        String webSocketRequestString = null;
        try {
            Log.i("ForegroundService", "Websocket, sendmessage to update timer");
            webSocketRequestString = new JSONObject()
                    .put("action", "sendmessage")
                    .put("data", new JSONObject()
                            .put("type", "updateTimer")
                            .put("reason", updateReason)
                            .put("shareWith", shareWithArray)
                            .put("timer", fixFrigginTimer(timer))
                    )
                    .toString();
        } catch (Exception e) {
            Log.i("ForegroundService", "Websocket, sendmessage failed", e);
        }

        sendMessage(webSocketRequestString);
    }

    public void sendStopTimerToWebsocket(String timerId, Set<String> sharedWith) {
        JSONArray shareWithArray = new JSONArray();
        sharedWith.forEach(shareWithArray::put);

        String webSocketRequestString = null;
        try {
            webSocketRequestString = new JSONObject()
                    .put("action", "sendmessage")
                    .put("data", new JSONObject()
                            .put("type", "stopTimer")
                            .put("shareWith", shareWithArray)
                            .put("timerId", timerId)
                    )
                    .toString();
        } catch (Exception e) {
            Log.i("ForegroundService", "Websocket, sendmessage failed", e);
        }

        sendMessage(webSocketRequestString);
    }

    private void sendMessage(String message) {
        if (message != null && webSocket != null
                && webSocket.send(message)) {
            Log.i("WebsocketManager", "Websocket, sendmessage, sent - "
                    + webSocket.queueSize() + " messages in queue");
        } else {
            Log.i("WebsocketManager", "Websocket, sendmessage, failed to send");
            if (webSocket == null) {
                Log.i("WebsocketManager", "Websocket NULL");
                messageListener.onFailure("Websocket is NULL");
            } else if (message == null) {
                Log.i("WebsocketManager", "Request string NULL");
            } else {
                // Re-connect and retry
                messageListener.onFailure("Unknown");
                /*
                if (webSocket.send(message)) {
                    Log.i("WebsocketManager", "Websocket, sendmessage, sent");
                } else {
                    Log.i("WebsocketManager", "Websocket, sendmessage, failed to send");
                }
                 */
            }
        }
    }

    public void close() {
        Log.i("WebsocketManager", "Websocket closed");
        final int normalClosure = 1000;
        this.webSocket.close(normalClosure, "AppClosed");
    }
}
