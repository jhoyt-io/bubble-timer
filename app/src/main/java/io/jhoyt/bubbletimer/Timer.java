package io.jhoyt.bubbletimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.runtime.external.kotlinx.collections.immutable.ImmutableSet;

import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import io.jhoyt.bubbletimer.utils.TimerSharingValidator;

public class Timer {
    @SerializedName("id")
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("totalDuration")
    private String totalDuration;
    
    @SerializedName("remainingDuration")
    private String remainingDuration;
    
    @SerializedName("endTime")
    private String endTime;
    
    @SerializedName("sharedWith")
    private Set<String> sharedWith;

    @SerializedName("sharedBy")
    private String sharedBy;

    private TimerData timerData;

    public Timer() {
        this.sharedWith = new HashSet<>();
    }

    public Timer(TimerData timerData, Set<String> sharedWith) {
        this.timerData = timerData;
        this.sharedWith = sharedWith;
        this.sharedBy = null; // Default to null for non-shared timers
    }

    public Timer(TimerData timerData, Set<String> sharedWith, String sharedBy) {
        this.timerData = timerData;
        this.sharedWith = sharedWith;
        this.sharedBy = sharedBy;
    }

    public Timer(String userId, String name, Duration duration, Set<String> tags) {
        this(new TimerData(
                String.valueOf(UUID.randomUUID()),
                userId != null ? userId : "unknown",
                name != null ? name : "Unknown Timer",
                duration != null ? duration : java.time.Duration.ZERO,
                duration != null ? duration : java.time.Duration.ZERO,
                null,
                tags != null ? tags : new HashSet<>()
        ), new HashSet<>());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Timer)) {
            return false;
        }

        Timer timer = (Timer) obj;
        return this.timerData.equals(timer.getTimerData()) &&
                this.sharedWith.containsAll(timer.getSharedWith()) &&
                timer.getSharedWith().containsAll(this.sharedWith);
    }

    @Override
    public int hashCode() {
        return this.timerData.hashCode();
    }

    public Timer copy() {
        return new Timer(this.timerData.copy(), new HashSet<>(this.getSharedWith()), this.sharedBy);
    }

    public static Timer timerFromJson(JSONObject jsonTimer) throws JSONException {
        Set<String> tags = new HashSet<>();

        if (jsonTimer.has("tags")) {
            JSONArray tagsJson = jsonTimer.getJSONArray("tags");
            for(int i=0; i<tagsJson.length(); i++) {
                tags.add(tagsJson.getString(i));
            }
        }
        Timer timer = new Timer(new TimerData(
                jsonTimer.getString("id"),
                jsonTimer.getString("userId"),
                jsonTimer.getString("name"),
                jsonTimer.has("totalDuration") ?
                        Duration.parse(jsonTimer.getString("totalDuration"))
                        : null,
                jsonTimer.has("remainingDuration") ?
                        Duration.parse(jsonTimer.getString("remainingDuration"))
                        : null,
                jsonTimer.has("timerEnd") ?
                        LocalDateTime.parse(jsonTimer.getString("timerEnd"))
                        : null,
                tags
        ), new HashSet<>());
        JSONArray sharedWithJson = jsonTimer.getJSONArray("sharedWith");
        for(int i=0; i<sharedWithJson.length(); i++) {
            timer.shareWith(sharedWithJson.getString(i));
        }

        return timer;
    }

    static JSONObject timerToJson(Timer timer) throws JSONException {
        JSONObject jsonTimer = new JSONObject();

        TimerData timerData = timer.getTimerData();
        jsonTimer.put("id", timerData.id);
        jsonTimer.put("userId", timerData.userId);
        jsonTimer.put("name", timerData.name);
        jsonTimer.put("totalDuration", timerData.totalDuration);
        jsonTimer.put("remainingDuration", timerData.remainingDurationWhenPaused);
        jsonTimer.put("timerEnd",  timerData.timerEnd);
        jsonTimer.put("sharedWith", new JSONArray(timer.getSharedWith()));
        jsonTimer.put("tags", new JSONArray(timer.getTags()));
        return jsonTimer;
    }

    /*
    public Timer(String name, Duration duration, Duration remainingDuration) {
        this();

        this.timerData = new TimerData(
                timerData.id,
                timerData.userId,
                name,
                duration,
                remainingDuration,
                timerData.timerEnd
        );
    }
     */

    public Set<String> getTags() {
        return this.timerData.tags;
    }

    public void setTags(Set<String> tags) {
        this.timerData = new TimerData(
                timerData.id,
                timerData.userId,
                timerData.name,
                timerData.totalDuration,
                timerData.remainingDurationWhenPaused,
                timerData.timerEnd,
                Set.copyOf(tags)
        );
    }

    public Set<String> getSharedWith() {
        return this.sharedWith;
    }

    public void setSharedWith(Set<String> sharedWith) {
        if (sharedWith == null) {
            this.sharedWith = Collections.unmodifiableSet(new HashSet<>());
            return;
        }
        
        try {
            // Use TimerSharingValidator to clean and validate the set
            Set<String> cleanedSharedWith = TimerSharingValidator.cleanSharedWithSet(sharedWith);
            Set<String> validatedSharedWith = TimerSharingValidator.ensureCreatorIncluded(cleanedSharedWith, this.getUserId());
            
            // Validate the final set
            if (!TimerSharingValidator.isValidSharedWithSet(validatedSharedWith, this.getUserId())) {
                System.err.println("Warning: Invalid sharedWith set provided to Timer.setSharedWith() for timer: " + this.getId());
                // Use cleaned set as fallback
                validatedSharedWith = TimerSharingValidator.cleanSharedWithSet(validatedSharedWith);
            }
            
            this.sharedWith = Collections.unmodifiableSet(validatedSharedWith);
        } catch (Exception e) {
            System.err.println("Error in Timer.setSharedWith() for timer " + this.getId() + ": " + e.getMessage());
            // Fallback to empty set
            this.sharedWith = Collections.unmodifiableSet(new HashSet<>());
        }
    }

    public void shareWith(String userName) {
        if (!TimerSharingValidator.isValidUserId(userName)) {
            return; // Don't add invalid user names
        }
        
        try {
            Set<String> shareWith = new HashSet<>();
            shareWith.addAll(this.sharedWith);
            shareWith.add(userName.trim());
            
            // Use TimerSharingValidator to ensure creator is included
            shareWith = TimerSharingValidator.ensureCreatorIncluded(shareWith, this.getUserId());
            
            this.sharedWith = Collections.unmodifiableSet(shareWith);
        } catch (Exception e) {
            System.err.println("Error in Timer.shareWith() for timer " + this.getId() + ": " + e.getMessage());
            // Keep existing sharedWith set on error
        }
    }

    public String[] getFriendNames() {
        // This would ideally be dynamic, but for now return the hardcoded list
        // The TimerView will handle adding the current user
        return new String[]{"ouchthathoyt", "jill", "tester"};
    }

    public TimerData getTimerData() {
        return this.timerData;
    }

    public String getId() {
        return id != null ? id : (timerData != null ? timerData.id : null);
    }

    public String getName() {
        return name != null ? name : (timerData != null ? timerData.name : null);
    }

    public String getUserId() {
        return userId != null ? userId : (timerData != null ? timerData.userId : null);
    }

    public String getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(String sharedBy) {
        this.sharedBy = sharedBy;
    }

    public void addTime(Duration duration) {
        this.timerData = new TimerData(
                timerData.id,
                timerData.userId,
                timerData.name,
                timerData.totalDuration.plus(duration),
                (timerData.timerEnd == null) ?
                        timerData.remainingDurationWhenPaused.plus(duration)
                        : timerData.remainingDurationWhenPaused,
                (timerData.timerEnd == null) ?
                        timerData.timerEnd
                        : timerData.timerEnd.plus(duration),
                timerData.tags
        );
    }

    public boolean isPaused() {
        return this.timerData.remainingDurationWhenPaused != null;
    }

    public void pause() {
        if (this.timerData.timerEnd == null) {
            // already paused
            return;
        }

        Duration remainingDuration = getRemainingDuration();
        if (remainingDuration == null) {
            // If we can't calculate remaining duration, use total duration
            remainingDuration = timerData.totalDuration;
        }

        this.timerData = new TimerData(
                timerData.id,
                timerData.userId,
                timerData.name,
                timerData.totalDuration,
                remainingDuration,
                null,
                timerData.tags
        );
    }

    public void unpause() {
        if (this.timerData.remainingDurationWhenPaused == null) {
            // already unpaused
            return;
        }

        this.timerData = new TimerData(
                timerData.id,
                timerData.userId,
                timerData.name,
                timerData.totalDuration,
                null,
                LocalDateTime.now().plus(this.timerData.remainingDurationWhenPaused),
                timerData.tags
        );
    }

    public Duration getTotalDuration() {
        if (totalDuration != null) {
            try {
                return Duration.parse(totalDuration);
            } catch (Exception e) {
                // Fall back to timerData
            }
        }
        return timerData != null ? timerData.totalDuration : null;
    }

    public Duration getRemainingDuration() {
        if (remainingDuration != null) {
            try {
                return Duration.parse(remainingDuration);
            } catch (Exception e) {
                // Fall back to timerData
            }
        }
        if (this.timerData != null) {
            if (this.timerData.remainingDurationWhenPaused == null) {
                if (this.timerData.timerEnd == null) {
                    return this.timerData.totalDuration;
                }

                return Duration.between(LocalDateTime.now(), this.timerData.timerEnd);
            }

            return this.timerData.remainingDurationWhenPaused;
        }
        return null;
    }

    public LocalDateTime getTimerEnd() {
        if (endTime != null) {
            try {
                return LocalDateTime.parse(endTime);
            } catch (Exception e) {
                // Fall back to timerData
            }
        }
        return timerData != null ? timerData.timerEnd : null;
    }
}
