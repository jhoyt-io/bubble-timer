package io.jhoyt.bubbletimer.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.usecases.timer.GetAllTimersUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.GetTimersByTagUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.GetTimerByIdUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.StartTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.PauseTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.ResumeTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.StopTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.DeleteTimerUseCase;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Updated TimerViewModel that uses domain use cases.
 * This demonstrates how to integrate the new domain layer with existing ViewModels.
 */
public class TimerViewModel extends ViewModel {
    
    private final GetAllTimersUseCase getAllTimersUseCase;
    private final GetTimersByTagUseCase getTimersByTagUseCase;
    private final GetTimerByIdUseCase getTimerByIdUseCase;
    private final StartTimerUseCase startTimerUseCase;
    private final PauseTimerUseCase pauseTimerUseCase;
    private final ResumeTimerUseCase resumeTimerUseCase;
    private final StopTimerUseCase stopTimerUseCase;
    private final DeleteTimerUseCase deleteTimerUseCase;
    
    private final MutableLiveData<List<Timer>> allTimers = new MutableLiveData<>();
    private final MutableLiveData<List<Timer>> timersByTag = new MutableLiveData<>();
    private final MutableLiveData<Timer> selectedTimer = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    @Inject
    public TimerViewModel(GetAllTimersUseCase getAllTimersUseCase,
                           GetTimersByTagUseCase getTimersByTagUseCase,
                           GetTimerByIdUseCase getTimerByIdUseCase,
                           StartTimerUseCase startTimerUseCase,
                           PauseTimerUseCase pauseTimerUseCase,
                           ResumeTimerUseCase resumeTimerUseCase,
                           StopTimerUseCase stopTimerUseCase,
                           DeleteTimerUseCase deleteTimerUseCase) {
        this.getAllTimersUseCase = getAllTimersUseCase;
        this.getTimersByTagUseCase = getTimersByTagUseCase;
        this.getTimerByIdUseCase = getTimerByIdUseCase;
        this.startTimerUseCase = startTimerUseCase;
        this.pauseTimerUseCase = pauseTimerUseCase;
        this.resumeTimerUseCase = resumeTimerUseCase;
        this.stopTimerUseCase = stopTimerUseCase;
        this.deleteTimerUseCase = deleteTimerUseCase;
    }
    
    /**
     * Load all timers using the domain use case
     */
    public void loadAllTimers() {
        isLoading.setValue(true);
        
        Result<List<Timer>> result = getAllTimersUseCase.execute();
        
        result.onSuccess(timers -> {
            allTimers.setValue(timers);
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isLoading.setValue(false);
        });
    }
    
    /**
     * Load timers by tag using the domain use case
     */
    public void loadTimersByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            loadAllTimers();
            return;
        }
        
        isLoading.setValue(true);
        
        Result<List<Timer>> result = getTimersByTagUseCase.execute(tag);
        
        result.onSuccess(timers -> {
            timersByTag.setValue(timers);
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isLoading.setValue(false);
        });
    }
    
    /**
     * Load timer by ID using the domain use case
     */
    public void loadTimerById(String timerId) {
        if (timerId == null || timerId.trim().isEmpty()) {
            errorMessage.setValue("Invalid timer ID");
            return;
        }
        
        isLoading.setValue(true);
        
        Result<Timer> result = getTimerByIdUseCase.execute(timerId);
        
        result.onSuccess(timer -> {
            selectedTimer.setValue(timer);
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isLoading.setValue(false);
        });
    }
    
    /**
     * Start a new timer using the domain use case
     */
    public void startNewTimer(String name, int durationMinutes, String userId) {
        isLoading.setValue(true);
        
        Result<Timer> result = startTimerUseCase.execute(name, durationMinutes, userId);
        
        result.onSuccess(timer -> {
            // Refresh the timer list to include the new timer
            loadAllTimers();
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isLoading.setValue(false);
        });
    }
    
    /**
     * Pause a timer using the domain use case
     */
    public void pauseTimer(String timerId) {
        if (timerId == null || timerId.trim().isEmpty()) {
            errorMessage.setValue("Invalid timer ID");
            return;
        }
        
        isLoading.setValue(true);
        
        Result<Timer> result = pauseTimerUseCase.execute(timerId);
        
        result.onSuccess(pausedTimer -> {
            // Update the timer in the list
            updateTimerInList(pausedTimer);
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isLoading.setValue(false);
        });
    }
    
    /**
     * Resume a timer using the domain use case
     */
    public void resumeTimer(String timerId) {
        if (timerId == null || timerId.trim().isEmpty()) {
            errorMessage.setValue("Invalid timer ID");
            return;
        }
        
        isLoading.setValue(true);
        
        Result<Timer> result = resumeTimerUseCase.execute(timerId);
        
        result.onSuccess(resumedTimer -> {
            // Update the timer in the list
            updateTimerInList(resumedTimer);
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isLoading.setValue(false);
        });
    }
    
    /**
     * Stop a timer using the domain use case
     */
    public void stopTimer(String timerId) {
        if (timerId == null || timerId.trim().isEmpty()) {
            errorMessage.setValue("Invalid timer ID");
            return;
        }
        
        isLoading.setValue(true);
        
        Result<Timer> result = stopTimerUseCase.execute(timerId);
        
        result.onSuccess(stoppedTimer -> {
            // Update the timer in the list
            updateTimerInList(stoppedTimer);
            isLoading.setValue(false);
        }).onFailure(error -> {
            errorMessage.setValue(error.getUserFriendlyMessage());
            isLoading.setValue(false);
        });
    }
    
    /**
     * Delete a timer using the domain use case
     */
    public void deleteTimer(String timerId) {
        if (timerId == null || timerId.trim().isEmpty()) {
            errorMessage.setValue("Invalid timer ID");
            return;
        }
        
        isLoading.setValue(true);
        
        Result<Void> result = deleteTimerUseCase.execute(timerId);
        
        result.onSuccess(__ -> {
            // Refresh the timer list to remove the deleted timer
            loadAllTimers();
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
    
    /**
     * Update a timer in the current list
     */
    private void updateTimerInList(Timer updatedTimer) {
        List<Timer> currentTimers = allTimers.getValue();
        if (currentTimers != null) {
            for (int i = 0; i < currentTimers.size(); i++) {
                if (currentTimers.get(i).getId().equals(updatedTimer.getId())) {
                    currentTimers.set(i, updatedTimer);
                    allTimers.setValue(currentTimers);
                    break;
                }
            }
        }
        
        // Also update in tag-filtered list if it exists
        List<Timer> currentTagTimers = timersByTag.getValue();
        if (currentTagTimers != null) {
            for (int i = 0; i < currentTagTimers.size(); i++) {
                if (currentTagTimers.get(i).getId().equals(updatedTimer.getId())) {
                    currentTagTimers.set(i, updatedTimer);
                    timersByTag.setValue(currentTagTimers);
                    break;
                }
            }
        }
        
        // Update selected timer if it's the same one
        Timer currentSelected = selectedTimer.getValue();
        if (currentSelected != null && currentSelected.getId().equals(updatedTimer.getId())) {
            selectedTimer.setValue(updatedTimer);
        }
    }
    
    // LiveData getters
    
    public LiveData<List<Timer>> getAllTimers() {
        return allTimers;
    }
    
    public LiveData<List<Timer>> getTimersByTag() {
        return timersByTag;
    }
    
    public LiveData<Timer> getSelectedTimer() {
        return selectedTimer;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}
