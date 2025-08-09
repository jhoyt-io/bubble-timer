package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use case for getting a timer by ID.
 */
@Singleton
public class GetTimerByIdUseCase {
    
    private final TimerRepository timerRepository;
    
    @Inject
    public GetTimerByIdUseCase(TimerRepository timerRepository) {
        this.timerRepository = timerRepository;
    }
    
    /**
     * Execute the use case to get a timer by ID.
     * @param timerId ID of the timer to get
     * @return Result containing the timer or error
     */
    public Result<Timer> execute(String timerId) {
        if (timerId == null || timerId.trim().isEmpty()) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.ValidationException(
                "Timer ID cannot be null or empty"
            ));
        }
        
        try {
            Timer timer = timerRepository.getTimerById(timerId);
            if (timer == null) {
                return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                    "Timer not found with ID: " + timerId
                ));
            }
            return Result.success(timer);
        } catch (Exception e) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                "Failed to get timer by ID: " + e.getMessage(),
                e
            ));
        }
    }
}
