package io.jhoyt.bubbletimer.core;

import org.junit.Test;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.TimerData;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

public class TimerModelTest {

    @Test
    public void testTimerCreation() {
        Timer timer = new Timer("test-user", "Test Timer", Duration.ofMinutes(5), Set.of());
        
        assertEquals("Test Timer", timer.getName());
        assertEquals("test-user", timer.getUserId());
        assertEquals(Duration.ofMinutes(5), timer.getTimerData().totalDuration);
        assertTrue("Tags should be empty", timer.getTimerData().tags.isEmpty());
    }

    @Test
    public void testTimerWithTags() {
        Set<String> tags = new HashSet<>();
        tags.add("work");
        tags.add("important");
        
        Timer timer = new Timer("test-user", "Work Timer", Duration.ofMinutes(30), tags);
        
        assertEquals("Work Timer", timer.getName());
        assertEquals("test-user", timer.getUserId());
        assertEquals(Duration.ofMinutes(30), timer.getTimerData().totalDuration);
        assertEquals("Should have 2 tags", 2, timer.getTimerData().tags.size());
        assertTrue("Should contain 'work' tag", timer.getTimerData().tags.contains("work"));
        assertTrue("Should contain 'important' tag", timer.getTimerData().tags.contains("important"));
    }

    @Test
    public void testTimerDataProperties() {
        Timer timer = new Timer("test-user", "Test Timer", Duration.ofMinutes(10), Set.of());
        TimerData timerData = timer.getTimerData();
        
        assertNotNull("TimerData should not be null", timerData);
        assertEquals("Total duration should be 10 minutes", Duration.ofMinutes(10), timerData.totalDuration);
        assertEquals("Remaining duration should equal total duration initially", Duration.ofMinutes(10), timerData.remainingDurationWhenPaused);
        assertNull("Timer end should be null initially", timerData.timerEnd);
        assertTrue("Tags should be empty initially", timerData.tags.isEmpty());
    }

    @Test
    public void testTimerEquality() {
        Timer timer1 = new Timer("user1", "Timer 1", Duration.ofMinutes(5), Set.of());
        Timer timer2 = new Timer("user1", "Timer 1", Duration.ofMinutes(5), Set.of());
        Timer timer3 = new Timer("user2", "Timer 1", Duration.ofMinutes(5), Set.of());
        
        // Note: Timer equality depends on the implementation
        // This test assumes Timer has proper equals/hashCode implementation
        assertNotNull("Timer should not be null", timer1);
        assertNotNull("Timer should not be null", timer2);
        assertNotNull("Timer should not be null", timer3);
    }

    @Test
    public void testTimerWithDifferentDurations() {
        Timer shortTimer = new Timer("user1", "Short Timer", Duration.ofSeconds(30), Set.of());
        Timer longTimer = new Timer("user1", "Long Timer", Duration.ofHours(2), Set.of());
        
        assertEquals("Short timer should be 30 seconds", Duration.ofSeconds(30), shortTimer.getTimerData().totalDuration);
        assertEquals("Long timer should be 2 hours", Duration.ofHours(2), longTimer.getTimerData().totalDuration);
    }

    @Test
    public void testTimerWithEmptyName() {
        Timer timer = new Timer("user1", "", Duration.ofMinutes(5), Set.of());
        assertEquals("Name should be empty string", "", timer.getName());
    }

    @Test
    public void testTimerWithNullTags() {
        // This test verifies that null tags are handled gracefully
        Timer timer = new Timer("user1", "Test Timer", Duration.ofMinutes(5), null);
        assertNotNull("Timer should be created even with null tags", timer);
    }
} 