package io.jhoyt.bubbletimer.domain.entities;

/**
 * Represents the status of a shared timer invitation in the domain layer.
 * This enum is framework-agnostic and contains pure business logic.
 */
public enum ShareStatus {
    /**
     * Timer invitation is pending user response
     */
    PENDING,
    
    /**
     * Timer invitation has been accepted
     */
    ACCEPTED,
    
    /**
     * Timer invitation has been rejected
     */
    REJECTED;
    
    /**
     * Check if the invitation is still active (pending or accepted)
     * @return true if the invitation is active
     */
    public boolean isActive() {
        return this == PENDING || this == ACCEPTED;
    }
    
    /**
     * Check if the invitation is in a final state (accepted or rejected)
     * @return true if the invitation is in a final state
     */
    public boolean isFinal() {
        return this == ACCEPTED || this == REJECTED;
    }
    
    /**
     * Check if the invitation can be accepted
     * @return true if the invitation can be accepted
     */
    public boolean canAccept() {
        return this == PENDING;
    }
    
    /**
     * Check if the invitation can be rejected
     * @return true if the invitation can be rejected
     */
    public boolean canReject() {
        return this == PENDING;
    }
}
