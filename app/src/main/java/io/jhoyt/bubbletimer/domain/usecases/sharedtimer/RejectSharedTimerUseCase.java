package io.jhoyt.bubbletimer.domain.usecases.sharedtimer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.SharedTimer;
import io.jhoyt.bubbletimer.domain.repositories.SharedTimerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use case for rejecting a shared timer.
 */
@Singleton
public class RejectSharedTimerUseCase {
    
    private final SharedTimerRepository sharedTimerRepository;
    
    @Inject
    public RejectSharedTimerUseCase(SharedTimerRepository sharedTimerRepository) {
        this.sharedTimerRepository = sharedTimerRepository;
    }
    
    /**
     * Execute the use case to reject a shared timer.
     * @param timerId ID of the timer to reject
     * @param userId ID of the user rejecting the timer
     * @return Result containing the rejected shared timer or error
     */
    public Result<SharedTimer> execute(String timerId, String userId) {
        if (timerId == null || timerId.trim().isEmpty()) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.ValidationException(
                "Timer ID cannot be null or empty"
            ));
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.ValidationException(
                "User ID cannot be null or empty"
            ));
        }
        
        try {
            sharedTimerRepository.rejectSharedTimer(timerId, userId);
            
            // Get the updated shared timer
            SharedTimer rejectedTimer = sharedTimerRepository.getSharedTimerByTimerId(timerId);
            if (rejectedTimer == null) {
                return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                    "Shared timer not found after rejection"
                ));
            }
            
            return Result.success(rejectedTimer);
        } catch (Exception e) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                "Failed to reject shared timer: " + e.getMessage(),
                e
            ));
        }
    }
}
