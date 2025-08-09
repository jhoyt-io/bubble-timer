package io.jhoyt.bubbletimer.domain.core;

import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.entities.TimerState;
import io.jhoyt.bubbletimer.domain.exceptions.TimerException;
import io.jhoyt.bubbletimer.domain.exceptions.ValidationException;

import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Unit tests for Result pattern.
 * Tests functional error handling and composition.
 */
public class ResultTest {
    
    @Test
    public void testSuccess_CreatesSuccessfulResult() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        
        // Act
        Result<Timer> result = Result.success(timer);
        
        // Assert
        assertTrue("Result should be successful", result.isSuccess());
        assertFalse("Result should not be a failure", result.isFailure());
        assertEquals("Data should match", timer, result.getData());
        assertFalse("Should not be recoverable", result.isRecoverable());
        assertNull("User-friendly message should be null", result.getUserFriendlyMessage());
    }
    
    @Test
    public void testFailure_CreatesFailedResult() {
        // Arrange
        ValidationException error = new ValidationException("Invalid input");
        
        // Act
        Result<Timer> result = Result.failure(error);
        
        // Assert
        assertFalse("Result should not be successful", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        assertEquals("Error should match", error, result.getError());
        assertTrue("Should be recoverable", result.isRecoverable());
        assertEquals("User-friendly message should match", "Please check your input and try again.", result.getUserFriendlyMessage());
    }
    
    @Test
    public void testFailure_WithGenericException() {
        // Arrange
        RuntimeException error = new RuntimeException("Generic error");
        
        // Act
        Result<Timer> result = Result.failure(error);
        
        // Assert
        assertFalse("Result should not be successful", result.isSuccess());
        assertTrue("Result should be a failure", result.isFailure());
        assertEquals("Error code should be GENERIC_ERROR", "GENERIC_ERROR", result.getError().getErrorCode());
    }
    
    @Test
    public void testGetData_OnSuccess_ReturnsData() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        
        // Act & Assert
        assertEquals("Data should match", timer, result.getData());
    }
    
    @Test(expected = IllegalStateException.class)
    public void testGetData_OnFailure_ThrowsException() {
        // Arrange
        Result<Timer> result = Result.failure(new ValidationException("Invalid input"));
        
        // Act
        result.getData(); // Should throw IllegalStateException
    }
    
    @Test
    public void testGetError_OnFailure_ReturnsError() {
        // Arrange
        ValidationException error = new ValidationException("Invalid input");
        Result<Timer> result = Result.failure(error);
        
        // Act & Assert
        assertEquals("Error should match", error, result.getError());
    }
    
    @Test(expected = IllegalStateException.class)
    public void testGetError_OnSuccess_ThrowsException() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        
        // Act
        result.getError(); // Should throw IllegalStateException
    }
    
    @Test
    public void testGetDataOrThrow_OnSuccess_ReturnsData() throws Exception {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        
        // Act & Assert
        assertEquals("Data should match", timer, result.getDataOrThrow());
    }
    
    @Test(expected = ValidationException.class)
    public void testGetDataOrThrow_OnFailure_ThrowsError() throws Exception {
        // Arrange
        ValidationException error = new ValidationException("Invalid input");
        Result<Timer> result = Result.failure(error);
        
        // Act
        result.getDataOrThrow(); // Should throw ValidationException
    }
    
    @Test
    public void testMap_OnSuccess_TransformsData() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        
        // Act
        Result<String> mappedResult = result.map(Timer::getName);
        
        // Assert
        assertTrue("Mapped result should be successful", mappedResult.isSuccess());
        assertEquals("Mapped data should be timer name", "Test Timer", mappedResult.getData());
    }
    
    @Test
    public void testMap_OnFailure_PropagatesError() {
        // Arrange
        ValidationException error = new ValidationException("Invalid input");
        Result<Timer> result = Result.failure(error);
        
        // Act
        Result<String> mappedResult = result.map(Timer::getName);
        
        // Assert
        assertFalse("Mapped result should be a failure", mappedResult.isSuccess());
        assertEquals("Error should be propagated", error, mappedResult.getError());
    }
    
    @Test
    public void testFlatMap_OnSuccess_TransformsData() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        
        // Act
        Result<String> flatMappedResult = result.flatMap(t -> Result.success(t.getName()));
        
        // Assert
        assertTrue("Flat-mapped result should be successful", flatMappedResult.isSuccess());
        assertEquals("Flat-mapped data should be timer name", "Test Timer", flatMappedResult.getData());
    }
    
    @Test
    public void testFlatMap_OnFailure_PropagatesError() {
        // Arrange
        ValidationException error = new ValidationException("Invalid input");
        Result<Timer> result = Result.failure(error);
        
        // Act
        Result<String> flatMappedResult = result.flatMap(t -> Result.success(t.getName()));
        
        // Assert
        assertFalse("Flat-mapped result should be a failure", flatMappedResult.isSuccess());
        assertEquals("Error should be propagated", error, flatMappedResult.getError());
    }
    
    @Test
    public void testOnSuccess_OnSuccess_ExecutesConsumer() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        AtomicBoolean executed = new AtomicBoolean(false);
        
        // Act
        Result<Timer> chainedResult = result.onSuccess(t -> executed.set(true));
        
        // Assert
        assertTrue("Consumer should have been executed", executed.get());
        assertEquals("Should return same result", result, chainedResult);
    }
    
    @Test
    public void testOnSuccess_OnFailure_DoesNotExecuteConsumer() {
        // Arrange
        Result<Timer> result = Result.failure(new ValidationException("Invalid input"));
        AtomicBoolean executed = new AtomicBoolean(false);
        
        // Act
        Result<Timer> chainedResult = result.onSuccess(t -> executed.set(true));
        
        // Assert
        assertFalse("Consumer should not have been executed", executed.get());
        assertEquals("Should return same result", result, chainedResult);
    }
    
    @Test
    public void testOnFailure_OnFailure_ExecutesConsumer() {
        // Arrange
        ValidationException error = new ValidationException("Invalid input");
        Result<Timer> result = Result.failure(error);
        AtomicBoolean executed = new AtomicBoolean(false);
        
        // Act
        Result<Timer> chainedResult = result.onFailure(e -> executed.set(true));
        
        // Assert
        assertTrue("Consumer should have been executed", executed.get());
        assertEquals("Should return same result", result, chainedResult);
    }
    
    @Test
    public void testOnFailure_OnSuccess_DoesNotExecuteConsumer() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        AtomicBoolean executed = new AtomicBoolean(false);
        
        // Act
        Result<Timer> chainedResult = result.onFailure(e -> executed.set(true));
        
        // Assert
        assertFalse("Consumer should not have been executed", executed.get());
        assertEquals("Should return same result", result, chainedResult);
    }
    
    @Test
    public void testGetDataOrDefault_OnSuccess_ReturnsData() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        Timer defaultTimer = Timer.create("Default Timer", "user123", Duration.ofMinutes(15), Set.of());
        
        // Act
        Timer data = result.getDataOrDefault(defaultTimer);
        
        // Assert
        assertEquals("Should return actual data", timer, data);
    }
    
    @Test
    public void testGetDataOrDefault_OnFailure_ReturnsDefault() {
        // Arrange
        Result<Timer> result = Result.failure(new ValidationException("Invalid input"));
        Timer defaultTimer = Timer.create("Default Timer", "user123", Duration.ofMinutes(15), Set.of());
        
        // Act
        Timer data = result.getDataOrDefault(defaultTimer);
        
        // Assert
        assertEquals("Should return default data", defaultTimer, data);
    }
    
    @Test
    public void testGetDataOrDefault_WithSupplier_OnSuccess_ReturnsData() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        AtomicInteger supplierCalls = new AtomicInteger(0);
        
        // Act
        Timer data = result.getDataOrDefault(() -> {
            supplierCalls.incrementAndGet();
            return Timer.create("Default Timer", "user123", Duration.ofMinutes(15), Set.of());
        });
        
        // Assert
        assertEquals("Should return actual data", timer, data);
        assertEquals("Supplier should not have been called", 0, supplierCalls.get());
    }
    
    @Test
    public void testGetDataOrDefault_WithSupplier_OnFailure_ReturnsDefault() {
        // Arrange
        Result<Timer> result = Result.failure(new ValidationException("Invalid input"));
        AtomicInteger supplierCalls = new AtomicInteger(0);
        
        // Act
        Timer data = result.getDataOrDefault(() -> {
            supplierCalls.incrementAndGet();
            return Timer.create("Default Timer", "user123", Duration.ofMinutes(15), Set.of());
        });
        
        // Assert
        assertEquals("Should return default data", "Default Timer", data.getName());
        assertEquals("Supplier should have been called once", 1, supplierCalls.get());
    }
    
    @Test
    public void testIsRecoverable_OnSuccess_ReturnsFalse() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        
        // Act & Assert
        assertFalse("Should not be recoverable", result.isRecoverable());
    }
    
    @Test
    public void testIsRecoverable_OnFailure_ReturnsErrorRecoverability() {
        // Arrange
        ValidationException recoverableError = new ValidationException("Invalid input");
        TimerException nonRecoverableError = new TimerException("Timer not found");
        nonRecoverableError = new TimerException("Timer not found") {
            @Override
            public boolean isRecoverable() {
                return false;
            }
        };
        
        Result<Timer> recoverableResult = Result.failure(recoverableError);
        Result<Timer> nonRecoverableResult = Result.failure(nonRecoverableError);
        
        // Act & Assert
        assertTrue("Should be recoverable", recoverableResult.isRecoverable());
        assertFalse("Should not be recoverable", nonRecoverableResult.isRecoverable());
    }
    
    @Test
    public void testToString_OnSuccess_ReturnsSuccessString() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        
        // Act
        String toString = result.toString();
        
        // Assert
        assertTrue("Should contain success indicator", toString.contains("Result.success"));
        assertTrue("Should contain timer data", toString.contains("Test Timer"));
    }
    
    @Test
    public void testToString_OnFailure_ReturnsFailureString() {
        // Arrange
        ValidationException error = new ValidationException("Invalid input");
        Result<Timer> result = Result.failure(error);
        
        // Act
        String toString = result.toString();
        
        // Assert
        assertTrue("Should contain failure indicator", toString.contains("Result.failure"));
        assertTrue("Should contain error message", toString.contains("Invalid input"));
    }
    
    @Test
    public void testChaining_OnSuccess_ExecutesAllSteps() {
        // Arrange
        Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
        Result<Timer> result = Result.success(timer);
        AtomicInteger stepCount = new AtomicInteger(0);
        
        // Act
        Result<String> chainedResult = result
            .onSuccess(t -> stepCount.incrementAndGet())
            .map(Timer::getName)
            .onSuccess(name -> stepCount.incrementAndGet())
            .map(String::toUpperCase)
            .onSuccess(upperName -> stepCount.incrementAndGet());
        
        // Assert
        assertTrue("Chained result should be successful", chainedResult.isSuccess());
        assertEquals("Chained data should be uppercase name", "TEST TIMER", chainedResult.getData());
        assertEquals("All steps should have been executed", 3, stepCount.get());
    }
    
    @Test
    public void testChaining_OnFailure_StopsAtFailure() {
        // Arrange
        Result<Timer> result = Result.failure(new ValidationException("Invalid input"));
        AtomicInteger stepCount = new AtomicInteger(0);
        
        // Act
        Result<String> chainedResult = result
            .onSuccess(t -> stepCount.incrementAndGet())
            .map(Timer::getName)
            .onSuccess(name -> stepCount.incrementAndGet())
            .map(String::toUpperCase)
            .onSuccess(upperName -> stepCount.incrementAndGet());
        
        // Assert
        assertFalse("Chained result should be a failure", chainedResult.isSuccess());
        assertEquals("No steps should have been executed", 0, stepCount.get());
    }
}
