package io.jhoyt.bubbletimer.domain.exceptions;

/**
 * Exception thrown when authentication operations fail.
 * This covers login failures, expired tokens, and other authentication issues.
 */
public class AuthenticationException extends DomainException {
    
    /**
     * Create an authentication exception with message
     * @param message Error message
     */
    public AuthenticationException(String message) {
        super(message, "AUTHENTICATION_ERROR");
    }
    
    /**
     * Create an authentication exception with message and cause
     * @param message Error message
     * @param cause Original exception that caused this authentication exception
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, "AUTHENTICATION_ERROR", cause);
    }
    
    /**
     * Create an authentication exception with specific error code
     * @param message Error message
     * @param errorCode Specific error code
     */
    public AuthenticationException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    /**
     * Create an authentication exception with specific error code and cause
     * @param message Error message
     * @param errorCode Specific error code
     * @param cause Original exception that caused this authentication exception
     */
    public AuthenticationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
    
    @Override
    public boolean isRecoverable() {
        // Authentication errors are recoverable - user can re-authenticate
        return true;
    }
    
    @Override
    public String getUserFriendlyMessage() {
        return "Authentication failed. Please log in again.";
    }
}
