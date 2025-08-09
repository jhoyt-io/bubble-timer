package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.exceptions.TimerNotFoundException;
import io.jhoyt.bubbletimer.domain.exceptions.TimerException;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

/**
 * Use case for stopping a timer.
 * This encapsulates the business logic for stopping a timer (setting it to stopped state).
 */
public class StopTimerUseCase {
    
    private final TimerRepository timerRepository;
    
    /**
     * Create a new StopTimerUseCase
     * @param timerRepository Repository for timer operations
     */
    public StopTimerUseCase(TimerRepository timerRepository) {
        this.timerRepository = timerRepository;
    }
    
    /**
     * Execute the use case to stop a timer
     * @param timerId ID of the timer to stop
     * @return Result containing the stopped timer or an error
     */
    public Result<Timer> execute(String timerId) {
        try {
            // Validate input
            if (timerId == null || timerId.trim().isEmpty()) {
                return Result.failure(new TimerException("Timer ID cannot be empty"));
            }
            
            // Get current timer
            Timer currentTimer = timerRepository.getTimerById(timerId);
            if (currentTimer == null) {
                return Result.failure(new TimerNotFoundException(timerId));
            }
            
            // Stop the timer (can be stopped from any state)
            Timer stoppedTimer = currentTimer.stop();
            
            // Update repository
            timerRepository.updateTimer(stoppedTimer);
            
            return Result.success(stoppedTimer);
            
        } catch (Exception e) {
            return Result.failure(new TimerException("Failed to stop timer", e));
        }
    }
}
