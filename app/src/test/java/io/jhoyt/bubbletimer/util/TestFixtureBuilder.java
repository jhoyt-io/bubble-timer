package io.jhoyt.bubbletimer.util;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.db.ActiveTimer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Builder class for creating complex test fixtures and scenarios.
 */
public class TestFixtureBuilder {

    private String userId = "test-user";
    private String timerName = "Test Timer";
    private Duration duration = Duration.ofMinutes(5);
    private Set<String> tags = new HashSet<>();
    private Set<String> sharedUsers = new HashSet<>();
    private boolean isRunning = false;
    private boolean isPaused = false;
    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime pauseTime = null;

    /**
     * Set the user ID for the test fixture
     */
    public TestFixtureBuilder withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Set the timer name for the test fixture
     */
    public TestFixtureBuilder withTimerName(String timerName) {
        this.timerName = timerName;
        return this;
    }

    /**
     * Set the duration for the test fixture
     */
    public TestFixtureBuilder withDuration(Duration duration) {
        this.duration = duration;
        return this;
    }

    /**
     * Set the duration in minutes for the test fixture
     */
    public TestFixtureBuilder withDurationMinutes(int minutes) {
        this.duration = Duration.ofMinutes(minutes);
        return this;
    }

    /**
     * Set the duration in seconds for the test fixture
     */
    public TestFixtureBuilder withDurationSeconds(int seconds) {
        this.duration = Duration.ofSeconds(seconds);
        return this;
    }

    /**
     * Set the duration in hours for the test fixture
     */
    public TestFixtureBuilder withDurationHours(int hours) {
        this.duration = Duration.ofHours(hours);
        return this;
    }

    /**
     * Add tags to the test fixture
     */
    public TestFixtureBuilder withTags(String... tags) {
        for (String tag : tags) {
            this.tags.add(tag);
        }
        return this;
    }

    /**
     * Add shared users to the test fixture
     */
    public TestFixtureBuilder withSharedUsers(String... sharedUsers) {
        for (String user : sharedUsers) {
            this.sharedUsers.add(user);
        }
        return this;
    }

    /**
     * Set the timer as running
     */
    public TestFixtureBuilder asRunning() {
        this.isRunning = true;
        this.isPaused = false;
        return this;
    }

    /**
     * Set the timer as paused
     */
    public TestFixtureBuilder asPaused() {
        this.isRunning = false;
        this.isPaused = true;
        this.pauseTime = LocalDateTime.now();
        return this;
    }

    /**
     * Set the timer as stopped
     */
    public TestFixtureBuilder asStopped() {
        this.isRunning = false;
        this.isPaused = false;
        return this;
    }

    /**
     * Set the start time for the test fixture
     */
    public TestFixtureBuilder withStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    /**
     * Set the pause time for the test fixture
     */
    public TestFixtureBuilder withPauseTime(LocalDateTime pauseTime) {
        this.pauseTime = pauseTime;
        return this;
    }

    /**
     * Build a Timer object
     */
    public Timer buildTimer() {
        Timer timer = new Timer(userId, timerName, duration, tags);
        for (String user : sharedUsers) {
            timer.shareWith(user);
        }
        return timer;
    }

    /**
     * Build an ActiveTimer object
     */
    public ActiveTimer buildActiveTimer() {
        ActiveTimer activeTimer = new ActiveTimer();
        activeTimer.id = UUID.randomUUID().toString();
        activeTimer.userId = userId;
        activeTimer.name = timerName;
        activeTimer.totalDuration = duration;
        activeTimer.remainingDurationWhenPaused = duration;
        activeTimer.timerEnd = startTime.plus(duration);
        
        // Include creator in sharedWithString to match Timer behavior
        // Creator should always be first in the list
        List<String> allSharedUsers = new ArrayList<>();
        allSharedUsers.add(userId); // Add creator first
        allSharedUsers.addAll(sharedUsers); // Add other users
        activeTimer.sharedWithString = String.join(",", allSharedUsers);
        
        activeTimer.tagsString = String.join(",", tags);
        return activeTimer;
    }

    /**
     * Reset the builder to default values
     */
    public TestFixtureBuilder reset() {
        this.userId = "test-user";
        this.timerName = "Test Timer";
        this.duration = Duration.ofMinutes(5);
        this.tags = new HashSet<>();
        this.sharedUsers = new HashSet<>();
        this.isRunning = false;
        this.isPaused = false;
        this.startTime = LocalDateTime.now();
        this.pauseTime = null;
        return this;
    }

    /**
     * Create a builder for a work timer
     */
    public static TestFixtureBuilder workTimer() {
        return new TestFixtureBuilder()
                .withTimerName("Work Timer")
                .withDurationMinutes(25)
                .withTags("work", "pomodoro", "focus");
    }

    /**
     * Create a builder for a break timer
     */
    public static TestFixtureBuilder breakTimer() {
        return new TestFixtureBuilder()
                .withTimerName("Break Timer")
                .withDurationMinutes(5)
                .withTags("break", "rest", "relax");
    }

    /**
     * Create a builder for a short timer
     */
    public static TestFixtureBuilder shortTimer() {
        return new TestFixtureBuilder()
                .withTimerName("Short Timer")
                .withDurationSeconds(30);
    }

