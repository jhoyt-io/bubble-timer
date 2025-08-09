package io.jhoyt.bubbletimer.data.converters;

import io.jhoyt.bubbletimer.domain.entities.TimerState;
import io.jhoyt.bubbletimer.db.ActiveTimer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts between domain Timer entities and existing Room database entities.
 * This bridge allows the domain layer to work with existing data models.
 */
public final class DomainTimerConverter {
    
    private DomainTimerConverter() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Converts a domain Timer to an ActiveTimer for Room database storage.
     */
    public static ActiveTimer domainTimerToActiveTimer(io.jhoyt.bubbletimer.domain.entities.Timer domainTimer) {
        ActiveTimer activeTimer = new ActiveTimer();
        
        activeTimer.id = domainTimer.getId();
        activeTimer.userId = domainTimer.getUserId();
        activeTimer.name = domainTimer.getName();
        activeTimer.totalDuration = domainTimer.getTotalDuration();
        activeTimer.remainingDurationWhenPaused = domainTimer.getRemainingDurationWhenPaused();
        activeTimer.timerEnd = domainTimer.getEndTime();
        
        // Convert tags to string format
        Set<String> tags = domainTimer.getTags();
        activeTimer.tagsString = (tags == null || tags.isEmpty()) ? "" : String.join("#~#", tags);
        
        // Convert shared users to string format
        Set<String> sharedWith = domainTimer.getSharedWith();
        activeTimer.sharedWithString = (sharedWith == null || sharedWith.isEmpty()) ? "" : String.join("#~#", sharedWith);
        
        return activeTimer;
    }
    
    /**
     * Converts an ActiveTimer from Room database to a domain Timer.
     */
    public static io.jhoyt.bubbletimer.domain.entities.Timer activeTimerToDomainTimer(ActiveTimer activeTimer) {
        if (activeTimer == null) {
            return null;
        }
        
        // Parse tags from string
        Set<String> tags = Set.of();
        if (activeTimer.tagsString != null && !activeTimer.tagsString.isEmpty()) {
            tags = Set.of(activeTimer.tagsString.split("#~#"));
        }
        
        // Parse shared users from string
        Set<String> sharedWith = Set.of();
        if (activeTimer.sharedWithString != null && !activeTimer.sharedWithString.isEmpty()) {
            sharedWith = Set.of(activeTimer.sharedWithString.split("#~#"))
                .stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        }
        
        // Create domain timer with all data
        return io.jhoyt.bubbletimer.domain.entities.Timer.fromData(
            activeTimer.id,
            activeTimer.name,
            activeTimer.userId,
            activeTimer.totalDuration,
            activeTimer.remainingDurationWhenPaused,
            activeTimer.timerEnd,
            tags,
            sharedWith,
            determineTimerState(activeTimer),
            LocalDateTime.now(), // ActiveTimer doesn't have createdAt
            LocalDateTime.now()  // ActiveTimer doesn't have updatedAt
        );
    }
    
    /**
     * Converts a domain Timer to a Room Timer entity.
     */
    public static io.jhoyt.bubbletimer.db.Timer domainTimerToRoomTimer(io.jhoyt.bubbletimer.domain.entities.Timer domainTimer) {
        io.jhoyt.bubbletimer.db.Timer roomTimer = new io.jhoyt.bubbletimer.db.Timer();
        
        roomTimer.id = Integer.parseInt(domainTimer.getId()); // Assuming ID is numeric
        roomTimer.title = domainTimer.getName();
        roomTimer.duration = domainTimer.getTotalDuration();
        
        // Convert tags to string format
        Set<String> tags = domainTimer.getTags();
        roomTimer.tagsString = (tags == null || tags.isEmpty()) ? "" : String.join("#~#", tags);
        
        return roomTimer;
    }
    
    /**
     * Converts a Room Timer entity to a domain Timer.
     */
    public static io.jhoyt.bubbletimer.domain.entities.Timer roomTimerToDomainTimer(io.jhoyt.bubbletimer.db.Timer roomTimer) {
        if (roomTimer == null) {
            return null;
        }
        
        // Parse tags from string
        Set<String> tags = Set.of();
        if (roomTimer.tagsString != null && !roomTimer.tagsString.isEmpty()) {
            tags = Set.of(roomTimer.tagsString.split("#~#"));
        }
        
        // Create domain timer (Room Timer is for saved templates, not active timers)
        return io.jhoyt.bubbletimer.domain.entities.Timer.create(
            roomTimer.title,
            "unknown", // Room Timer doesn't have userId
            roomTimer.duration,
            tags
        );
    }
    
    /**
     * Determines the TimerState based on ActiveTimer data.
     */
    private static TimerState determineTimerState(ActiveTimer activeTimer) {
        if (activeTimer.timerEnd == null) {
            // No end time means timer is paused
            return TimerState.PAUSED;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (activeTimer.timerEnd.isBefore(now)) {
            // End time has passed
            return TimerState.EXPIRED;
        } else {
            // End time is in the future
            return TimerState.RUNNING;
        }
    }
    
    /**
     * Creates a new domain Timer from basic data.
     * This is used when creating new timers that don't exist in the database yet.
     */
    public static io.jhoyt.bubbletimer.domain.entities.Timer createNewDomainTimer(String name, String userId, Duration duration, Set<String> tags) {
        return io.jhoyt.bubbletimer.domain.entities.Timer.create(name, userId, duration, tags);
    }
    
    /**
     * Updates an existing domain Timer with new data.
     * This preserves the original ID and creation time.
     */
    public static io.jhoyt.bubbletimer.domain.entities.Timer updateDomainTimer(io.jhoyt.bubbletimer.domain.entities.Timer existingTimer, String name, Duration duration, Set<String> tags) {
        return io.jhoyt.bubbletimer.domain.entities.Timer.fromData(
            existingTimer.getId(),
            name,
            existingTimer.getUserId(),
            duration,
            existingTimer.getRemainingDurationWhenPaused(),
            existingTimer.getEndTime(),
            tags,
            existingTimer.getSharedWith(),
            existingTimer.getState(),
            existingTimer.getCreatedAt(),
            LocalDateTime.now() // Update timestamp
        );
    }
}
