package io.jhoyt.bubbletimer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.internal.ws.RealWebSocket;

public class WebsocketManager {
    private final OkHttpClient okHttpClient;
    private final WebsocketMessageListener messageListener;
    private WebSocket webSocket;

    public interface WebsocketMessageListener {
        void onFailure(String reason);
        void onActiveTimerList(JSONArray timerList) throws JSONException;
        void onUpdateTimer(JSONObject timer) throws JSONException;
        void onStopTimer(String timerId) throws JSONException;
    }

    public WebsocketManager(WebsocketMessageListener messageListener) {
        this.okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        this.messageListener = messageListener;
        this.webSocket = null;
    }

    public void initialize(String authToken, String deviceId) {
        //if (webSocket == null) {
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
                                messageListener.onActiveTimerList(timerList);

                                return;

                            case "updateTimer":
                                JSONObject timer = jsonData.getJSONObject("timer");
                                messageListener.onUpdateTimer(timer);

                                return;

                            case "stopTimer":
                                String timerId = jsonData.getString("timerId");
                                messageListener.onStopTimer(timerId);

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
        //} else {
        //    ((RealWebSocket) webSocket).connect(okHttpClient);
        //}
    }

    public void sendMessage(String message) {
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
