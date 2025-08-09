package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.exceptions.TimerNotFoundException;
import io.jhoyt.bubbletimer.domain.exceptions.InvalidTimerStateException;
import io.jhoyt.bubbletimer.domain.exceptions.TimerException;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

/**
 * Use case for pausing a timer.
 * This encapsulates the business logic for pausing a running timer.
 */
public class PauseTimerUseCase {
    
    private final TimerRepository timerRepository;
    
    /**
     * Create a new PauseTimerUseCase
     * @param timerRepository Repository for timer operations
     */
    public PauseTimerUseCase(TimerRepository timerRepository) {
        this.timerRepository = timerRepository;
    }
    
    /**
     * Execute the use case to pause a timer
     * @param timerId ID of the timer to pause
     * @return Result containing the paused timer or an error
     */
    public Result<Timer> execute(String timerId) {
        try {
            // Validate input
            if (timerId == null || timerId.trim().isEmpty()) {
                return Result.failure(new InvalidTimerStateException("Timer ID cannot be empty"));
            }
            
            // Get current timer
            Timer currentTimer = timerRepository.getTimerById(timerId);
            if (currentTimer == null) {
                return Result.failure(new TimerNotFoundException(timerId));
            }
            
            // Check if timer can be paused
            if (!currentTimer.canPause()) {
                return Result.failure(new InvalidTimerStateException(
                    currentTimer.getState(), "pause"));
            }
            
            // Pause the timer
            Timer pausedTimer = currentTimer.pause();
            
            // Update repository
            timerRepository.updateTimer(pausedTimer);
            
            return Result.success(pausedTimer);
            
        } catch (Exception e) {
            return Result.failure(new TimerException("Failed to pause timer", e));
        }
    }
}
