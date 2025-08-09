package io.jhoyt.bubbletimer.service;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Map;

public class FcmTokenManager {
    private static final String TAG = "FcmTokenManager";
    private static final String BASE_URL = "https://5jv67tlnd1.execute-api.us-east-1.amazonaws.com/prod/";
    
    private final ApiService apiService;
    private final Context context;
    
    public FcmTokenManager(Context context) {
        this.context = context;
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        this.apiService = retrofit.create(ApiService.class);
    }
    
    public void registerDeviceToken(String fcmToken) {
        Amplify.Auth.getCurrentUser(authUser -> {
            String userId = authUser.getUsername();
            if (userId == null || userId.isEmpty()) {
                Log.w(TAG, "Cannot register token - no authenticated user");
                return;
            }
            
            Amplify.Auth.fetchAuthSession(authSession -> {
                AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) authSession;
                String idToken = cognitoAuthSession.getUserPoolTokensResult().getValue().getIdToken();
                
                if (idToken == null || idToken.isEmpty()) {
                    Log.w(TAG, "Cannot register token - no auth token");
                    return;
                }
                
                String deviceId = Settings.Secure.getString(
                        context.getContentResolver(), 
                        Settings.Secure.ANDROID_ID
                );
                
                ApiService.DeviceTokenRequest request = new ApiService.DeviceTokenRequest(deviceId, fcmToken);
                
                apiService.registerDeviceToken("Bearer " + idToken, request)
                        .enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                if (response.isSuccessful()) {
                                    Log.i(TAG, "FCM token registered successfully with backend");
                                } else {
                                    Log.e(TAG, "Failed to register FCM token with backend: " + response.code());
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                Log.e(TAG, "Error registering FCM token with backend", t);
                            }
                        });
                
            }, error -> {
                Log.e(TAG, "Error fetching auth session for token registration", error);
            });
            
        }, error -> {
            Log.e(TAG, "Error getting current user for token registration", error);
        });
    }
    
    public void removeDeviceToken() {
        Amplify.Auth.getCurrentUser(authUser -> {
            String userId = authUser.getUsername();
            if (userId == null || userId.isEmpty()) {
                Log.w(TAG, "Cannot remove token - no authenticated user");
                return;
            }
            
            Amplify.Auth.fetchAuthSession(authSession -> {
                AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) authSession;
                String idToken = cognitoAuthSession.getUserPoolTokensResult().getValue().getIdToken();
                
                if (idToken == null || idToken.isEmpty()) {
                    Log.w(TAG, "Cannot remove token - no auth token");
                    return;
                }
                
                String deviceId = Settings.Secure.getString(
                        context.getContentResolver(), 
                        Settings.Secure.ANDROID_ID
                );
                
                apiService.removeDeviceToken("Bearer " + idToken, deviceId)
                        .enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                if (response.isSuccessful()) {
                                    Log.i(TAG, "FCM token removed successfully from backend");
                                } else {
                                    Log.e(TAG, "Failed to remove FCM token from backend: " + response.code());
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                Log.e(TAG, "Error removing FCM token from backend", t);
                            }
                        });
                
            }, error -> {
                Log.e(TAG, "Error fetching auth session for token removal", error);
            });
            
        }, error -> {
            Log.e(TAG, "Error getting current user for token removal", error);
        });
    }
}
