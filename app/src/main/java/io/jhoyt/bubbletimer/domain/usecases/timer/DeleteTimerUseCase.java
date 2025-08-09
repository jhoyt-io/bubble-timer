package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use case for deleting a timer.
 */
@Singleton
public class DeleteTimerUseCase {
    
    private final TimerRepository timerRepository;
    
    @Inject
    public DeleteTimerUseCase(TimerRepository timerRepository) {
        this.timerRepository = timerRepository;
    }
    
    /**
     * Execute the use case to delete a timer.
     * @param timerId ID of the timer to delete
     * @return Result indicating success or error
     */
    public Result<Void> execute(String timerId) {
        if (timerId == null || timerId.trim().isEmpty()) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.ValidationException(
                "Timer ID cannot be null or empty"
            ));
        }
        
        try {
            timerRepository.deleteTimer(timerId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                "Failed to delete timer: " + e.getMessage(),
                e
            ));
        }
    }
}
