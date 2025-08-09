package io.jhoyt.bubbletimer.utils;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for validating and managing timer sharing relationships.
 * This serves as a single source of truth for all timer sharing validation logic,
 * particularly the creator inclusion logic that was previously scattered across
 * multiple components.
 */
public class TimerSharingValidator {
    private static final String TAG = "TimerSharingValidator";

    /**
     * Ensures that the timer creator is always included in the sharedWith list.
     * This is critical for maintaining consistent sharing state across all devices.
     * 
     * @param sharedWith The current set of users the timer is shared with
     * @param creatorId The ID of the timer creator
     * @return A new set with the creator guaranteed to be included
     */
    public static Set<String> ensureCreatorIncluded(Set<String> sharedWith, String creatorId) {
        if (sharedWith == null) {
            sharedWith = new HashSet<>();
        }
        
        if (creatorId == null || creatorId.trim().isEmpty()) {
            try {
                Log.w(TAG, "Creator ID is null or empty, cannot ensure inclusion");
            } catch (Exception e) {
                // Ignore logging exceptions in test environment
            }
            return new HashSet<>(sharedWith);
        }
        
        String trimmedCreatorId = creatorId.trim();
        Set<String> result = new HashSet<>(sharedWith);
        
        if (!result.contains(trimmedCreatorId)) {
            result.add(trimmedCreatorId);
            try {
                Log.d(TAG, "Added creator '" + trimmedCreatorId + "' to sharedWith list. " +
                          "Original size: " + sharedWith.size() + ", New size: " + result.size());
            } catch (Exception e) {
                // Ignore logging exceptions in test environment
            }
        } else {
            try {
                Log.d(TAG, "Creator '" + trimmedCreatorId + "' already in sharedWith list");
            } catch (Exception e) {
                // Ignore logging exceptions in test environment
            }
        }
        
        return result;
    }

    /**
     * Validates a sharedWith set for consistency and correctness.
     * 
     * @param sharedWith The set to validate
     * @param creatorId The timer creator ID for validation
     * @return true if the set is valid, false otherwise
     */
    public static boolean isValidSharedWithSet(Set<String> sharedWith, String creatorId) {
        if (sharedWith == null) {
            try {
                Log.w(TAG, "sharedWith set is null");
            } catch (Exception e) {
                // Ignore logging exceptions in test environment
            }
            return false;
        }
        
        // Check for null or empty values
        for (String userId : sharedWith) {
            if (userId == null || userId.trim().isEmpty()) {
                try {
                    Log.w(TAG, "sharedWith set contains null or empty user ID");
                } catch (Exception e) {
                    // Ignore logging exceptions in test environment
                }
                return false;
            }
        }
        
        // Check for duplicates (case-insensitive)
        Set<String> lowerCaseIds = sharedWith.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (lowerCaseIds.size() != sharedWith.size()) {
            try {
                Log.w(TAG, "sharedWith set contains duplicate user IDs (case-insensitive)");
            } catch (Exception e) {
                // Ignore logging exceptions in test environment
            }
            return false;
        }
        
        // Ensure creator is included if this is a shared timer
        if (creatorId != null && !creatorId.trim().isEmpty() && !sharedWith.isEmpty()) {
            if (!sharedWith.contains(creatorId.trim())) {
                try {
                    Log.w(TAG, "sharedWith set does not include creator '" + creatorId + "'");
                } catch (Exception e) {
                    // Ignore logging exceptions in test environment
                }
                return false;
            }
        }
        
        return true;
    }

    /**
     * Cleans and validates a sharedWith set by removing null/empty values and trimming whitespace.
     * 
     * @param sharedWith The set to clean
     * @return A cleaned set with valid user IDs only
     */
    public static Set<String> cleanSharedWithSet(Set<String> sharedWith) {
        if (sharedWith == null) {
            return new HashSet<>();
        }
        
        Set<String> cleaned = sharedWith.stream()
                .filter(userId -> userId != null && !userId.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toSet());
        
        if (cleaned.size() != sharedWith.size()) {
            try {
                Log.d(TAG, "Cleaned sharedWith set: removed " + (sharedWith.size() - cleaned.size()) + " invalid entries");
            } catch (Exception e) {
                // Ignore logging exceptions in test environment
            }
        }
        
        return cleaned;
    }

    /**
     * Validates that a user ID is valid for sharing operations.
     * 
     * @param userId The user ID to validate
     * @return true if the user ID is valid, false otherwise
     */
    public static boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        // Additional validation rules can be added here
        // For example, checking for valid characters, length limits, etc.
        
        return true;
    }

    /**
     * Checks if a timer is actually shared (has more than just the creator in sharedWith).
     * 
     * @param sharedWith The set of users the timer is shared with
     * @param creatorId The timer creator ID
     * @return true if the timer is shared with other users, false otherwise
     */
    public static boolean isTimerShared(Set<String> sharedWith, String creatorId) {
        if (sharedWith == null || sharedWith.isEmpty()) {
            return false;
        }
        
        if (creatorId == null || creatorId.trim().isEmpty()) {
            // If we don't know the creator, consider it shared if there are any users
            return true;
        }
        
        String trimmedCreatorId = creatorId.trim();
        Set<String> otherUsers = sharedWith.stream()
                .filter(userId -> !userId.equals(trimmedCreatorId))
                .collect(Collectors.toSet());
        
        return !otherUsers.isEmpty();
    }

    /**
     * Gets the list of users the timer is shared with, excluding the creator.
     * 
     * @param sharedWith The set of users the timer is shared with
     * @param creatorId The timer creator ID
     * @return A set of users excluding the creator
     */
    public static Set<String> getSharedUsersExcludingCreator(Set<String> sharedWith, String creatorId) {
        if (sharedWith == null || sharedWith.isEmpty()) {
            return new HashSet<>();
        }
        
        if (creatorId == null || creatorId.trim().isEmpty()) {
            return new HashSet<>(sharedWith);
        }
        
        String trimmedCreatorId = creatorId.trim();
        return sharedWith.stream()
                .filter(userId -> !userId.equals(trimmedCreatorId))
                .collect(Collectors.toSet());
    }

    /**
     * Logs the current state of a sharedWith set for debugging purposes.
     * 
     * @param sharedWith The set to log
     * @param creatorId The timer creator ID
     * @param context Additional context for the log message
     */
    public static void logSharedWithState(Set<String> sharedWith, String creatorId, String context) {
        try {
            if (sharedWith == null) {
                Log.d(TAG, context + " - sharedWith: null");
                return;
            }
            
            Log.d(TAG, context + " - sharedWith size: " + sharedWith.size());
            Log.d(TAG, context + " - sharedWith contents: " + sharedWith);
            
            if (creatorId != null && !creatorId.trim().isEmpty()) {
                boolean creatorIncluded = sharedWith.contains(creatorId.trim());
                Log.d(TAG, context + " - creator '" + creatorId.trim() + "' included: " + creatorIncluded);
            }
            
            boolean isShared = isTimerShared(sharedWith, creatorId);
            Log.d(TAG, context + " - timer is shared: " + isShared);
        } catch (Exception e) {
            // Ignore logging exceptions in test environment
        }
    }
}
