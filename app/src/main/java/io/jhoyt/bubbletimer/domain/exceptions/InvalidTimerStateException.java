package io.jhoyt.bubbletimer.domain.exceptions;

import io.jhoyt.bubbletimer.domain.entities.TimerState;

/**
 * Exception thrown when an invalid timer state transition is attempted.
 * This is thrown when trying to perform an operation that is not allowed
 * in the current timer state.
 */
public class InvalidTimerStateException extends TimerException {
    
    /**
     * Create an invalid timer state exception
     * @param currentState Current timer state
     * @param attemptedOperation Operation that was attempted
     */
    public InvalidTimerStateException(TimerState currentState, String attemptedOperation) {
        super("Cannot perform '" + attemptedOperation + "' in current state: " + currentState, 
              "INVALID_TIMER_STATE");
    }
    
    /**
     * Create an invalid timer state exception with custom message
     * @param message Custom error message
     */
    public InvalidTimerStateException(String message) {
        super(message, "INVALID_TIMER_STATE");
    }
    
    @Override
    public boolean isRecoverable() {
        // Invalid state transitions are not recoverable - the operation is fundamentally wrong
        return false;
    }
    
    @Override
    public String getUserFriendlyMessage() {
        return "This operation is not allowed in the current timer state.";
    }
}
