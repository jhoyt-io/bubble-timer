package io.jhoyt.bubbletimer.util;

import org.junit.Test;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.TimerData;

import java.time.Duration;
import java.util.Set;

public class TestDataFactoryTest {

    @Test
    public void testCreateTestTimer() {
        Timer timer = TestDataFactory.createTestTimer();
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("User ID should be 'test-user'", "test-user", timer.getUserId());
        assertEquals("Name should be 'Test Timer'", "Test Timer", timer.getName());
        assertEquals("Duration should be 5 minutes", Duration.ofMinutes(5), timer.getTimerData().totalDuration);
        assertTrue("Tags should be empty", timer.getTimerData().tags.isEmpty());
    }

    @Test
    public void testCreateTestTimerWithCustomName() {
        Timer timer = TestDataFactory.createTestTimer("Custom Timer", Duration.ofMinutes(10));
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("User ID should be 'test-user'", "test-user", timer.getUserId());
        assertEquals("Name should be 'Custom Timer'", "Custom Timer", timer.getName());
        assertEquals("Duration should be 10 minutes", Duration.ofMinutes(10), timer.getTimerData().totalDuration);
        assertTrue("Tags should be empty", timer.getTimerData().tags.isEmpty());
    }

    @Test
    public void testCreateTestTimerWithDifferentDurations() {
        Timer shortTimer = TestDataFactory.createTestTimer("Short Timer", Duration.ofSeconds(30));
        Timer longTimer = TestDataFactory.createTestTimer("Long Timer", Duration.ofHours(2));
        
        assertEquals("Short timer duration should be 30 seconds", Duration.ofSeconds(30), shortTimer.getTimerData().totalDuration);
        assertEquals("Long timer duration should be 2 hours", Duration.ofHours(2), longTimer.getTimerData().totalDuration);
    }

    @Test
    public void testCreateTestTimerWithEmptyName() {
        Timer timer = TestDataFactory.createTestTimer("", Duration.ofMinutes(5));
        
        assertNotNull("Timer should not be null", timer);
        assertEquals("Name should be empty string", "", timer.getName());
    }

    @Test
    public void testCreateTestTimerWithNullName() {
        Timer timer = TestDataFactory.createTestTimer(null, Duration.ofMinutes(5));
        
        assertNotNull("Timer should not be null", timer);
        // The factory provides a default name when null is passed
        assertEquals("Should have default name", "Unknown Timer", timer.getName());
    }

    @Test
    public void testCreateTestTimerWithNullDuration() {
        Timer timer = TestDataFactory.createTestTimer("Test Timer", null);
        
        assertNotNull("Timer should not be null", timer);
        // The factory provides a default duration when null is passed
        assertEquals("Should have default duration", Duration.ZERO, timer.getTimerData().totalDuration);
    }

    @Test
    public void testTimerDataProperties() {
        Timer timer = TestDataFactory.createTestTimer();
        TimerData timerData = timer.getTimerData();
        
        assertNotNull("TimerData should not be null", timerData);
        assertEquals("Total duration should be 5 minutes", Duration.ofMinutes(5), timerData.totalDuration);
        assertEquals("Remaining duration should equal total duration initially", Duration.ofMinutes(5), timerData.remainingDurationWhenPaused);
        assertNull("Timer end should be null initially", timerData.timerEnd);
        assertTrue("Tags should be empty initially", timerData.tags.isEmpty());
    }

    @Test
    public void testMultipleTimerCreation() {
        Timer timer1 = TestDataFactory.createTestTimer();
        Timer timer2 = TestDataFactory.createTestTimer();
        
        assertNotNull("First timer should not be null", timer1);
        assertNotNull("Second timer should not be null", timer2);
        
        // Both timers should have the same default values
        assertEquals("Both timers should have same user ID", timer1.getUserId(), timer2.getUserId());
        assertEquals("Both timers should have same name", timer1.getName(), timer2.getName());
        assertEquals("Both timers should have same duration", timer1.getTimerData().totalDuration, timer2.getTimerData().totalDuration);
    }

    @Test
    public void testTimerImmutability() {
        Timer timer = TestDataFactory.createTestTimer();
        TimerData originalData = timer.getTimerData();
        
        // Verify that the timer data is properly structured
        assertNotNull("Timer data should not be null", originalData);
        assertEquals("Total duration should be 5 minutes", Duration.ofMinutes(5), originalData.totalDuration);
        
        // Test that we can create multiple timers without interference
        Timer timer2 = TestDataFactory.createTestTimer("Another Timer", Duration.ofMinutes(15));
        TimerData data2 = timer2.getTimerData();
        
        assertNotNull("Second timer data should not be null", data2);
        assertEquals("Second timer should have different duration", Duration.ofMinutes(15), data2.totalDuration);
        assertEquals("First timer should still have original duration", Duration.ofMinutes(5), originalData.totalDuration);
    }
} 