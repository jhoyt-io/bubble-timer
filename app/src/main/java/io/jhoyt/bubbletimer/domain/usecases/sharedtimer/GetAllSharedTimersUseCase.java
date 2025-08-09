package io.jhoyt.bubbletimer.domain.usecases.sharedtimer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.SharedTimer;
import io.jhoyt.bubbletimer.domain.repositories.SharedTimerRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use case for getting all shared timers.
 */
@Singleton
public class GetAllSharedTimersUseCase {
    
    private final SharedTimerRepository sharedTimerRepository;
    
    @Inject
    public GetAllSharedTimersUseCase(SharedTimerRepository sharedTimerRepository) {
        this.sharedTimerRepository = sharedTimerRepository;
    }
    
    /**
     * Execute the use case to get all shared timers.
     * @return Result containing list of shared timers or error
     */
    public Result<List<SharedTimer>> execute() {
        try {
            List<SharedTimer> sharedTimers = sharedTimerRepository.getAllSharedTimers();
            return Result.success(sharedTimers);
        } catch (Exception e) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                "Failed to get shared timers: " + e.getMessage(),
                e
            ));
        }
    }
}
