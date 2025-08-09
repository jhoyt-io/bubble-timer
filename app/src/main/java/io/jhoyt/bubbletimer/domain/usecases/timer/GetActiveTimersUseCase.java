package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.exceptions.TimerException;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

import java.util.List;

/**
 * Use case for getting active timers.
 * This encapsulates the business logic for retrieving active (running or paused) timers.
 */
public class GetActiveTimersUseCase {
    
    private final TimerRepository timerRepository;
    
    /**
     * Create a new GetActiveTimersUseCase
     * @param timerRepository Repository for timer operations
     */
    public GetActiveTimersUseCase(TimerRepository timerRepository) {
        this.timerRepository = timerRepository;
    }
    
    /**
     * Execute the use case to get all active timers
     * @return Result containing the list of active timers or an error
     */
    public Result<List<Timer>> execute() {
        try {
            List<Timer> activeTimers = timerRepository.getActiveTimers();
            return Result.success(activeTimers);
            
        } catch (Exception e) {
            return Result.failure(new TimerException("Failed to get active timers", e));
        }
    }
    
    /**
     * Execute the use case to get active timers for a specific user
     * @param userId User ID to filter by
     * @return Result containing the list of active timers for the user or an error
     */
    public Result<List<Timer>> execute(String userId) {
        try {
            // Validate input
            if (userId == null || userId.trim().isEmpty()) {
                return Result.failure(new TimerException("User ID cannot be empty"));
            }
            
            List<Timer> allActiveTimers = timerRepository.getActiveTimers();
            
            // Filter by user ID
            List<Timer> userActiveTimers = allActiveTimers.stream()
                .filter(timer -> userId.equals(timer.getUserId()))
                .toList();
            
            return Result.success(userActiveTimers);
            
        } catch (Exception e) {
            return Result.failure(new TimerException("Failed to get active timers for user", e));
        }
    }
}
