package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.exceptions.ValidationException;
import io.jhoyt.bubbletimer.domain.exceptions.TimerException;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

import java.time.Duration;
import java.util.Set;

/**
 * Use case for starting a new timer.
 * This encapsulates the business logic for creating and starting a timer.
 */
public class StartTimerUseCase {
    
    private final TimerRepository timerRepository;
    
    /**
     * Create a new StartTimerUseCase
     * @param timerRepository Repository for timer operations
     */
    public StartTimerUseCase(TimerRepository timerRepository) {
        this.timerRepository = timerRepository;
    }
    
    /**
     * Execute the use case to start a new timer
     * @param name Timer name
     * @param duration Timer duration
     * @param tags Set of tags for the timer
     * @param userId User ID who is creating the timer
     * @return Result containing the created timer or an error
     */
    public Result<Timer> execute(String name, Duration duration, Set<String> tags, String userId) {
        try {
            // Validate inputs
            Result<Void> validationResult = validateInputs(name, duration, userId);
            if (validationResult.isFailure()) {
                return Result.failure(validationResult.getError());
            }
            
            // Create domain entity
            Timer timer = Timer.create(name, userId, duration, tags);
            
            // Save to repository
            timerRepository.saveTimer(timer);
            
            return Result.success(timer);
            
        } catch (Exception e) {
            return Result.failure(new TimerException("Failed to start timer", e));
        }
    }
    
    /**
     * Execute the use case with minimal parameters
     * @param name Timer name
     * @param durationMinutes Duration in minutes
     * @param userId User ID who is creating the timer
     * @return Result containing the created timer or an error
     */
    public Result<Timer> execute(String name, int durationMinutes, String userId) {
        Duration duration = Duration.ofMinutes(durationMinutes);
        return execute(name, duration, Set.of(), userId);
    }
    
    /**
     * Execute the use case with duration in seconds
     * @param name Timer name
     * @param durationSeconds Duration in seconds
     * @param userId User ID who is creating the timer
     * @return Result containing the created timer or an error
     */
    public Result<Timer> execute(String name, long durationSeconds, String userId) {
        Duration duration = Duration.ofSeconds(durationSeconds);
        return execute(name, duration, Set.of(), userId);
    }
    
    /**
     * Validate the input parameters
     * @param name Timer name
     * @param duration Timer duration
     * @param userId User ID
     * @return Result indicating validation success or failure
     */
    private Result<Void> validateInputs(String name, Duration duration, String userId) {
        // Validate name
        if (name == null || name.trim().isEmpty()) {
            return Result.failure(new ValidationException("Timer name cannot be empty"));
        }
        
        if (name.trim().length() > 100) {
            return Result.failure(new ValidationException("Timer name cannot exceed 100 characters"));
        }
        
        // Validate duration
        if (duration == null) {
            return Result.failure(new ValidationException("Timer duration cannot be null"));
        }
        
        if (duration.isNegative() || duration.isZero()) {
            return Result.failure(new ValidationException("Timer duration must be positive"));
        }
        
        if (duration.toHours() > 24) {
            return Result.failure(new ValidationException("Timer duration cannot exceed 24 hours"));
        }
        
        // Validate user ID
        if (userId == null || userId.trim().isEmpty()) {
            return Result.failure(new ValidationException("User ID cannot be empty"));
        }
        
        return Result.success(null);
    }
}
