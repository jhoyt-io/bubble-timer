package io.jhoyt.bubbletimer.util;

import org.junit.Test;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.db.ActiveTimer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class TestFixtureBuilderTest {

    @Test
    public void testBuildTimer() {
        Timer timer = new TestFixtureBuilder()
                .withUserId("test-user")
                .withTimerName("Test Timer")
                .withDurationMinutes(10)
                .withTags("work", "important")
                .withSharedUsers("user1", "user2")
                .buildTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct user ID", "test-user", timer.getUserId());
        assertEquals("Should have correct name", "Test Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofMinutes(10), timer.getTotalDuration());
        assertTrue("Should contain work tag", timer.getTags().contains("work"));
        assertTrue("Should contain important tag", timer.getTags().contains("important"));
        assertTrue("Should contain user1", timer.getSharedWith().contains("user1"));
        assertTrue("Should contain user2", timer.getSharedWith().contains("user2"));
    }

    @Test
    public void testBuildActiveTimer() {
        ActiveTimer activeTimer = new TestFixtureBuilder()
                .withUserId("test-user")
                .withTimerName("Test Active Timer")
                .withDurationMinutes(15)
                .withTags("work", "important")
                .withSharedUsers("user1", "user2")
                .asRunning()
                .buildActiveTimer();
        
        assertNotNull("ActiveTimer should not be null", activeTimer);
        assertEquals("Should have correct user ID", "test-user", activeTimer.userId);
        assertEquals("Should have correct name", "Test Active Timer", activeTimer.name);
        assertEquals("Should have correct total duration", Duration.ofMinutes(15), activeTimer.totalDuration);
        assertEquals("Should have correct remaining duration", Duration.ofMinutes(15), activeTimer.remainingDurationWhenPaused);
        assertNotNull("Should have timer end", activeTimer.timerEnd);
        // Creator is automatically included, so we expect "test-user,user1,user2"
        assertEquals("Should have correct shared with string", "test-user,user1,user2", activeTimer.sharedWithString);
        assertTrue("Should contain work tag", activeTimer.tagsString.contains("work"));
        assertTrue("Should contain important tag", activeTimer.tagsString.contains("important"));
    }

    @Test
    public void testWorkTimer() {
        Timer timer = TestFixtureBuilder.workTimer().buildTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Work Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofMinutes(25), timer.getTotalDuration());
        assertTrue("Should contain work tag", timer.getTags().contains("work"));
        assertTrue("Should contain pomodoro tag", timer.getTags().contains("pomodoro"));
        assertTrue("Should contain focus tag", timer.getTags().contains("focus"));
    }

    @Test
    public void testBreakTimer() {
        Timer timer = TestFixtureBuilder.breakTimer().buildTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Break Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofMinutes(5), timer.getTotalDuration());
        assertTrue("Should contain break tag", timer.getTags().contains("break"));
        assertTrue("Should contain rest tag", timer.getTags().contains("rest"));
        assertTrue("Should contain relax tag", timer.getTags().contains("relax"));
    }

    @Test
    public void testShortTimer() {
        Timer timer = TestFixtureBuilder.shortTimer().buildTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Short Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofSeconds(30), timer.getTotalDuration());
    }

    @Test
    public void testLongTimer() {
        Timer timer = TestFixtureBuilder.longTimer().buildTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Long Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofHours(2), timer.getTotalDuration());
    }

    @Test
    public void testSharedTimer() {
        Timer timer = TestFixtureBuilder.sharedTimer().buildTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Shared Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofMinutes(10), timer.getTotalDuration());
        assertTrue("Should contain user1", timer.getSharedWith().contains("user1"));
        assertTrue("Should contain user2", timer.getSharedWith().contains("user2"));
        assertTrue("Should contain user3", timer.getSharedWith().contains("user3"));
    }

    @Test
    public void testRunningTimer() {
        ActiveTimer activeTimer = TestFixtureBuilder.runningTimer().buildActiveTimer();
        
        assertNotNull("ActiveTimer should not be null", activeTimer);
        assertEquals("Should have correct name", "Running Timer", activeTimer.name);
        assertNotNull("Should have timer end", activeTimer.timerEnd);
        assertTrue("Should be in the future", activeTimer.timerEnd.isAfter(LocalDateTime.now()));
    }

    @Test
    public void testPausedTimer() {
        ActiveTimer activeTimer = TestFixtureBuilder.pausedTimer().buildActiveTimer();
        
        assertNotNull("ActiveTimer should not be null", activeTimer);
        assertEquals("Should have correct name", "Paused Timer", activeTimer.name);
        assertEquals("Should have remaining duration", Duration.ofMinutes(5), activeTimer.remainingDurationWhenPaused);
    }

    @Test
    public void testCreateMultipleTimers() {
        List<Timer> timers = TestFixtureBuilder.createMultipleTimers();
        
        assertNotNull("Timers list should not be null", timers);
        assertEquals("Should have 7 timers", 7, timers.size());
        
        // Check work timer
        assertEquals("First timer should be work timer", "Work Timer", timers.get(0).getName());
        assertTrue("Work timer should have work tag", timers.get(0).getTags().contains("work"));
        
        // Check break timer
        assertEquals("Second timer should be break timer", "Break Timer", timers.get(1).getName());
        assertTrue("Break timer should have break tag", timers.get(1).getTags().contains("break"));
        
        // Check short timer
        assertEquals("Third timer should be short timer", "Short Timer", timers.get(2).getName());
        assertEquals("Short timer should have 30 seconds", Duration.ofSeconds(30), timers.get(2).getTotalDuration());
        
        // Check long timer
        assertEquals("Fourth timer should be long timer", "Long Timer", timers.get(3).getName());
        assertEquals("Long timer should have 2 hours", Duration.ofHours(2), timers.get(3).getTotalDuration());
        
        // Check shared timer
        assertEquals("Fifth timer should be shared timer", "Shared Timer", timers.get(4).getName());
        assertTrue("Shared timer should have shared users", timers.get(4).getSharedWith().size() > 0);
        
        // Check running timer
        assertEquals("Sixth timer should be running timer", "Running Timer", timers.get(5).getName());
        
        // Check paused timer
        assertEquals("Seventh timer should be paused timer", "Paused Timer", timers.get(6).getName());
    }

    @Test
    public void testCreateMultipleActiveTimers() {
        List<ActiveTimer> activeTimers = TestFixtureBuilder.createMultipleActiveTimers();
        
        assertNotNull("ActiveTimers list should not be null", activeTimers);
        assertEquals("Should have 7 active timers", 7, activeTimers.size());
        
        // Check work timer
        assertEquals("First timer should be work timer", "Work Timer", activeTimers.get(0).name);
        assertTrue("Work timer should have work tag", activeTimers.get(0).tagsString.contains("work"));
        
        // Check break timer
        assertEquals("Second timer should be break timer", "Break Timer", activeTimers.get(1).name);
        assertTrue("Break timer should have break tag", activeTimers.get(1).tagsString.contains("break"));
        
        // Check short timer
        assertEquals("Third timer should be short timer", "Short Timer", activeTimers.get(2).name);
        assertEquals("Short timer should have 30 seconds", Duration.ofSeconds(30), activeTimers.get(2).totalDuration);
        
        // Check long timer
        assertEquals("Fourth timer should be long timer", "Long Timer", activeTimers.get(3).name);
        assertEquals("Long timer should have 2 hours", Duration.ofHours(2), activeTimers.get(3).totalDuration);
        
        // Check shared timer
        assertEquals("Fifth timer should be shared timer", "Shared Timer", activeTimers.get(4).name);
        assertTrue("Shared timer should have shared users", activeTimers.get(4).sharedWithString.length() > 0);
        
        // Check running timer
        assertEquals("Sixth timer should be running timer", "Running Timer", activeTimers.get(5).name);
        assertNotNull("Running timer should have timer end", activeTimers.get(5).timerEnd);
        
        // Check paused timer
        assertEquals("Seventh timer should be paused timer", "Paused Timer", activeTimers.get(6).name);
        assertNotNull("Paused timer should have remaining duration", activeTimers.get(6).remainingDurationWhenPaused);
    }

    @Test
    public void testCreateScenario() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario();
        
        assertNotNull("Scenario should not be null", scenario);
        assertEquals("Should start with 0 timers", 0, scenario.getTimerCount());
    }

    @Test
    public void testAddWorkTimer() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addWorkTimer();
        
        assertEquals("Should have 1 timer", 1, scenario.getTimerCount());
        assertEquals("Should be work timer", "Work Timer", scenario.getTimer(0).getName());
        assertTrue("Should have work tag", scenario.getTimer(0).getTags().contains("work"));
    }

    @Test
    public void testAddBreakTimer() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addBreakTimer();
        
        assertEquals("Should have 1 timer", 1, scenario.getTimerCount());
        assertEquals("Should be break timer", "Break Timer", scenario.getTimer(0).getName());
        assertTrue("Should have break tag", scenario.getTimer(0).getTags().contains("break"));
    }

    @Test
    public void testAddShortTimer() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addShortTimer();
        
        assertEquals("Should have 1 timer", 1, scenario.getTimerCount());
        assertEquals("Should be short timer", "Short Timer", scenario.getTimer(0).getName());
        assertEquals("Should have 30 seconds", Duration.ofSeconds(30), scenario.getTimer(0).getTotalDuration());
    }

    @Test
    public void testAddLongTimer() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addLongTimer();
        
        assertEquals("Should have 1 timer", 1, scenario.getTimerCount());
        assertEquals("Should be long timer", "Long Timer", scenario.getTimer(0).getName());
        assertEquals("Should have 2 hours", Duration.ofHours(2), scenario.getTimer(0).getTotalDuration());
    }

    @Test
    public void testAddSharedTimer() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addSharedTimer();
        
        assertEquals("Should have 1 timer", 1, scenario.getTimerCount());
        assertEquals("Should be shared timer", "Shared Timer", scenario.getTimer(0).getName());
        assertTrue("Should have shared users", scenario.getTimer(0).getSharedWith().size() > 0);
    }

    @Test
    public void testAddRunningTimer() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addRunningTimer();
        
        assertEquals("Should have 1 timer", 1, scenario.getTimerCount());
        assertEquals("Should be running timer", "Running Timer", scenario.getTimer(0).getName());
    }

    @Test
    public void testAddPausedTimer() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addPausedTimer();
        
        assertEquals("Should have 1 timer", 1, scenario.getTimerCount());
        assertEquals("Should be paused timer", "Paused Timer", scenario.getTimer(0).getName());
    }

    @Test
    public void testAddCustomTimer() {
        TestFixtureBuilder customBuilder = new TestFixtureBuilder()
                .withTimerName("Custom Timer")
                .withDurationMinutes(45)
                .withTags("custom", "test");
        
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addCustomTimer(customBuilder);
        
        assertEquals("Should have 1 timer", 1, scenario.getTimerCount());
        assertEquals("Should be custom timer", "Custom Timer", scenario.getTimer(0).getName());
        assertEquals("Should have 45 minutes", Duration.ofMinutes(45), scenario.getTimer(0).getTotalDuration());
        assertTrue("Should have custom tag", scenario.getTimer(0).getTags().contains("custom"));
        assertTrue("Should have test tag", scenario.getTimer(0).getTags().contains("test"));
    }

    @Test
    public void testCreateWorkSessionScenario() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createWorkSessionScenario();
        
        assertEquals("Should have 5 timers", 5, scenario.getTimerCount());
        
        // Check pattern: work, break, work, break, work
        assertEquals("First timer should be work timer", "Work Timer", scenario.getTimer(0).getName());
        assertEquals("Second timer should be break timer", "Break Timer", scenario.getTimer(1).getName());
        assertEquals("Third timer should be work timer", "Work Timer", scenario.getTimer(2).getName());
        assertEquals("Fourth timer should be break timer", "Break Timer", scenario.getTimer(3).getName());
        assertEquals("Fifth timer should be work timer", "Work Timer", scenario.getTimer(4).getName());
    }

    @Test
    public void testCreateStressTestScenario() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createStressTestScenario();
        
        assertEquals("Should have 10 timers", 10, scenario.getTimerCount());
        
        // Check that all timers have stress and test tags
        for (int i = 0; i < scenario.getTimerCount(); i++) {
            Timer timer = scenario.getTimer(i);
            assertTrue("Timer " + i + " should have stress tag", timer.getTags().contains("stress"));
            assertTrue("Timer " + i + " should have test tag", timer.getTags().contains("test"));
            assertTrue("Timer " + i + " should have correct name", timer.getName().startsWith("Stress Timer "));
        }
    }

    @Test
    public void testReset() {
        TestFixtureBuilder builder = new TestFixtureBuilder()
                .withUserId("custom-user")
                .withTimerName("Custom Timer")
                .withDurationMinutes(30)
                .withTags("custom")
                .withSharedUsers("user1")
                .asRunning();
        
        Timer timer1 = builder.buildTimer();
        assertEquals("Should have custom user ID", "custom-user", timer1.getUserId());
        assertEquals("Should have custom name", "Custom Timer", timer1.getName());
        
        builder.reset();
        Timer timer2 = builder.buildTimer();
        assertEquals("Should have default user ID", "test-user", timer2.getUserId());
        assertEquals("Should have default name", "Test Timer", timer2.getName());
    }

    @Test
    public void testGetTimers() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addWorkTimer()
                .addBreakTimer();
        
        List<Timer> timers = scenario.getTimers();
        
        assertNotNull("Timers list should not be null", timers);
        assertEquals("Should have 2 timers", 2, timers.size());
        assertEquals("First timer should be work timer", "Work Timer", timers.get(0).getName());
        assertEquals("Second timer should be break timer", "Break Timer", timers.get(1).getName());
    }

    @Test
    public void testGetActiveTimers() {
        TestFixtureBuilder.TimerScenario scenario = TestFixtureBuilder.createScenario()
                .addWorkTimer()
                .addBreakTimer();
        
        List<ActiveTimer> activeTimers = scenario.getActiveTimers();
        
        assertNotNull("ActiveTimers list should not be null", activeTimers);
        assertEquals("Should have 2 active timers", 2, activeTimers.size());
        assertEquals("First timer should be work timer", "Work Timer", activeTimers.get(0).name);
        assertEquals("Second timer should be break timer", "Break Timer", activeTimers.get(1).name);
    }
} 