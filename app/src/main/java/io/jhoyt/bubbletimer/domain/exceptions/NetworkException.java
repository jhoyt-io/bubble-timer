package io.jhoyt.bubbletimer.domain.exceptions;

/**
 * Exception thrown when network operations fail.
 * This covers connection issues, timeouts, and other network-related errors.
 */
public class NetworkException extends DomainException {
    
    /**
     * Create a network exception with message
     * @param message Error message
     */
    public NetworkException(String message) {
        super(message, "NETWORK_ERROR");
    }
    
    /**
     * Create a network exception with message and cause
     * @param message Error message
     * @param cause Original exception that caused this network exception
     */
    public NetworkException(String message, Throwable cause) {
        super(message, "NETWORK_ERROR", cause);
    }
    
    /**
     * Create a network exception with specific error code
     * @param message Error message
     * @param errorCode Specific error code
     */
    public NetworkException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    /**
     * Create a network exception with specific error code and cause
     * @param message Error message
     * @param errorCode Specific error code
     * @param cause Original exception that caused this network exception
     */
    public NetworkException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
    
    @Override
    public boolean isRecoverable() {
        // Network errors are generally recoverable - can retry when connection is restored
        return true;
    }
    
    @Override
    public String getUserFriendlyMessage() {
        return "Network connection failed. Please check your internet connection and try again.";
    }
}
