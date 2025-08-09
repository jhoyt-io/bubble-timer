package io.jhoyt.bubbletimer;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.jhoyt.bubbletimer.db.ActiveTimerRepository;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActiveTimerViewModel with focus on duration handling.
 * These tests specifically verify that timer durations with seconds are preserved correctly.
 */
@RunWith(AndroidJUnit4.class)
public class ActiveTimerViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private ActiveTimerRepository mockRepository;

    @Mock
    private Context mockContext;

    @Mock
    private Observer<Set<Timer>> activeTimersObserver;

    @Mock
    private Observer<Timer> primaryTimerObserver;

    @Mock
    private Observer<Boolean> loadingObserver;

    private ActiveTimerViewModel activeTimerViewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock the repository getAllActiveTimers to return an empty LiveData
        androidx.lifecycle.MutableLiveData<java.util.List<io.jhoyt.bubbletimer.Timer>> mockLiveData = 
            new androidx.lifecycle.MutableLiveData<>();
        when(mockRepository.getAllActiveTimers()).thenReturn(mockLiveData);
        
        // Create ViewModel with mocked repository and context
        activeTimerViewModel = new ActiveTimerViewModel(mockRepository, mockContext);
        
        // Observe LiveData to ensure they're active
        activeTimerViewModel.getActiveTimers().observeForever(activeTimersObserver);
        activeTimerViewModel.getPrimaryTimer().observeForever(primaryTimerObserver);
        activeTimerViewModel.getIsLoading().observeForever(loadingObserver);
    }

    @After
    public void tearDown() {
        // Clean up observers
        activeTimerViewModel.getActiveTimers().removeObserver(activeTimersObserver);
        activeTimerViewModel.getPrimaryTimer().removeObserver(primaryTimerObserver);
        activeTimerViewModel.getIsLoading().removeObserver(loadingObserver);
    }

    /**
     * Test that would have caught the seconds truncation bug.
     * Verifies that timer durations with seconds are preserved exactly.
     */
    @Test
    public void testStartNewTimer_WithSeconds_PreservesExactDuration() {
        // Arrange
        String timerName = "Test Timer";
        String userId = "user123";
        Duration durationWithSeconds = Duration.ofMinutes(1).plusSeconds(23); // 1:23
        
        // Act
        activeTimerViewModel.startNewTimer(timerName, durationWithSeconds, userId);
        
        // Assert
        verify(mockRepository).insert(argThat(timer -> {
            assertNotNull("Timer should not be null", timer);
            assertEquals("Timer name should match", timerName, timer.getName());
            assertEquals("Timer user ID should match", userId, timer.getUserId());
            
            // This assertion would have caught the bug!
            Duration actualDuration = timer.getTotalDuration();
            assertEquals("Timer duration should preserve seconds exactly", 
                durationWithSeconds.getSeconds(), actualDuration.getSeconds());
            assertEquals("Timer total seconds should be 83 (1:23)", 
                83L, actualDuration.getSeconds());
            
            return true;
        }));
    }

    /**
     * Test various duration formats to ensure comprehensive coverage.
     */
    @Test
    public void testStartNewTimer_VariousDurations_AllPreserved() {
        String userId = "user123";
        
        // Test cases covering different duration formats
        Duration[] testDurations = {
            Duration.ofSeconds(45),                    // 0:45
            Duration.ofMinutes(2).plusSeconds(30),     // 2:30  
            Duration.ofMinutes(5).plusSeconds(07),     // 5:07
            Duration.ofMinutes(10).plusSeconds(59),    // 10:59
            Duration.ofHours(1).plusMinutes(15).plusSeconds(42), // 1:15:42
            Duration.ofSeconds(3661)                   // 1:01:01 (edge case)
        };
        
        for (int i = 0; i < testDurations.length; i++) {
            Duration expectedDuration = testDurations[i];
            String timerName = "Timer " + i;
            
            // Act
            activeTimerViewModel.startNewTimer(timerName, expectedDuration, userId);
            
            // Assert - verify this specific timer was inserted correctly
            verify(mockRepository, atLeastOnce()).insert(argThat(timer -> {
                if (!timer.getName().equals(timerName)) return false;
                
                Duration actualDuration = timer.getTotalDuration();
                assertEquals("Duration should be preserved exactly for " + timerName,
                    expectedDuration.getSeconds(), actualDuration.getSeconds());
                
                return true;
            }));
        }
        
        // Verify total number of insertions
        verify(mockRepository, times(testDurations.length)).insert(any(Timer.class));
    }

    /**
     * Test edge cases that could cause precision loss.
     */
    @Test
    public void testStartNewTimer_EdgeCases_NoPrecisionLoss() {
        String userId = "user123";
        
        // Edge cases that could expose duration truncation bugs
        Duration[] edgeCases = {
            Duration.ofSeconds(1),       // Minimal duration
            Duration.ofSeconds(59),      // Just under 1 minute
            Duration.ofSeconds(60),      // Exactly 1 minute  
            Duration.ofSeconds(61),      // Just over 1 minute
            Duration.ofSeconds(3599),    // Just under 1 hour
            Duration.ofSeconds(3600),    // Exactly 1 hour
            Duration.ofSeconds(3661),    // 1 hour 1 minute 1 second
        };
        
        for (int i = 0; i < edgeCases.length; i++) {
            Duration expectedDuration = edgeCases[i];
            String timerName = "EdgeCase " + i;
            
            // Act
            activeTimerViewModel.startNewTimer(timerName, expectedDuration, userId);
            
            // Assert exact preservation - verify this specific timer was inserted correctly
            verify(mockRepository, atLeastOnce()).insert(argThat(timer -> {
                if (!timer.getName().equals(timerName)) return false;
                
                Duration actualDuration = timer.getTotalDuration();
                
                // Verify total seconds match exactly (no truncation)
                assertEquals("Total seconds should be preserved exactly",
                    expectedDuration.getSeconds(), actualDuration.getSeconds());
                
                // Verify individual components if applicable
                long expectedMinutes = expectedDuration.toMinutes();
                long expectedSecondsRemainder = expectedDuration.getSeconds() % 60;
                long actualMinutes = actualDuration.toMinutes();
                long actualSecondsRemainder = actualDuration.getSeconds() % 60;
                
                assertEquals("Minutes component should match", expectedMinutes, actualMinutes);
                assertEquals("Seconds remainder should match", expectedSecondsRemainder, actualSecondsRemainder);
                
                return true;
            }));
        }
        
        // Verify total number of insertions
        verify(mockRepository, times(edgeCases.length)).insert(any(Timer.class));
    }

    /**
     * Test that simulates the original bug scenario.
     * This test would have failed before the fix.
     */
    @Test
    public void testStartNewTimer_OriginalBugScenario_1Minutes23Seconds() {
        // Arrange - This is the exact scenario the user reported
        String timerName = "Bug Test Timer";
        String userId = "user123";
        Duration originalDuration = Duration.ofMinutes(1).plusSeconds(23); // 1:23
        
        // Act
        activeTimerViewModel.startNewTimer(timerName, originalDuration, userId);
        
        // Assert - This would have failed with the old bug
        verify(mockRepository).insert(argThat(timer -> {
            Duration storedDuration = timer.getTotalDuration();
            
            // The bug would have stored only 60 seconds (1:00) instead of 83 seconds (1:23)
            assertNotEquals("Duration should NOT be truncated to just minutes", 
                60L, storedDuration.getSeconds());
            assertEquals("Duration should be exactly 83 seconds (1:23)", 
                83L, storedDuration.getSeconds());
            assertEquals("Duration minutes should be 1", 
                1L, storedDuration.toMinutes());
            assertEquals("Duration seconds remainder should be 23", 
                23L, storedDuration.getSeconds() % 60);
            
            return true;
        }));
    }

    /**
     * Integration test that verifies the timer is created with correct state.
     */
    @Test
    public void testStartNewTimer_CreatesTimerWithCorrectState() throws InterruptedException {
        // Arrange
        String timerName = "Integration Test Timer";
        String userId = "user123";
        Duration duration = Duration.ofMinutes(2).plusSeconds(45); // 2:45
        CountDownLatch latch = new CountDownLatch(1);
        
        // Mock the repository to simulate successful insertion
        doAnswer(invocation -> {
            Timer timer = invocation.getArgument(0);
            // Simulate successful insertion by updating the ViewModel's state
            latch.countDown();
            return null;
        }).when(mockRepository).insert(any(Timer.class));
        
        // Act
        activeTimerViewModel.startNewTimer(timerName, duration, userId);
        
        // Wait for async operation
        assertTrue("Timer creation should complete within timeout", 
            latch.await(1, TimeUnit.SECONDS));
        
        // Assert
        verify(mockRepository).insert(argThat(timer -> {
            assertNotNull("Timer should be created", timer);
            assertEquals("Timer should have correct name", timerName, timer.getName());
            assertEquals("Timer should have correct user ID", userId, timer.getUserId());
            assertEquals("Timer should have correct duration", duration, timer.getTotalDuration());
            assertFalse("Timer should be running (not paused)", timer.isPaused());
            assertNotNull("Timer should have an ID", timer.getId());
            assertNotNull("Timer should have user ID", timer.getUserId());
            return true;
        }));
        
        // Verify loading state was managed correctly
        verify(loadingObserver, atLeastOnce()).onChanged(true);
        verify(loadingObserver, atLeastOnce()).onChanged(false);
    }

    /**
     * Test that validates timer duration formatting for display.
     */
    @Test
    public void testDurationFormatting_PreservesUserExpectations() {
        String userId = "user123";
        
        // Test durations that users commonly enter
        class DurationTest {
            final Duration duration;
            final String expectedDisplayFormat;
            
            DurationTest(Duration duration, String expectedDisplayFormat) {
                this.duration = duration;
                this.expectedDisplayFormat = expectedDisplayFormat;
            }
        }
        
        DurationTest[] tests = {
            new DurationTest(Duration.ofSeconds(45), "0:45"),
            new DurationTest(Duration.ofMinutes(1).plusSeconds(23), "1:23"),
            new DurationTest(Duration.ofMinutes(5).plusSeconds(7), "5:07"),
            new DurationTest(Duration.ofMinutes(10), "10:00"),
            new DurationTest(Duration.ofMinutes(25).plusSeconds(30), "25:30")
        };
        
        for (int i = 0; i < tests.length; i++) {
            DurationTest test = tests[i];
            String timerName = "Test Timer " + i; // Use unique timer names
            
            activeTimerViewModel.startNewTimer(timerName, test.duration, userId);
            
            verify(mockRepository, atLeastOnce()).insert(argThat(timer -> {
                if (!timer.getName().equals(timerName)) return false; // Only match this specific timer
                
                Duration storedDuration = timer.getTotalDuration();
                
                // Verify the stored duration would display correctly
                long minutes = storedDuration.toMinutes();
                long seconds = storedDuration.getSeconds() % 60;
                String actualFormat = String.format("%d:%02d", minutes, seconds);
                
                assertEquals("Duration should format as expected: " + test.expectedDisplayFormat,
                    test.expectedDisplayFormat, actualFormat);
                
                return true;
            }));
        }
        
        // Verify total number of insertions
        verify(mockRepository, times(tests.length)).insert(any(Timer.class));
    }
}

/**
 * Helper class for duration tests
 */
class DurationTest {
    final Duration duration;
    final String expectedDisplayFormat;
    
    DurationTest(Duration duration, String expectedDisplayFormat) {
        this.duration = duration;
        this.expectedDisplayFormat = expectedDisplayFormat;
    }
}
