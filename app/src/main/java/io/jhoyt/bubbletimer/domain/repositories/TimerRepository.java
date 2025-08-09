package io.jhoyt.bubbletimer.domain.repositories;

import io.jhoyt.bubbletimer.domain.entities.Timer;

import java.util.List;

/**
 * Repository interface for Timer domain entities.
 * This interface defines the contract for timer data access operations.
 * Implementations will handle the actual data storage and retrieval.
 */
public interface TimerRepository {
    
    // Query methods
    
    /**
     * Get all timers
     * @return List of all timers
     */
    List<Timer> getAllTimers();
    
    /**
     * Get timers by tag
     * @param tag Tag to filter by
     * @return List of timers with the specified tag
     */
    List<Timer> getTimersByTag(String tag);
    
    /**
     * Get timer by ID
     * @param id Timer ID
     * @return Timer with the specified ID, or null if not found
     */
    Timer getTimerById(String id);
    
    /**
     * Get all active timers (running or paused)
     * @return List of active timers
     */
    List<Timer> getActiveTimers();
    
    /**
     * Get timers created by a specific user
     * @param userId User ID
     * @return List of timers created by the user
     */
    List<Timer> getTimersByUserId(String userId);
    
    /**
     * Get timers shared with a specific user
     * @param userId User ID
     * @return List of timers shared with the user
     */
    List<Timer> getTimersSharedWithUser(String userId);
    
    // Command methods
    
    /**
     * Save a new timer
     * @param timer Timer to save
     */
    void saveTimer(Timer timer);
    
    /**
     * Update an existing timer
     * @param timer Timer to update
     */
    void updateTimer(Timer timer);
    
    /**
     * Delete a timer by ID
     * @param id Timer ID to delete
     */
    void deleteTimer(String id);
    
    /**
     * Delete all timers
     */
    void deleteAllTimers();
    
    /**
     * Delete timers by user ID
     * @param userId User ID whose timers to delete
     */
    void deleteTimersByUserId(String userId);
    
    // Specialized timer operations
    
    /**
     * Start a timer (change state to running)
     * @param id Timer ID to start
     */
    void startTimer(String id);
    
    /**
     * Stop a timer (change state to stopped)
     * @param id Timer ID to stop
     */
    void stopTimer(String id);
    
    /**
     * Pause a timer (change state to paused)
     * @param id Timer ID to pause
     */
    void pauseTimer(String id);
    
    /**
     * Resume a timer (change state to running)
     * @param id Timer ID to resume
     */
    void resumeTimer(String id);
    
    /**
     * Add time to a timer
     * @param id Timer ID
     * @param additionalDuration Duration to add
     */
    void addTimeToTimer(String id, java.time.Duration additionalDuration);
    
    /**
     * Share a timer with a user
     * @param timerId Timer ID
     * @param userId User ID to share with
     */
    void shareTimer(String timerId, String userId);
    
    /**
     * Remove sharing of a timer with a user
     * @param timerId Timer ID
     * @param userId User ID to remove sharing with
     */
    void removeTimerSharing(String timerId, String userId);
    
    /**
     * Add a tag to a timer
     * @param timerId Timer ID
     * @param tag Tag to add
     */
    void addTagToTimer(String timerId, String tag);
    
    /**
     * Remove a tag from a timer
     * @param timerId Timer ID
     * @param tag Tag to remove
     */
    void removeTagFromTimer(String timerId, String tag);
    
    // Observable methods (for reactive programming)
    // Note: These return domain entities, not Android LiveData
    
    /**
     * Observe all timers
     * @return Observable list of all timers
     */
    Observable<List<Timer>> observeAllTimers();
    
    /**
     * Observe active timers
     * @return Observable list of active timers
     */
    Observable<List<Timer>> observeActiveTimers();
    
    /**
     * Observe timer by ID
     * @param id Timer ID
     * @return Observable timer
     */
    Observable<Timer> observeTimerById(String id);
    
    /**
     * Observe timers by user ID
     * @param userId User ID
     * @return Observable list of timers created by the user
     */
    Observable<List<Timer>> observeTimersByUserId(String userId);
    
    /**
     * Observe timers shared with a user
     * @param userId User ID
     * @return Observable list of timers shared with the user
     */
    Observable<List<Timer>> observeTimersSharedWithUser(String userId);
    
    /**
     * Simple Observable interface for reactive programming
     * This is a simplified version that can be implemented without Android dependencies
     */
    interface Observable<T> {
        /**
         * Subscribe to changes
         * @param observer Observer to notify of changes
         */
        void subscribe(Observer<T> observer);
        
        /**
         * Unsubscribe from changes
         * @param observer Observer to remove
         */
        void unsubscribe(Observer<T> observer);
        
        /**
         * Get current value
         * @return Current value
         */
        T getValue();
    }
    
    /**
     * Observer interface for reactive programming
     */
    interface Observer<T> {
        /**
         * Called when the observed value changes
         * @param value New value
         */
        void onChanged(T value);
    }
}
