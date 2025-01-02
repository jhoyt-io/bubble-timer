package io.jhoyt.bubbletimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Timer {
    private TimerData timerData;

    private Set<String> sharedWith;

    public Timer(TimerData timerData) {
        this.timerData = timerData;
        this.sharedWith = new HashSet<>();
    }

    public Timer() {
        this(new TimerData(String.valueOf(UUID.randomUUID()), null, null, null, null, null));
    }


    public Timer(String userId, String name, Duration duration) {
        this(new TimerData(
                String.valueOf(UUID.randomUUID()),
                userId,
                name,
                duration,
                duration,
                null
        ));
    }

    public static Timer timerFromJson(JSONObject jsonTimer) throws JSONException {
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
                        : null
        ));
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

    public Set<String> getSharedWith() {
        return this.sharedWith;
    }

    public void shareWith(String userName) {
        this.sharedWith.add(userName);
    }

    public TimerData getTimerData() {
        return this.timerData;
    }

    public String getId() {
        return this.timerData.id;
    }

    public String getName() {
        return this.timerData.name;
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
                        : timerData.timerEnd.plus(duration)
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

        this.timerData = new TimerData(
                timerData.id,
                timerData.userId,
                timerData.name,
                timerData.totalDuration,
                getRemainingDuration(),
                null
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
                LocalDateTime.now().plus(this.timerData.remainingDurationWhenPaused)
        );
    }

    public Duration getTotalDuration() {
        return this.timerData.totalDuration;
    }

    public Duration getRemainingDuration() {
        if (this.timerData.remainingDurationWhenPaused == null) {
            if (this.timerData.timerEnd == null) {
                return this.timerData.totalDuration;
            }

            return Duration.between(LocalDateTime.now(), this.timerData.timerEnd);
        }

        return this.timerData.remainingDurationWhenPaused;
    }
}
