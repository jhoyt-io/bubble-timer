package io.jhoyt.bubbletimer.domain.repositories;

import io.jhoyt.bubbletimer.domain.entities.SharedTimer;

import java.util.List;

/**
 * Repository interface for SharedTimer domain entities.
 * This interface defines the contract for shared timer data access operations.
 * Implementations will handle the actual data storage and retrieval.
 */
public interface SharedTimerRepository {
    
    // Query methods
    
    /**
     * Get all shared timers
     * @return List of all shared timers
     */
    List<SharedTimer> getAllSharedTimers();
    
    /**
     * Get shared timer by timer ID
     * @param timerId Timer ID
     * @return SharedTimer with the specified timer ID, or null if not found
     */
    SharedTimer getSharedTimerByTimerId(String timerId);
    
    /**
     * Get shared timers by user ID (timers shared with this user)
     * @param userId User ID
     * @return List of shared timers for the user
     */
    List<SharedTimer> getSharedTimersByUserId(String userId);
    
    /**
     * Get shared timers by status
     * @param status ShareStatus to filter by
     * @return List of shared timers with the specified status
     */
    List<SharedTimer> getSharedTimersByStatus(io.jhoyt.bubbletimer.domain.entities.ShareStatus status);
    
    /**
     * Get pending shared timers for a user
     * @param userId User ID
     * @return List of pending shared timers for the user
     */
    List<SharedTimer> getPendingSharedTimersByUserId(String userId);
    
    /**
     * Get accepted shared timers for a user
     * @param userId User ID
     * @return List of accepted shared timers for the user
     */
    List<SharedTimer> getAcceptedSharedTimersByUserId(String userId);
    
    /**
     * Get shared timers shared by a specific user
     * @param sharedBy User ID who shared the timers
     * @return List of shared timers shared by the user
     */
    List<SharedTimer> getSharedTimersBySharedBy(String sharedBy);
    
    // Command methods
    
    /**
     * Save a new shared timer
     * @param sharedTimer SharedTimer to save
     */
    void saveSharedTimer(SharedTimer sharedTimer);
    
    /**
     * Update an existing shared timer
     * @param sharedTimer SharedTimer to update
     */
    void updateSharedTimer(SharedTimer sharedTimer);
    
    /**
     * Delete a shared timer by timer ID
     * @param timerId Timer ID to delete
     */
    void deleteSharedTimer(String timerId);
    
    /**
     * Delete all shared timers
     */
    void deleteAllSharedTimers();
    
    /**
     * Delete shared timers by user ID
     * @param userId User ID whose shared timers to delete
     */
    void deleteSharedTimersByUserId(String userId);
    
    /**
     * Delete shared timers shared by a specific user
     * @param sharedBy User ID who shared the timers
     */
    void deleteSharedTimersBySharedBy(String sharedBy);
    
    // Specialized operations
    
    /**
     * Accept a shared timer invitation
     * @param timerId Timer ID to accept
     * @param userId User ID accepting the invitation
     */
    void acceptSharedTimer(String timerId, String userId);
    
    /**
     * Reject a shared timer invitation
     * @param timerId Timer ID to reject
     * @param userId User ID rejecting the invitation
     */
    void rejectSharedTimer(String timerId, String userId);
    
    /**
     * Share a timer with a user
     * @param timerId Timer ID to share
     * @param userId User ID to share with
     * @param sharedBy User ID who is sharing the timer
     */
    void shareTimerWithUser(String timerId, String userId, String sharedBy);
    
    /**
     * Remove sharing of a timer with a user
     * @param timerId Timer ID
     * @param userId User ID to remove sharing with
     */
    void removeTimerSharing(String timerId, String userId);
    
    // Observable methods (for reactive programming)
    
    /**
     * Observe all shared timers
     * @return Observable list of all shared timers
     */
    Observable<List<SharedTimer>> observeAllSharedTimers();
    
    /**
     * Observe shared timer by timer ID
     * @param timerId Timer ID
     * @return Observable shared timer
     */
    Observable<SharedTimer> observeSharedTimerByTimerId(String timerId);
    
    /**
     * Observe shared timers by user ID
     * @param userId User ID
     * @return Observable list of shared timers for the user
     */
    Observable<List<SharedTimer>> observeSharedTimersByUserId(String userId);
    
    /**
     * Observe pending shared timers for a user
     * @param userId User ID
     * @return Observable list of pending shared timers for the user
     */
    Observable<List<SharedTimer>> observePendingSharedTimersByUserId(String userId);
    
    /**
     * Observe accepted shared timers for a user
     * @param userId User ID
     * @return Observable list of accepted shared timers for the user
     */
    Observable<List<SharedTimer>> observeAcceptedSharedTimersByUserId(String userId);
    
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
