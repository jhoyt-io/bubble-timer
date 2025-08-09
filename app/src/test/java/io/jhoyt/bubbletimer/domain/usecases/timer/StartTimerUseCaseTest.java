package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.entities.TimerState;
import io.jhoyt.bubbletimer.domain.exceptions.ValidationException;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StartTimerUseCase.
 * Tests business logic for starting new timers with validation.
 */
@RunWith(MockitoJUnitRunner.class)
public class StartTimerUseCaseTest {
    
    @Mock
    private TimerRepository timerRepository;
    
    private StartTimerUseCase startTimerUseCase;
    
    @Before
    public void setUp() {
        startTimerUseCase = new StartTimerUseCase(timerRepository);
    }
    
    @Test
    public void testExecute_ValidInput_ReturnsSuccess() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ofMinutes(30);
        Set<String> tags = Set.of("work", "focus");
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, tags, userId);
        
        // Assert
        assertTrue("Result should be successful", result.isSuccess());
        assertFalse("Result should not be a failure", result.isFailure());
        
        Timer timer = result.getData();
        assertNotNull("Timer should not be null", timer);
        assertEquals("Timer name should match", name, timer.getName());
        assertEquals("Timer duration should match", duration, timer.getTotalDuration());
        assertEquals("Timer user ID should match", userId, timer.getUserId());
        assertEquals("Timer state should be RUNNING", TimerState.RUNNING, timer.getState());
        assertTrue("Timer should be active", timer.isActive());
        assertTrue("Timer should be running", timer.isRunning());
        assertFalse("Timer should not be paused", timer.isPaused());
        assertFalse("Timer should not be expired", timer.isExpired());
        assertFalse("Timer should not be stopped", timer.isStopped());
        
        // Verify repository was called
        verify(timerRepository, times(1)).saveTimer(any(Timer.class));
    }
    
    @Test
    public void testExecute_WithMinutes_ReturnsSuccess() {
        // Arrange
        String name = "Quick Timer";
        int durationMinutes = 15;
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, durationMinutes, userId);
        
        // Assert
        assertTrue("Result should be successful", result.isSuccess());
        
        Timer timer = result.getData();
        assertEquals("Timer duration should be 15 minutes", Duration.ofMinutes(15), timer.getTotalDuration());
        assertEquals("Timer name should match", name, timer.getName());
        assertEquals("Timer user ID should match", userId, timer.getUserId());
    }
    
    @Test
    public void testExecute_WithSeconds_ReturnsSuccess() {
        // Arrange
        String name = "Short Timer";
        long durationSeconds = 300; // 5 minutes
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, durationSeconds, userId);
        
        // Assert
        assertTrue("Result should be successful", result.isSuccess());
        
        Timer timer = result.getData();
        assertEquals("Timer duration should be 300 seconds", Duration.ofSeconds(300), timer.getTotalDuration());
        assertEquals("Timer name should match", name, timer.getName());
        assertEquals("Timer user ID should match", userId, timer.getUserId());
    }
    
    @Test
    public void testExecute_EmptyName_ReturnsValidationError() {
        // Arrange
        String name = "";
        Duration duration = Duration.ofMinutes(30);
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertEquals("Error code should be VALIDATION_ERROR", "VALIDATION_ERROR", result.getError().getErrorCode());
        assertTrue("Error should be recoverable", result.isRecoverable());
        
        // Verify repository was not called
        verify(timerRepository, never()).saveTimer(any(Timer.class));
    }
    
    @Test
    public void testExecute_NullName_ReturnsValidationError() {
        // Arrange
        String name = null;
        Duration duration = Duration.ofMinutes(30);
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertEquals("Error code should be VALIDATION_ERROR", "VALIDATION_ERROR", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_NameTooLong_ReturnsValidationError() {
        // Arrange
        String name = "A".repeat(101); // 101 characters
        Duration duration = Duration.ofMinutes(30);
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertEquals("Error code should be VALIDATION_ERROR", "VALIDATION_ERROR", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_NullDuration_ReturnsValidationError() {
        // Arrange
        String name = "Test Timer";
        Duration duration = null;
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertEquals("Error code should be VALIDATION_ERROR", "VALIDATION_ERROR", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_ZeroDuration_ReturnsValidationError() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ZERO;
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertEquals("Error code should be VALIDATION_ERROR", "VALIDATION_ERROR", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_NegativeDuration_ReturnsValidationError() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ofMinutes(-5);
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertEquals("Error code should be VALIDATION_ERROR", "VALIDATION_ERROR", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_DurationTooLong_ReturnsValidationError() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ofHours(25); // 25 hours
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertEquals("Error code should be VALIDATION_ERROR", "VALIDATION_ERROR", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_EmptyUserId_ReturnsValidationError() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ofMinutes(30);
        String userId = "";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertEquals("Error code should be VALIDATION_ERROR", "VALIDATION_ERROR", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_NullUserId_ReturnsValidationError() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ofMinutes(30);
        String userId = null;
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertEquals("Error code should be VALIDATION_ERROR", "VALIDATION_ERROR", result.getError().getErrorCode());
    }
    
    @Test
    public void testExecute_RepositoryThrowsException_ReturnsTimerError() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ofMinutes(30);
        String userId = "user123";
        
        doThrow(new RuntimeException("Database error"))
            .when(timerRepository).saveTimer(any(Timer.class));
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        
        assertEquals("Error code should be TIMER_ERROR", "TIMER_ERROR", result.getError().getErrorCode());
        assertTrue("Error should be recoverable", result.isRecoverable());
    }
    
    @Test
    public void testExecute_WithEmptyTags_ReturnsSuccess() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ofMinutes(30);
        Set<String> tags = Set.of();
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, tags, userId);
        
        // Assert
        assertTrue("Result should be successful", result.isSuccess());
        
        Timer timer = result.getData();
        assertTrue("Timer tags should be empty", timer.getTags().isEmpty());
    }
    
    @Test
    public void testExecute_WithNullTags_ReturnsSuccess() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ofMinutes(30);
        Set<String> tags = null;
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, tags, userId);
        
        // Assert
        assertTrue("Result should be successful", result.isSuccess());
        
        Timer timer = result.getData();
        assertTrue("Timer tags should be empty", timer.getTags().isEmpty());
    }
}
