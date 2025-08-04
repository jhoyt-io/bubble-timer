package io.jhoyt.bubbletimer.db;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.service.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SharedTimerRepository {
    private static final String TAG = "SharedTimerRepository";
    private final SharedTimerDao sharedTimerDao;
    private final ApiService apiService;
    private final ExecutorService executorService;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public SharedTimerRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        this.sharedTimerDao = db.sharedTimerDao();
        this.executorService = Executors.newFixedThreadPool(4);

        // Initialize Retrofit for API calls
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://5jv67tlnd1.execute-api.us-east-1.amazonaws.com/prod/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.apiService = retrofit.create(ApiService.class);
    }

    public LiveData<List<SharedTimer>> getAllSharedTimers() {
        return sharedTimerDao.getAllSharedTimers();
    }

    public LiveData<List<SharedTimer>> getPendingSharedTimers() {
        return sharedTimerDao.getSharedTimersByStatus("PENDING");
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public SharedTimer getSharedTimerById(String timerId) {
        try {
            return sharedTimerDao.getSharedTimerById(timerId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting shared timer by ID: " + timerId, e);
            return null;
        }
    }

    public void refreshSharedTimers(String authToken) {
        if (authToken == null || authToken.isEmpty()) {
            Log.w(TAG, "Cannot refresh shared timers: no auth token provided");
            error.postValue("Authentication required");
            isLoading.postValue(false);
            return;
        }

        isLoading.postValue(true);
        error.postValue(null);

        apiService.getSharedTimers("Bearer " + authToken).enqueue(new Callback<List<Timer>>() {
            @Override
            public void onResponse(Call<List<Timer>> call, Response<List<Timer>> response) {
                Log.i(TAG, "API Response received - Code: " + response.code() + ", Success: " + response.isSuccessful());
                isLoading.postValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "API call successful, processing " + response.body().size() + " timers");
                    executorService.execute(() -> {
                        // Convert Timer objects to SharedTimer entities
                        List<SharedTimer> sharedTimers = response.body().stream()
                                .filter(timer -> timer.getId() != null) // Only process timers with valid IDs
                                .map(timer -> {
                                    Log.d(TAG, "Processing timer: " + timer.getId() + 
                                          ", name: " + timer.getName() + 
                                          ", userId: " + timer.getUserId() + 
                                          ", totalDuration: " + timer.getTotalDuration());
                                    
                                    SharedTimer sharedTimer = new SharedTimer();
                                    sharedTimer.timerId = timer.getId();
                                    sharedTimer.name = timer.getName() != null ? timer.getName() : "Unknown Timer";
                                    sharedTimer.userId = timer.getUserId() != null ? timer.getUserId() : "unknown";
                                    sharedTimer.totalDuration = timer.getTotalDuration() != null ? timer.getTotalDuration() : Duration.ZERO;
                                    sharedTimer.remainingDuration = timer.getRemainingDuration();
                                    sharedTimer.timerEnd = timer.getTimerEnd();
                                    sharedTimer.status = "PENDING"; // Default status for new shared timers
                                    sharedTimer.sharedBy = timer.getUserId() != null ? timer.getUserId() : "unknown"; // The user who shared the timer
                                    sharedTimer.createdAt = LocalDateTime.now();
                                    return sharedTimer;
                                })
                                .toList();
                        
                        Log.i(TAG, "Filtered to " + sharedTimers.size() + " valid shared timers out of " + response.body().size() + " total");
                        
                        if (!sharedTimers.isEmpty()) {
                            // Insert into local database
                            sharedTimerDao.insertAll(sharedTimers);
                            Log.i(TAG, "Refreshed " + sharedTimers.size() + " shared timers");
                        } else {
                            Log.w(TAG, "No valid shared timers to insert");
                        }
                    });
                } else {
                    String errorMsg = "Failed to fetch shared timers: " + response.code();
                    if (response.code() == 401) {
                        errorMsg = "Authentication failed. Please log in again.";
                    } else if (response.code() == 403) {
                        errorMsg = "Access denied. You may not have permission to view shared timers.";
                    }
                    error.postValue(errorMsg);
                    Log.e(TAG, "API call failed with code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        } catch (IOException e) {
                            Log.e(TAG, "Could not read error body", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Timer>> call, Throwable t) {
                Log.e(TAG, "API call failed with exception", t);
                isLoading.postValue(false);
                error.postValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error fetching shared timers", t);
            }
        });
    }

    public void acceptSharedTimer(String timerId) {
        executorService.execute(() -> {
            sharedTimerDao.updateStatus(timerId, "ACCEPTED");
            Log.i(TAG, "Accepted shared timer: " + timerId);
        });
    }

    public void rejectSharedTimer(String timerId, String authToken) {
        Log.i(TAG, "Rejecting shared timer: " + timerId);
        
        if (authToken == null || authToken.isEmpty()) {
            Log.w(TAG, "Cannot reject shared timer: no auth token provided");
            error.postValue("Authentication required");
            return;
        }
        
        // Call backend API to reject the shared timer invitation
        apiService.rejectSharedTimer("Bearer " + authToken, timerId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Log.i(TAG, "Reject API Response - Code: " + response.code() + ", Success: " + response.isSuccessful());
                
                if (response.isSuccessful()) {
                    Log.i(TAG, "Successfully rejected shared timer: " + timerId);
                    // Update local database status
                    executorService.execute(() -> {
                        sharedTimerDao.updateStatus(timerId, "REJECTED");
                        Log.i(TAG, "Updated local status to REJECTED for timer: " + timerId);
                    });
                } else {
                    String errorMsg = "Failed to reject shared timer: " + response.code();
                    if (response.code() == 401) {
                        errorMsg = "Authentication failed. Please log in again.";
                    } else if (response.code() == 404) {
                        errorMsg = "Shared timer invitation not found.";
                    }
                    error.postValue(errorMsg);
                    Log.e(TAG, "Reject API call failed with code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Reject API call failed with exception", t);
                error.postValue("Network error rejecting shared timer: " + t.getMessage());
            }
        });
    }

    public void deleteSharedTimer(String timerId) {
        executorService.execute(() -> {
            sharedTimerDao.deleteById(timerId);
            Log.i(TAG, "Deleted shared timer: " + timerId);
        });
    }

    public void clearRejectedTimers() {
        executorService.execute(() -> {
            sharedTimerDao.deleteByStatus("REJECTED");
            Log.i(TAG, "Cleared rejected shared timers");
        });
    }
} 