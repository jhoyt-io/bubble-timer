package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.entities.TimerState;
import io.jhoyt.bubbletimer.domain.exceptions.InvalidTimerStateException;
import io.jhoyt.bubbletimer.domain.exceptions.TimerNotFoundException;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PauseTimerUseCase.
 * Tests business logic for pausing timers with state validation.
 */
@RunWith(MockitoJUnitRunner.class)
public class PauseTimerUseCaseTest {
    
    @Mock
    private TimerRepository timerRepository;
    
    private PauseTimerUseCase pauseTimerUseCase;
    
    @Before
    public void setUp() {
        pauseTimerUseCase = new PauseTimerUseCase(timerRepository);
    }
    
    @Test
    public void testExecute_RunningTimer_ReturnsPausedTimer() {
        // Arrange
        String timerId = "timer123";
        Timer runningTimer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        
        when(timerRepository.getTimerById(timerId)).thenReturn(runningTimer);
        
        // Act
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        // Assert
        assertTrue("Result should be successful", result.isSuccess());
        assertFalse("Result should not be a failure", result.isFailure());
        
        Timer pausedTimer = result.getData();
        assertNotNull("Paused timer should not be null", pausedTimer);
        assertEquals("Timer state should be PAUSED", TimerState.PAUSED, pausedTimer.getState());
        assertTrue("Timer should be active", pausedTimer.isActive());
        assertTrue("Timer should be paused", pausedTimer.isPaused());
        assertFalse("Timer should not be running", pausedTimer.isRunning());
        assertFalse("Timer should not be expired", pausedTimer.isExpired());
        assertFalse("Timer should not be stopped", pausedTimer.isStopped());
        
        // Verify repository was called
        verify(timerRepository, times(1)).getTimerById(timerId);
        verify(timerRepository, times(1)).updateTimer(any(Timer.class));
    }
    
    @Test
    public void testExecute_AlreadyPausedTimer_ReturnsInvalidStateError() {
        // Arrange
        String timerId = "timer123";
        Timer pausedTimer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of())
            .pause();
        
        when(timerRepository.getTimerById(timerId)).thenReturn(pausedTimer);
        
        // Act
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be InvalidTimerStateException", result.getError() instanceof InvalidTimerStateException);
        assertEquals("Error code should be INVALID_TIMER_STATE", "INVALID_TIMER_STATE", result.getError().getErrorCode());
        assertFalse("Error should not be recoverable", result.isRecoverable());
        
        // Verify repository was not updated
        verify(timerRepository, times(1)).getTimerById(timerId);
        verify(timerRepository, never()).updateTimer(any(Timer.class));
    }
    
    @Test
    public void testExecute_ExpiredTimer_ReturnsInvalidStateError() {
        // Arrange
        String timerId = "timer123";
        Timer expiredTimer = Timer.fromData(
            timerId, "Test Timer", "user123", Duration.ofMinutes(30),
            null, LocalDateTime.now().minusMinutes(1), Set.of(), Set.of(),
            TimerState.EXPIRED, LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(timerRepository.getTimerById(timerId)).thenReturn(expiredTimer);
        
        // Act
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be InvalidTimerStateException", result.getError() instanceof InvalidTimerStateException);
        assertEquals("Error code should be INVALID_TIMER_STATE", "INVALID_TIMER_STATE", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_StoppedTimer_ReturnsInvalidStateError() {
        // Arrange
        String timerId = "timer123";
        Timer stoppedTimer = Timer.fromData(
            timerId, "Test Timer", "user123", Duration.ofMinutes(30),
            null, LocalDateTime.now().plusMinutes(30), Set.of(), Set.of(),
            TimerState.STOPPED, LocalDateTime.now(), LocalDateTime.now()
        );
        
        when(timerRepository.getTimerById(timerId)).thenReturn(stoppedTimer);
        
        // Act
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be InvalidTimerStateException", result.getError() instanceof InvalidTimerStateException);
        assertEquals("Error code should be INVALID_TIMER_STATE", "INVALID_TIMER_STATE", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_TimerNotFound_ReturnsNotFoundError() {
        // Arrange
        String timerId = "nonexistent";
        when(timerRepository.getTimerById(timerId)).thenReturn(null);
        
        // Act
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be TimerNotFoundException", result.getError() instanceof TimerNotFoundException);
        assertEquals("Error code should be TIMER_NOT_FOUND", "TIMER_NOT_FOUND", result.getError().getErrorCode());
        assertFalse("Error should not be recoverable", result.isRecoverable());
        
        // Verify repository was not updated
        verify(timerRepository, times(1)).getTimerById(timerId);
        verify(timerRepository, never()).updateTimer(any(Timer.class));
    }
    
    @Test
    public void testExecute_EmptyTimerId_ReturnsInvalidStateError() {
        // Arrange
        String timerId = "";
        
        // Act
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be InvalidTimerStateException", result.getError() instanceof InvalidTimerStateException);
        assertEquals("Error code should be INVALID_TIMER_STATE", "INVALID_TIMER_STATE", result.getError().getErrorCode());
        
        // Verify repository was not called
        verify(timerRepository, never()).getTimerById(any(String.class));
        verify(timerRepository, never()).updateTimer(any(Timer.class));
    }
    
    @Test
    public void testExecute_NullTimerId_ReturnsInvalidStateError() {
        // Arrange
        String timerId = null;
        
        // Act
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be InvalidTimerStateException", result.getError() instanceof InvalidTimerStateException);
        assertEquals("Error code should be INVALID_TIMER_STATE", "INVALID_TIMER_STATE", result.getError().getErrorCode());
        
        // Verify repository was not called
        verify(timerRepository, never()).getTimerById(any(String.class));
        verify(timerRepository, never()).updateTimer(any(Timer.class));
    }
    
    @Test
    public void testExecute_RepositoryThrowsException_ReturnsTimerError() {
        // Arrange
        String timerId = "timer123";
        Timer runningTimer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        
        when(timerRepository.getTimerById(timerId)).thenReturn(runningTimer);
        doThrow(new RuntimeException("Database error"))
            .when(timerRepository).updateTimer(any(Timer.class));
        
        // Act
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertEquals("Error code should be TIMER_ERROR", "TIMER_ERROR", result.getError().getErrorCode());
        assertTrue("Error should be recoverable", result.isRecoverable());
    }
    
    @Test
    public void testExecute_PausedTimerHasRemainingDuration() {
        // Arrange
        String timerId = "timer123";
        Timer runningTimer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        
        when(timerRepository.getTimerById(timerId)).thenReturn(runningTimer);
        
        // Act
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        // Assert
        assertTrue("Result should be successful", result.isSuccess());
        
        Timer pausedTimer = result.getData();
        assertNotNull("Remaining duration when paused should not be null", pausedTimer.getRemainingDurationWhenPaused());
        assertTrue("Remaining duration should be positive", pausedTimer.getRemainingDurationWhenPaused().toMinutes() > 0);
        assertTrue("Remaining duration should be less than total duration", 
            pausedTimer.getRemainingDurationWhenPaused().compareTo(pausedTimer.getTotalDuration()) < 0);
    }
}
