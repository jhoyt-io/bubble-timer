package io.jhoyt.bubbletimer.core;

import org.junit.Test;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.Timer;

import java.time.Duration;
import java.util.Set;

public class TimerTest {

    @Test
    public void testTimerCreation() {
        Timer timer = new Timer("test-user", "Test Timer", Duration.ofMinutes(5), Set.of());
        
        assertEquals("Test Timer", timer.getName());
        assertEquals("test-user", timer.getUserId());
        assertEquals(Duration.ofMinutes(5), timer.getTimerData().totalDuration);
    }
} 