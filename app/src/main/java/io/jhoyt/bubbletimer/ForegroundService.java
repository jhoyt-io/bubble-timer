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

import io.jhoyt.bubbletimer.overlay.OverlayWindowFactory;

@AndroidEntryPoint
public class ForegroundService extends LifecycleService implements OverlayWindowFactory.BubbleEventListener {
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
    
    // Counter to reduce notification update frequency (update every 1000ms instead of 100ms)
    private int updateCounter = 0;

    @Inject
    WebsocketManager websocketManager;

    @Inject
    ActiveTimerRepository activeTimerRepository;
    private List<Timer> activeTimers;
    private Map<String, OverlayWindowFactory.IOverlayWindow> windowsByTimerId;

    private OverlayWindowFactory.IOverlayWindow expandedWindow;

    // Simplified shared timer tracking - integrated directly into ForegroundService
    private final Set<String> sharedTimerIds = new HashSet<>();
    private final Set<String> pendingInvitationIds = new HashSet<>();
    private final Set<String> activeSharedTimerIds = new HashSet<>();
    private final Set<String> triggeredAlarmTimerIds = new HashSet<>(); // Track timers that have already triggered alarms

    private Boolean isOverlayShown = false;
    private boolean isDebugModeEnabled = false;  // Track if debug mode is enabled
    private long lastAuthTokenRequest = 0;
    private static final long AUTH_TOKEN_DEBOUNCE_MS = 1000; // Reduced from 5000ms to 1000ms for better testing
    private static final boolean BYPASS_THROTTLING_FOR_TESTING = true; // Set to false in production
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
                } else if ("acceptTimer".equals(callback)) {
                    Log.i("ForegroundService", "Received auth token for acceptTimer callback");
                    // Send the auth token directly to the NotificationActionReceiver
                    // Use a different action to avoid the infinite loop
                    Intent responseMessage = new Intent("notification-action-auth-response");
                    responseMessage.putExtra("authToken", authToken);
                    responseMessage.putExtra("callback", "acceptTimer");
                    responseMessage.putExtra("timerId", intent.getStringExtra("timerId"));
                    responseMessage.putExtra("timerName", intent.getStringExtra("timerName"));
                    responseMessage.putExtra("sharerName", intent.getStringExtra("sharerName"));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(responseMessage);
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
            } else if (command.equals("timerUpdated")) {
                String timerId = intent.getStringExtra("timerId");
                Log.i("ForegroundService", "timerUpdated command received for timer: " + timerId);
                
                if (timerId != null) {
                    // Get the updated timer from repository and trigger WebSocket update
                    Timer updatedTimer = activeTimerRepository.getById(timerId);
                    if (updatedTimer != null) {
                        Log.i("ForegroundService", "Triggering WebSocket update for timer: " + timerId + 
                              " - sharedWith: " + (updatedTimer.getSharedWith() != null ? updatedTimer.getSharedWith().toString() : "null"));
                        onTimerUpdated(updatedTimer);
                    } else {
                        Log.w("ForegroundService", "Timer not found in repository: " + timerId);
                    }
                } else {
                    Log.w("ForegroundService", "timerUpdated command received but timerId is null");
                }
            } else if (command.equals("stopTimer")) {
                String timerId = intent.getStringExtra("timerId");
                Log.i("ForegroundService", "stopTimer command received for timer: " + timerId);
                
                if (timerId != null) {
                    Timer timerToStop = activeTimerRepository.getById(timerId);
                    if (timerToStop != null) {
                        Log.i("ForegroundService", "Stopping timer: " + timerId);
                        onTimerStopped(timerToStop);
                    } else {
                        Log.w("ForegroundService", "Timer to stop not found in repository: " + timerId);
                    }
                } else {
                    Log.w("ForegroundService", "stopTimer command received but timerId is null");
                }
            } else if (command.equals("testAlarmActivity")) {
                Log.i("ForegroundService", "testAlarmActivity command received - launching test alarm");
                
                // Create a test timer for debugging
                Timer testTimer = new Timer("test_user", "Test Timer", Duration.ofMinutes(1), new HashSet<>());

                // Log system state and attempt launch
                logSystemState();
                launchTimerAlarmActivity(testTimer);
            } else if (command.equals("alarmDismissed")) {
                String timerId = intent.getStringExtra("timerId");
                Log.i("ForegroundService", "alarmDismissed command received for timer: " + timerId);
                
                // Release wake lock if we're holding one for this alarm
                if (wakeLock != null && wakeLock.isHeld()) {
                    Log.i("ForegroundService", "⚡ WAKE LOCK: Releasing after alarm dismissed");
                    wakeLock.release();
                }
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
                if (!BYPASS_THROTTLING_FOR_TESTING && currentTime - lastAuthTokenRequest < AUTH_TOKEN_DEBOUNCE_MS) {
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
                    OverlayWindowFactory.IOverlayWindow window = windowsByTimerId.get(timer.getId());
                    if (window.isOpen()) {
                        // Update the timer in the window
                        window.getTimerView().setTimer(timer);
                        // Note: invalidate() is not in the interface, but the underlying implementations handle updates
                    }
                }
                
                // If this is a new timer and overlay is shown, create a new window for it
                if (!windowsByTimerId.containsKey(timer.getId()) && isOverlayShown) {
                    Log.d("ForegroundService", "Creating new window with currentUserId: " + currentUserId);
                    OverlayWindowFactory.IOverlayWindow window = OverlayWindowFactory.createOverlayWindow(
                        getApplicationContext(), false, currentUserId);
                    window.open(timer, ForegroundService.this);
                    windowsByTimerId.put(timer.getId(), window);
                }
            }

            @Override
            public void onTimerRemoved(String timerId) {
                Log.i("ForegroundService", "Received timer removal from WebSocket: " + timerId);
                
                // IMPORTANT: Dismiss any active alarm activity for this timer across all devices
                dismissActiveAlarmActivity(timerId);
                
                // Clean up the specific timer's window
                OverlayWindowFactory.IOverlayWindow window = windowsByTimerId.get(timerId);
                Log.i("ForegroundService", "Found window for timerId " + timerId + ": " + (window != null ? "yes" : "no"));
                if (window != null) {
                    Log.i("ForegroundService", "Closing window for timerId: " + timerId);
                    window.close();
                    window.cleanup();
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
                        OverlayWindowFactory.IOverlayWindow window = OverlayWindowFactory.createOverlayWindow(
                            getApplicationContext(), false, currentUserId);

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
                        OverlayWindowFactory.IOverlayWindow window = windowsByTimerId.get(timer.getId());
                        if (window != null) {
                            try {
                                window.close();
                                window.cleanup();
                            } catch (Exception e) {
                                Log.e("ForegroundService", "Error while closing: ", e);
                            }
                            windowsByTimerId.remove(timer.getId());
                        }

                        // Clean up alarm tracking for removed timer
                        triggeredAlarmTimerIds.remove(timer.getId());

                        websocketManager.sendStopTimerToWebsocket(timer);
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
            updateCounter++;

            // Update UI and notifications every 1000ms (every 10 cycles at 100ms intervals)
            boolean shouldUpdateUI = (updateCounter % 10) == 0;
            
            if (shouldUpdateUI) {
                // Periodically invalidate overlay timer views to update countdown arc/text
                if (windowsByTimerId != null) {
                    windowsByTimerId.forEach((timerId, window) -> {
                        try {
                            TimerView tv = window.getTimerView();
                            if (tv != null) tv.invalidate();
                        } catch (Exception ignored) { }
                    });
                }
                if (this.expandedWindow != null) {
                    try {
                        TimerView tv = this.expandedWindow.getTimerView();
                        if (tv != null) tv.invalidate();
                    } catch (Exception ignored) { }
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
            }

            // CRITICAL: Always check for expired timers every 100ms for immediate alarm response
            boolean shouldAlarm = false;
            Timer expiredTimer = null;
            if (!activeTimers.isEmpty()) {
                // Update notification every 1000ms only
                if (shouldUpdateUI) {
                    String name = activeTimers.get(0).getName();
                    Duration remainingDuration = activeTimers.get(0).getRemainingDuration();
                    String text = "[" + name + "] " + DurationUtil.getFormattedDuration(remainingDuration);

                    notificationBuilder.setContentText(text);
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                }

                // But check for expiration every cycle (100ms) for immediate alarm response
                for (Timer timer : activeTimers) {
                    if (timer.getRemainingDuration().isNegative() || timer.getRemainingDuration().isZero()) {
                        shouldAlarm = true;
                        expiredTimer = timer;
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

                // Launch full-screen alarm activity for the expired timer
                if (expiredTimer != null && !triggeredAlarmTimerIds.contains(expiredTimer.getId())) {
                    Log.i("ForegroundService", "Timer expired: " + expiredTimer.getId() + 
                          " - currentUserId: " + currentUserId + 
                          " - timer.getUserId(): " + expiredTimer.getUserId() +
                          " - isShared: " + (expiredTimer.getSharedWith() != null && !expiredTimer.getSharedWith().isEmpty()));
                    
                    // CRITICAL: Acquire wake lock IMMEDIATELY when timer expires to wake screen
                    if (wakeLock != null) {
                        Log.i("ForegroundService", "⚡ WAKE LOCK: Acquiring to wake screen for expired timer");
                        wakeLock.acquire(30000); // Hold for 30 seconds max (alarm activity will manage its own wake)
                    }
                    
                    // Check full-screen intent permission status
                    checkFullScreenIntentPermission();
                    
                    // Determine if current user should get full-screen alarm
                    boolean shouldShowFullScreenAlarm = shouldShowFullScreenAlarmForUser(expiredTimer);
                    boolean hasFullScreenPermission = FullScreenIntentPermissionHelper.hasFullScreenIntentPermission(this);
                    
                    if (shouldShowFullScreenAlarm && hasFullScreenPermission) {
                        Log.i("ForegroundService", "Showing full-screen alarm for timer: " + expiredTimer.getId());
                        launchTimerAlarmActivity(expiredTimer);
                    } else {
                        String reason = !hasFullScreenPermission ? "missing full-screen permission" : "unknown";
                        Log.i("ForegroundService", "Showing enhanced notification (" + reason + ") for timer: " + expiredTimer.getId());
                        showEnhancedAlarmNotification(expiredTimer);
                    }
                    
                    triggeredAlarmTimerIds.add(expiredTimer.getId());
                }
            }
            timerHandler.postDelayed(updater, 100); // Check every 100ms for more responsive alarm triggering
        };
        timerHandler.post(updater);

        // Request auth data to initialize websocket
        Intent message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "sendAuthToken");
        LocalBroadcastManager.getInstance(ForegroundService.this).sendBroadcast(message);
        }
    
    /**
     * Check and log the status of full-screen intent permission and system settings.
     */
    private void checkFullScreenIntentPermission() {
        boolean hasPermission = FullScreenIntentPermissionHelper.hasFullScreenIntentPermission(this);
        Log.i("ForegroundService", "Full-screen intent permission status: " + hasPermission);
        
        if (!hasPermission) {
            Log.w("ForegroundService", "Full screen intent not allowed! User needs to grant permission in system settings.");
            Log.w("ForegroundService", "Timers will show enhanced notifications instead of full-screen alarms.");
        }
        
        // Check if Do Not Disturb might be interfering
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.NotificationManager.Policy policy = notificationManager.getNotificationPolicy();
                Log.d("ForegroundService", "Do Not Disturb policy: " + (policy != null ? policy.toString() : "null"));
            }
        } catch (Exception e) {
            Log.d("ForegroundService", "Could not check Do Not Disturb policy", e);
        }
    }
    
    /**
     * Determine if the current user should receive a full-screen alarm for the expired timer.
     * Full-screen alarms are shown to ALL users for both shared and personal timers.
     * This ensures everyone can easily stop the vibration with the prominent stop button.
     */
    private boolean shouldShowFullScreenAlarmForUser(Timer expiredTimer) {
        if (currentUserId == null) {
            Log.w("ForegroundService", "currentUserId is null, defaulting to full-screen alarm");
            return true; // Default to full-screen if we can't determine user
        }
        
        // Show full-screen alarm to ALL users (both personal and shared timers)
        Set<String> sharedWith = expiredTimer.getSharedWith();
        if (sharedWith == null || sharedWith.isEmpty()) {
            Log.d("ForegroundService", "Personal timer - showing full-screen alarm");
            return true;
        } else {
            Log.d("ForegroundService", "Shared timer - showing full-screen alarm to all participants for easy stop access");
            return true;
        }
    }
    
    /**
     * Log detailed system state for debugging full-screen intent issues
     */
    private void logSystemState() {
        try {
            Log.i("ForegroundService", "=== SYSTEM STATE DEBUG ===");
            
            // Device info
            Log.i("ForegroundService", "Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
            Log.i("ForegroundService", "API Level: " + android.os.Build.VERSION.SDK_INT);
            
            // Power management
            android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                Log.i("ForegroundService", "Is Screen On: " + powerManager.isInteractive());
                Log.i("ForegroundService", "Is Device Idle: " + powerManager.isDeviceIdleMode());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Log.i("ForegroundService", "Is Doze Mode: " + powerManager.isDeviceIdleMode());
                    Log.i("ForegroundService", "Is Power Save Mode: " + powerManager.isPowerSaveMode());
                }
            }
            
            // Keyguard state
            android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                Log.i("ForegroundService", "Is Keyguard Locked: " + keyguardManager.isKeyguardLocked());
                Log.i("ForegroundService", "Is Device Secure: " + keyguardManager.isDeviceSecure());
            }
            
            // Notification settings
            if (notificationManager != null) {
                Log.i("ForegroundService", "Notifications Enabled: " + notificationManager.areNotificationsEnabled());
                
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    Log.i("ForegroundService", "Can Use Full Screen Intent: " + notificationManager.canUseFullScreenIntent());
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.app.NotificationManager.Policy policy = notificationManager.getNotificationPolicy();
                    if (policy != null) {
                        Log.i("ForegroundService", "DND Policy Priority Categories: " + policy.priorityCategories);
                        Log.i("ForegroundService", "DND Policy Suppressed Visual Effects: " + policy.suppressedVisualEffects);
                    }
                    
                    int filter = notificationManager.getCurrentInterruptionFilter();
                    String filterName = getInterruptionFilterName(filter);
                    Log.i("ForegroundService", "Current Interruption Filter: " + filterName + " (" + filter + ")");
                }
            }
            
            // Activity Manager - check if we can start activities
            android.app.ActivityManager activityManager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Check background activity starts
                Log.i("ForegroundService", "Activity Manager available for background start checks");
            }
            
            Log.i("ForegroundService", "=== END SYSTEM STATE DEBUG ===");
            
        } catch (Exception e) {
            Log.e("ForegroundService", "Error logging system state", e);
        }
    }
    
    private String getInterruptionFilterName(int filter) {
        switch (filter) {
            case android.app.NotificationManager.INTERRUPTION_FILTER_NONE:
                return "NONE (Do Not Disturb - No notifications)";
            case android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                return "PRIORITY (Do Not Disturb - Priority only)";
            case android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS:
                return "ALARMS (Do Not Disturb - Alarms only)";
            case android.app.NotificationManager.INTERRUPTION_FILTER_ALL:
                return "ALL (Normal)";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * Launch the TimerAlarmActivity to show a full-screen alarm for the expired timer.
     * Uses a hybrid approach: direct activity launch + full-screen intent notification as backup.
     */
    private void launchTimerAlarmActivity(Timer expiredTimer) {
        try {
            Log.i("ForegroundService", "=== LAUNCHING FULL-SCREEN ALARM ===");
            Log.i("ForegroundService", "Timer ID: " + expiredTimer.getId());
            Log.i("ForegroundService", "Timer Name: " + expiredTimer.getName());
            Log.i("ForegroundService", "Android Version: " + android.os.Build.VERSION.SDK_INT);
            Log.i("ForegroundService", "Android Release: " + android.os.Build.VERSION.RELEASE);
            
            // Create the intent for the alarm activity
            Intent alarmIntent = new Intent(this, TimerAlarmActivity.class);
            alarmIntent.putExtra(TimerAlarmActivity.EXTRA_TIMER_ID, expiredTimer.getId());
            alarmIntent.putExtra(TimerAlarmActivity.EXTRA_TIMER_NAME, expiredTimer.getName());
            alarmIntent.putExtra(TimerAlarmActivity.EXTRA_TIMER_TOTAL_DURATION, expiredTimer.getTotalDuration().toString());
            alarmIntent.putExtra(TimerAlarmActivity.EXTRA_TIMER_REMAINING_DURATION, expiredTimer.getRemainingDuration().toString());
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                               Intent.FLAG_ACTIVITY_CLEAR_TOP |
                               Intent.FLAG_ACTIVITY_SINGLE_TOP |
                               Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            
            Log.i("ForegroundService", "Intent extras - Name: " + expiredTimer.getName() + 
                  ", TotalDuration: " + expiredTimer.getTotalDuration() + 
                  ", RemainingDuration: " + expiredTimer.getRemainingDuration());
            
            // Log detailed system state
            logSystemState();
            
            // Try direct activity launch first (works better for immediate display)
            boolean directLaunchSuccess = false;
            try {
                Log.i("ForegroundService", "Attempting direct activity launch...");
                startActivity(alarmIntent);
                directLaunchSuccess = true;
                Log.i("ForegroundService", "✓ Direct activity launch successful for timer: " + expiredTimer.getId());
            } catch (Exception directLaunchException) {
                Log.w("ForegroundService", "✗ Direct activity launch failed, creating notification as fallback", directLaunchException);
            }
            
            // Only create notification if direct launch failed
            if (!directLaunchSuccess) {
                Log.i("ForegroundService", "Creating full-screen intent notification as fallback");
                createFullScreenIntentNotification(expiredTimer, alarmIntent, directLaunchSuccess);
            } else {
                Log.i("ForegroundService", "Skipping notification - direct activity launch successful");
            }
            
        } catch (Exception e) {
            Log.e("ForegroundService", "Failed to launch TimerAlarmActivity", e);
            // Fallback: Enhanced notification if everything fails
            showEnhancedAlarmNotification(expiredTimer);
        }
    }
    
    /**
     * Create a full-screen intent notification for the alarm.
     */
    private void createFullScreenIntentNotification(Timer expiredTimer, Intent alarmIntent, boolean directLaunchWorked) {
        try {
            Log.i("ForegroundService", "=== CREATING FULL-SCREEN INTENT NOTIFICATION ===");
            
            // Create pending intent for the full-screen alarm
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this, 
                expiredTimer.getId().hashCode(), 
                alarmIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Log.i("ForegroundService", "Created PendingIntent with hash: " + expiredTimer.getId().hashCode());
            
            // Create a high-priority notification channel for alarms
            String alarmChannelId = "timer_fullscreen_alarm_channel";
            NotificationChannel alarmChannel = new NotificationChannel(
                alarmChannelId, 
                "Timer Full-Screen Alarms", 
                NotificationManager.IMPORTANCE_HIGH
            );
            alarmChannel.setDescription("Full-screen notifications for timer alarms");
            alarmChannel.enableVibration(false); // We handle vibration separately
            alarmChannel.enableLights(true);
            alarmChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            alarmChannel.setBypassDnd(true); // Bypass Do Not Disturb
            notificationManager.createNotificationChannel(alarmChannel);
            Log.i("ForegroundService", "Created notification channel: " + alarmChannelId);
            
            String contentText = directLaunchWorked ? 
                "Tap if alarm activity didn't appear" : 
                "Tap to open timer alarm";
            
            // Build notification with full-screen intent
            NotificationCompat.Builder fullScreenBuilder = new NotificationCompat.Builder(this, alarmChannelId)
                .setContentTitle("⏰ Timer Expired!")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.bubble_logo)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Use MAX priority for alarms
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false) // Don't auto-cancel - let user dismiss
                .setOngoing(false)
                .setFullScreenIntent(fullScreenPendingIntent, true) // This should launch the activity
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(fullScreenPendingIntent); // Also set as content intent for manual tap
            
            Notification notification = fullScreenBuilder.build();
            Log.i("ForegroundService", "Built notification - flags: " + notification.flags);
            Log.i("ForegroundService", "Full screen intent set: " + (notification.fullScreenIntent != null));
            Log.i("ForegroundService", "Content intent set: " + (notification.contentIntent != null));
            
            // Show the notification with full-screen intent
            int alarmNotificationId = 2000 + expiredTimer.getId().hashCode();
            Log.i("ForegroundService", "Posting notification with ID: " + alarmNotificationId);
            
            notificationManager.notify(alarmNotificationId, notification);
            
            Log.i("ForegroundService", "✓ Full-screen intent notification posted for timer: " + expiredTimer.getId());
            
            // Check if notification was actually posted
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.service.notification.StatusBarNotification[] activeNotifications = 
                    notificationManager.getActiveNotifications();
                boolean found = false;
                for (android.service.notification.StatusBarNotification sbn : activeNotifications) {
                    if (sbn.getId() == alarmNotificationId) {
                        found = true;
                        Log.i("ForegroundService", "✓ Notification confirmed active in system");
                        break;
                    }
                }
                if (!found) {
                    Log.w("ForegroundService", "✗ Notification not found in active notifications!");
                }
            }
            
        } catch (Exception e) {
            Log.e("ForegroundService", "Failed to create full-screen intent notification", e);
        }
    }
    
    /**
     * Dismiss any active alarm activity for the specified timer.
     * This is called when a timer is stopped via WebSocket to ensure 
     * alarm activities are dismissed on all devices.
     */
    private void dismissActiveAlarmActivity(String timerId) {
        Log.i("ForegroundService", "Dismissing active alarm activity for timer: " + timerId);
        
        // Send broadcast to dismiss the alarm activity if it's currently running
        Intent dismissIntent = new Intent(TimerAlarmActivity.DISMISS_ALARM_ACTION);
        dismissIntent.putExtra(TimerAlarmActivity.EXTRA_TIMER_ID, timerId);
        
        // Send as both local broadcast and system broadcast to ensure delivery
        LocalBroadcastManager.getInstance(this).sendBroadcast(dismissIntent);
        sendBroadcast(dismissIntent);
        
        Log.i("ForegroundService", "Sent dismiss alarm broadcast for timer: " + timerId);
    }
    
    /**
     * Show enhanced notification as fallback when full-screen alarm fails.
     */
    private void showEnhancedAlarmNotification(Timer expiredTimer) {
        Log.i("ForegroundService", "Showing enhanced notification for expired timer: " + expiredTimer.getId());
        
        // Create a more prominent notification for alarm
        String channelId = "timer_alarm_channel";
        NotificationChannel alarmChannel = new NotificationChannel(
            channelId, 
            "Timer Alarms", 
            NotificationManager.IMPORTANCE_HIGH
        );
        alarmChannel.setDescription("Notifications for expired timers");
        alarmChannel.enableVibration(true);
        alarmChannel.enableLights(true);
        alarmChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(alarmChannel);
        
        // Create stop timer action
        Intent stopIntent = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        stopIntent.putExtra("command", "stopTimer");
        stopIntent.putExtra("timerId", expiredTimer.getId());
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
            this, 
            expiredTimer.getId().hashCode(), 
            stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Enhanced notification is now only used when full-screen permission is missing
        String contentTitle = "⏰ Timer Expired!";
        String contentText = expiredTimer.getName() + " - Time's up! (Tap to view)";
        
        NotificationCompat.Builder alarmBuilder = new NotificationCompat.Builder(this, channelId)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.bubble_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(null, true)
            .addAction(R.drawable.bubble_logo, "STOP", stopPendingIntent);
        
        // Use unique notification ID for each timer alarm
        int notificationId = 1000 + expiredTimer.getId().hashCode();
        notificationManager.notify(notificationId, alarmBuilder.build());
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
                this.expandedWindow = OverlayWindowFactory.createOverlayWindow(
                    getApplicationContext(), true, currentUserId);
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
        this.websocketManager.sendStopTimerToWebsocket(timer);

        notificationBuilder.setContentText("No active timers.");
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public void onBubbleDismiss(Timer timer) {
        // Only dismiss the bubble, do not stop the timer
        OverlayWindowFactory.IOverlayWindow window = windowsByTimerId.get(timer.getId());
        if (window != null) {
            window.close();
        }
        if (expandedWindow != null && expandedWindow.isOpen()) {
            expandedWindow.close();
        }
    }

    @Override
    public void onBubbleClick(Timer clickedTimer) {
        OverlayWindowFactory.IOverlayWindow window = windowsByTimerId.get(clickedTimer.getId());
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

    /**
     * Getter for the broadcast receiver - used for testing
     */
    public BroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        windowsByTimerId.forEach((timerId, window) -> {
            window.close();
            window.cleanup();
        });

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        websocketManager.close();
        
        // Clean up shared timer tracking
        sharedTimerIds.clear();
        pendingInvitationIds.clear();
        activeSharedTimerIds.clear();
        triggeredAlarmTimerIds.clear();
    }
}
