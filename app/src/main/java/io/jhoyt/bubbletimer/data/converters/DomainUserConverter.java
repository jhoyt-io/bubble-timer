package io.jhoyt.bubbletimer.data.converters;

import io.jhoyt.bubbletimer.domain.entities.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Converts between domain User entities and existing Room database entities.
 * This bridge allows the domain layer to work with existing user data models.
 */
public final class DomainUserConverter {
    
    private DomainUserConverter() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a placeholder user for development and testing.
     * This is used when actual user data sources are not yet implemented.
     */
    public static User createPlaceholderUser() {
        return User.create(
            "placeholder-user-id",
            "placeholder-username",
            "placeholder@example.com"
        );
    }
    
    /**
     * Converts a domain User to a Room User entity.
     * TODO: Implement when Room User entity exists
     */
    public static Object domainUserToRoomUser(User domainUser) {
        if (domainUser == null) {
            return null;
        }
        
        // TODO: Implement conversion when Room User entity exists
        // For now, return null as Room User entity doesn't exist yet
        return null;
    }
    
    /**
     * Converts a Room User entity to a domain User.
     * TODO: Implement when Room User entity exists
     */
    public static User roomUserToDomainUser(Object roomUser) {
        if (roomUser == null) {
            return null;
        }
        
        // TODO: Implement conversion when Room User entity exists
        // For now, return placeholder user
        return createPlaceholderUser();
    }
    
    /**
     * Creates a new domain User from basic data.
     * This is used when creating new users that don't exist in the database yet.
     */
    public static User createNewDomainUser(String email, String username, String displayName) {
        return User.create(
            UUID.randomUUID().toString(),
            username,
            email
        );
    }
    
    /**
     * Updates an existing domain User with new data.
     * This preserves the original ID and creation time.
     */
    public static User updateDomainUser(User existingUser, String email, String username, String displayName) {
        return User.fromData(
            existingUser.getId(),
            username,
            email,
            existingUser.isAuthenticated(),
            existingUser.getLastLoginAt(),
            existingUser.getCreatedAt()
        );
    }
    
    /**
     * Validates user data for domain rules.
     * Returns true if the user data is valid according to domain rules.
     */
    public static boolean isValidUserData(String email, String username, String displayName) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation
        if (!email.contains("@")) {
            return false;
        }
        
        // Username validation (alphanumeric and underscore only)
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return false;
        }
        
        return true;
    }
}
