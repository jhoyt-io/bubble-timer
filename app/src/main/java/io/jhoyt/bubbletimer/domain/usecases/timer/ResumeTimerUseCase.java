package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.exceptions.TimerNotFoundException;
import io.jhoyt.bubbletimer.domain.exceptions.InvalidTimerStateException;
import io.jhoyt.bubbletimer.domain.exceptions.TimerException;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

/**
 * Use case for resuming a paused timer.
 * This encapsulates the business logic for resuming a paused timer.
 */
public class ResumeTimerUseCase {
    
    private final TimerRepository timerRepository;
    
    /**
     * Create a new ResumeTimerUseCase
     * @param timerRepository Repository for timer operations
     */
    public ResumeTimerUseCase(TimerRepository timerRepository) {
        this.timerRepository = timerRepository;
    }
    
    /**
     * Execute the use case to resume a timer
     * @param timerId ID of the timer to resume
     * @return Result containing the resumed timer or an error
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
            
            // Check if timer can be resumed
            if (!currentTimer.canResume()) {
                return Result.failure(new InvalidTimerStateException(
                    currentTimer.getState(), "resume"));
            }
            
            // Resume the timer
            Timer resumedTimer = currentTimer.resume();
            
            // Update repository
            timerRepository.updateTimer(resumedTimer);
            
            return Result.success(resumedTimer);
            
        } catch (Exception e) {
            return Result.failure(new TimerException("Failed to resume timer", e));
        }
    }
}
