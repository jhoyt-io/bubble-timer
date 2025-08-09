package io.jhoyt.bubbletimer.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.entities.SharedTimer;
import io.jhoyt.bubbletimer.domain.usecases.sharedtimer.GetAllSharedTimersUseCase;
import io.jhoyt.bubbletimer.domain.usecases.sharedtimer.AcceptSharedTimerUseCase;

import java.util.List;

import javax.inject.Inject;

/**
 * SharedTimerViewModel that uses domain use cases.
 * This demonstrates how to integrate the new domain layer with existing ViewModels.
 */
public class SharedTimerViewModel extends ViewModel {
    
    private final GetAllSharedTimersUseCase getAllSharedTimersUseCase;
    private final AcceptSharedTimerUseCase acceptSharedTimerUseCase;
    
    private final MutableLiveData<List<SharedTimer>> sharedTimers = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    @Inject
    public SharedTimerViewModel(GetAllSharedTimersUseCase getAllSharedTimersUseCase,
                              AcceptSharedTimerUseCase acceptSharedTimerUseCase) {
        this.getAllSharedTimersUseCase = getAllSharedTimersUseCase;
        this.acceptSharedTimerUseCase = acceptSharedTimerUseCase;
    }
    
    /**
     * Load shared timers using the domain use case
     */
    public void loadSharedTimers() {
        isLoading.setValue(true);
        
        Result<List<SharedTimer>> result = getAllSharedTimersUseCase.execute();
        
        result.onSuccess(timers -> {
            sharedTimers.setValue(timers);
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isLoading.setValue(false);
        });
    }
    
    /**
     * Accept a shared timer using the domain use case
     */
    public void acceptSharedTimer(String timerId, String userId) {
        if (timerId == null || timerId.trim().isEmpty()) {
            errorMessage.setValue("Invalid timer ID");
            return;
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            errorMessage.setValue("Invalid user ID");
            return;
        }
        
        isLoading.setValue(true);
        
        Result<SharedTimer> result = acceptSharedTimerUseCase.execute(timerId, userId);
        
        result.onSuccess(acceptedTimer -> {
            // Refresh the shared timers list
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isLoading.setValue(false);
        });
    }
    
    /**
     * Clear the error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    // LiveData getters
    
    public LiveData<List<SharedTimer>> getSharedTimers() {
        return sharedTimers;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}
