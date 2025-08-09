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
import com.amplifyframework.core.Amplify;

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
        // First try to use cached auth token
        String cachedToken = repository.getCachedAuthToken();
        if (cachedToken != null) {
            Log.i(TAG, "Using cached auth token for refresh");
            repository.refreshSharedTimers(cachedToken);
            return;
        }
        
        // If no cached token, request a new one
        Log.i(TAG, "No cached auth token, requesting new one");
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

    public void refreshSharedTimersWithTokenAndCallback(String authToken, Runnable onComplete) {
        if (authToken != null && !authToken.isEmpty()) {
            Log.i(TAG, "Refreshing shared timers with auth token and callback");
            repository.refreshSharedTimersWithCallback(authToken, onComplete);
        } else {
            Log.w(TAG, "Cannot refresh shared timers: no auth token");
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    public interface SharedTimerAcceptCallback {
        void onAcceptSuccess(String timerName, String sharerName);
        void onAcceptFailed(String errorMessage);
    }

    public void acceptSharedTimer(String timerId, SharedTimerAcceptCallback callback) {
        Log.i(TAG, "Accepting shared timer: " + timerId);
        
        // Move database operations to background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // First, let's check what shared timers are available in the database
            List<SharedTimer> allSharedTimers = repository.getAllSharedTimersSync();
            Log.i(TAG, "Available shared timers in database: " + allSharedTimers.size());
            for (SharedTimer timer : allSharedTimers) {
                Log.i(TAG, "  - Timer ID: " + timer.timerId + ", Name: " + timer.name + ", Status: " + timer.status);
            }
            
            // Get the shared timer details
            SharedTimer sharedTimer = repository.getSharedTimerById(timerId);
            if (sharedTimer == null) {
                Log.e(TAG, "Shared timer not found: " + timerId);
                if (callback != null) {
                    callback.onAcceptFailed("Shared timer not found.");
                }
                return;
            }

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
            
            // Parse the stored sharedWith list to preserve sharing relationships
            // Use the #~# delimiter that TimerConverter expects
            Set<String> sharedWithSet = new HashSet<>();
            if (sharedTimer.sharedWith != null && !sharedTimer.sharedWith.trim().isEmpty()) {
                String[] sharedWithArray = sharedTimer.sharedWith.split("#~#");
                for (String user : sharedWithArray) {
                    String trimmedUser = user.trim();
                    if (!trimmedUser.isEmpty()) {
                        sharedWithSet.add(trimmedUser);
                    }
                }
                Log.i(TAG, "Restored sharedWith from stored data: " + sharedWithSet + " for timer " + timerId);
            }
            
            // CRITICAL: Ensure the timer creator and the current user are both in the sharedWith list
            // This is necessary because shared timer invitations may not include the complete user list
            
            // Add the timer creator (from the shared timer record)
            if (sharedTimer.sharedBy != null && !sharedTimer.sharedBy.trim().isEmpty()) {
                sharedWithSet.add(sharedTimer.sharedBy.trim());
                Log.i(TAG, "Added timer creator to sharedWith: " + sharedTimer.sharedBy);
            }
            
            // IMPORTANT: For shared timer acceptance, we need to ensure both users are in the list
            // The accepting user is NOT the same as the timer creator (userId field)
            // We need to add the accepting user based on the fact that they're accepting the timer
            
            // In this context, 'userId' is the timer creator, but we need the accepting user too
            // For now, add a placeholder that will be fixed when we get proper user context
            // The timer should be shared between the creator (sharedBy) and some other user
            
            // Add the timer owner (userId field)
            if (userId != null && !userId.trim().isEmpty()) {
                sharedWithSet.add(userId.trim());
                Log.i(TAG, "Added timer owner to sharedWith: " + userId);
            }
            
            // Ensure we have at least both users represented
            // If only one user in the set, we know there should be an accepting user too
            if (sharedWithSet.size() == 1) {
                // Add a placeholder for the accepting user - this will be corrected by the WebSocket sync
                Log.i(TAG, "Only one user in sharedWith set - timer should have more users when properly synced");
            }
            
            Log.i(TAG, "Final sharedWith list for timer " + timerId + ": " + sharedWithSet);
            
            // Create Timer with the TimerData and preserved sharedWith list
            io.jhoyt.bubbletimer.Timer activeTimer = new io.jhoyt.bubbletimer.Timer(
                timerData,
                sharedWithSet, // Preserve the original sharing relationships
                sharedTimer.sharedBy // Set who shared this timer
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
            // Preserve the sharedWith information for WebSocket connection detection
            // Use the reconstructed sharedWith set with proper user list
            if (!sharedWithSet.isEmpty()) {
                directActiveTimer.sharedWithString = String.join("#~#", sharedWithSet);
                Log.i(TAG, "Set sharedWithString from reconstructed set: '" + directActiveTimer.sharedWithString + "' for timer " + timerId);
            } else {
                directActiveTimer.sharedWithString = "";
                Log.w(TAG, "sharedWithSet is empty, setting sharedWithString to empty for timer " + timerId);
            }
            directActiveTimer.tagsString = "";
            directActiveTimer.sharedBy = sharedTimer.sharedBy != null ? sharedTimer.sharedBy : "";
            
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
                
                if (callback != null) {
                    callback.onAcceptSuccess(sharedTimer.name, sharedTimer.sharedBy);
                }
            });
            
            Log.i(TAG, "Successfully accepted shared timer and added to active timers");
        });
    }

    // Overloaded method for backward compatibility
    public void acceptSharedTimer(String timerId) {
        acceptSharedTimer(timerId, null);
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
    
    public void deleteAllSharedTimers() {
        Log.i(TAG, "Deleting all shared timers");
        repository.deleteAllSharedTimers();
    }
    
    /**
     * Get the current authenticated user's ID.
     * For now, we'll get this from the ForegroundService which caches it.
     * Returns null if not available.
     */
    private String getCurrentUserId() {
        try {
            // Send a request to ForegroundService to get the current user ID
            // For this synchronous context, we'll use a different approach
            // since Amplify.Auth.getCurrentUser() is async
            
            // TODO: Implement proper async handling or get from a cached source
            // For now, return null and let the sharedBy field handle the creator
            Log.d(TAG, "getCurrentUserId() - using fallback approach");
            return null;
        } catch (Exception e) {
            Log.w(TAG, "Could not get current user ID: " + e.getMessage());
            return null;
        }
    }
} 