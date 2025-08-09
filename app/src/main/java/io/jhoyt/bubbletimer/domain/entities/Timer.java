package io.jhoyt.bubbletimer.domain.entities;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a timer in the domain layer.
 * This is a pure domain entity with no Android framework dependencies.
 * Contains rich business logic and domain methods.
 */
public class Timer {
    private final String id;
    private final String name;
    private final String userId;
    private final Duration totalDuration;
    private final Duration remainingDurationWhenPaused;
    private final LocalDateTime endTime;
    private final Set<String> sharedWith;
    private final Set<String> tags;
    private final TimerState state;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Private constructor to enforce immutability and ensure valid state
     */
    private Timer(String id, String name, String userId, Duration totalDuration,
                  Duration remainingDurationWhenPaused, LocalDateTime endTime,
                  Set<String> sharedWith, Set<String> tags, TimerState state,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.totalDuration = totalDuration;
        this.remainingDurationWhenPaused = remainingDurationWhenPaused;
        this.endTime = endTime;
        this.sharedWith = Collections.unmodifiableSet(new HashSet<>(sharedWith));
        this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Factory methods for creating timers

    /**
     * Create a new running timer
     * @param name Timer name
     * @param userId User ID who created the timer
     * @param duration Total duration of the timer
     * @param tags Set of tags for the timer
     * @return New Timer instance
     */
    public static Timer create(String name, String userId, Duration duration, Set<String> tags) {
        LocalDateTime now = LocalDateTime.now();
        return new Timer(
            UUID.randomUUID().toString(),
            name,
            userId,
            duration,
            null, // remainingDurationWhenPaused is null for running timers
            now.plus(duration),
            new HashSet<>(),
            tags != null ? tags : new HashSet<>(),
            TimerState.RUNNING,
            now,
            now
        );
    }

    /**
     * Create a timer from existing data (for loading from storage)
     * @param id Timer ID
     * @param name Timer name
     * @param userId User ID
     * @param totalDuration Total duration
     * @param remainingDurationWhenPaused Remaining duration when paused (null if running)
     * @param endTime End time (null if paused)
     * @param sharedWith Set of users the timer is shared with
     * @param tags Set of tags
     * @param state Current timer state
     * @param createdAt Creation timestamp
     * @param updatedAt Last update timestamp
     * @return Timer instance
     */
    public static Timer fromData(String id, String name, String userId, Duration totalDuration,
                                Duration remainingDurationWhenPaused, LocalDateTime endTime,
                                Set<String> sharedWith, Set<String> tags, TimerState state,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Timer(
            id, name, userId, totalDuration, remainingDurationWhenPaused, endTime,
            sharedWith, tags, state, createdAt, updatedAt
        );
    }

    // Domain methods

    /**
     * Check if the timer is currently paused
     * @return true if the timer is paused
     */
    public boolean isPaused() {
        return state == TimerState.PAUSED;
    }

    /**
     * Check if the timer is currently running
     * @return true if the timer is running
     */
    public boolean isRunning() {
        return state == TimerState.RUNNING;
    }

    /**
     * Check if the timer has expired
     * @return true if the timer has expired
     */
    public boolean isExpired() {
        return state == TimerState.EXPIRED || getRemainingDuration().isNegative() || getRemainingDuration().isZero();
    }

    /**
     * Check if the timer is stopped
     * @return true if the timer is stopped
     */
    public boolean isStopped() {
        return state == TimerState.STOPPED;
    }

    /**
     * Check if the timer is active (running or paused)
     * @return true if the timer is active
     */
    public boolean isActive() {
        return state.isActive();
    }

    /**
     * Check if the timer is in a final state (expired or stopped)
     * @return true if the timer is in a final state
     */
    public boolean isFinal() {
        return state.isFinal();
    }

    /**
     * Get the current remaining duration
     * @return Remaining duration
     */
    public Duration getRemainingDuration() {
        if (isPaused()) {
            return remainingDurationWhenPaused != null ? remainingDurationWhenPaused : Duration.ZERO;
        } else if (isRunning() && endTime != null) {
            Duration remaining = Duration.between(LocalDateTime.now(), endTime);
            return remaining.isNegative() ? Duration.ZERO : remaining;
        } else {
            return Duration.ZERO;
        }
    }

    /**
     * Get the elapsed duration
     * @return Elapsed duration
     */
    public Duration getElapsedDuration() {
        return totalDuration.minus(getRemainingDuration());
    }

    /**
     * Get the progress as a percentage (0.0 to 1.0)
     * @return Progress percentage
     */
    public double getProgressPercentage() {
        if (totalDuration.isZero()) {
            return 0.0;
        }
        return (double) getElapsedDuration().toSeconds() / totalDuration.toSeconds();
    }

    /**
     * Check if the timer is shared with any users
     * @return true if the timer is shared
     */
    public boolean isShared() {
        return !sharedWith.isEmpty();
    }

    /**
     * Check if the timer is shared with a specific user
     * @param userId User ID to check
     * @return true if the timer is shared with the user
     */
    public boolean isSharedWith(String userId) {
        return sharedWith.contains(userId);
    }

    /**
     * Check if the timer has a specific tag
     * @param tag Tag to check
     * @return true if the timer has the tag
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    /**
     * Check if the timer can be paused
     * @return true if the timer can be paused
     */
    public boolean canPause() {
        return state.canPause();
    }

    /**
     * Check if the timer can be resumed
     * @return true if the timer can be resumed
     */
    public boolean canResume() {
        return state.canResume();
    }

    // State transition methods

    /**
     * Pause the timer
     * @return New Timer instance in paused state
     * @throws IllegalStateException if timer cannot be paused
     */
    public Timer pause() {
        if (!canPause()) {
            throw new IllegalStateException("Timer cannot be paused in current state: " + state);
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Timer(
            id, name, userId, totalDuration,
            getRemainingDuration(), // Store current remaining time
            null, // Clear end time when paused
            sharedWith, tags, TimerState.PAUSED,
            createdAt, now
        );
    }

    /**
     * Resume the timer
     * @return New Timer instance in running state
     * @throws IllegalStateException if timer cannot be resumed
     */
    public Timer resume() {
        if (!canResume()) {
            throw new IllegalStateException("Timer cannot be resumed in current state: " + state);
        }
        
        LocalDateTime now = LocalDateTime.now();
        Duration remaining = remainingDurationWhenPaused != null ? remainingDurationWhenPaused : Duration.ZERO;
        
        return new Timer(
            id, name, userId, totalDuration,
            null, // Clear remaining duration when running
            now.plus(remaining),
            sharedWith, tags, TimerState.RUNNING,
            createdAt, now
        );
    }

    /**
     * Stop the timer
     * @return New Timer instance in stopped state
     */
    public Timer stop() {
        LocalDateTime now = LocalDateTime.now();
        return new Timer(
            id, name, userId, totalDuration,
            Duration.ZERO, // Clear remaining duration
            null, // Clear end time
            sharedWith, tags, TimerState.STOPPED,
            createdAt, now
        );
    }

    /**
     * Add time to the timer
     * @param additionalDuration Duration to add
     * @return New Timer instance with updated duration
     */
    public Timer addTime(Duration additionalDuration) {
        if (additionalDuration.isNegative() || additionalDuration.isZero()) {
            throw new IllegalArgumentException("Additional duration must be positive");
        }
        
        LocalDateTime now = LocalDateTime.now();
        Duration newTotalDuration = totalDuration.plus(additionalDuration);
        
        if (isRunning()) {
            // For running timers, extend the end time
            LocalDateTime newEndTime = endTime != null ? endTime.plus(additionalDuration) : now.plus(additionalDuration);
            return new Timer(
                id, name, userId, newTotalDuration,
                null, newEndTime,
                sharedWith, tags, TimerState.RUNNING,
                createdAt, now
            );
        } else if (isPaused()) {
            // For paused timers, add to remaining duration
            Duration newRemaining = (remainingDurationWhenPaused != null ? remainingDurationWhenPaused : Duration.ZERO)
                .plus(additionalDuration);
            return new Timer(
                id, name, userId, newTotalDuration,
                newRemaining, null,
                sharedWith, tags, TimerState.PAUSED,
                createdAt, now
            );
        } else {
            // For stopped/expired timers, just update total duration
            return new Timer(
                id, name, userId, newTotalDuration,
                remainingDurationWhenPaused, endTime,
                sharedWith, tags, state,
                createdAt, now
            );
        }
    }

    /**
     * Share the timer with a user
     * @param userId User ID to share with
     * @return New Timer instance with updated sharing
     */
    public Timer shareWith(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        Set<String> newSharedWith = new HashSet<>(sharedWith);
        newSharedWith.add(userId.trim());
        
        LocalDateTime now = LocalDateTime.now();
        return new Timer(
            id, name, userId, totalDuration,
            remainingDurationWhenPaused, endTime,
            newSharedWith, tags, state,
            createdAt, now
        );
    }

    /**
     * Remove sharing with a user
     * @param userIdToRemove User ID to remove sharing with
     * @return New Timer instance with updated sharing
     */
    public Timer removeSharing(String userIdToRemove) {
        // PROTECTION: Never allow removing the timer creator from sharedWith list
        // This ensures consistent sharing state across all devices
        if (userIdToRemove != null && userIdToRemove.equals(this.userId)) {
            throw new IllegalArgumentException("Cannot remove timer creator '" + userIdToRemove + "' from shared timer. Creator must always be included in sharing.");
        }
        
        Set<String> newSharedWith = new HashSet<>(sharedWith);
        newSharedWith.remove(userIdToRemove);
        
        LocalDateTime now = LocalDateTime.now();
        return new Timer(
            id, name, userId, totalDuration,
            remainingDurationWhenPaused, endTime,
            newSharedWith, tags, state,
            createdAt, now
        );
    }

    /**
     * Add a tag to the timer
     * @param tag Tag to add
     * @return New Timer instance with updated tags
     */
    public Timer addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag cannot be null or empty");
        }
        
        Set<String> newTags = new HashSet<>(tags);
        newTags.add(tag.trim());
        
        LocalDateTime now = LocalDateTime.now();
        return new Timer(
            id, name, userId, totalDuration,
            remainingDurationWhenPaused, endTime,
            sharedWith, newTags, state,
            createdAt, now
        );
    }

