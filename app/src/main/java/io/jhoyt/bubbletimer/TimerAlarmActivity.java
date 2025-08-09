package io.jhoyt.bubbletimer;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.time.Duration;

import dagger.hilt.android.AndroidEntryPoint;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;

import javax.inject.Inject;

@AndroidEntryPoint
public class TimerAlarmActivity extends AppCompatActivity {
    private static final String TAG = "TimerAlarmActivity";
    private static final long AUTO_DISMISS_TIMEOUT_MS = 30000; // 30 seconds
    
    public static final String EXTRA_TIMER_ID = "timer_id";
    public static final String EXTRA_TIMER_NAME = "timer_name";
    public static final String EXTRA_TIMER_TOTAL_DURATION = "timer_total_duration";
    public static final String EXTRA_TIMER_REMAINING_DURATION = "timer_remaining_duration";
    
    // Broadcast action for dismissing alarm activities across devices
    public static final String DISMISS_ALARM_ACTION = "io.jhoyt.bubbletimer.DISMISS_ALARM";
    
    @Inject
    ActiveTimerRepository activeTimerRepository;
    
    private TimerView timerView;
    private Button stopButton;
    private String timerId;
    private Timer timer;
    private Handler handler;
    private Runnable autoDismissRunnable;
    private Runnable timerUpdateRunnable;
    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver dismissReceiver;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(TAG, "=== TIMER ALARM ACTIVITY CREATED ===");
        Log.i(TAG, "Activity Task ID: " + getTaskId());
        Log.i(TAG, "Activity Instance: " + this.hashCode());
        Log.i(TAG, "Intent Action: " + getIntent().getAction());
        Log.i(TAG, "Intent Flags: " + Integer.toHexString(getIntent().getFlags()));
        
        // Configure full-screen activity to wake screen and bypass lock screen
        setupFullScreenFlags();
        
        // Acquire wake lock to keep screen on
        acquireWakeLock();
        
        setContentView(R.layout.activity_timer_alarm);
        
        Log.i(TAG, "✓ TimerAlarmActivity layout set and fully initialized");
        
        // Get timer ID from intent
        timerId = getIntent().getStringExtra(EXTRA_TIMER_ID);
        if (timerId == null) {
            Log.e(TAG, "No timer ID provided in intent");
            finish();
            return;
        }
        
        Log.i(TAG, "Timer alarm activity started for timer: " + timerId);
        
        // Initialize views
        timerView = findViewById(R.id.timer_view);
        stopButton = findViewById(R.id.stop_button);
        
        // Load timer data
        loadTimerData();
        
        // Set up stop button
        stopButton.setOnClickListener(v -> stopTimer());
        
        // Set up auto-dismiss timeout
        handler = new Handler();
        autoDismissRunnable = this::autoDismiss;
        handler.postDelayed(autoDismissRunnable, AUTO_DISMISS_TIMEOUT_MS);
        
        // Hide overlays while alarm activity is shown
        hideOverlays();
        
