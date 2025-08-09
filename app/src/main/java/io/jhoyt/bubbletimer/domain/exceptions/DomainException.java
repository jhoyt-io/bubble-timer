package io.jhoyt.bubbletimer.domain.exceptions;

/**
 * Base exception class for all domain-related exceptions.
 * This provides a common foundation for domain-specific error handling.
 */
public abstract class DomainException extends Exception {
    private final String errorCode;
    private final String errorType;

    /**
     * Create a domain exception with message and error code
     * @param message Human-readable error message
     * @param errorCode Machine-readable error code
     */
    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = this.getClass().getSimpleName();
    }

    /**
     * Create a domain exception with message, error code, and cause
     * @param message Human-readable error message
     * @param errorCode Machine-readable error code
     * @param cause Original exception that caused this domain exception
     */
    protected DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = this.getClass().getSimpleName();
    }

    /**
     * Get the machine-readable error code
     * @return Error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Get the type of this exception
     * @return Exception type
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * Check if this exception is recoverable
     * @return true if the operation can be retried
     */
    public boolean isRecoverable() {
        return false; // Default to non-recoverable, override in subclasses
    }

    /**
     * Get a user-friendly error message
     * @return User-friendly error message
     */
    public String getUserFriendlyMessage() {
        return getMessage(); // Default to the exception message, override in subclasses
    }
}
