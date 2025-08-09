package io.jhoyt.bubbletimer.data.converters;

import io.jhoyt.bubbletimer.domain.entities.SharedTimer;
import io.jhoyt.bubbletimer.domain.entities.ShareStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Converts between domain SharedTimer entities and existing Room database entities.
 * This bridge allows the domain layer to work with existing shared timer data models.
 */
public final class DomainSharedTimerConverter {
    
    private DomainSharedTimerConverter() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a placeholder shared timer for development and testing.
     * This is used when actual shared timer data sources are not yet implemented.
     */
    public static SharedTimer createPlaceholderSharedTimer(String timerId) {
        return SharedTimer.create(
            timerId,
            "Placeholder Timer",
            "placeholder-user-id",
            java.time.Duration.ofMinutes(5),
            java.time.Duration.ofMinutes(3),
            LocalDateTime.now().plusMinutes(5),
            "placeholder-shared-by"
        );
    }
    
    /**
     * Converts a domain SharedTimer to a Room SharedTimer entity.
     * TODO: Implement when Room SharedTimer entity exists
     */
    public static Object domainSharedTimerToRoomSharedTimer(SharedTimer domainSharedTimer) {
        if (domainSharedTimer == null) {
            return null;
        }
        
        // TODO: Implement conversion when Room SharedTimer entity exists
        // For now, return null as Room SharedTimer entity doesn't exist yet
        return null;
    }
    
    /**
     * Converts a Room SharedTimer entity to a domain SharedTimer.
     * TODO: Implement when Room SharedTimer entity exists
     */
    public static SharedTimer roomSharedTimerToDomainSharedTimer(Object roomSharedTimer) {
        if (roomSharedTimer == null) {
            return null;
        }
        
        // TODO: Implement conversion when Room SharedTimer entity exists
        // For now, return placeholder shared timer
        return createPlaceholderSharedTimer("placeholder-timer-id");
    }
    
    /**
     * Creates a new domain SharedTimer from basic data.
     * This is used when creating new shared timers that don't exist in the database yet.
     */
    public static SharedTimer createNewSharedTimer(String timerId, String userId, String sharedBy) {
        return SharedTimer.create(
            timerId,
            "New Shared Timer",
            userId,
            java.time.Duration.ofMinutes(5),
            java.time.Duration.ofMinutes(5),
            LocalDateTime.now().plusMinutes(5),
            sharedBy
        );
    }
    
    /**
     * Updates an existing domain SharedTimer with new data.
     * This preserves the original ID and creation time.
     */
    public static SharedTimer updateDomainSharedTimer(SharedTimer existingSharedTimer, String timerId, String userId, String sharedBy, ShareStatus status) {
        return SharedTimer.fromData(
            timerId,
            existingSharedTimer.getName(),
            userId,
            existingSharedTimer.getTotalDuration(),
            existingSharedTimer.getRemainingDuration(),
            existingSharedTimer.getTimerEnd(),
            status,
            sharedBy,
            existingSharedTimer.getCreatedAt()
        );
    }
    
    /**
     * Validates shared timer data for domain rules.
     * Returns true if the shared timer data is valid according to domain rules.
     */
    public static boolean isValidSharedTimerData(String timerId, String userId, String sharedBy) {
        if (timerId == null || timerId.trim().isEmpty()) {
            return false;
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        if (sharedBy == null || sharedBy.trim().isEmpty()) {
            return false;
        }
        
        // Ensure timer ID is not empty
        if (timerId.length() < 1) {
            return false;
        }
        
        // Ensure user ID is not empty
        if (userId.length() < 1) {
            return false;
        }
        
        // Ensure shared by is not empty
        if (sharedBy.length() < 1) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Creates a shared timer invitation with pending status.
     */
    public static SharedTimer createInvitation(String timerId, String userId, String sharedBy) {
        if (!isValidSharedTimerData(timerId, userId, sharedBy)) {
            throw new IllegalArgumentException("Invalid shared timer data");
        }
        
        return SharedTimer.create(
            timerId,
            "Shared Timer Invitation",
            userId,
            java.time.Duration.ofMinutes(5),
            java.time.Duration.ofMinutes(5),
            LocalDateTime.now().plusMinutes(5),
            sharedBy
        );
    }
    
    /**
     * Creates an accepted shared timer.
     */
    public static SharedTimer createAcceptedSharedTimer(String timerId, String userId, String sharedBy) {
        if (!isValidSharedTimerData(timerId, userId, sharedBy)) {
            throw new IllegalArgumentException("Invalid shared timer data");
        }
        
        return SharedTimer.create(
            timerId,
            "Accepted Shared Timer",
            userId,
            java.time.Duration.ofMinutes(5),
            java.time.Duration.ofMinutes(5),
            LocalDateTime.now().plusMinutes(5),
            sharedBy
        );
    }
    
    /**
     * Creates a rejected shared timer.
     */
    public static SharedTimer createRejectedSharedTimer(String timerId, String userId, String sharedBy) {
        if (!isValidSharedTimerData(timerId, userId, sharedBy)) {
            throw new IllegalArgumentException("Invalid shared timer data");
        }
        
        return SharedTimer.create(
            timerId,
            "Rejected Shared Timer",
            userId,
            java.time.Duration.ofMinutes(5),
            java.time.Duration.ofMinutes(5),
            LocalDateTime.now().plusMinutes(5),
            sharedBy
        );
    }
}
