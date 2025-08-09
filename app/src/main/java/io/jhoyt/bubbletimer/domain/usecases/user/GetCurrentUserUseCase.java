package io.jhoyt.bubbletimer.domain.usecases.user;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.User;
import io.jhoyt.bubbletimer.domain.repositories.UserRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use case for getting the current user.
 */
@Singleton
public class GetCurrentUserUseCase {
    
    private final UserRepository userRepository;
    
    @Inject
    public GetCurrentUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Execute the use case to get the current user.
     * @return Result containing the current user or error
     */
    public Result<User> execute() {
        try {
            User currentUser = userRepository.getCurrentUser();
            if (currentUser == null) {
                return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.UserException(
                    "No current user found"
                ));
            }
            return Result.success(currentUser);
        } catch (Exception e) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.UserException(
                "Failed to get current user: " + e.getMessage(),
                e
            ));
        }
    }
}
