package io.jhoyt.bubbletimer.service;

import android.content.Context;
import android.util.Log;

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Map;

public class TimerSharingService {
    private static final String TAG = "TimerSharingService";
    private static final String BASE_URL = "https://5jv67tlnd1.execute-api.us-east-1.amazonaws.com/prod/";
    
    private ApiService apiService;
    
    public interface SharingCallback {
        void onSharingSuccess(List<String> successUsers, List<String> failedUsers);
        void onSharingError(String error);
    }
    
    public TimerSharingService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        this.apiService = retrofit.create(ApiService.class);
    }
    
    /**
     * Share a timer with multiple users via REST API (triggers push notifications)
     * @param timerId The ID of the timer to share
     * @param userIds The list of user IDs to share with
     * @param callback Callback to handle success/failure
     */
    public void shareTimerWithUsers(String timerId, Set<String> userIds, SharingCallback callback) {
        shareTimerWithUsers(timerId, userIds, null, callback);
    }
    
    /**
     * Share a timer with multiple users via REST API (triggers push notifications)
     * @param timerId The ID of the timer to share
     * @param userIds The list of user IDs to share with
     * @param timerData Timer data for backend creation if timer doesn't exist
     * @param callback Callback to handle success/failure
     */
    public void shareTimerWithUsers(String timerId, Set<String> userIds, Map<String, Object> timerData, SharingCallback callback) {
        Log.i(TAG, "Sharing timer " + timerId + " with users: " + userIds);
        
        if (timerId == null || timerId.trim().isEmpty()) {
            callback.onSharingError("Timer ID cannot be null or empty");
            return;
        }
        
        if (userIds == null || userIds.isEmpty()) {
            callback.onSharingError("No users specified for sharing");
            return;
        }
        
        // Get auth token
        Amplify.Auth.fetchAuthSession(authSession -> {
            AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) authSession;
            String idToken = cognitoAuthSession.getUserPoolTokensResult().getValue().getIdToken();
            
            if (idToken == null || idToken.isEmpty()) {
                callback.onSharingError("Authentication token not available");
                return;
            }
            
            // Convert Set to List for API request
            List<String> userIdList = new ArrayList<>(userIds);
            ApiService.ShareTimerRequest request = new ApiService.ShareTimerRequest(timerId, userIdList, timerData);
            
            // Call the sharing API
            apiService.shareTimerWithUsers("Bearer " + idToken, request)
                    .enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Map<String, Object> result = response.body();
                                if (result != null) {
                                    @SuppressWarnings("unchecked")
                                    List<String> successUsers = (List<String>) result.get("success");
                                    @SuppressWarnings("unchecked")
                                    List<String> failedUsers = (List<String>) result.get("failed");
                                    
                                    Log.i(TAG, "Timer sharing completed - Success: " + 
                                          (successUsers != null ? successUsers.size() : 0) + 
                                          ", Failed: " + (failedUsers != null ? failedUsers.size() : 0));
                                    
                                    callback.onSharingSuccess(
                                        successUsers != null ? successUsers : new ArrayList<>(),
                                        failedUsers != null ? failedUsers : new ArrayList<>()
                                    );
                                } else {
                                    callback.onSharingError("Empty response from server");
                                }
                            } else {
                                String errorMsg = "Failed to share timer: " + response.code();
                                Log.e(TAG, errorMsg);
                                callback.onSharingError(errorMsg);
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            String errorMsg = "Network error while sharing timer: " + t.getMessage();
                            Log.e(TAG, errorMsg, t);
                            callback.onSharingError(errorMsg);
                        }
                    });
            
        }, error -> {
            String errorMsg = "Authentication error: " + error.getMessage();
            Log.e(TAG, errorMsg, error);
            callback.onSharingError(errorMsg);
        });
    }
}
