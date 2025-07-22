package io.jhoyt.bubbletimer;

import android.os.Handler;
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
    private static final int MAX_RECONNECT_ATTEMPTS = Integer.MAX_VALUE; // Never stop trying to reconnect
    private static final long RECONNECT_DELAY_MS = 5000; // 5 seconds
    private static final long PING_INTERVAL_MS = 15000; // Send ping every 15 seconds
    private static final long PONG_TIMEOUT_MS = 5000; // Wait 5 seconds for pong response
    private static final int MAX_QUEUE_SIZE = 1000; // Increased from 100 to handle bursts of messages
    private static final long QUEUE_CHECK_INTERVAL_MS = 5000; // Check queue every 5 seconds

    private final OkHttpClient okHttpClient;
    private final WebsocketMessageListener messageListener;
    private WebSocket webSocket;
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private String authToken;
    private String deviceId;
    private String cognitoUserName;
    private final ActiveTimerRepository activeTimerRepository;
    private Handler queueCheckHandler;
    private Handler pingHandler;
    private Handler reconnectHandler;
    private Runnable queueCheckRunnable;
    private Runnable pingRunnable;
    private long lastSuccessfulMessageTime = 0;
    private long lastPongTime = 0;
    private boolean hasReceivedPong = false;

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
        void onTimerReceived(Timer timer);
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
        
        // Initialize handlers
        this.queueCheckHandler = new Handler();
        this.pingHandler = new Handler();
        this.reconnectHandler = new Handler();

        // Initialize queue check handler
        this.queueCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentState == ConnectionState.CONNECTED && webSocket != null) {
                    long queueSize = webSocket.queueSize();
                    long timeSinceLastMessage = System.currentTimeMillis() - lastSuccessfulMessageTime;
                    
                    // Only force reconnect if queue is large AND we haven't had a successful message in a while
                    if (queueSize > MAX_QUEUE_SIZE && timeSinceLastMessage > QUEUE_CHECK_INTERVAL_MS) {
                        Log.w(TAG, "Queue size " + queueSize + " is too large and no recent successful messages, forcing reconnection");
                        setConnectionState(ConnectionState.RECONNECTING);
                        attemptReconnect();
                    }
                }
                queueCheckHandler.postDelayed(this, QUEUE_CHECK_INTERVAL_MS);
            }
        };

        // Initialize ping handler
        this.pingRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentState == ConnectionState.CONNECTED && webSocket != null) {
                    try {
                        long timestamp = System.currentTimeMillis();
                        // Send ping as a direct WebSocket message with user info
                        JSONObject pingMessage = new JSONObject()
                                .put("action", "sendmessage")
                                .put("data", new JSONObject()
                                    .put("type", "ping")
                                    .put("timestamp", timestamp)
                                    .put("direct", true)
                                );
                        String pingMessageStr = pingMessage.toString();
                        Log.d(TAG, "Sending ping message: " + pingMessageStr);
                        webSocket.send(pingMessageStr);
                        
                        // Only check pong timeout if we've received at least one pong
                        if (hasReceivedPong) {
                            long timeSinceLastPong = System.currentTimeMillis() - lastPongTime;
                            if (timeSinceLastPong > PONG_TIMEOUT_MS) {
                                Log.w(TAG, "No pong received in " + timeSinceLastPong + "ms, forcing reconnection");
                                setConnectionState(ConnectionState.RECONNECTING);
                                attemptReconnect();
                            }
                        } else {
                            Log.d(TAG, "Waiting for first pong response (sent ping at " + timestamp + ")");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send ping", e);
                        setConnectionState(ConnectionState.RECONNECTING);
                        attemptReconnect();
                    }
                }
                pingHandler.postDelayed(this, PING_INTERVAL_MS);
            }
        };
    }

    private void setConnectionState(ConnectionState newState) {
        if (currentState != newState) {
            currentState = newState;
            messageListener.onConnectionStateChanged(newState);
            Log.i(TAG, "Connection state changed to: " + newState);
            
            if (newState == ConnectionState.CONNECTED) {
                queueCheckHandler.post(queueCheckRunnable);
                pingHandler.post(pingRunnable);
                // Reset pong tracking on new connection
                hasReceivedPong = false;
                lastPongTime = 0;
            } else {
                queueCheckHandler.removeCallbacks(queueCheckRunnable);
                pingHandler.removeCallbacks(pingRunnable);
            }
        }
    }

    private void upsertLocalTimerList(Timer timer) {
        List<Timer> timers = this.activeTimerRepository.getAllActiveTimers().getValue();

        int i=0;
        for (; i<timers.size(); i++) {
            Timer t = timers.get(i);
            if (t.getId().equals(timer.getId())) {
                this.activeTimerRepository.update(timer.copy());
                messageListener.onTimerReceived(timer.copy());
                break;
            }
        }

        // if we got to the end and didn't find it, add it
        if (i == timers.size()) {
            this.activeTimerRepository.insert(timer.copy());
            messageListener.onTimerReceived(timer.copy());
        }
    }

    private void removeTimerFromLocalTimerList(String timerId) {
        this.activeTimerRepository.deleteById(timerId);
    }

    public void initialize(String authToken, String deviceId, String cognitoUserName) {
        Log.i(TAG, "Initializing WebSocket with:");
        Log.i(TAG, "  authToken: " + (authToken != null ? authToken.substring(0, Math.min(20, authToken.length())) + "..." : "null"));
        Log.i(TAG, "  deviceId: " + deviceId);
        Log.i(TAG, "  cognitoUserName: " + cognitoUserName);
        
        if (authToken == null || authToken.isEmpty()) {
            Log.e(TAG, "Cannot initialize WebSocket: authToken is null or empty");
            messageListener.onFailure("Auth token is null or empty");
            return;
        }
        
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Cannot initialize WebSocket: deviceId is null or empty");
            messageListener.onFailure("Device ID is null or empty");
            return;
        }
        
        if (cognitoUserName == null || cognitoUserName.isEmpty()) {
            Log.e(TAG, "Cannot initialize WebSocket: cognitoUserName is null or empty");
            messageListener.onFailure("Cognito username is null or empty");
            return;
        }
        
        if (isConnecting.get()) {
            Log.i(TAG, "Already connecting, ignoring initialize call");
            return;
        }

        this.authToken = authToken;
        this.deviceId = deviceId;
        this.cognitoUserName = cognitoUserName;
        connect();
    }

    private void connect() {
        Log.i(TAG, "Attempting to connect to WebSocket...");
        Log.i(TAG, "Current state: " + currentState);
        Log.i(TAG, "Is connecting: " + isConnecting.get());
        
        if (isConnecting.get()) {
            Log.i(TAG, "Already connecting, skipping this connection attempt");
            return;
        }

        isConnecting.set(true);
        setConnectionState(ConnectionState.CONNECTING);

        Log.i(TAG, "Building WebSocket request with:");
        Log.i(TAG, "  Authorization header: " + (authToken != null ? "present" : "null"));
        Log.i(TAG, "  DeviceId header: " + deviceId);
        Log.i(TAG, "  URL: wss://zc4ahryh1l.execute-api.us-east-1.amazonaws.com/prod/");

        Request webSocketRequest = new Request.Builder()
                .header("Authorization", authToken)
                .header("DeviceId", deviceId)
                .header("User-Agent", "BubbleTimer-Android/1.0")
                .url("wss://zc4ahryh1l.execute-api.us-east-1.amazonaws.com/prod/")
                .build();

        WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.i(TAG, "WebSocket opened successfully");
                Log.i(TAG, "Response code: " + response.code());
                Log.i(TAG, "Response message: " + response.message());
                Log.i(TAG, "Response headers: " + response.headers());
                isConnecting.set(false);
                reconnectAttempts.set(0);
                setConnectionState(ConnectionState.CONNECTED);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                Log.e(TAG, "WebSocket failure:");
                Log.e(TAG, "  Response code: " + (response != null ? response.code() : "null"));
                Log.e(TAG, "  Response message: " + (response != null ? response.message() : "null"));
                Log.e(TAG, "  Response body: " + (response != null ? response.body() : "null"));
                Log.e(TAG, "  Throwable: " + t.getMessage());
                Log.e(TAG, "  Throwable type: " + t.getClass().getSimpleName());
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
                    
                    // Check if this is an error message from the server
                    if (jsonData.has("message") && jsonData.has("connectionId")) {
                        Log.e(TAG, "Received server error message: " + jsonData.toString(2));
                        String errorMessage = jsonData.optString("message", "Unknown error");
                        Log.e(TAG, "Server error: " + errorMessage);
                        
                        // Don't immediately fail the connection for server errors
                        // Instead, log the error and continue
                        Log.w(TAG, "Continuing connection despite server error");
                        return;
                    }
                    
                    // Check if message has a type field
                    if (!jsonData.has("type")) {
                        Log.w(TAG, "Received message without type field: " + jsonData.toString(2));
                        return;
                    }
                    
                    String type = jsonData.getString("type");
                    Log.i(TAG, "Message type: " + type + ", full message: " + jsonData.toString(2));
                    
                    // Handle pong messages directly
                    if (type.equals("pong")) {
                        hasReceivedPong = true;
                        lastPongTime = System.currentTimeMillis();
                        Log.d(TAG, "Received pong response with timestamp: " + jsonData.optLong("timestamp", 0));
                        return;
                    }
                    
                    // Send acknowledgment for received messages
                    try {
                        JSONObject ackMessage = new JSONObject()
                            .put("action", "sendmessage")
                            .put("data", new JSONObject()
                                .put("type", "acknowledge")
                                .put("messageId", jsonData.optString("messageId", ""))
                            );
                        webSocket.send(ackMessage.toString());
                        Log.d(TAG, "Sent acknowledgment: " + ackMessage.toString(2));
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send acknowledgment", e);
                    }

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
                            // Notify the UI to clean up any windows/views for this timer
                            messageListener.onTimerReceived(null);
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
            reconnectHandler.postDelayed(this::connect, RECONNECT_DELAY_MS);
        } else {
            Log.e(TAG, "Max reconnection attempts reached");
            setConnectionState(ConnectionState.DISCONNECTED);
            messageListener.onFailure("Max reconnection attempts reached");
        }
    }

    private JSONObject fixFrigginTimer(Timer timer) throws JSONException {
        Log.i(TAG, "Fixing timer data for WebSocket transmission:");
        Log.i(TAG, "  Timer ID: " + (timer != null ? timer.getId() : "null"));
        Log.i(TAG, "  Timer userId: " + (timer != null ? timer.getUserId() : "null"));
        Log.i(TAG, "  Timer name: " + (timer != null ? timer.getName() : "null"));
        
        JSONObject result = Timer.timerToJson(timer);

        if (!result.has("userId")) {
            Log.w(TAG, "userId is not set in timer JSON, adding placeholder");
            result.put("userId", "whattheheck");
        } else {
            Log.i(TAG, "userId is set in timer JSON: " + result.getString("userId"));
        }
        
        Log.i(TAG, "Final timer JSON: " + result.toString(2));
        return result;
    }

    public void sendUpdateTimerToWebsocket(Timer timer, String updateReason) {
        Log.i(TAG, "Attempting to send timer update:");
        Log.i(TAG, "  Current state: " + currentState);
        Log.i(TAG, "  Timer ID: " + (timer != null ? timer.getId() : "null"));
        Log.i(TAG, "  Timer userId: " + (timer != null ? timer.getUserId() : "null"));
        Log.i(TAG, "  Update reason: " + updateReason);
        
        if (currentState != ConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot send update: WebSocket not connected (state: " + currentState + ")");
            
            // Try to reconnect if we're disconnected
            if (currentState == ConnectionState.DISCONNECTED && authToken != null && deviceId != null && cognitoUserName != null) {
                Log.i(TAG, "Attempting to reconnect before sending update");
                connect();
            }
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

        try {
            // Log the message we're about to send
            JSONObject messageJson = new JSONObject(message);
            Log.d(TAG, "Sending message: " + messageJson.toString(2));
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse message for logging", e);
        }

        if (!webSocket.send(message)) {
            Log.e(TAG, "Failed to send message");
            messageListener.onFailure("Failed to send message");
            // Attempt to reconnect if message fails to send
            if (currentState == ConnectionState.CONNECTED) {
                setConnectionState(ConnectionState.RECONNECTING);
                attemptReconnect();
            }
        } else {
            lastSuccessfulMessageTime = System.currentTimeMillis();
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
        queueCheckHandler.removeCallbacks(queueCheckRunnable);
        pingHandler.removeCallbacks(pingRunnable);
        setConnectionState(ConnectionState.DISCONNECTED);
    }

    public ConnectionState getConnectionState() {
        return currentState;
    }
    
    public void forceReconnect() {
        Log.i(TAG, "Force reconnecting WebSocket");
        if (authToken != null && deviceId != null && cognitoUserName != null) {
            setConnectionState(ConnectionState.DISCONNECTED);
            connect();
        } else {
            Log.e(TAG, "Cannot force reconnect: missing credentials");
        }
    }
}
