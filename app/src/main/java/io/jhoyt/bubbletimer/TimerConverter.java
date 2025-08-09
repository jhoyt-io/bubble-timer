package io.jhoyt.bubbletimer;

import java.util.HashSet;
import java.util.Set;

import io.jhoyt.bubbletimer.db.ActiveTimer;
import io.jhoyt.bubbletimer.utils.TimerSharingValidator;

public class TimerConverter {
    public static Timer fromActiveTimer(ActiveTimer activeTimer) {
        if (activeTimer == null) {
            throw new IllegalArgumentException("ActiveTimer cannot be null");
        }
        
        // Validate and clean the sharedWith string
        Set<String> sharedWith = Set.of();
        if (activeTimer.sharedWithString != null && !activeTimer.sharedWithString.isEmpty()) {
            try {
                sharedWith = Set.of(activeTimer.sharedWithString.split("#~#")).stream()
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toSet());
                
                // Use TimerSharingValidator to ensure creator is included
                sharedWith = TimerSharingValidator.ensureCreatorIncluded(sharedWith, activeTimer.userId);
                
                // Validate the cleaned set
                if (!TimerSharingValidator.isValidSharedWithSet(sharedWith, activeTimer.userId)) {
                    System.err.println("Warning: Invalid sharedWith set detected in ActiveTimer: " + activeTimer.id);
                    // Use cleaned set as fallback
                    sharedWith = TimerSharingValidator.cleanSharedWithSet(sharedWith);
                }
            } catch (Exception e) {
                System.err.println("Error processing sharedWith string for timer " + activeTimer.id + ": " + e.getMessage());
                // Fallback to empty set
                sharedWith = Set.of();
            }
        }
        
        // Validate and clean the tags string
        Set<String> tags = Set.of();
        if (activeTimer.tagsString != null && !activeTimer.tagsString.isEmpty()) {
            try {
                tags = Set.of(activeTimer.tagsString.split("#~#")).stream()
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toSet());
            } catch (Exception e) {
                System.err.println("Error processing tags string for timer " + activeTimer.id + ": " + e.getMessage());
                // Fallback to empty set
                tags = Set.of();
            }
        }
        
        return new Timer(new TimerData(
                activeTimer.id,
                activeTimer.userId,
                activeTimer.name,
                activeTimer.totalDuration,
                activeTimer.remainingDurationWhenPaused,
                activeTimer.timerEnd,
                tags
        ), sharedWith, activeTimer.sharedBy);
    }

    public static ActiveTimer toActiveTimer(Timer timer) {
        if (timer == null) {
            throw new IllegalArgumentException("Timer cannot be null");
        }
        
        TimerData timerData = timer.getTimerData();
        if (timerData == null) {
            throw new IllegalArgumentException("Timer data cannot be null");
        }

        ActiveTimer activeTimer = new ActiveTimer();
        
        // Validate and set basic fields
        activeTimer.id = timerData.id != null ? timerData.id : "unknown";
        activeTimer.userId = timerData.userId != null ? timerData.userId : "unknown";
        activeTimer.name = timerData.name != null ? timerData.name : "Unknown Timer";
        activeTimer.totalDuration = timerData.totalDuration != null ? timerData.totalDuration : java.time.Duration.ZERO;
        activeTimer.remainingDurationWhenPaused = timerData.remainingDurationWhenPaused; // Can be null for unpaused timers
        activeTimer.timerEnd = timerData.timerEnd; // Can be null for paused timers
        
        // Validate and clean sharedWith set
        Set<String> sharedWith = timer.getSharedWith();
        if (sharedWith != null) {
            try {
                // Use TimerSharingValidator to clean and validate the set
                Set<String> cleanedSharedWith = TimerSharingValidator.cleanSharedWithSet(sharedWith);
                Set<String> validatedSharedWith = TimerSharingValidator.ensureCreatorIncluded(cleanedSharedWith, activeTimer.userId);
                
                if (!TimerSharingValidator.isValidSharedWithSet(validatedSharedWith, activeTimer.userId)) {
                    System.err.println("Warning: Invalid sharedWith set detected in Timer: " + activeTimer.id);
                    // Use cleaned set as fallback
                    validatedSharedWith = TimerSharingValidator.cleanSharedWithSet(validatedSharedWith);
                }
                
                activeTimer.sharedWithString = validatedSharedWith.isEmpty() ? "" : String.join("#~#", validatedSharedWith);
            } catch (Exception e) {
                System.err.println("Error processing sharedWith set for timer " + activeTimer.id + ": " + e.getMessage());
                activeTimer.sharedWithString = "";
            }
        } else {
            activeTimer.sharedWithString = "";
        }
        
        // Validate and clean tags set
        Set<String> tags = timer.getTags();
        if (tags != null) {
            try {
                Set<String> cleanedTags = tags.stream()
                        .filter(tag -> tag != null && !tag.trim().isEmpty())
                        .map(String::trim)
                        .collect(java.util.stream.Collectors.toSet());
                activeTimer.tagsString = cleanedTags.isEmpty() ? "" : String.join("#~#", cleanedTags);
            } catch (Exception e) {
                System.err.println("Error processing tags set for timer " + activeTimer.id + ": " + e.getMessage());
                activeTimer.tagsString = "";
            }
        } else {
            activeTimer.tagsString = "";
        }
        
        // Set sharedBy field
        activeTimer.sharedBy = timer.getSharedBy() != null ? timer.getSharedBy() : "";

        // Debug logging
        System.out.println("TimerConverter - ActiveTimer values:");
        System.out.println("  id: " + activeTimer.id);
        System.out.println("  userId: " + activeTimer.userId);
        System.out.println("  name: " + activeTimer.name);
        System.out.println("  totalDuration: " + activeTimer.totalDuration);
        System.out.println("  remainingDurationWhenPaused: " + activeTimer.remainingDurationWhenPaused + " (null=unpaused)");
        System.out.println("  timerEnd: " + activeTimer.timerEnd + " (null=paused)");
        System.out.println("  sharedWithString: '" + activeTimer.sharedWithString + "'");
        System.out.println("  tagsString: '" + activeTimer.tagsString + "'");
        System.out.println("  sharedBy: '" + activeTimer.sharedBy + "'");

        return activeTimer;
    }
}
