package io.jhoyt.bubbletimer.service;

import io.jhoyt.bubbletimer.Timer;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Body;

import java.util.List;
import java.util.Map;

public interface ApiService {
    @GET("timers/shared")
    Call<List<Timer>> getSharedTimers(@Header("Authorization") String authToken);
    
    @DELETE("timers/shared")
    Call<Map<String, Object>> rejectSharedTimer(@Header("Authorization") String authToken, @Body RejectTimerRequest request);
    
    @POST("timers/shared")
    Call<Map<String, Object>> shareTimerWithUsers(@Header("Authorization") String authToken, @Body ShareTimerRequest request);
    
    @POST("device-tokens")
    Call<Map<String, Object>> registerDeviceToken(@Header("Authorization") String authToken, @Body DeviceTokenRequest request);
    
    @DELETE("device-tokens/{deviceId}")
    Call<Map<String, Object>> removeDeviceToken(@Header("Authorization") String authToken, @Query("deviceId") String deviceId);
    
    class DeviceTokenRequest {
        public String deviceId;
        public String fcmToken;
        
        public DeviceTokenRequest(String deviceId, String fcmToken) {
            this.deviceId = deviceId;
            this.fcmToken = fcmToken;
        }
    }
    
    class ShareTimerRequest {
        public String timerId;
        public List<String> userIds;
        public Map<String, Object> timer; // Timer data for backend creation if needed
        
        public ShareTimerRequest(String timerId, List<String> userIds) {
            this.timerId = timerId;
            this.userIds = userIds;
        }
        
        public ShareTimerRequest(String timerId, List<String> userIds, Map<String, Object> timer) {
            this.timerId = timerId;
            this.userIds = userIds;
            this.timer = timer;
        }
    }
    
    class RejectTimerRequest {
        public String timerId;
        
        public RejectTimerRequest(String timerId) {
            this.timerId = timerId;
        }
    }
} 