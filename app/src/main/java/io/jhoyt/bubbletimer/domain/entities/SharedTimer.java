package io.jhoyt.bubbletimer.domain.entities;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a shared timer invitation in the domain layer.
 * This is a pure domain entity with no Android framework dependencies.
 */
public class SharedTimer {
    private final String timerId;
    private final String name;
    private final String userId;
    private final Duration totalDuration;
    private final Duration remainingDuration;
    private final LocalDateTime timerEnd;
    private final ShareStatus status;
    private final String sharedBy;
    private final LocalDateTime createdAt;

    /**
     * Private constructor to enforce immutability
     */
    private SharedTimer(String timerId, String name, String userId, Duration totalDuration,
                       Duration remainingDuration, LocalDateTime timerEnd, ShareStatus status,
                       String sharedBy, LocalDateTime createdAt) {
        this.timerId = timerId;
        this.name = name;
        this.userId = userId;
        this.totalDuration = totalDuration;
        this.remainingDuration = remainingDuration;
        this.timerEnd = timerEnd;
        this.status = status;
        this.sharedBy = sharedBy;
        this.createdAt = createdAt;
    }

    /**
     * Create a new shared timer invitation
     * @param timerId ID of the timer being shared
     * @param name Timer name
     * @param userId User ID who created the timer
     * @param totalDuration Total duration of the timer
     * @param remainingDuration Remaining duration
     * @param timerEnd End time of the timer
     * @param sharedBy User ID who shared the timer
     * @return New SharedTimer instance
     */
    public static SharedTimer create(String timerId, String name, String userId, Duration totalDuration,
                                   Duration remainingDuration, LocalDateTime timerEnd, String sharedBy) {
        LocalDateTime now = LocalDateTime.now();
        return new SharedTimer(timerId, name, userId, totalDuration, remainingDuration, timerEnd,
                             ShareStatus.PENDING, sharedBy, now);
    }

    /**
     * Create a shared timer from existing data
     * @param timerId ID of the timer being shared
     * @param name Timer name
     * @param userId User ID who created the timer
     * @param totalDuration Total duration of the timer
     * @param remainingDuration Remaining duration
     * @param timerEnd End time of the timer
     * @param status Current share status
     * @param sharedBy User ID who shared the timer
     * @param createdAt Creation timestamp
     * @return SharedTimer instance
     */
    public static SharedTimer fromData(String timerId, String name, String userId, Duration totalDuration,
                                     Duration remainingDuration, LocalDateTime timerEnd, ShareStatus status,
                                     String sharedBy, LocalDateTime createdAt) {
        return new SharedTimer(timerId, name, userId, totalDuration, remainingDuration, timerEnd,
                             status, sharedBy, createdAt);
    }

    /**
     * Accept the shared timer invitation
     * @return New SharedTimer instance with accepted status
     */
    public SharedTimer accept() {
        if (!status.canAccept()) {
            throw new IllegalStateException("Cannot accept shared timer in status: " + status);
        }
        return new SharedTimer(timerId, name, userId, totalDuration, remainingDuration, timerEnd,
                             ShareStatus.ACCEPTED, sharedBy, createdAt);
    }

    /**
     * Reject the shared timer invitation
     * @return New SharedTimer instance with rejected status
     */
    public SharedTimer reject() {
        if (!status.canReject()) {
            throw new IllegalStateException("Cannot reject shared timer in status: " + status);
        }
        return new SharedTimer(timerId, name, userId, totalDuration, remainingDuration, timerEnd,
                             ShareStatus.REJECTED, sharedBy, createdAt);
    }

    /**
     * Check if the invitation is pending
     * @return true if the invitation is pending
     */
    public boolean isPending() {
        return status == ShareStatus.PENDING;
    }

    /**
     * Check if the invitation has been accepted
     * @return true if the invitation has been accepted
     */
    public boolean isAccepted() {
        return status == ShareStatus.ACCEPTED;
    }

    /**
     * Check if the invitation has been rejected
     * @return true if the invitation has been rejected
     */
    public boolean isRejected() {
        return status == ShareStatus.REJECTED;
    }

    /**
     * Check if the invitation is active (pending or accepted)
     * @return true if the invitation is active
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Check if the invitation is in a final state (accepted or rejected)
     * @return true if the invitation is in a final state
     */
    public boolean isFinal() {
        return status.isFinal();
    }

    /**
     * Check if the invitation can be accepted
     * @return true if the invitation can be accepted
     */
    public boolean canAccept() {
        return status.canAccept();
    }

    /**
     * Check if the invitation can be rejected
     * @return true if the invitation can be rejected
     */
    public boolean canReject() {
        return status.canReject();
    }

    // Getters

    public String getTimerId() { return timerId; }
    public String getName() { return name; }
    public String getUserId() { return userId; }
    public Duration getTotalDuration() { return totalDuration; }
    public Duration getRemainingDuration() { return remainingDuration; }
    public LocalDateTime getTimerEnd() { return timerEnd; }
    public ShareStatus getStatus() { return status; }
    public String getSharedBy() { return sharedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Object methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedTimer that = (SharedTimer) o;
        return Objects.equals(timerId, that.timerId) &&
               Objects.equals(name, that.name) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(totalDuration, that.totalDuration) &&
               Objects.equals(remainingDuration, that.remainingDuration) &&
               Objects.equals(timerEnd, that.timerEnd) &&
               status == that.status &&
               Objects.equals(sharedBy, that.sharedBy) &&
               Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timerId, name, userId, totalDuration, remainingDuration, timerEnd, status, sharedBy, createdAt);
    }

    @Override
    public String toString() {
        return "SharedTimer{" +
                "timerId='" + timerId + '\'' +
                ", name='" + name + '\'' +
                ", userId='" + userId + '\'' +
                ", totalDuration=" + totalDuration +
                ", remainingDuration=" + remainingDuration +
                ", status=" + status +
                ", sharedBy='" + sharedBy + '\'' +
                '}';
    }
}
