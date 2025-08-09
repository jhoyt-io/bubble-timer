package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use case for getting all timers.
 */
@Singleton
public class GetAllTimersUseCase {
    
    private final TimerRepository timerRepository;
    
    @Inject
    public GetAllTimersUseCase(TimerRepository timerRepository) {
        this.timerRepository = timerRepository;
    }
    
    /**
     * Execute the use case to get all timers.
     * @return Result containing list of timers or error
     */
    public Result<List<Timer>> execute() {
        try {
            List<Timer> timers = timerRepository.getAllTimers();
            return Result.success(timers);
        } catch (Exception e) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                "Failed to get timers: " + e.getMessage(),
                e
            ));
        }
    }
}
