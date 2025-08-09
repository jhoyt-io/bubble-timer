package io.jhoyt.bubbletimer.domain.exceptions;

/**
 * Exception thrown when input validation fails.
 * This is thrown when user input or business rule validation fails.
 */
public class ValidationException extends DomainException {
    
    /**
     * Create a validation exception with message
     * @param message Error message
     */
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
    
    /**
     * Create a validation exception with message and cause
     * @param message Error message
     * @param cause Original exception that caused this validation exception
     */
    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", cause);
    }
    
    /**
     * Create a validation exception with specific error code
     * @param message Error message
     * @param errorCode Specific error code
     */
    public ValidationException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    @Override
    public boolean isRecoverable() {
        // Validation errors are recoverable - user can fix the input and retry
        return true;
    }
    
    @Override
    public String getUserFriendlyMessage() {
        return "Please check your input and try again.";
    }
}
