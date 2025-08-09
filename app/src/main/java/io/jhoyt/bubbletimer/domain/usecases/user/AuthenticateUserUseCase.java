package io.jhoyt.bubbletimer.domain.usecases.user;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.User;
import io.jhoyt.bubbletimer.domain.repositories.UserRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use case for authenticating a user.
 */
@Singleton
public class AuthenticateUserUseCase {
    
    private final UserRepository userRepository;
    
    @Inject
    public AuthenticateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Execute the use case to authenticate a user.
     * @param username Username for authentication
     * @param password Password for authentication
     * @return Result containing the authenticated user or error
     */
    public Result<User> execute(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.ValidationException(
                "Username cannot be null or empty"
            ));
        }
        
        if (password == null || password.trim().isEmpty()) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.ValidationException(
                "Password cannot be null or empty"
            ));
        }
        
        try {
            User authenticatedUser = userRepository.authenticateUser(username, password);
            if (authenticatedUser == null) {
                return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.UserException(
                    "Authentication failed: invalid credentials"
                ));
            }
            return Result.success(authenticatedUser);
        } catch (Exception e) {
            return Result.failure(new io.jhoyt.bubbletimer.domain.exceptions.UserException(
                "Authentication failed: " + e.getMessage(),
                e
            ));
        }
    }
}
