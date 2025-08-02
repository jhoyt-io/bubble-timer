package io.jhoyt.bubbletimer.util;

import org.junit.Test;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.db.ActiveTimer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

public class TestDataFactoryEnhancedTest {

    @Test
    public void testCreateTestTimer() {
        Timer timer = TestDataFactory.createTestTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct user ID", "test-user", timer.getUserId());
        assertEquals("Should have correct name", "Test Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofMinutes(5), timer.getTotalDuration());
        assertTrue("Should have empty tags", timer.getTags().isEmpty());
        assertTrue("Should have empty shared users", timer.getSharedWith().isEmpty());
    }

    @Test
    public void testCreateTestTimerWithParameters() {
        Timer timer = TestDataFactory.createTestTimer("custom-user", "Custom Timer", Duration.ofMinutes(10));
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct user ID", "custom-user", timer.getUserId());
        assertEquals("Should have correct name", "Custom Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofMinutes(10), timer.getTotalDuration());
    }

    @Test
    public void testCreateTestTimerWithTags() {
        Timer timer = TestDataFactory.createTestTimerWithTags("Tagged Timer", Duration.ofMinutes(15), "work", "important");
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Tagged Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofMinutes(15), timer.getTotalDuration());
        assertTrue("Should contain work tag", timer.getTags().contains("work"));
        assertTrue("Should contain important tag", timer.getTags().contains("important"));
        assertEquals("Should have 2 tags", 2, timer.getTags().size());
    }

    @Test
    public void testCreateTestTimerWithSharedUsers() {
        Timer timer = TestDataFactory.createTestTimerWithSharedUsers("Shared Timer", Duration.ofMinutes(20), "user1", "user2");
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Shared Timer", timer.getName());
        assertTrue("Should contain user1", timer.getSharedWith().contains("user1"));
        assertTrue("Should contain user2", timer.getSharedWith().contains("user2"));
        assertEquals("Should have 2 shared users", 2, timer.getSharedWith().size());
    }

    @Test
    public void testCreateShortTimer() {
        Timer timer = TestDataFactory.createShortTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Short Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofSeconds(30), timer.getTotalDuration());
    }

