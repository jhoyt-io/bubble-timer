package io.jhoyt.bubbletimer.service;

import io.jhoyt.bubbletimer.Timer;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

import java.util.List;
import java.util.Map;

public interface ApiService {
    @GET("timers/shared")
    Call<List<Timer>> getSharedTimers(@Header("Authorization") String authToken);
    
    @DELETE("timers/shared")
    Call<Map<String, Object>> rejectSharedTimer(@Header("Authorization") String authToken, @Query("timerId") String timerId);
} 