package io.jhoyt.bubbletimer.domain.usecases.timer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use case for getting timers by tag.
 */
@Singleton
public class GetTimersByTagUseCase {
    
    private final TimerRepository timerRepository;
    
    @Inject
    public GetTimersByTagUseCase(TimerRepository timerRepository) {
        this.timerRepository = timerRepository;
    }
    
    /**
     * Execute the use case to get timers by tag.
     * @param tag Tag to filter timers by
     * @return Result containing list of timers or error
     */
    public Result<List<Timer>> execute(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.ValidationException(
                "Tag cannot be null or empty"
            ));
        }
        
        try {
            List<Timer> timers = timerRepository.getTimersByTag(tag);
            return Result.success(timers);
        } catch (Exception e) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                "Failed to get timers by tag: " + e.getMessage(),
                e
            ));
        }
    }
}