    /**
     * Remove a tag from the timer
     * @param tag Tag to remove
     * @return New Timer instance with updated tags
     */
    public Timer removeTag(String tag) {
        Set<String> newTags = new HashSet<>(tags);
        newTags.remove(tag);
        
        LocalDateTime now = LocalDateTime.now();
        return new Timer(
            id, name, userId, totalDuration,
            remainingDurationWhenPaused, endTime,
            sharedWith, newTags, state,
            createdAt, now
        );
    }

    // Getters (immutable)

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUserId() { return userId; }
    public Duration getTotalDuration() { return totalDuration; }
    public Duration getRemainingDurationWhenPaused() { return remainingDurationWhenPaused; }
    public LocalDateTime getEndTime() { return endTime; }
    public Set<String> getSharedWith() { return sharedWith; }
    public Set<String> getTags() { return tags; }
    public TimerState getState() { return state; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Object methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Timer timer = (Timer) o;
        return Objects.equals(id, timer.id) &&
               Objects.equals(name, timer.name) &&
               Objects.equals(userId, timer.userId) &&
               Objects.equals(totalDuration, timer.totalDuration) &&
               Objects.equals(remainingDurationWhenPaused, timer.remainingDurationWhenPaused) &&
               Objects.equals(endTime, timer.endTime) &&
               Objects.equals(sharedWith, timer.sharedWith) &&
               Objects.equals(tags, timer.tags) &&
               state == timer.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, userId, totalDuration, remainingDurationWhenPaused, 
                          endTime, sharedWith, tags, state);
    }

    @Override
    public String toString() {
        return "Timer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", userId='" + userId + '\'' +
                ", totalDuration=" + totalDuration +
                ", remainingDuration=" + getRemainingDuration() +
                ", state=" + state +
                ", isShared=" + isShared() +
                ", tags=" + tags +
                '}';
    }
}
