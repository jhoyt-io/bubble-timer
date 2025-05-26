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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final String TAG = "WebsocketManager";
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY_MS = 5000; // 5 seconds

    private final OkHttpClient okHttpClient;
    private final WebsocketMessageListener messageListener;
    private WebSocket webSocket;
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private String authToken;
    private String deviceId;
    private final ActiveTimerRepository activeTimerRepository;

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }

    private ConnectionState currentState = ConnectionState.DISCONNECTED;

    public interface WebsocketMessageListener {
        void onFailure(String reason);
        void onConnectionStateChanged(ConnectionState newState);
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

    private void setConnectionState(ConnectionState newState) {
        if (currentState != newState) {
            currentState = newState;
            messageListener.onConnectionStateChanged(newState);
            Log.i(TAG, "Connection state changed to: " + newState);
        }
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
        if (isConnecting.get()) {
            Log.i(TAG, "Already connecting, ignoring initialize call");
            return;
        }

        this.authToken = authToken;
        this.deviceId = deviceId;
        connect();
    }

    private void connect() {
        if (isConnecting.get()) {
            return;
        }

        isConnecting.set(true);
        setConnectionState(ConnectionState.CONNECTING);

        Request webSocketRequest = new Request.Builder()
                .header("Authorization", authToken)
                .header("DeviceId", deviceId)
                .url("wss://zc4ahryh1l.execute-api.us-east-1.amazonaws.com/prod/")
                .build();

        WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.i(TAG, "WebSocket opened successfully");
                isConnecting.set(false);
                reconnectAttempts.set(0);
                setConnectionState(ConnectionState.CONNECTED);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                Log.e(TAG, "WebSocket failure: " + (response != null ? response.body() : ""));
                Log.e(TAG, "Caused by: ", t);
                isConnecting.set(false);

                if (currentState == ConnectionState.CONNECTED) {
                    setConnectionState(ConnectionState.RECONNECTING);
                    attemptReconnect();
                } else {
                    setConnectionState(ConnectionState.DISCONNECTED);
                    messageListener.onFailure(t.getMessage());
                }
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
                Log.i(TAG, "WebSocket closed: " + reason);
                setConnectionState(ConnectionState.DISCONNECTED);
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
                Log.i(TAG, "WebSocket closing: " + reason);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.i(TAG, "Received message: " + text);

                try {
                    JSONObject jsonData = new JSONObject(text);
                    String type = jsonData.getString("type");
                    Log.i(TAG, "Message type: " + type);
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
                            Log.e(TAG, "Unsupported message type: " + type);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse message: ", e);
                }
            }
        };

        webSocket = okHttpClient.newWebSocket(webSocketRequest, webSocketListener);
    }

    private void attemptReconnect() {
        if (reconnectAttempts.incrementAndGet() <= MAX_RECONNECT_ATTEMPTS) {
            Log.i(TAG, "Attempting to reconnect (attempt " + reconnectAttempts.get() + ")");
            new android.os.Handler().postDelayed(this::connect, RECONNECT_DELAY_MS);
        } else {
            Log.e(TAG, "Max reconnection attempts reached");
            setConnectionState(ConnectionState.DISCONNECTED);
            messageListener.onFailure("Max reconnection attempts reached");
        }
    }

    private JSONObject fixFrigginTimer(Timer timer) throws JSONException {
        JSONObject result = Timer.timerToJson(timer);

        if (!result.has("userId")) {
            Log.i(TAG, "userId is not set");
            result.put("userId", "whattheheck");
        }
        return result;
    }

    public void sendUpdateTimerToWebsocket(Timer timer, String updateReason) {
        if (currentState != ConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot send update: WebSocket not connected");
            return;
        }

        JSONArray shareWithArray = new JSONArray();
        timer.getSharedWith().forEach(shareWithArray::put);

        String webSocketRequestString = null;
        try {
            Log.i(TAG, "Sending timer update");
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
            Log.e(TAG, "Failed to create update message", e);
        }

        sendMessage(webSocketRequestString);
    }

    public void sendStopTimerToWebsocket(String timerId, Set<String> sharedWith) {
        if (currentState != ConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot send stop: WebSocket not connected");
            return;
        }

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
            Log.e(TAG, "Failed to create stop message", e);
        }

        sendMessage(webSocketRequestString);
    }

    private void sendMessage(String message) {
        if (message == null) {
            Log.e(TAG, "Cannot send null message");
            return;
        }

        if (webSocket == null) {
            Log.e(TAG, "WebSocket is null");
            messageListener.onFailure("WebSocket is null");
            return;
        }

        if (!webSocket.send(message)) {
            Log.e(TAG, "Failed to send message");
            messageListener.onFailure("Failed to send message");
        } else {
            Log.i(TAG, "Message sent successfully (queue size: " + webSocket.queueSize() + ")");
        }
    }

    public void close() {
        Log.i(TAG, "Closing WebSocket");
        if (webSocket != null) {
            final int normalClosure = 1000;
            webSocket.close(normalClosure, "AppClosed");
            webSocket = null;
        }
        setConnectionState(ConnectionState.DISCONNECTED);
    }

    public ConnectionState getConnectionState() {
        return currentState;
    }
}
