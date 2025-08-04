package io.jhoyt.bubbletimer.db;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.HashSet;
import java.time.Duration;
import java.util.Set;
import java.time.LocalDateTime;

import javax.inject.Inject;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.jhoyt.bubbletimer.ForegroundService;
import io.jhoyt.bubbletimer.TimerData;
import io.jhoyt.bubbletimer.db.AppDatabase;
import io.jhoyt.bubbletimer.db.ActiveTimer;

public class SharedTimerViewModel extends AndroidViewModel {
    private static final String TAG = "SharedTimerViewModel";
    private final SharedTimerRepository repository;
    private final MutableLiveData<String> authToken = new MutableLiveData<>();
    private ActiveTimerRepository activeTimerRepository;

    @Inject
    public SharedTimerViewModel(@NonNull Application application) {
        super(application);
        this.repository = new SharedTimerRepository(application);
        this.activeTimerRepository = new ActiveTimerRepository(application);
    }

    public LiveData<List<SharedTimer>> getSharedTimers() {
        return repository.getAllSharedTimers();
    }

    public LiveData<List<SharedTimer>> getPendingSharedTimers() {
        return repository.getPendingSharedTimers();
    }

    public LiveData<Boolean> getIsLoading() {
        return repository.getIsLoading();
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    public void setAuthToken(String token) {
        authToken.setValue(token);
        Log.d(TAG, "Auth token set");
    }

    public void refreshSharedTimers() {
        // Get auth token from ForegroundService
        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "getAuthToken");
        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(message);
        
        // The actual refresh will be triggered when we receive the auth token
        // This is handled in the broadcast receiver in the fragment
    }

    public void refreshSharedTimersWithToken(String authToken) {
        if (authToken != null && !authToken.isEmpty()) {
            Log.i(TAG, "Refreshing shared timers with auth token");
            repository.refreshSharedTimers(authToken);
        } else {
            Log.w(TAG, "Cannot refresh shared timers: no auth token");
        }
    }

    public void acceptSharedTimer(String timerId) {
        Log.i(TAG, "Accepting shared timer: " + timerId);
        
        // Move database operations to background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Get the shared timer details
            SharedTimer sharedTimer = repository.getSharedTimerById(timerId);
            if (sharedTimer != null) {
                Log.i(TAG, "Found shared timer: " + sharedTimer.name);
                
                // Ensure we have valid values for required fields
                String userId = sharedTimer.userId != null ? sharedTimer.userId : "unknown";
                String name = sharedTimer.name != null ? sharedTimer.name : "Unknown Timer";
                Duration totalDuration = sharedTimer.totalDuration != null ? sharedTimer.totalDuration : Duration.ZERO;
                Set<String> tags = new HashSet<>(); // No tags for shared timers
                
                // Calculate remaining time based on timerEnd if available
                Duration remainingDuration = totalDuration;
                LocalDateTime timerEnd = null;
                if (sharedTimer.timerEnd != null) {
                    timerEnd = sharedTimer.timerEnd;
                    // For unpaused timers, we use timerEnd and set remainingDurationWhenPaused to null
                    // The remaining time will be calculated dynamically from timerEnd
                } else if (sharedTimer.remainingDuration != null) {
                    // If we have remainingDuration but no timerEnd, the timer might be paused
                    remainingDuration = sharedTimer.remainingDuration;
                    // Keep remainingDurationWhenPaused set for paused timers
                }
                
                // Create TimerData with the specific timer ID and correct timing
                TimerData timerData = new TimerData(
                    timerId, // Use the specific timer ID
                    userId,
                    name,
                    totalDuration,
                    sharedTimer.remainingDuration, // Use original remainingDuration (null if unpaused)
                    timerEnd, // Use the original timer end time
                    tags
                );
                
                // Create Timer with the TimerData
                io.jhoyt.bubbletimer.Timer activeTimer = new io.jhoyt.bubbletimer.Timer(
                    timerData,
                    new HashSet<>() // No sharedWith for accepted timers
                );
                
                // Debug logging
                Log.i(TAG, "Created Timer object:");
                Log.i(TAG, "  ID: " + activeTimer.getId());
                Log.i(TAG, "  Name: " + activeTimer.getName());
                Log.i(TAG, "  UserId: " + activeTimer.getUserId());
                Log.i(TAG, "  TotalDuration: " + activeTimer.getTotalDuration());
                Log.i(TAG, "  RemainingDuration: " + activeTimer.getRemainingDuration());
                Log.i(TAG, "  TimerEnd: " + activeTimer.getTimerEnd());
                Log.i(TAG, "  TimerData ID: " + activeTimer.getTimerData().id);
                Log.i(TAG, "  TimerData Name: " + activeTimer.getTimerData().name);
                Log.i(TAG, "  TimerData UserId: " + activeTimer.getTimerData().userId);
                
                // Create ActiveTimer directly with correct timing
                ActiveTimer directActiveTimer = new ActiveTimer();
                directActiveTimer.id = timerId;
                directActiveTimer.userId = userId;
                directActiveTimer.name = name;
                directActiveTimer.totalDuration = totalDuration;
                directActiveTimer.remainingDurationWhenPaused = sharedTimer.remainingDuration; // null for unpaused, value for paused
                directActiveTimer.timerEnd = timerEnd; // Use original timer end time
                directActiveTimer.sharedWithString = "";
                directActiveTimer.tagsString = "";
                
                Log.i(TAG, "Created direct ActiveTimer:");
                Log.i(TAG, "  ID: " + directActiveTimer.id);
                Log.i(TAG, "  Name: " + directActiveTimer.name);
                Log.i(TAG, "  UserId: " + directActiveTimer.userId);
                Log.i(TAG, "  TotalDuration: " + directActiveTimer.totalDuration);
                Log.i(TAG, "  RemainingDurationWhenPaused: " + directActiveTimer.remainingDurationWhenPaused + " (null=unpaused)");
                Log.i(TAG, "  TimerEnd: " + directActiveTimer.timerEnd);
                Log.i(TAG, "  SharedWithString: '" + directActiveTimer.sharedWithString + "'");
                Log.i(TAG, "  TagsString: '" + directActiveTimer.tagsString + "'");
                
                // Add to active timers using direct ActiveTimer
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getDatabase(getApplication()).activeTimerDao().insert(directActiveTimer);
                });
                
                // Update shared timer status
                repository.acceptSharedTimer(timerId);
                
                // Trigger WebSocket connection for shared timers (on main thread)
                getApplication().getMainExecutor().execute(() -> {
                    Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
                    message.putExtra("command", "connectForSharedTimers");
                    LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(message);
                });
                
                Log.i(TAG, "Successfully accepted shared timer and added to active timers");
            } else {
                Log.e(TAG, "Shared timer not found: " + timerId);
            }
        });
    }

    public void rejectSharedTimer(String timerId) {
        Log.i(TAG, "Rejecting shared timer: " + timerId);
        
        // Get auth token from ForegroundService
        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "getAuthToken");
        message.putExtra("callback", "rejectSharedTimer");
        message.putExtra("timerId", timerId);
        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(message);
    }

    public void rejectSharedTimerWithToken(String timerId, String authToken) {
        Log.i(TAG, "Rejecting shared timer with token: " + timerId);
        repository.rejectSharedTimer(timerId, authToken);
    }

    public void deleteSharedTimer(String timerId) {
        Log.i(TAG, "Deleting shared timer: " + timerId);
        repository.deleteSharedTimer(timerId);
    }

    public void clearRejectedTimers() {
        Log.i(TAG, "Clearing rejected timers");
        repository.clearRejectedTimers();
    }
} 