    @Test
    public void testCreateLongTimer() {
        Timer timer = TestDataFactory.createLongTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Long Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofHours(2), timer.getTotalDuration());
    }

    @Test
    public void testCreateWorkTimer() {
        Timer timer = TestDataFactory.createWorkTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Work Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofMinutes(25), timer.getTotalDuration());
        assertTrue("Should contain work tag", timer.getTags().contains("work"));
        assertTrue("Should contain pomodoro tag", timer.getTags().contains("pomodoro"));
    }

    @Test
    public void testCreateBreakTimer() {
        Timer timer = TestDataFactory.createBreakTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Break Timer", timer.getName());
        assertEquals("Should have correct duration", Duration.ofMinutes(5), timer.getTotalDuration());
        assertTrue("Should contain break tag", timer.getTags().contains("break"));
        assertTrue("Should contain rest tag", timer.getTags().contains("rest"));
    }

    @Test
    public void testCreateTimerWithSpecialName() {
        Timer timer = TestDataFactory.createTimerWithSpecialName();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Timer with @#$%^&*()", timer.getName());
    }

    @Test
    public void testCreateTimerWithUnicodeName() {
        Timer timer = TestDataFactory.createTimerWithUnicodeName();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name", "Timer with 用户", timer.getName());
    }

    @Test
    public void testCreateTimerWithLongName() {
        Timer timer = TestDataFactory.createTimerWithLongName();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have correct name length", 1000, timer.getName().length());
        assertTrue("Should start with A", timer.getName().startsWith("A"));
    }

    @Test
    public void testCreateTimerWithEmptyName() {
        Timer timer = TestDataFactory.createTimerWithEmptyName();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Should have empty name", "", timer.getName());
    }

    @Test
    public void testCreateTimerWithNullName() {
        Timer timer = TestDataFactory.createTimerWithNullName();
        
        assertNotNull("Timer should not be null", timer);
        assertNull("Should have null name", timer.getName());
    }

    @Test
    public void testCreateMultipleTimers() {
        Timer[] timers = TestDataFactory.createMultipleTimers();
        
        assertNotNull("Timers array should not be null", timers);
        assertEquals("Should have 5 timers", 5, timers.length);
        
        // Check first timer
        assertEquals("First timer should be Timer 1", "Timer 1", timers[0].getName());
        assertEquals("First timer should have 5 minutes", Duration.ofMinutes(5), timers[0].getTotalDuration());
        
        // Check work timer
        assertEquals("Work timer should have correct name", "Work Timer", timers[3].getName());
        assertTrue("Work timer should have work tag", timers[3].getTags().contains("work"));
    }

    @Test
    public void testCreateActiveTimer() {
        ActiveTimer activeTimer = TestDataFactory.createActiveTimer();
        
        assertNotNull("ActiveTimer should not be null", activeTimer);
        assertNotNull("Should have timer ID", activeTimer.id);
        assertEquals("Should have correct user ID", "test-user", activeTimer.userId);
        assertEquals("Should have correct name", "Test Active Timer", activeTimer.name);
        assertEquals("Should have correct total duration", Duration.ofMinutes(5), activeTimer.totalDuration);
        assertEquals("Should have correct remaining duration", Duration.ofMinutes(5), activeTimer.remainingDurationWhenPaused);
        assertNotNull("Should have timer end", activeTimer.timerEnd);
        assertEquals("Should have empty shared with string", "", activeTimer.sharedWithString);
        assertEquals("Should have empty tags string", "", activeTimer.tagsString);
    }

    @Test
    public void testCreateActiveTimerWithParameters() {
        ActiveTimer activeTimer = TestDataFactory.createActiveTimer("test-id", "Custom Active Timer", Duration.ofMinutes(10));
        
        assertNotNull("ActiveTimer should not be null", activeTimer);
        assertEquals("Should have correct timer ID", "test-id", activeTimer.id);
        assertEquals("Should have correct name", "Custom Active Timer", activeTimer.name);
        assertEquals("Should have correct total duration", Duration.ofMinutes(10), activeTimer.totalDuration);
        assertEquals("Should have correct remaining duration", Duration.ofMinutes(10), activeTimer.remainingDurationWhenPaused);
    }

    @Test
    public void testCreateRunningTimer() {
        ActiveTimer activeTimer = TestDataFactory.createRunningTimer();
        
        assertNotNull("ActiveTimer should not be null", activeTimer);
        assertNotNull("Should have timer end", activeTimer.timerEnd);
        assertTrue("Should be in the future", activeTimer.timerEnd.isAfter(LocalDateTime.now()));
    }

    @Test
    public void testCreatePausedTimer() {
        ActiveTimer activeTimer = TestDataFactory.createPausedTimer();
        
        assertNotNull("ActiveTimer should not be null", activeTimer);
        assertEquals("Should have remaining duration", Duration.ofMinutes(3), activeTimer.remainingDurationWhenPaused);
    }

    @Test
    public void testCreateSharedTimer() {
        ActiveTimer activeTimer = TestDataFactory.createSharedTimer("user1", "user2", "user3");
        
        assertNotNull("ActiveTimer should not be null", activeTimer);
        assertEquals("Should have correct shared with string", "user1,user2,user3", activeTimer.sharedWithString);
    }

    @Test
    public void testCreateTaggedTimer() {
        ActiveTimer activeTimer = TestDataFactory.createTaggedTimer("work", "important");
        
        assertNotNull("ActiveTimer should not be null", activeTimer);
        assertEquals("Should have correct tags string", "work,important", activeTimer.tagsString);
    }

    @Test
    public void testCreateTestUserId() {
        String userId = TestDataFactory.createTestUserId();
        
        assertNotNull("User ID should not be null", userId);
        assertTrue("Should start with test-user-", userId.startsWith("test-user-"));
        assertEquals("Should have correct length", 18, userId.length());
    }

    @Test
    public void testCreateTestDeviceId() {
        String deviceId = TestDataFactory.createTestDeviceId();
        
        assertNotNull("Device ID should not be null", deviceId);
        assertTrue("Should start with test-device-", deviceId.startsWith("test-device-"));
        assertEquals("Should have correct length", 20, deviceId.length());
    }

    @Test
    public void testCreateTestAuthToken() {
        String authToken = TestDataFactory.createTestAuthToken();
        
        assertNotNull("Auth token should not be null", authToken);
        assertTrue("Should start with test-auth-token-", authToken.startsWith("test-auth-token-"));
        assertEquals("Should have correct length", 32, authToken.length());
    }

    @Test
    public void testCreateTestWebSocketMessage() {
        String message = TestDataFactory.createTestWebSocketMessage();
        
        assertNotNull("Message should not be null", message);
        assertTrue("Should contain type field", message.contains("\"type\""));
        assertTrue("Should contain data field", message.contains("\"data\""));
        assertTrue("Should contain timer_update", message.contains("timer_update"));
    }

    @Test
    public void testCreateTestWebSocketMessageWithParameters() {
        String message = TestDataFactory.createTestWebSocketMessage("custom_type", "{\"custom\":\"data\"}");
        
        assertNotNull("Message should not be null", message);
        assertTrue("Should contain custom_type", message.contains("custom_type"));
        assertTrue("Should contain custom data", message.contains("{\"custom\":\"data\"}"));
    }

    @Test
    public void testCreateRandomDuration() {
        Duration duration = TestDataFactory.createRandomDuration();
        
        assertNotNull("Duration should not be null", duration);
        assertTrue("Should be positive", duration.toSeconds() > 0);
        assertTrue("Should be reasonable duration", duration.toHours() <= 1);
    }

    @Test
    public void testCreateShortDuration() {
        Duration duration = TestDataFactory.createShortDuration();
        
        assertEquals("Should be 30 seconds", Duration.ofSeconds(30), duration);
    }

    @Test
    public void testCreateMediumDuration() {
        Duration duration = TestDataFactory.createMediumDuration();
        
        assertEquals("Should be 5 minutes", Duration.ofMinutes(5), duration);
    }

    @Test
    public void testCreateLongDuration() {
        Duration duration = TestDataFactory.createLongDuration();
        
        assertEquals("Should be 1 hour", Duration.ofHours(1), duration);
    }

    @Test
    public void testCreateWorkTags() {
        Set<String> tags = TestDataFactory.createWorkTags();
        
        assertNotNull("Tags should not be null", tags);
        assertEquals("Should have 3 tags", 3, tags.size());
        assertTrue("Should contain work", tags.contains("work"));
        assertTrue("Should contain pomodoro", tags.contains("pomodoro"));
        assertTrue("Should contain focus", tags.contains("focus"));
    }

    @Test
    public void testCreateBreakTags() {
        Set<String> tags = TestDataFactory.createBreakTags();
        
        assertNotNull("Tags should not be null", tags);
        assertEquals("Should have 3 tags", 3, tags.size());
        assertTrue("Should contain break", tags.contains("break"));
        assertTrue("Should contain rest", tags.contains("rest"));
        assertTrue("Should contain relax", tags.contains("relax"));
    }

    @Test
    public void testCreatePersonalTags() {
        Set<String> tags = TestDataFactory.createPersonalTags();
        
        assertNotNull("Tags should not be null", tags);
        assertEquals("Should have 3 tags", 3, tags.size());
        assertTrue("Should contain personal", tags.contains("personal"));
        assertTrue("Should contain health", tags.contains("health"));
        assertTrue("Should contain exercise", tags.contains("exercise"));
    }

    @Test
    public void testCreateEmptyTags() {
        Set<String> tags = TestDataFactory.createEmptyTags();
        
        assertNotNull("Tags should not be null", tags);
        assertTrue("Should be empty", tags.isEmpty());
    }

    @Test
    public void testCreateSharedUsers() {
        Set<String> users = TestDataFactory.createSharedUsers();
        
        assertNotNull("Users should not be null", users);
        assertEquals("Should have 3 users", 3, users.size());
        assertTrue("Should contain user1", users.contains("user1"));
        assertTrue("Should contain user2", users.contains("user2"));
        assertTrue("Should contain user3", users.contains("user3"));
    }

    @Test
    public void testCreateEmptySharedUsers() {
        Set<String> users = TestDataFactory.createEmptySharedUsers();
        
        assertNotNull("Users should not be null", users);
        assertTrue("Should be empty", users.isEmpty());
    }

    @Test
    public void testCreateTimerName() {
        String name = TestDataFactory.createTimerName("Test");
        
        assertNotNull("Name should not be null", name);
        assertTrue("Should start with Test", name.startsWith("Test "));
        assertEquals("Should have correct length", 9, name.length());
    }

    @Test
    public void testCreateWorkTimerName() {
        String name = TestDataFactory.createWorkTimerName();
        
        assertNotNull("Name should not be null", name);
        assertTrue("Should start with Work", name.startsWith("Work "));
    }

    @Test
    public void testCreateBreakTimerName() {
        String name = TestDataFactory.createBreakTimerName();
        
        assertNotNull("Name should not be null", name);
        assertTrue("Should start with Break", name.startsWith("Break "));
    }

    @Test
    public void testCreatePersonalTimerName() {
        String name = TestDataFactory.createPersonalTimerName();
        
        assertNotNull("Name should not be null", name);
        assertTrue("Should start with Personal", name.startsWith("Personal "));
    }
} 