    /**
     * Create a builder for a long timer
     */
    public static TestFixtureBuilder longTimer() {
        return new TestFixtureBuilder()
                .withTimerName("Long Timer")
                .withDurationHours(2);
    }

    /**
     * Create a builder for a shared timer
     */
    public static TestFixtureBuilder sharedTimer() {
        return new TestFixtureBuilder()
                .withTimerName("Shared Timer")
                .withDurationMinutes(10)
                .withSharedUsers("user1", "user2", "user3");
    }

    /**
     * Create a builder for a running timer
     */
    public static TestFixtureBuilder runningTimer() {
        return new TestFixtureBuilder()
                .withTimerName("Running Timer")
                .withDurationMinutes(5)
                .asRunning();
    }

    /**
     * Create a builder for a paused timer
     */
    public static TestFixtureBuilder pausedTimer() {
        return new TestFixtureBuilder()
                .withTimerName("Paused Timer")
                .withDurationMinutes(5)
                .asPaused();
    }

    /**
     * Create a list of multiple timers
     */
    public static List<Timer> createMultipleTimers() {
        List<Timer> timers = new ArrayList<>();
        
        timers.add(workTimer().buildTimer());
        timers.add(breakTimer().buildTimer());
        timers.add(shortTimer().buildTimer());
        timers.add(longTimer().buildTimer());
        timers.add(sharedTimer().buildTimer());
        timers.add(runningTimer().buildTimer());
        timers.add(pausedTimer().buildTimer());
        
        return timers;
    }

    /**
     * Create a list of multiple active timers
     */
    public static List<ActiveTimer> createMultipleActiveTimers() {
        List<ActiveTimer> activeTimers = new ArrayList<>();
        
        activeTimers.add(workTimer().buildActiveTimer());
        activeTimers.add(breakTimer().buildActiveTimer());
        activeTimers.add(shortTimer().buildActiveTimer());
        activeTimers.add(longTimer().buildActiveTimer());
        activeTimers.add(sharedTimer().buildActiveTimer());
        activeTimers.add(runningTimer().buildActiveTimer());
        activeTimers.add(pausedTimer().buildActiveTimer());
        
        return activeTimers;
    }

    /**
     * Create a scenario with multiple timers of different types
     */
    public static class TimerScenario {
        private final List<Timer> timers = new ArrayList<>();
        private final List<ActiveTimer> activeTimers = new ArrayList<>();

        public TimerScenario addWorkTimer() {
            timers.add(workTimer().buildTimer());
            activeTimers.add(workTimer().buildActiveTimer());
            return this;
        }

        public TimerScenario addBreakTimer() {
            timers.add(breakTimer().buildTimer());
            activeTimers.add(breakTimer().buildActiveTimer());
            return this;
        }

        public TimerScenario addShortTimer() {
            timers.add(shortTimer().buildTimer());
            activeTimers.add(shortTimer().buildActiveTimer());
            return this;
        }

        public TimerScenario addLongTimer() {
            timers.add(longTimer().buildTimer());
            activeTimers.add(longTimer().buildActiveTimer());
            return this;
        }

        public TimerScenario addSharedTimer() {
            timers.add(sharedTimer().buildTimer());
            activeTimers.add(sharedTimer().buildActiveTimer());
            return this;
        }

        public TimerScenario addRunningTimer() {
            timers.add(runningTimer().buildTimer());
            activeTimers.add(runningTimer().buildActiveTimer());
            return this;
        }

        public TimerScenario addPausedTimer() {
            timers.add(pausedTimer().buildTimer());
            activeTimers.add(pausedTimer().buildActiveTimer());
            return this;
        }

        public TimerScenario addCustomTimer(TestFixtureBuilder builder) {
            timers.add(builder.buildTimer());
            activeTimers.add(builder.buildActiveTimer());
            return this;
        }

        public List<Timer> getTimers() {
            return new ArrayList<>(timers);
        }

        public List<ActiveTimer> getActiveTimers() {
            return new ArrayList<>(activeTimers);
        }

        public Timer getTimer(int index) {
            return timers.get(index);
        }

        public ActiveTimer getActiveTimer(int index) {
            return activeTimers.get(index);
        }

        public int getTimerCount() {
            return timers.size();
        }
    }

    /**
     * Create a new timer scenario
     */
    public static TimerScenario createScenario() {
        return new TimerScenario();
    }

    /**
     * Create a typical work session scenario
     */
    public static TimerScenario createWorkSessionScenario() {
        return createScenario()
                .addWorkTimer()
                .addBreakTimer()
                .addWorkTimer()
                .addBreakTimer()
                .addWorkTimer();
    }

    /**
     * Create a stress test scenario with many timers
     */
    public static TimerScenario createStressTestScenario() {
        TimerScenario scenario = createScenario();
        for (int i = 0; i < 10; i++) {
            scenario.addCustomTimer(
                new TestFixtureBuilder()
                    .withTimerName("Stress Timer " + i)
                    .withDurationMinutes(i + 1)
                    .withTags("stress", "test")
            );
        }
        return scenario;
    }
} 