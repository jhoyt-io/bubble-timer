package io.jhoyt.bubbletimer.util;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.db.ActiveTimer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TestDataFactory {

    // Timer creation methods
    public static Timer createTestTimer() {
        return new Timer("test-user", "Test Timer", Duration.ofMinutes(5), Set.of());
    }

    public static Timer createTestTimer(String name, Duration duration) {
        return new Timer("test-user", name, duration, Set.of());
    }

    public static Timer createTestTimer(String userId, String name, Duration duration) {
        return new Timer(userId, name, duration, Set.of());
    }

    public static Timer createTestTimer(String userId, String name, Duration duration, Set<String> tags) {
        return new Timer(userId, name, duration, tags);
    }

    public static Timer createTestTimerWithTags(String name, Duration duration, String... tags) {
        Set<String> tagSet = new HashSet<>(Arrays.asList(tags));
        return new Timer("test-user", name, duration, tagSet);
    }

    public static Timer createTestTimerWithSharedUsers(String name, Duration duration, String... sharedUsers) {
        Timer timer = new Timer("test-user", name, duration, Set.of());
        for (String user : sharedUsers) {
            timer.shareWith(user);
        }
        return timer;
    }

    // Short duration timers
    public static Timer createShortTimer() {
        return new Timer("test-user", "Short Timer", Duration.ofSeconds(30), Set.of());
    }

    public static Timer createShortTimer(String name) {
        return new Timer("test-user", name, Duration.ofSeconds(30), Set.of());
    }

    // Long duration timers
    public static Timer createLongTimer() {
        return new Timer("test-user", "Long Timer", Duration.ofHours(2), Set.of());
    }

    public static Timer createLongTimer(String name) {
        return new Timer("test-user", name, Duration.ofHours(2), Set.of());
    }

    // Work-related timers
    public static Timer createWorkTimer() {
        return createTestTimerWithTags("Work Timer", Duration.ofMinutes(25), "work", "pomodoro");
    }

    public static Timer createBreakTimer() {
        return createTestTimerWithTags("Break Timer", Duration.ofMinutes(5), "break", "rest");
    }

    // Timer with special characters
    public static Timer createTimerWithSpecialName() {
        return new Timer("test-user", "Timer with @#$%^&*()", Duration.ofMinutes(10), Set.of());
    }

    public static Timer createTimerWithUnicodeName() {
        return new Timer("test-user", "Timer with 用户", Duration.ofMinutes(10), Set.of());
    }

    // Timer with very long name
    public static Timer createTimerWithLongName() {
        String longName = "A".repeat(1000);
        return new Timer("test-user", longName, Duration.ofMinutes(10), Set.of());
    }

    // Timer with empty name
    public static Timer createTimerWithEmptyName() {
        return new Timer("test-user", "", Duration.ofMinutes(10), Set.of());
    }

    // Timer with null name
    public static Timer createTimerWithNullName() {
        return new Timer("test-user", null, Duration.ofMinutes(10), Set.of());
    }

    // Multiple timers for testing
    public static Timer[] createMultipleTimers() {
        return new Timer[]{
            createTestTimer("Timer 1", Duration.ofMinutes(5)),
            createTestTimer("Timer 2", Duration.ofMinutes(10)),
            createTestTimer("Timer 3", Duration.ofMinutes(15)),
            createWorkTimer(),
            createBreakTimer()
        };
    }

    // ActiveTimer creation methods
    public static ActiveTimer createActiveTimer() {
        ActiveTimer activeTimer = new ActiveTimer();
        activeTimer.id = UUID.randomUUID().toString();
        activeTimer.userId = "test-user";
        activeTimer.name = "Test Active Timer";
        activeTimer.totalDuration = Duration.ofMinutes(5);
        activeTimer.remainingDurationWhenPaused = Duration.ofMinutes(5);
        activeTimer.timerEnd = LocalDateTime.now().plusMinutes(5);
        activeTimer.sharedWithString = "";
        activeTimer.tagsString = "";
        return activeTimer;
    }

    public static ActiveTimer createActiveTimer(String timerId, String name, Duration duration) {
        ActiveTimer activeTimer = new ActiveTimer();
        activeTimer.id = timerId;
        activeTimer.userId = "test-user";
        activeTimer.name = name;
        activeTimer.totalDuration = duration;
        activeTimer.remainingDurationWhenPaused = duration;
        activeTimer.timerEnd = LocalDateTime.now().plus(duration);
        activeTimer.sharedWithString = "";
        activeTimer.tagsString = "";
        return activeTimer;
    }

    public static ActiveTimer createRunningTimer() {
        ActiveTimer activeTimer = createActiveTimer();
        // Note: Running state is determined by timerEnd vs current time
        activeTimer.timerEnd = LocalDateTime.now().plusMinutes(5);
        return activeTimer;
    }

    public static ActiveTimer createPausedTimer() {
        ActiveTimer activeTimer = createActiveTimer();
        // Note: Paused state is determined by remainingDurationWhenPaused
        activeTimer.remainingDurationWhenPaused = Duration.ofMinutes(3);
        return activeTimer;
    }

    public static ActiveTimer createSharedTimer(String... sharedUsers) {
        ActiveTimer activeTimer = createActiveTimer();
        // Include creator in sharedWithString to match Timer behavior
        // Creator should always be first in the list
        List<String> allSharedUsers = new ArrayList<>();
        allSharedUsers.add("test-user"); // Add creator first
        allSharedUsers.addAll(Arrays.asList(sharedUsers)); // Add other users
        activeTimer.sharedWithString = String.join(",", allSharedUsers);
        return activeTimer;
    }

    public static ActiveTimer createTaggedTimer(String... tags) {
        ActiveTimer activeTimer = createActiveTimer();
        activeTimer.tagsString = String.join(",", tags);
        return activeTimer;
    }

    // User data
    public static String createTestUserId() {
        return "test-user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String createTestDeviceId() {
        return "test-device-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String createTestAuthToken() {
        return "test-auth-token-" + UUID.randomUUID().toString().substring(0, 16);
    }

    // WebSocket data
    public static String createTestWebSocketMessage() {
        return "{\"type\":\"timer_update\",\"data\":{\"timerId\":\"" + UUID.randomUUID().toString() + "\"}}";
    }

    public static String createTestWebSocketMessage(String type, String data) {
        return "{\"type\":\"" + type + "\",\"data\":" + data + "}";
    }

    // Duration utilities
    public static Duration createRandomDuration() {
        long[] durations = {30, 60, 300, 600, 1800, 3600}; // 30s, 1m, 5m, 10m, 30m, 1h
        int index = (int) (Math.random() * durations.length);
        return Duration.ofSeconds(durations[index]);
    }

    public static Duration createShortDuration() {
        return Duration.ofSeconds(30);
    }

    public static Duration createMediumDuration() {
        return Duration.ofMinutes(5);
    }

    public static Duration createLongDuration() {
        return Duration.ofHours(1);
    }

    // Tag utilities
    public static Set<String> createWorkTags() {
        return new HashSet<>(Arrays.asList("work", "pomodoro", "focus"));
    }

    public static Set<String> createBreakTags() {
        return new HashSet<>(Arrays.asList("break", "rest", "relax"));
    }

    public static Set<String> createPersonalTags() {
        return new HashSet<>(Arrays.asList("personal", "health", "exercise"));
    }

    public static Set<String> createEmptyTags() {
        return new HashSet<>();
    }

    // Shared user utilities
    public static Set<String> createSharedUsers() {
        return new HashSet<>(Arrays.asList("user1", "user2", "user3"));
    }

    public static Set<String> createEmptySharedUsers() {
        return new HashSet<>();
    }

    // Timer name utilities
    public static String createTimerName(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 4);
    }

    public static String createWorkTimerName() {
        return createTimerName("Work");
    }

    public static String createBreakTimerName() {
        return createTimerName("Break");
    }

    public static String createPersonalTimerName() {
        return createTimerName("Personal");
    }
} 