        // Set up broadcast receiver to listen for dismiss commands from other devices
        setupDismissReceiver();
    }
    
    private void setupFullScreenFlags() {
        Window window = getWindow();
        
        // These flags work for all API levels and ensure the activity shows over the lock screen
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // For API 27+ (Android 8.1+), use the newer methods as well
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        
        // DO NOT automatically dismiss keyguard - let user see the alarm over the lock screen
        // This allows the alarm to show without requiring password entry
        
        // Make it full screen but keep some navigation for accessibility
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        
        Log.d(TAG, "Full screen flags configured for lock screen display");
    }
    
    private void setupDismissReceiver() {
        dismissReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DISMISS_ALARM_ACTION.equals(intent.getAction())) {
                    String receivedTimerId = intent.getStringExtra(EXTRA_TIMER_ID);
                    Log.i(TAG, "Received dismiss alarm broadcast for timer: " + receivedTimerId);
                    
                    // Only dismiss if this activity is for the same timer
                    if (timerId != null && timerId.equals(receivedTimerId)) {
                        Log.i(TAG, "Dismissing alarm activity due to remote stop for timer: " + timerId);
                        remoteDismiss();
                    }
                }
            }
        };
        
        // Register for both local and system broadcasts
        IntentFilter filter = new IntentFilter(DISMISS_ALARM_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(dismissReceiver, filter);
        
        // For system broadcasts, we need RECEIVER_NOT_EXPORTED on API 34+ since this is an internal app broadcast
        if (android.os.Build.VERSION.SDK_INT >= 34) { // Android 14 (API 34)
            registerReceiver(dismissReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dismissReceiver, filter);
        }
        
        Log.d(TAG, "Dismiss broadcast receiver registered for timer: " + timerId);
    }
    
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | 
                PowerManager.ACQUIRE_CAUSES_WAKEUP | 
                PowerManager.ON_AFTER_RELEASE,
                "bubbletimer::AlarmWakeLock"
            );
            wakeLock.acquire(AUTO_DISMISS_TIMEOUT_MS + 5000); // Extra 5 seconds buffer
            Log.d(TAG, "Wake lock acquired");
        }
    }
    
    private void loadTimerData() {
        // Try to get timer from repository first
        timer = activeTimerRepository.getById(timerId);
        
        if (timer == null) {
            Log.w(TAG, "Timer not found in repository: " + timerId + ", creating from intent extras");
            
            // Timer might have been removed from repository after expiration
            // Create timer from intent extras
            String timerName = getIntent().getStringExtra(EXTRA_TIMER_NAME);
            String totalDurationStr = getIntent().getStringExtra(EXTRA_TIMER_TOTAL_DURATION);
            String remainingDurationStr = getIntent().getStringExtra(EXTRA_TIMER_REMAINING_DURATION);
            
            if (timerName == null || totalDurationStr == null) {
                Log.e(TAG, "Timer data not available in intent extras either. TimerName: " + timerName + ", TotalDuration: " + totalDurationStr);
                finish();
                return;
            }
            
            try {
                Duration totalDuration = Duration.parse(totalDurationStr);
                Duration remainingDuration = remainingDurationStr != null ? Duration.parse(remainingDurationStr) : Duration.ZERO;
                
                // Calculate timer end time based on remaining duration
                java.time.LocalDateTime timerEnd;
                Duration remainingDurationWhenPaused;
                
                if (remainingDuration.isNegative() || remainingDuration.isZero()) {
                    // Timer has expired - calculate when it expired
                    timerEnd = java.time.LocalDateTime.now().minus(remainingDuration.abs());
                    remainingDurationWhenPaused = null; // Running (expired)
                } else {
                    // Timer still has time (shouldn't happen in alarm context, but handle it)
                    timerEnd = java.time.LocalDateTime.now().plus(remainingDuration);
                    remainingDurationWhenPaused = null; // Running
                }
                
                // Create TimerData with the proper structure
                TimerData timerData = new TimerData(
                    timerId,                          // id
                    "alarm_user",                     // userId
                    timerName,                        // name
                    totalDuration,                    // totalDuration
                    remainingDurationWhenPaused,      // remainingDurationWhenPaused
                    timerEnd,                         // timerEnd
                    new java.util.HashSet<>()        // tags
                );
                
                // Create Timer with the TimerData
                timer = new Timer(timerData, new java.util.HashSet<>());
                
                Log.i(TAG, "Created timer from intent extras: " + timer.getName() + " (" + timer.getId() + ")");
                Log.i(TAG, "Timer end time: " + timerEnd + ", Remaining: " + timer.getRemainingDuration());
            } catch (Exception e) {
                Log.e(TAG, "Error creating timer from intent extras", e);
                finish();
                return;
            }
        } else {
            Log.i(TAG, "Loaded timer from repository: " + timer.getName() + " (" + timer.getId() + ")");
        }
        
        // Configure timer view for alarm display
        timerView.setTimer(timer);
        timerView.setCurrentUserId(timer.getUserId());
        timerView.setLayoutMode(TimerView.MODE_ALARM); // Use alarm mode for large display without buttons
        timerView.setExpandedMode(false); // No expanded mode needed in alarm
        
        // Force invalidation to draw the timer
        timerView.invalidate();
        
        Log.i(TAG, "Timer view configured and ready for display");
        
        // Start timer update loop to show continuing countdown
        startTimerUpdateLoop();
    }
    
    /**
     * Start the timer update loop to continuously refresh the timer display
     * This shows the continuing countdown/elapsed time since expiration
     */
    private void startTimerUpdateLoop() {
        if (handler == null || timerView == null) {
            Log.w(TAG, "Cannot start timer update loop - handler or timerView is null");
            return;
        }
        
        timerUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // Update the timer view to reflect current time
                    if (timerView != null) {
                        timerView.invalidate();
                    }
                    
                    // Schedule next update in 1 second
                    if (handler != null && timerUpdateRunnable != null) {
                        handler.postDelayed(timerUpdateRunnable, 1000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in timer update loop", e);
                }
            }
        };
        
        // Start the update loop
        handler.post(timerUpdateRunnable);
        Log.d(TAG, "Timer update loop started");
    }
    
    /**
     * Stop the timer update loop
     */
    private void stopTimerUpdateLoop() {
        if (handler != null && timerUpdateRunnable != null) {
            handler.removeCallbacks(timerUpdateRunnable);
            timerUpdateRunnable = null;
            Log.d(TAG, "Timer update loop stopped");
        }
    }
    
    private void stopTimer() {
        Log.i(TAG, "Stop button pressed for timer: " + timerId);
        
        // Stop the timer update loop
        stopTimerUpdateLoop();
        
        // Cancel the full-screen alarm notification
        dismissFullScreenAlarmNotification();
        
        // Show overlays again before stopping timer
        showOverlays();
        
        // Send stop command to ForegroundService
        Intent stopIntent = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        stopIntent.putExtra("command", "stopTimer");
        stopIntent.putExtra("timerId", timerId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(stopIntent);
        
        // Notify ForegroundService that alarm was dismissed (to release wake lock)
        Intent alarmDismissedIntent = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        alarmDismissedIntent.putExtra("command", "alarmDismissed");
        alarmDismissedIntent.putExtra("timerId", timerId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(alarmDismissedIntent);
        
        // Dismiss this activity
        finish();
    }
    
    private void hideOverlays() {
        Log.d(TAG, "Hiding overlays for alarm activity");
        Intent hideIntent = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        hideIntent.putExtra("command", "hideOverlay");
        LocalBroadcastManager.getInstance(this).sendBroadcast(hideIntent);
    }
    
    private void showOverlays() {
        Log.d(TAG, "Showing overlays after alarm activity");
        Intent showIntent = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        showIntent.putExtra("command", "showOverlay");
        LocalBroadcastManager.getInstance(this).sendBroadcast(showIntent);
    }
    
    private void dismissFullScreenAlarmNotification() {
        if (timerId != null) {
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                int alarmNotificationId = 2000 + timerId.hashCode();
                notificationManager.cancel(alarmNotificationId);
                Log.d(TAG, "Dismissed full-screen alarm notification for timer: " + timerId);
            }
        }
    }
    
    private void autoDismiss() {
        Log.i(TAG, "Auto-dismissing timer alarm activity due to timeout");
        
        // Stop the timer update loop
        stopTimerUpdateLoop();
        
        // Cancel the full-screen alarm notification
        dismissFullScreenAlarmNotification();
        
        // Show overlays again after auto-dismiss
        showOverlays();
        
        // Notify ForegroundService that alarm was dismissed (to release wake lock)
        Intent alarmDismissedIntent = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        alarmDismissedIntent.putExtra("command", "alarmDismissed");
        alarmDismissedIntent.putExtra("timerId", timerId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(alarmDismissedIntent);
        
        finish();
    }
    
    /**
     * Dismiss the alarm activity when stopped remotely by another device.
     * This is different from autoDismiss because we don't need to send stop commands
     * since the timer was already stopped by another device.
     */
    private void remoteDismiss() {
        Log.i(TAG, "Remote dismissing timer alarm activity due to timer stop from another device");
        
        // Stop the timer update loop
        stopTimerUpdateLoop();
        
        // Cancel the full-screen alarm notification
        dismissFullScreenAlarmNotification();
        
        // Show overlays again after remote dismiss
        showOverlays();
        
        // Notify ForegroundService that alarm was dismissed (to release wake lock)
        Intent alarmDismissedIntent = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        alarmDismissedIntent.putExtra("command", "alarmDismissed");
        alarmDismissedIntent.putExtra("timerId", timerId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(alarmDismissedIntent);
        
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop timer update loop
        stopTimerUpdateLoop();
        
        // Cancel auto-dismiss timeout
        if (handler != null && autoDismissRunnable != null) {
            handler.removeCallbacks(autoDismissRunnable);
        }
        
        // Unregister broadcast receiver
        if (dismissReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(dismissReceiver);
                unregisterReceiver(dismissReceiver);
                Log.d(TAG, "Dismiss broadcast receiver unregistered");
            } catch (IllegalArgumentException e) {
                // Receiver was not registered, ignore
                Log.d(TAG, "Broadcast receiver was not registered");
            }
        }
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "Wake lock released");
        }
        
        Log.i(TAG, "TimerAlarmActivity destroyed");
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from dismissing alarm - user must use stop button
        Log.d(TAG, "Back button pressed - ignored in alarm mode");
        // Don't call super.onBackPressed() to prevent dismissal
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "TimerAlarmActivity onStart()");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "TimerAlarmActivity onResume() - Activity is now visible to user");
        // Resume timer updates when visible again
        if (timerView != null) {
            startTimerUpdateLoop();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "TimerAlarmActivity onPause() - Activity losing focus");
        // Pause timer updates when not visible to save battery
        stopTimerUpdateLoop();
        // Keep activity visible even when other apps try to take focus
        // This is important for alarm functionality
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "TimerAlarmActivity onStop() - Activity no longer visible");
    }
}
