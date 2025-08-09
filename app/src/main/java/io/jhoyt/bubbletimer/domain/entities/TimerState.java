package io.jhoyt.bubbletimer.domain.entities;

/**
 * Represents the state of a timer in the domain layer.
 * This enum is framework-agnostic and contains pure business logic.
 */
public enum TimerState {
    /**
     * Timer is currently running and counting down
     */
    RUNNING,
    
    /**
     * Timer has been paused by the user
     */
    PAUSED,
    
    /**
     * Timer has reached zero and is expired
     */
    EXPIRED,
    
    /**
     * Timer has been stopped by the user
     */
    STOPPED;
    
    /**
     * Check if the timer is active (running or paused)
     * @return true if the timer is in an active state
     */
    public boolean isActive() {
        return this == RUNNING || this == PAUSED;
    }
    
    /**
     * Check if the timer is in a final state (expired or stopped)
     * @return true if the timer is in a final state
     */
    public boolean isFinal() {
        return this == EXPIRED || this == STOPPED;
    }
    
    /**
     * Check if the timer can be resumed
     * @return true if the timer can be resumed
     */
    public boolean canResume() {
        return this == PAUSED;
    }
    
    /**
     * Check if the timer can be paused
     * @return true if the timer can be paused
     */
    public boolean canPause() {
        return this == RUNNING;
    }
}
