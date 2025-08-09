package io.jhoyt.bubbletimer.integration;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;
import io.jhoyt.bubbletimer.integration.TestTimerRepository;
import io.jhoyt.bubbletimer.ui.viewmodels.TimerViewModel;

import static org.junit.Assert.*;

/**
 * Integration tests for ViewModels with the new architecture.
 * Tests the complete data flow from ViewModels through use cases to repositories.
 */
@RunWith(AndroidJUnit4.class)
public class ViewModelIntegrationTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TimerRepository timerRepository;
    private TimerViewModel timerViewModel;

    @Before
    public void setUp() {
        // Create simple test repository
        timerRepository = new TestTimerRepository();
        
        // Initialize ViewModel with real dependencies
        timerViewModel = new TimerViewModel(
            new io.jhoyt.bubbletimer.domain.usecases.timer.GetAllTimersUseCase(timerRepository),
            new io.jhoyt.bubbletimer.domain.usecases.timer.GetTimersByTagUseCase(timerRepository),
            new io.jhoyt.bubbletimer.domain.usecases.timer.GetTimerByIdUseCase(timerRepository),
            new io.jhoyt.bubbletimer.domain.usecases.timer.StartTimerUseCase(timerRepository),
            new io.jhoyt.bubbletimer.domain.usecases.timer.PauseTimerUseCase(timerRepository),
            new io.jhoyt.bubbletimer.domain.usecases.timer.ResumeTimerUseCase(timerRepository),
            new io.jhoyt.bubbletimer.domain.usecases.timer.StopTimerUseCase(timerRepository),
            new io.jhoyt.bubbletimer.domain.usecases.timer.DeleteTimerUseCase(timerRepository)
        );
    }

    @Test
    public void testViewModelLoadAllTimers() throws InterruptedException {
        // Create some test data
        timerViewModel.startNewTimer("Test Timer 1", 5, "test-user");
        timerViewModel.startNewTimer("Test Timer 2", 10, "test-user");
        
        // Load all timers
        timerViewModel.loadAllTimers();
        
        // Wait for LiveData to update
        CountDownLatch latch = new CountDownLatch(1);
        timerViewModel.getAllTimers().observeForever(timers -> {
            if (timers != null && timers.size() >= 2) {
                latch.countDown();
            }
        });
        
        assertTrue("Should receive timer updates within 5 seconds", latch.await(5, TimeUnit.SECONDS));
        
        List<Timer> timers = timerViewModel.getAllTimers().getValue();
        assertNotNull("Timers should not be null", timers);
        assertTrue("Should have at least 2 timers", timers.size() >= 2);
        
        // Verify timer data
        boolean foundTimer1 = timers.stream().anyMatch(timer -> "Test Timer 1".equals(timer.getName()));
        boolean foundTimer2 = timers.stream().anyMatch(timer -> "Test Timer 2".equals(timer.getName()));
        
        assertTrue("Should find Test Timer 1", foundTimer1);
        assertTrue("Should find Test Timer 2", foundTimer2);
    }

    @Test
    public void testViewModelStartNewTimer() throws InterruptedException {
        // Start a new timer
        timerViewModel.startNewTimer("New Test Timer", 15, "test-user");
        
        // Wait for LiveData to update
        CountDownLatch latch = new CountDownLatch(1);
        timerViewModel.getAllTimers().observeForever(timers -> {
            if (timers != null && timers.size() >= 1) {
                latch.countDown();
            }
        });
        
        assertTrue("Should receive timer updates within 5 seconds", latch.await(5, TimeUnit.SECONDS));
        
        List<Timer> timers = timerViewModel.getAllTimers().getValue();
        assertNotNull("Timers should not be null", timers);
        assertTrue("Should have at least 1 timer", timers.size() >= 1);
        
        // Find the new timer
        Timer newTimer = timers.stream()
                .filter(timer -> "New Test Timer".equals(timer.getName()))
                .findFirst()
                .orElse(null);
        
        assertNotNull("Should find the new timer", newTimer);
        assertEquals("Timer name should match", "New Test Timer", newTimer.getName());
        assertEquals("Timer duration should be 15 minutes", java.time.Duration.ofMinutes(15), newTimer.getTotalDuration());
        assertEquals("Timer should be in RUNNING state", io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING, newTimer.getState());
    }

    @Test
    public void testViewModelPauseAndResumeTimer() throws InterruptedException {
        // Start a timer
        timerViewModel.startNewTimer("Pause Test Timer", 20, "test-user");
        
        // Wait for timer to be created
        CountDownLatch createLatch = new CountDownLatch(1);
        timerViewModel.getAllTimers().observeForever(timers -> {
            if (timers != null && timers.size() >= 1) {
                createLatch.countDown();
            }
        });
        assertTrue("Should create timer within 5 seconds", createLatch.await(5, TimeUnit.SECONDS));
        
        // Get the timer
        List<Timer> timers = timerViewModel.getAllTimers().getValue();
        Timer timer = timers.stream()
                .filter(t -> "Pause Test Timer".equals(t.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull("Should find the timer", timer);
        
        // Pause the timer
        timerViewModel.pauseTimer(timer.getId());
        
        // Wait for pause to complete
        CountDownLatch pauseLatch = new CountDownLatch(1);
        timerViewModel.getAllTimers().observeForever(updatedTimers -> {
            Timer updatedTimer = updatedTimers.stream()
                    .filter(t -> timer.getId().equals(t.getId()))
                    .findFirst()
                    .orElse(null);
            if (updatedTimer != null && io.jhoyt.bubbletimer.domain.entities.TimerState.PAUSED.equals(updatedTimer.getState())) {
                pauseLatch.countDown();
            }
        });
        assertTrue("Should pause timer within 5 seconds", pauseLatch.await(5, TimeUnit.SECONDS));
        
        // Resume the timer
        timerViewModel.resumeTimer(timer.getId());
        
        // Wait for resume to complete
        CountDownLatch resumeLatch = new CountDownLatch(1);
        timerViewModel.getAllTimers().observeForever(updatedTimers -> {
            Timer updatedTimer = updatedTimers.stream()
                    .filter(t -> timer.getId().equals(t.getId()))
                    .findFirst()
                    .orElse(null);
            if (updatedTimer != null && io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING.equals(updatedTimer.getState())) {
                resumeLatch.countDown();
            }
        });
        assertTrue("Should resume timer within 5 seconds", resumeLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testViewModelStopTimer() throws InterruptedException {
        // Start a timer
        timerViewModel.startNewTimer("Stop Test Timer", 25, "test-user");
        
        // Wait for timer to be created
        CountDownLatch createLatch = new CountDownLatch(1);
        timerViewModel.getAllTimers().observeForever(timers -> {
            if (timers != null && timers.size() >= 1) {
                createLatch.countDown();
            }
        });
        assertTrue("Should create timer within 5 seconds", createLatch.await(5, TimeUnit.SECONDS));
        
        // Get the timer
        List<Timer> timers = timerViewModel.getAllTimers().getValue();
        Timer timer = timers.stream()
                .filter(t -> "Stop Test Timer".equals(t.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull("Should find the timer", timer);
        
        // Stop the timer
        timerViewModel.stopTimer(timer.getId());
        
        // Wait for stop to complete
        CountDownLatch stopLatch = new CountDownLatch(1);
        timerViewModel.getAllTimers().observeForever(updatedTimers -> {
            Timer updatedTimer = updatedTimers.stream()
                    .filter(t -> timer.getId().equals(t.getId()))
                    .findFirst()
                    .orElse(null);
            if (updatedTimer != null && io.jhoyt.bubbletimer.domain.entities.TimerState.STOPPED.equals(updatedTimer.getState())) {
                stopLatch.countDown();
            }
        });
        assertTrue("Should stop timer within 5 seconds", stopLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testViewModelDeleteTimer() throws InterruptedException {
        // Start a timer
        timerViewModel.startNewTimer("Delete Test Timer", 30, "test-user");
        
        // Wait for timer to be created
        CountDownLatch createLatch = new CountDownLatch(1);
        timerViewModel.getAllTimers().observeForever(timers -> {
            if (timers != null && timers.size() >= 1) {
                createLatch.countDown();
            }
        });
        assertTrue("Should create timer within 5 seconds", createLatch.await(5, TimeUnit.SECONDS));
        
        // Get the timer
        List<Timer> timers = timerViewModel.getAllTimers().getValue();
        Timer timer = timers.stream()
                .filter(t -> "Delete Test Timer".equals(t.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull("Should find the timer", timer);
        
        int initialCount = timers.size();
        
        // Delete the timer
        timerViewModel.deleteTimer(timer.getId());
        
        // Wait for delete to complete
        CountDownLatch deleteLatch = new CountDownLatch(1);
        timerViewModel.getAllTimers().observeForever(updatedTimers -> {
            if (updatedTimers.size() < initialCount) {
                deleteLatch.countDown();
            }
        });
        assertTrue("Should delete timer within 5 seconds", deleteLatch.await(5, TimeUnit.SECONDS));
        
        // Verify timer is deleted
        List<Timer> updatedTimers = timerViewModel.getAllTimers().getValue();
        boolean timerStillExists = updatedTimers.stream()
                .anyMatch(t -> timer.getId().equals(t.getId()));
        assertFalse("Timer should be deleted", timerStillExists);
    }

    @Test
    public void testViewModelErrorHandling() throws InterruptedException {
        // Try to pause a non-existent timer
        timerViewModel.pauseTimer("non-existent-id");
        
        // Wait for error to be set
        CountDownLatch errorLatch = new CountDownLatch(1);
        timerViewModel.getErrorMessage().observeForever(errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                errorLatch.countDown();
            }
        });
        
        assertTrue("Should receive error message within 5 seconds", errorLatch.await(5, TimeUnit.SECONDS));
        
        String errorMessage = timerViewModel.getErrorMessage().getValue();
        assertNotNull("Error message should not be null", errorMessage);
        assertFalse("Error message should not be empty", errorMessage.isEmpty());
    }

    @Test
    public void testViewModelLoadingStates() throws InterruptedException {
        // Test loading state during operations
        CountDownLatch loadingLatch = new CountDownLatch(1);
        timerViewModel.getIsLoading().observeForever(isLoading -> {
            if (isLoading) {
                loadingLatch.countDown();
            }
        });
        
        // Start a timer to trigger loading state
        timerViewModel.startNewTimer("Loading Test Timer", 35, "test-user");
        
        assertTrue("Should set loading state within 5 seconds", loadingLatch.await(5, TimeUnit.SECONDS));
        
        // Wait for loading to complete
        CountDownLatch notLoadingLatch = new CountDownLatch(1);
        timerViewModel.getIsLoading().observeForever(isLoading -> {
            if (!isLoading) {
                notLoadingLatch.countDown();
            }
        });
        
        assertTrue("Should clear loading state within 5 seconds", notLoadingLatch.await(5, TimeUnit.SECONDS));
    }
}
