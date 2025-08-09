package io.jhoyt.bubbletimer.domain.exceptions;

/**
 * Exception thrown when a timer is not found.
 * This is thrown when trying to access a timer that doesn't exist.
 */
public class TimerNotFoundException extends TimerException {
    
    /**
     * Create a timer not found exception
     * @param timerId ID of the timer that was not found
     */
    public TimerNotFoundException(String timerId) {
        super("Timer not found: " + timerId, "TIMER_NOT_FOUND");
    }
    
    @Override
    public boolean isRecoverable() {
        // Timer not found is not recoverable - the timer simply doesn't exist
        return false;
    }
    
    @Override
    public String getUserFriendlyMessage() {
        return "Timer not found. It may have been deleted or you may not have permission to access it.";
    }
}
