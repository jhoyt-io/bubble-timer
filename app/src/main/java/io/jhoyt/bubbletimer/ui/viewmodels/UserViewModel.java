package io.jhoyt.bubbletimer.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.User;
import io.jhoyt.bubbletimer.domain.usecases.user.GetCurrentUserUseCase;
import io.jhoyt.bubbletimer.domain.usecases.user.AuthenticateUserUseCase;

import javax.inject.Inject;

/**
 * Updated UserViewModel that uses domain use cases.
 * This demonstrates how to integrate the new domain layer with existing ViewModels.
 */
public class UserViewModel extends ViewModel {
    
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAuthenticated = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    @Inject
    public UserViewModel(GetCurrentUserUseCase getCurrentUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
    }
    
    /**
     * Load current user using the domain use case
     */
    public void loadCurrentUser() {
        isLoading.setValue(true);
        
        Result<User> result = getCurrentUserUseCase.execute();
        
        result.onSuccess(user -> {
            currentUser.setValue(user);
            isAuthenticated.setValue(user.isAuthenticated());
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isAuthenticated.setValue(false);
            isLoading.setValue(false);
        });
    }
    
    /**
     * Authenticate user using the domain use case
     */
    public void authenticateUser(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            errorMessage.setValue("Username cannot be empty");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("Password cannot be empty");
            return;
        }
        
        isLoading.setValue(true);
        
        Result<User> result = authenticateUserUseCase.execute(username, password);
        
        result.onSuccess(user -> {
            currentUser.setValue(user);
            isAuthenticated.setValue(user.isAuthenticated());
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isAuthenticated.setValue(false);
            isLoading.setValue(false);
        });
    }
    
    /**
     * Logout current user
     */
    public void logoutUser() {
        // For now, just clear the current user
        // In a real implementation, this would call a logout use case
        currentUser.setValue(null);
        isAuthenticated.setValue(false);
    }
    
    /**
     * Clear the error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    // LiveData getters
    
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }
    
    public LiveData<Boolean> getIsAuthenticated() {
        return isAuthenticated;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}
