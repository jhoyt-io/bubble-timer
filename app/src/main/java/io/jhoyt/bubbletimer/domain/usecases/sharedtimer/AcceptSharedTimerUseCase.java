package io.jhoyt.bubbletimer.domain.usecases.sharedtimer;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.SharedTimer;
import io.jhoyt.bubbletimer.domain.repositories.SharedTimerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use case for accepting a shared timer.
 */
@Singleton
public class AcceptSharedTimerUseCase {
    
    private final SharedTimerRepository sharedTimerRepository;
    
    @Inject
    public AcceptSharedTimerUseCase(SharedTimerRepository sharedTimerRepository) {
        this.sharedTimerRepository = sharedTimerRepository;
    }
    
    /**
     * Execute the use case to accept a shared timer.
     * @param timerId ID of the timer to accept
     * @param userId ID of the user accepting the timer
     * @return Result containing the accepted shared timer or error
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
            sharedTimerRepository.acceptSharedTimer(timerId, userId);
            
            // Get the updated shared timer
            SharedTimer acceptedTimer = sharedTimerRepository.getSharedTimerByTimerId(timerId);
            if (acceptedTimer == null) {
                return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                    "Shared timer not found after acceptance"
                ));
            }
            
            return Result.success(acceptedTimer);
        } catch (Exception e) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.TimerException(
                "Failed to accept shared timer: " + e.getMessage(),
                e
            ));
        }
    }
}
