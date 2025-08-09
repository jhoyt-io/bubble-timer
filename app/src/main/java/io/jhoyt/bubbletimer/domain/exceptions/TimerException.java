package io.jhoyt.bubbletimer.domain.exceptions;

/**
 * Exception thrown when timer-related operations fail.
 * This covers general timer errors that don't fit into more specific categories.
 */
public class TimerException extends DomainException {
    
    /**
     * Create a timer exception with message
     * @param message Error message
     */
    public TimerException(String message) {
        super(message, "TIMER_ERROR");
    }
    
    /**
     * Create a timer exception with message and cause
     * @param message Error message
     * @param cause Original exception that caused this timer exception
     */
    public TimerException(String message, Throwable cause) {
        super(message, "TIMER_ERROR", cause);
    }
    
    /**
     * Create a timer exception with specific error code
     * @param message Error message
     * @param errorCode Specific error code
     */
    public TimerException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    /**
     * Create a timer exception with specific error code and cause
     * @param message Error message
     * @param errorCode Specific error code
     * @param cause Original exception that caused this timer exception
     */
    public TimerException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
    
    @Override
    public boolean isRecoverable() {
        // Timer exceptions are generally recoverable (can retry the operation)
        return true;
    }
    
    @Override
    public String getUserFriendlyMessage() {
        return "Timer operation failed. Please try again.";
    }
}
