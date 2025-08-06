package io.jhoyt.bubbletimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.CombinedVibration;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings.Secure;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import dagger.hilt.android.AndroidEntryPoint;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import javax.inject.Inject;

@AndroidEntryPoint
public class ForegroundService extends LifecycleService implements Window.BubbleEventListener {
    public static final int NOTIFICATION_ID = 2;
    public static String MESSAGE_RECEIVER_ACTION = "foreground-service-message-receiver";
    static final String channelId = "jhoyt.io.permanence.v3";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private VibratorManager vibratorManager;
    private Vibrator vibrator;

    private PowerManager.WakeLock wakeLock;

    private String currentUserId;
    private final Handler timerHandler;
    private Runnable updater;

    @Inject
    WebsocketManager websocketManager;

    @Inject
    ActiveTimerRepository activeTimerRepository;
    private List<Timer> activeTimers;
    private Map<String, Window> windowsByTimerId;

    private Window expandedWindow;

    // Simplified shared timer tracking - integrated directly into ForegroundService
    private final Set<String> sharedTimerIds = new HashSet<>();
    private final Set<String> pendingInvitationIds = new HashSet<>();
    private final Set<String> activeSharedTimerIds = new HashSet<>();

    private Boolean isOverlayShown = false;
    private boolean isDebugModeEnabled = false;  // Track if debug mode is enabled
    private long lastAuthTokenRequest = 0;
    private static final long AUTH_TOKEN_DEBOUNCE_MS = 5000; // 5 seconds
    private Timer pendingTimerUpdate = null; // Timer to send when WebSocket connects

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ForegroundService", "onReceive called");

            String command = intent.getStringExtra("command");
            Log.i("ForegroundService", "Broadcast received with command: " + command);

            if (command == null) {
                Log.w("ForegroundService", "Broadcast received with null command");
                return;
            }

