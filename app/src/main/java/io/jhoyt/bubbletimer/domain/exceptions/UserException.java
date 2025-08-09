package io.jhoyt.bubbletimer.domain.exceptions;

/**
 * Exception thrown when user-related operations fail.
 * This is a domain-specific exception for user management errors.
 */
public class UserException extends DomainException {
    
    /**
     * Create a new UserException with a message.
     * @param message Error message
     */
    public UserException(String message) {
        super(message, "USER_ERROR");
    }
    
    /**
     * Create a new UserException with a message and cause.
     * @param message Error message
     * @param cause Original exception that caused this error
     */
    public UserException(String message, Throwable cause) {
        super(message, "USER_ERROR", cause);
    }
    
    @Override
    public String getUserFriendlyMessage() {
        return "User operation failed: " + getMessage();
    }
    
    @Override
    public boolean isRecoverable() {
        // User exceptions are generally not recoverable without user intervention
        return false;
    }
}
