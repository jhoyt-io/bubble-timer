package io.jhoyt.bubbletimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;

/**
 * Updated ActiveTimerViewModel that uses domain use cases.
 * This demonstrates how to integrate the new domain layer with existing ViewModels.
 */
@HiltViewModel
public class ActiveTimerViewModel extends ViewModel {
    
    private final ActiveTimerRepository activeTimerRepository;
    private final Context applicationContext;
    private final MutableLiveData<Set<Timer>> activeTimers = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Timer> primaryTimer = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    // Observer reference for cleanup
    private Observer<List<Timer>> databaseObserver;
    
    @Inject
    public ActiveTimerViewModel(ActiveTimerRepository activeTimerRepository, @ApplicationContext Context applicationContext) {
        this.activeTimerRepository = activeTimerRepository;
        this.applicationContext = applicationContext;
        // Observe database changes for real-time synchronization
        observeDatabaseChanges();
    }
    
    /**
     * Notify ForegroundService of timer updates for WebSocket synchronization
     */
    private void notifyForegroundServiceOfUpdate(Timer timer) {
        if (timer == null) return;
        
        try {
            // Send broadcast to ForegroundService to trigger WebSocket update
            Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
            message.putExtra("command", "timerUpdated");
            message.putExtra("timerId", timer.getId());
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(message);
            
            android.util.Log.i("ActiveTimerViewModel", "Notified ForegroundService of timer update: " + timer.getId() + 
                  " - sharedWith: " + (timer.getSharedWith() != null ? timer.getSharedWith().toString() : "null"));
        } catch (Exception e) {
            // In unit test environment, LocalBroadcastManager may not be available
            android.util.Log.d("ActiveTimerViewModel", "Could not send broadcast (likely in test environment): " + e.getMessage());
        }
    }
    
    /**
     * Observe database changes for real-time synchronization with ForegroundService
     */
    private void observeDatabaseChanges() {
        // Create observer for database changes
        databaseObserver = timersFromDb -> {
            if (timersFromDb != null) {
                Set<Timer> timerSet = new HashSet<>(timersFromDb);
                activeTimers.setValue(timerSet);
                
                // Update primary timer
                if (!timerSet.isEmpty()) {
                    primaryTimer.setValue(timerSet.iterator().next());
                } else {
                    primaryTimer.setValue(null);
                }
            }
        };
        
        // Observe the database LiveData for real-time updates
        activeTimerRepository.getAllActiveTimers().observeForever(databaseObserver);
    }
    
    /**
     * Load active timers from database (manual refresh)
     */
    public void loadActiveTimers() {
        isLoading.setValue(true);
        
        // Get timers from database repository
        List<Timer> timersFromDb = activeTimerRepository.getAllActiveTimers().getValue();
        if (timersFromDb != null) {
            Set<Timer> timerSet = new HashSet<>(timersFromDb);
            activeTimers.setValue(timerSet);
            
            // Set primary timer if available
            if (!timerSet.isEmpty()) {
                primaryTimer.setValue(timerSet.iterator().next());
            }
        }
        
        isLoading.setValue(false);
    }
    
    /**
     * Start a new timer
     */
    public void startNewTimer(String name, java.time.Duration duration, String userId) {
        isLoading.setValue(true);
        
        // Create timer using old Timer class for compatibility
        Timer timer = new Timer(userId, name, duration, new HashSet<>());
        timer.unpause();
        
        // Save to database repository so ForegroundService can access it
        activeTimerRepository.insert(timer);
        
        // Notify ForegroundService for WebSocket synchronization
        notifyForegroundServiceOfUpdate(timer);
        
        // Add to active timers in memory
        Set<Timer> current = activeTimers.getValue();
        if (current == null) {
            current = new HashSet<>();
        }
        current.add(timer);
        activeTimers.setValue(current);
        
        // Set as primary timer if none exists
        if (primaryTimer.getValue() == null) {
            primaryTimer.setValue(timer);
        }
        
        isLoading.setValue(false);
    }
    
    /**
     * Pause a timer
     */
    public void pauseTimer(String timerId) {
        isLoading.setValue(true);
        
        Set<Timer> current = activeTimers.getValue();
        if (current != null) {
            Timer timerToPause = current.stream()
                .filter(timer -> timer.getId().equals(timerId))
                .findFirst()
                .orElse(null);
            
            if (timerToPause != null) {
                timerToPause.pause();
                // Update database so ForegroundService sees the change
                activeTimerRepository.update(timerToPause);
                // Notify ForegroundService for WebSocket synchronization
                notifyForegroundServiceOfUpdate(timerToPause);
                activeTimers.setValue(current);
            }
        }
        
        isLoading.setValue(false);
    }
    
    /**
     * Resume a timer
     */
    public void resumeTimer(String timerId) {
        isLoading.setValue(true);
        
        Set<Timer> current = activeTimers.getValue();
        if (current != null) {
            Timer timerToResume = current.stream()
                .filter(timer -> timer.getId().equals(timerId))
                .findFirst()
                .orElse(null);
            
            if (timerToResume != null) {
                timerToResume.unpause();
                // Update database so ForegroundService sees the change
                activeTimerRepository.update(timerToResume);
                // Notify ForegroundService for WebSocket synchronization
                notifyForegroundServiceOfUpdate(timerToResume);
                activeTimers.setValue(current);
            }
        }
        
        isLoading.setValue(false);
    }
    
    /**
     * Stop a timer
     */
    public void stopTimer(String timerId) {
        isLoading.setValue(true);
        
        Set<Timer> current = activeTimers.getValue();
        if (current != null) {
            Timer timerToStop = current.stream()
                .filter(timer -> timer.getId().equals(timerId))
                .findFirst()
                .orElse(null);
            
            if (timerToStop != null) {
                // Notify ForegroundService for WebSocket synchronization before deletion
                notifyForegroundServiceOfUpdate(timerToStop);
                
                // Remove from database so ForegroundService stops showing overlay
                activeTimerRepository.deleteById(timerToStop.getId());
                
                current.remove(timerToStop);
                activeTimers.setValue(current);
                
                // Update primary timer if it was the one stopped
                if (primaryTimer.getValue() != null && 
                    primaryTimer.getValue().getId().equals(timerId)) {
                    updatePrimaryTimer();
                }
            }
        }
        
        isLoading.setValue(false);
    }
    
    /**
     * Set the primary timer
     */
    public void setPrimaryTimer(Timer timer) {
        primaryTimer.setValue(timer);
    }
    
    /**
     * Get the primary timer LiveData
     */
    public LiveData<Timer> getPrimaryTimer() {
        return primaryTimer;
    }
    
    /**
     * Get active timers LiveData
     */
    public LiveData<Set<Timer>> getActiveTimers() {
        return activeTimers;
    }
    
    /**
     * Get error message LiveData
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Get loading state LiveData
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    /**
     * Update the primary timer when the current one is removed
     */
    private void updatePrimaryTimer() {
        Set<Timer> current = activeTimers.getValue();
        if (current != null && !current.isEmpty()) {
            primaryTimer.setValue(current.iterator().next());
        } else {
            primaryTimer.setValue(null);
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up the database observer to avoid memory leaks
        if (databaseObserver != null) {
            activeTimerRepository.getAllActiveTimers().removeObserver(databaseObserver);
        }
    }

}