            if (command.equals("showOverlay")) {
                isOverlayShown = true;
                if (!activeTimers.isEmpty()) {
                    activeTimers.forEach(timer -> {
                        if (windowsByTimerId.containsKey(timer.getId())) {
                            windowsByTimerId.get(timer.getId())
                                    .open(timer, ForegroundService.this);
                        }
                    });
                } else {
                    notificationBuilder.setContentText("No active timers.");
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                }
            } else if (command.equals("hideOverlay")) {
                isOverlayShown = false;
                if (!activeTimers.isEmpty()) {
                    activeTimers.forEach(timer -> {
                        if (windowsByTimerId.containsKey(timer.getId())) {
                            windowsByTimerId.get(timer.getId()).close();
                        }
                    });
                }

                if (ForegroundService.this.expandedWindow != null) {
                    ForegroundService.this.expandedWindow.close();
                }
            } else if (command.equals("receiveAuthToken")) {
                Log.i("ForegroundService", "Auth token received");

                String authToken = intent.getStringExtra("authToken");
                currentUserId = intent.getStringExtra("userId");
                String androidId = Secure.getString(
                        getApplicationContext().getContentResolver(),
                        Secure.ANDROID_ID);

                Log.i("ForegroundService", "Auth token length: " + (authToken != null ? authToken.length() : 0));
                Log.i("ForegroundService", "User id: " + currentUserId);
                Log.i("ForegroundService", "Android id: " + androidId);
                Log.d("ForegroundService", "Number of existing windows to update: " + windowsByTimerId.size());
                
                if (authToken == null || authToken.isEmpty()) {
                    Log.e("ForegroundService", "ERROR: Received null or empty auth token!");
                }
                
                if (currentUserId == null || currentUserId.isEmpty()) {
                    Log.e("ForegroundService", "ERROR: Received null or empty user ID!");
                }
                
                if (androidId == null || androidId.isEmpty()) {
                    Log.e("ForegroundService", "ERROR: Android ID is null or empty!");
                }

                Log.i("ForegroundService", "Initializing WebSocket with received credentials");
                websocketManager.initialize(authToken, androidId, currentUserId);
                
                // Update current user ID on all existing windows
                windowsByTimerId.forEach((timerId, window) -> {
                    Log.d("ForegroundService", "Updating currentUserId on existing window: " + currentUserId);
                    window.getTimerView().setCurrentUserId(currentUserId);
                });
                if (expandedWindow != null) {
                    Log.d("ForegroundService", "Updating currentUserId on expanded window: " + currentUserId);
                    expandedWindow.getTimerView().setCurrentUserId(currentUserId);
                }
                
                // Check if we need to connect based on current shared timers
                checkWebsocketConnectionNeeds();
                
                // Handle specific callbacks
                String callback = intent.getStringExtra("callback");
                if ("connectForSharedTimers".equals(callback)) {
                    Log.i("ForegroundService", "Received auth token for connectForSharedTimers callback");
                    websocketManager.connectIfNeeded();
                }
            } else if (command.equals("toggleDebugMode")) {
                isDebugModeEnabled = !isDebugModeEnabled;
                // Update debug mode for all windows
                windowsByTimerId.forEach((timerId, window) -> {
                    window.setDebugMode(isDebugModeEnabled);
                });
                if (expandedWindow != null) {
                    expandedWindow.setDebugMode(isDebugModeEnabled);
                }
            } else if (command.equals("getAuthToken")) {
                Log.i("ForegroundService", "getAuthToken command received");
                
                // Always request auth token from MainActivity, regardless of cached currentUserId
                // MainActivity will check the actual authentication state with Amplify
                Intent message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
                message.putExtra("command", "sendAuthToken");
                
                // Pass through callback information if provided
                String callback = intent.getStringExtra("callback");
                if (callback != null) {
                    message.putExtra("callback", callback);
                    String timerId = intent.getStringExtra("timerId");
                    if (timerId != null) {
                        message.putExtra("timerId", timerId);
                    }
                }
                
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(message);
            } else if (command.equals("connectForSharedTimers")) {
                Log.i("ForegroundService", "connectForSharedTimers command received");
                
                // Request fresh auth token before attempting to connect
                // This ensures we have valid credentials for the WebSocket connection
                Intent message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
                message.putExtra("command", "sendAuthToken");
                message.putExtra("callback", "connectForSharedTimers");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(message);
            }
        }
    };

    public ForegroundService() {
        Log.i("ForegroundService", "Constructing");
        this.timerHandler = new Handler();
        this.expandedWindow = null;
        this.vibrator = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Set up WebSocket message listener
        websocketManager.setMessageListener(new WebsocketManager.WebsocketMessageListener() {
            @Override
            public void onFailure(String reason) {
                Log.i("ForegroundService", "Websocket failure: " + reason);

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAuthTokenRequest < AUTH_TOKEN_DEBOUNCE_MS) {
                    Log.i("ForegroundService", "Ignoring WebSocket failure - too soon since last auth token request");
                    return;
                }
                lastAuthTokenRequest = currentTime;

                Intent message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
                message.putExtra("command", "sendAuthToken");
                LocalBroadcastManager.getInstance(ForegroundService.this).sendBroadcast(message);
            }

            @Override
            public void onConnectionStateChanged(WebsocketManager.ConnectionState newState) {
                Log.i("ForegroundService", "Websocket state changed to: " + newState);
                
                // Update notification based on connection state
                String statusText;
                switch (newState) {
                    case CONNECTED:
                        statusText = "Connected";
                        // Send any pending timer update when WebSocket connects
                        if (pendingTimerUpdate != null) {
                            Log.d("ForegroundService", "WebSocket connected, sending pending timer update: " + pendingTimerUpdate.getId());
                            websocketManager.sendUpdateTimerToWebsocket(pendingTimerUpdate, "pending update");
                            pendingTimerUpdate = null;
                        }
                        break;
                    case CONNECTING:
                        statusText = "Connecting...";
                        break;
                    case RECONNECTING:
                        statusText = "Reconnecting...";
                        break;
                    case DISCONNECTED:
                        statusText = "Disconnected";
                        break;
                    default:
                        statusText = "Unknown state";
                }
                
                notificationBuilder.setContentText(statusText);
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }

            @Override
            public void onTimerReceived(Timer timer) {
                Log.i("ForegroundService", "Received timer update from WebSocket: " + timer.getId());
                
                // Update the UI for this timer if it's currently displayed
                if (windowsByTimerId.containsKey(timer.getId())) {
                    Window window = windowsByTimerId.get(timer.getId());
                    if (window.isOpen()) {
                        // Update the timer in the window
                        window.getTimerView().setTimer(timer);
                        window.invalidate();
                    }
                }
                
                // If this is a new timer and overlay is shown, create a new window for it
                if (!windowsByTimerId.containsKey(timer.getId()) && isOverlayShown) {
                    Log.d("ForegroundService", "Creating new window with currentUserId: " + currentUserId);
                    Window window = new Window(getApplicationContext(), false, currentUserId);
                    window.open(timer, ForegroundService.this);
                    windowsByTimerId.put(timer.getId(), window);
                }
            }

            @Override
            public void onTimerRemoved(String timerId) {
                Log.i("ForegroundService", "Received timer removal from WebSocket: " + timerId);
                
                // Clean up the specific timer's window
                Window window = windowsByTimerId.get(timerId);
                Log.i("ForegroundService", "Found window for timerId " + timerId + ": " + (window != null ? "yes" : "no"));
                if (window != null) {
                    Log.i("ForegroundService", "Closing window for timerId: " + timerId);
                    window.close();
                    windowsByTimerId.remove(timerId);
                    Log.i("ForegroundService", "Removed window from map for timerId: " + timerId);
                }
                
                // Also close expanded window if it's for this timer
                if (expandedWindow != null && expandedWindow.isOpen()) {
                    Timer expandedTimer = expandedWindow.getTimerView().getTimer();
                    if (expandedTimer != null && expandedTimer.getId().equals(timerId)) {
                        Log.i("ForegroundService", "Closing expanded window for timerId: " + timerId);
                        expandedWindow.close();
                    }
                }
                
                Log.i("ForegroundService", "Completed timer removal handling for timerId: " + timerId);
            }
        });

        this.activeTimers = new ArrayList<>();
        this.windowsByTimerId = new HashMap<>();

        this.activeTimerRepository.getAllActiveTimers().observe(this, timers -> {
            Log.d("ForegroundService", "Repository observer triggered with " + (timers != null ? timers.size() : 0) + " timers");
            Log.i("ForegroundService", "Repository observer - timers: " + (timers != null ? timers.stream().map(t -> t.getId() + ":" + t.getSharedWith()).collect(java.util.stream.Collectors.joining(", ")) : "null"));
            
            // Update shared timer tracking
            updateSharedTimerTracking(timers);
            
            DiffUtil.calculateDiff(
                    new DiffUtil.Callback() {
                        @Override
                        public int getOldListSize() {
                            return activeTimers.size();
                        }

                        @Override
                        public int getNewListSize() {
                            return timers.size();
                        }

                        @Override
                        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                            return activeTimers.get(oldItemPosition).getId().equals(timers.get(newItemPosition).getId());
                        }

                        @Override
                        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                            return activeTimers.get(oldItemPosition).equals(timers.get(newItemPosition));
                        }
                    }
            ).dispatchUpdatesTo(new ListUpdateCallback() {
                @Override
                public void onInserted(int position, int count) {
                    timers.subList(position, position + count).forEach(timer -> {
                        Log.d("ForegroundService", "Creating new window (onInserted) with currentUserId: " + currentUserId);
                        Window window = new Window(getApplicationContext(), false, currentUserId);

                        if (isOverlayShown) {
                            window.open(timer, ForegroundService.this);
                        }

                        windowsByTimerId.put(timer.getId(), window);
                    });
                }

                @Override
                public void onRemoved(int position, int count) {
                    // stop vibrator every time - if the timer is still alarming it will start again
                    if (vibrator != null) {
                        vibrator.cancel();
                        vibrator = null;
                    }

                    activeTimers.subList(position, position + count).forEach(timer -> {
                        Window window = windowsByTimerId.get(timer.getId());
                        if (window != null) {
                            try {
                                window.close();
                            } catch (Exception e) {
                                Log.e("ForegroundService", "Error while closing: ", e);
                            }
                            windowsByTimerId.remove(timer.getId());
                        }

                        websocketManager.sendStopTimerToWebsocket(timer.getId(), timer.getSharedWith());
                    });
                }

                @Override
                public void onMoved(int fromPosition, int toPosition) {

                }

                @Override
                public void onChanged(int position, int count, @Nullable Object payload) {
                    activeTimers.subList(position, position + count).forEach(timer -> {
                    });
                }
            });
            this.activeTimers = List.copyOf(timers);
        });

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);

        String channelName = "Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Bubble Timer's service for managing timer bubbles");
        channel.setVibrationPattern(null);
        channel.setSound(null, null);
        channel.enableLights(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentTitle("Bubble Timer")
                .setContentText("Timers active")
                .setSmallIcon(R.drawable.bubble_logo)
                .setContentIntent(contentPendingIntent);

        this.startForeground(2, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, new IntentFilter(ForegroundService.MESSAGE_RECEIVER_ACTION));

        this.vibratorManager = (VibratorManager) getApplicationContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);

        Context context = getApplicationContext();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "bubbletimer::WakeLock");

        this.updater = () -> {
            windowsByTimerId.forEach((timerId, window) -> {
                window.invalidate();
            });

            if (this.expandedWindow != null) {
                this.expandedWindow.invalidate();
            }
            
            // Check WebSocket connection status and attempt reconnection if needed
            // Only reconnect if there are shared timers (on-demand mode)
            if (websocketManager != null && websocketManager.getConnectionState() == WebsocketManager.ConnectionState.DISCONNECTED) {
                if (!sharedTimerIds.isEmpty()) {
                    Log.i("ForegroundService", "WebSocket is disconnected but shared timers exist, attempting to reconnect");
                    websocketManager.forceReconnect();
                } else {
                    Log.d("ForegroundService", "WebSocket is disconnected but no shared timers - staying disconnected (on-demand mode)");
                }
            }

            boolean shouldAlarm = false;
            if (!activeTimers.isEmpty()) {
                String name = activeTimers.get(0).getName();
                Duration remainingDuration = activeTimers.get(0).getRemainingDuration();
                String text = "[" + name + "] " + DurationUtil.getFormattedDuration(remainingDuration);

                notificationBuilder.setContentText(text);
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

                for (Timer timer : activeTimers) {
                    if (timer.getRemainingDuration().isNegative() || timer.getRemainingDuration().isZero()) {
                        shouldAlarm = true;
                        break;
                    }
                }
            }

            if (shouldAlarm && vibrator == null) {
                long[] timing   = {600L, 400L};
                int[] amplitude = { 250,    0};
                vibratorManager.vibrate(
                        CombinedVibration.createParallel(
                                VibrationEffect.createWaveform(timing, amplitude, 0)
                        )
                );
                vibrator = vibratorManager.getDefaultVibrator();


                if (wakeLock != null) {
                    //acquire will turn on the display
                    wakeLock.acquire();

                    //release will release the lock from CPU, in case of that, screen will go back to sleep mode in defined time bt device settings
                    wakeLock.release();
                }
            }
            timerHandler.postDelayed(updater, 1000);
        };
        timerHandler.post(updater);

        // Request auth data to initialize websocket
        Intent message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "sendAuthToken");
        LocalBroadcastManager.getInstance(ForegroundService.this).sendBroadcast(message);
    }

    /**
     * Update shared timer tracking based on current timers.
     * This replaces the SharedTimerManager functionality with a simpler approach.
     */
    private void updateSharedTimerTracking(List<Timer> timers) {
        Log.i("ForegroundService", "updateSharedTimerTracking called with " + (timers != null ? timers.size() : 0) + " timers");
        
        Set<String> newSharedTimerIds = new HashSet<>();
        Set<String> newPendingInvitationIds = new HashSet<>();
        Set<String> newActiveSharedTimerIds = new HashSet<>();
        
        if (timers != null) {
            for (Timer timer : timers) {
                Set<String> sharedWith = timer.getSharedWith();
                Log.d("ForegroundService", "Checking timer " + timer.getId() + " - sharedWith: " + 
                      (sharedWith != null ? sharedWith.size() : "null") + 
                      " - sharedWith contents: " + (sharedWith != null ? sharedWith.toString() : "null"));
                
                // A timer is considered "shared" if it has non-empty sharedWith set
                // This means either:
                // 1. The current user shared this timer with other users
                // 2. Another user shared this timer with the current user
                if (sharedWith != null && !sharedWith.isEmpty()) {
                    newSharedTimerIds.add(timer.getId());
                    Log.i("ForegroundService", "Found shared timer: " + timer.getId() + " with users: " + sharedWith);
                    
                    // For now, consider all shared timers as active
                    // In a real implementation, you might track pending vs active more precisely
                    if (activeSharedTimerIds.contains(timer.getId())) {
                        newActiveSharedTimerIds.add(timer.getId());
                    } else {
                        newPendingInvitationIds.add(timer.getId());
                    }
                } else {
                    Log.d("ForegroundService", "Timer " + timer.getId() + " is not shared (sharedWith is null or empty)");
                }
            }
        }
        
        Log.i("ForegroundService", "Found " + newSharedTimerIds.size() + " shared timers out of " + (timers != null ? timers.size() : 0) + " total timers");
        
        // Update state
        sharedTimerIds.clear();
        sharedTimerIds.addAll(newSharedTimerIds);
        
        pendingInvitationIds.clear();
        pendingInvitationIds.addAll(newPendingInvitationIds);
        
        activeSharedTimerIds.clear();
        activeSharedTimerIds.addAll(newActiveSharedTimerIds);
        
        Log.i("ForegroundService", "Updated shared timer state: " + newSharedTimerIds.size() + " shared timers");
        Log.i("ForegroundService", "Shared timer IDs: " + newSharedTimerIds);
        Log.i("ForegroundService", "Pending invitations: " + newPendingInvitationIds);
        Log.i("ForegroundService", "Active shared timers: " + newActiveSharedTimerIds);
        
        // Check connection needs after state update
        checkWebsocketConnectionNeeds();
    }
    
    /**
     * Check if we need to establish or terminate WebSocket connection based on shared timers.
     * Only connect when there are active shared timers (timers with non-empty sharedWith sets).
     */
    private void checkWebsocketConnectionNeeds() {
        boolean hasSharedTimers = !sharedTimerIds.isEmpty();
        WebsocketManager.ConnectionState currentState = websocketManager.getConnectionState();
        
        Log.i("ForegroundService", "checkWebsocketConnectionNeeds - hasSharedTimers: " + hasSharedTimers + 
              ", current WebSocket state: " + currentState +
              ", sharedTimerIds.size(): " + sharedTimerIds.size() +
              ", sharedTimerIds: " + sharedTimerIds);
        
        if (hasSharedTimers) {
            if (currentState == WebsocketManager.ConnectionState.DISCONNECTED) {
                Log.i("ForegroundService", "Shared timers detected, connecting to WebSocket");
                websocketManager.connectIfNeeded();
            } else {
                Log.d("ForegroundService", "Shared timers detected but WebSocket already connected");
            }
        } else {
            if (currentState == WebsocketManager.ConnectionState.CONNECTED) {
                Log.i("ForegroundService", "No shared timers, disconnecting from WebSocket");
                websocketManager.close();
            } else {
                Log.d("ForegroundService", "No shared timers and WebSocket already disconnected");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (this.expandedWindow == null) {
                Log.d("ForegroundService", "Creating expanded window with currentUserId: " + currentUserId);
                this.expandedWindow = new Window(getApplicationContext(), true, currentUserId);
            }
        }

        timerHandler.post(updater);

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onTimerUpdated(Timer timer) {
        Log.i("ForegroundService", "onTimerUpdated called for timer: " + timer.getId() + 
              " - sharedWith: " + (timer.getSharedWith() != null ? timer.getSharedWith().toString() : "null"));
        
        if (timer.getUserId() == null) {
            Log.i("ForegroundService", "userId from view is null");
        } else {
            Log.i("ForegroundService", "userId from view is NOT null: " + timer.getUserId());
        }
        this.activeTimerRepository.update(timer);

        // hack to ensure userid is set?
        Timer timerFromRepository = this.activeTimerRepository.getById(timer.getId());
        if (timerFromRepository.getUserId() == null) {
            Log.i("ForegroundService", "userId from repo is null");
        } else {
            Log.i("ForegroundService", "userId from repo is NOT null: " + timerFromRepository.getUserId());
        }
        Log.i("ForegroundService", "Timer from repository - sharedWith: " + 
              (timerFromRepository.getSharedWith() != null ? timerFromRepository.getSharedWith().toString() : "null"));
        
        // Update shared timer tracking to ensure WebSocket connection is established
        // This is important when a timer becomes shared for the first time
        Log.d("ForegroundService", "activeTimers is null: " + (activeTimers == null));
        if (activeTimers != null) {
            Log.d("ForegroundService", "activeTimers size: " + activeTimers.size());
            Log.d("ForegroundService", "Updating shared timer tracking before sending WebSocket message");
            updateSharedTimerTracking(activeTimers);
        } else {
            Log.d("ForegroundService", "activeTimers is null, skipping shared timer tracking update");
        }
        
        // Check if WebSocket is connected before sending the message
        WebsocketManager.ConnectionState connectionState = websocketManager.getConnectionState();
        Log.d("ForegroundService", "Sending update timer to WebSocket - timer: " + timerFromRepository.getId() + 
              ", sharedWith: " + (timerFromRepository.getSharedWith() != null ? timerFromRepository.getSharedWith().toString() : "null") +
              ", WebSocket state: " + connectionState);
        
        if (connectionState == WebsocketManager.ConnectionState.CONNECTED) {
            this.websocketManager.sendUpdateTimerToWebsocket(timerFromRepository, "who knows");
            Log.d("ForegroundService", "WebSocket message sent successfully");
        } else {
            Log.d("ForegroundService", "WebSocket not connected (state: " + connectionState + "), will send message when connected");
            // Store the timer to send when WebSocket connects
            pendingTimerUpdate = timerFromRepository;
        }
    }

    @Override
    public void onTimerStopped(Timer timer) {
        this.activeTimerRepository.deleteById(timer.getId());
        this.websocketManager.sendStopTimerToWebsocket(timer.getId(), timer.getSharedWith());

        notificationBuilder.setContentText("No active timers.");
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public void onBubbleDismiss(Timer timer) {
        // Only dismiss the bubble, do not stop the timer
        Window window = windowsByTimerId.get(timer.getId());
        if (window != null) {
            window.close();
        }
        if (expandedWindow != null && expandedWindow.isOpen()) {
            expandedWindow.close();
        }
    }

    @Override
    public void onBubbleClick(Timer clickedTimer) {
        Window window = windowsByTimerId.get(clickedTimer.getId());
        if (window != null) {
            Timer timer = this.activeTimerRepository.getById(clickedTimer.getId());
            if (window.isOpen()) {
                window.close();
                expandedWindow.open(timer, this);
            } else {
                expandedWindow.close();
                window.open(timer, this);
            }
        } else if (expandedWindow != null && expandedWindow.isOpen()) {
            expandedWindow.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        windowsByTimerId.forEach((timerId, window) -> {
            window.close();
        });

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        websocketManager.close();
        
        // Clean up shared timer tracking
        sharedTimerIds.clear();
        pendingInvitationIds.clear();
        activeSharedTimerIds.clear();
    }
}
