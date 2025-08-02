package io.jhoyt.bubbletimer.repository;

import org.junit.Test;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.db.ActiveTimer;

import java.time.Duration;

public class ActiveTimerRepositoryTest {

    @Test
    public void testActiveTimerCreation() {
        ActiveTimer timer = new ActiveTimer();
        timer.id = "test-id";
        timer.name = "Test Timer";
        timer.userId = "test-user";
        timer.totalDuration = Duration.ofMinutes(5);
        timer.remainingDurationWhenPaused = Duration.ofMinutes(2);
        
        assertEquals("ID should match", "test-id", timer.id);
        assertEquals("Name should match", "Test Timer", timer.name);
        assertEquals("User ID should match", "test-user", timer.userId);
        assertEquals("Total duration should match", Duration.ofMinutes(5), timer.totalDuration);
        assertEquals("Remaining duration should match", Duration.ofMinutes(2), timer.remainingDurationWhenPaused);
    }


} 