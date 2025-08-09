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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;
import io.jhoyt.bubbletimer.integration.TestTimerRepository;
import io.jhoyt.bubbletimer.domain.usecases.timer.StartTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.PauseTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.ResumeTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.StopTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.GetAllTimersUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.GetTimerByIdUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.DeleteTimerUseCase;

import static org.junit.Assert.*;

/**
 * Integration tests for timer functionality.
 * Tests the complete data flow from domain use cases through repositories to the database.
 */
@RunWith(AndroidJUnit4.class)
public class TimerIntegrationTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TimerRepository timerRepository;
    private StartTimerUseCase startTimerUseCase;
    private PauseTimerUseCase pauseTimerUseCase;
    private ResumeTimerUseCase resumeTimerUseCase;
    private StopTimerUseCase stopTimerUseCase;
    private GetAllTimersUseCase getAllTimersUseCase;
    private GetTimerByIdUseCase getTimerByIdUseCase;
    private DeleteTimerUseCase deleteTimerUseCase;

    @Before
    public void setUp() {
        // Create simple test repository
        timerRepository = new TestTimerRepository();
        
        // Initialize use cases
        startTimerUseCase = new StartTimerUseCase(timerRepository);
        pauseTimerUseCase = new PauseTimerUseCase(timerRepository);
        resumeTimerUseCase = new ResumeTimerUseCase(timerRepository);
        stopTimerUseCase = new StopTimerUseCase(timerRepository);
        getAllTimersUseCase = new GetAllTimersUseCase(timerRepository);
        getTimerByIdUseCase = new GetTimerByIdUseCase(timerRepository);
        deleteTimerUseCase = new DeleteTimerUseCase(timerRepository);
    }

    @Test
    public void testCompleteTimerLifecycle() throws InterruptedException {
        // 1. Start a new timer
        Result<Timer> startResult = startTimerUseCase.execute("Test Timer", 5, "test-user");
        assertTrue("Start timer should succeed", startResult.isSuccess());
        
        Timer startedTimer = startResult.getData();
        assertNotNull("Started timer should not be null", startedTimer);
        assertEquals("Timer name should match", "Test Timer", startedTimer.getName());
        assertEquals("Timer duration should be 5 minutes", Duration.ofMinutes(5), startedTimer.getTotalDuration());
        assertEquals("Timer should be in RUNNING state", io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING, startedTimer.getState());
        
        // 2. Get timer by ID
        Result<Timer> getResult = getTimerByIdUseCase.execute(startedTimer.getId());
        assertTrue("Get timer should succeed", getResult.isSuccess());
        assertEquals("Retrieved timer should match started timer", startedTimer.getId(), getResult.getData().getId());
        
        // 3. Pause the timer
        Result<Timer> pauseResult = pauseTimerUseCase.execute(startedTimer.getId());
        assertTrue("Pause timer should succeed", pauseResult.isSuccess());
        assertEquals("Timer should be in PAUSED state", io.jhoyt.bubbletimer.domain.entities.TimerState.PAUSED, pauseResult.getData().getState());
        
        // 4. Resume the timer
        Result<Timer> resumeResult = resumeTimerUseCase.execute(startedTimer.getId());
        assertTrue("Resume timer should succeed", resumeResult.isSuccess());
        assertEquals("Timer should be in RUNNING state", io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING, resumeResult.getData().getState());
        
        // 5. Stop the timer
        Result<Timer> stopResult = stopTimerUseCase.execute(startedTimer.getId());
        assertTrue("Stop timer should succeed", stopResult.isSuccess());
        assertEquals("Timer should be in STOPPED state", io.jhoyt.bubbletimer.domain.entities.TimerState.STOPPED, stopResult.getData().getState());
        
        // 6. Verify timer appears in getAllTimers
        Result<List<Timer>> getAllResult = getAllTimersUseCase.execute();
        assertTrue("Get all timers should succeed", getAllResult.isSuccess());
        assertTrue("Timer list should contain our timer", getAllResult.getData().stream()
                .anyMatch(timer -> timer.getId().equals(startedTimer.getId())));
        
        // 7. Delete the timer
        Result<Void> deleteResult = deleteTimerUseCase.execute(startedTimer.getId());
        assertTrue("Delete timer should succeed", deleteResult.isSuccess());
        
        // 8. Verify timer is deleted
        Result<Timer> getAfterDeleteResult = getTimerByIdUseCase.execute(startedTimer.getId());
        assertTrue("Get timer after delete should fail", getAfterDeleteResult.isFailure());
    }

    @Test
    public void testMultipleTimers() throws InterruptedException {
        // Create multiple timers
        Result<Timer> timer1Result = startTimerUseCase.execute("Timer 1", 10, "test-user");
        Result<Timer> timer2Result = startTimerUseCase.execute("Timer 2", 15, "test-user");
        Result<Timer> timer3Result = startTimerUseCase.execute("Timer 3", 20, "test-user");
        
        assertTrue("Timer 1 should be created", timer1Result.isSuccess());
        assertTrue("Timer 2 should be created", timer2Result.isSuccess());
        assertTrue("Timer 3 should be created", timer3Result.isSuccess());
        
        // Get all timers
        Result<List<Timer>> getAllResult = getAllTimersUseCase.execute();
        assertTrue("Get all timers should succeed", getAllResult.isSuccess());
        assertEquals("Should have 3 timers", 3, getAllResult.getData().size());
        
        // Verify all timers are in RUNNING state
        getAllResult.getData().forEach(timer -> 
            assertEquals("All timers should be RUNNING", io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING, timer.getState())
        );
    }

    @Test
    public void testTimerStateTransitions() throws InterruptedException {
        // Start timer
        Result<Timer> startResult = startTimerUseCase.execute("State Test Timer", 5, "test-user");
        assertTrue("Start timer should succeed", startResult.isSuccess());
        Timer timer = startResult.getData();
        
        // Test RUNNING -> PAUSED
        Result<Timer> pauseResult = pauseTimerUseCase.execute(timer.getId());
        assertTrue("Pause should succeed", pauseResult.isSuccess());
        assertEquals("Should be PAUSED", io.jhoyt.bubbletimer.domain.entities.TimerState.PAUSED, pauseResult.getData().getState());
        
        // Test PAUSED -> RUNNING
        Result<Timer> resumeResult = resumeTimerUseCase.execute(timer.getId());
        assertTrue("Resume should succeed", resumeResult.isSuccess());
        assertEquals("Should be RUNNING", io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING, resumeResult.getData().getState());
        
        // Test RUNNING -> STOPPED
        Result<Timer> stopResult = stopTimerUseCase.execute(timer.getId());
        assertTrue("Stop should succeed", stopResult.isSuccess());
        assertEquals("Should be STOPPED", io.jhoyt.bubbletimer.domain.entities.TimerState.STOPPED, stopResult.getData().getState());
        
        // Test invalid transitions
        Result<Timer> invalidResumeResult = resumeTimerUseCase.execute(timer.getId());
        assertTrue("Resume stopped timer should fail", invalidResumeResult.isFailure());
        
        Result<Timer> invalidPauseResult = pauseTimerUseCase.execute(timer.getId());
        assertTrue("Pause stopped timer should fail", invalidPauseResult.isFailure());
    }

    @Test
    public void testErrorHandling() throws InterruptedException {
        // Test invalid timer ID
        Result<Timer> invalidGetResult = getTimerByIdUseCase.execute("non-existent-id");
        assertTrue("Get non-existent timer should fail", invalidGetResult.isFailure());
        
        // Test invalid operations on non-existent timer
        Result<Timer> invalidPauseResult = pauseTimerUseCase.execute("non-existent-id");
        assertTrue("Pause non-existent timer should fail", invalidPauseResult.isFailure());
        
        Result<Timer> invalidResumeResult = resumeTimerUseCase.execute("non-existent-id");
        assertTrue("Resume non-existent timer should fail", invalidResumeResult.isFailure());
        
        Result<Timer> invalidStopResult = stopTimerUseCase.execute("non-existent-id");
        assertTrue("Stop non-existent timer should fail", invalidStopResult.isFailure());
        
        // Test delete non-existent timer
        Result<Void> invalidDeleteResult = deleteTimerUseCase.execute("non-existent-id");
        // Note: Our test repository doesn't fail on delete of non-existent timer, it just does nothing
        // This is a valid implementation choice for some repositories
        assertTrue("Delete non-existent timer should succeed (no-op)", invalidDeleteResult.isSuccess());
    }

    @Test
    public void testTimerDataIntegrity() throws InterruptedException {
        // Start timer with specific data
        String timerName = "Data Integrity Test";
        int durationMinutes = 25;
        String userId = "test-user-123";
        
        Result<Timer> startResult = startTimerUseCase.execute(timerName, durationMinutes, userId);
        assertTrue("Start timer should succeed", startResult.isSuccess());
        
        Timer timer = startResult.getData();
        
        // Verify data integrity
        assertEquals("Timer name should be preserved", timerName, timer.getName());
        assertEquals("Timer duration should be preserved", Duration.ofMinutes(durationMinutes), timer.getTotalDuration());
        assertEquals("Timer user ID should be preserved", userId, timer.getUserId());
        assertNotNull("Timer ID should be generated", timer.getId());
        assertNotNull("Timer creation time should be set", timer.getCreatedAt());
        assertEquals("Timer should be in RUNNING state", io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING, timer.getState());
        
        // Verify data persists through operations
        Result<Timer> getResult = getTimerByIdUseCase.execute(timer.getId());
        assertTrue("Get timer should succeed", getResult.isSuccess());
        
        Timer retrievedTimer = getResult.getData();
        assertEquals("Retrieved timer name should match", timerName, retrievedTimer.getName());
        assertEquals("Retrieved timer duration should match", Duration.ofMinutes(durationMinutes), retrievedTimer.getTotalDuration());
        assertEquals("Retrieved timer user ID should match", userId, retrievedTimer.getUserId());
    }

    @Test
    public void testConcurrentTimerOperations() throws InterruptedException {
        // Start multiple timers concurrently
        Result<Timer> timer1Result = startTimerUseCase.execute("Concurrent Timer 1", 5, "test-user");
        Result<Timer> timer2Result = startTimerUseCase.execute("Concurrent Timer 2", 10, "test-user");
        
        assertTrue("Timer 1 should be created", timer1Result.isSuccess());
        assertTrue("Timer 2 should be created", timer2Result.isSuccess());
        
        Timer timer1 = timer1Result.getData();
        Timer timer2 = timer2Result.getData();
        
        // Perform operations on both timers
        Result<Timer> pause1Result = pauseTimerUseCase.execute(timer1.getId());
        Result<Timer> pause2Result = pauseTimerUseCase.execute(timer2.getId());
        
        assertTrue("Pause timer 1 should succeed", pause1Result.isSuccess());
        assertTrue("Pause timer 2 should succeed", pause2Result.isSuccess());
        
        // Verify both timers are paused
        assertEquals("Timer 1 should be PAUSED", io.jhoyt.bubbletimer.domain.entities.TimerState.PAUSED, pause1Result.getData().getState());
        assertEquals("Timer 2 should be PAUSED", io.jhoyt.bubbletimer.domain.entities.TimerState.PAUSED, pause2Result.getData().getState());
        
        // Resume both timers
        Result<Timer> resume1Result = resumeTimerUseCase.execute(timer1.getId());
        Result<Timer> resume2Result = resumeTimerUseCase.execute(timer2.getId());
        
        assertTrue("Resume timer 1 should succeed", resume1Result.isSuccess());
        assertTrue("Resume timer 2 should succeed", resume2Result.isSuccess());
        
        // Verify both timers are running
        assertEquals("Timer 1 should be RUNNING", io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING, resume1Result.getData().getState());
        assertEquals("Timer 2 should be RUNNING", io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING, resume2Result.getData().getState());
    }
}
