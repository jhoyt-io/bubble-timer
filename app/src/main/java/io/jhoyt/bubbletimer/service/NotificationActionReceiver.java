package io.jhoyt.bubbletimer.service;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.jhoyt.bubbletimer.db.SharedTimerViewModel;
import io.jhoyt.bubbletimer.Timer;

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationActionReceiver";
    private static final String BASE_URL = "https://5jv67tlnd1.execute-api.us-east-1.amazonaws.com/prod/";
    
    private ApiService apiService;
    
    // Runtime protection against infinite loops
    private static final AtomicInteger messageCounter = new AtomicInteger(0);
    private static final int MAX_MESSAGES_PER_MINUTE = 10;
    private static long lastResetTime = System.currentTimeMillis();
    
    public NotificationActionReceiver() {
        // Initialize API service for decline operations
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        this.apiService = retrofit.create(ApiService.class);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.w(TAG, "Received null intent");
            return;
        }
        
        // Runtime protection: Check for potential infinite loops
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime > 60000) { // Reset counter every minute
            messageCounter.set(0);
            lastResetTime = currentTime;
        }
        
        int currentCount = messageCounter.incrementAndGet();
        if (currentCount > MAX_MESSAGES_PER_MINUTE) {
            Log.e(TAG, "Potential infinite loop detected! Message count: " + currentCount + 
                      " in the last minute. Ignoring message.");
            return;
        }
        
        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "Received intent with null action");
            return;
        }
        
        Log.i(TAG, "Received notification action: " + action);
        
        switch (action) {
            case "ACCEPT_TIMER":
                String timerId = intent.getStringExtra("timerId");
                String timerName = intent.getStringExtra("timerName");
                String sharerName = intent.getStringExtra("sharerName");
                
                if (timerId != null) {
                    Log.i(TAG, "Received notification action: ACCEPT_TIMER for timer: " + timerId);
                    handleAcceptTimer(context, timerId, timerName, sharerName);
                } else {
                    Log.w(TAG, "ACCEPT_TIMER action received but timerId is null");
                }
                break;
            case "DECLINE_TIMER":
                String declineTimerId = intent.getStringExtra("timerId");
                String declineTimerName = intent.getStringExtra("timerName");
                String declineSharerName = intent.getStringExtra("sharerName");
                
                if (declineTimerId != null) {
                    Log.i(TAG, "Received notification action: DECLINE_TIMER for timer: " + declineTimerId);
                    handleDeclineTimer(context, declineTimerId, declineTimerName, declineSharerName);
                } else {
                    Log.w(TAG, "DECLINE_TIMER action received but timerId is null");
                }
                break;
            default:
                Log.w(TAG, "Unknown notification action: " + action);
        }
    }
    
    private void handleAcceptTimer(Context context, String timerId, String timerName, String sharerName) {
        Log.i(TAG, "Handling accept timer: " + timerId);
        
        // Use the same approach as the SHARED tab - send broadcast to get auth token
        Intent message = new Intent("foreground-service-message-receiver");
        message.putExtra("command", "getAuthToken");
        message.putExtra("callback", "acceptTimer");
        message.putExtra("timerId", timerId);
        message.putExtra("timerName", timerName);
        message.putExtra("sharerName", sharerName);
        
        // Send the broadcast to ForegroundService
        LocalBroadcastManager.getInstance(context).sendBroadcast(message);
        
        // Register a receiver to handle the auth token response
        BroadcastReceiver authTokenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String authToken = intent.getStringExtra("authToken");
                String callback = intent.getStringExtra("callback");
                
                if ("acceptTimer".equals(callback)) {
                    String responseTimerId = intent.getStringExtra("timerId");
                    String responseTimerName = intent.getStringExtra("timerName");
                    String responseSharerName = intent.getStringExtra("sharerName");
                    
                    if (authToken != null && !authToken.isEmpty()) {
                        Log.i(TAG, "Received auth token for accept timer: " + responseTimerId);
                        refreshSharedTimersAndAccept(context, "Bearer " + authToken, responseTimerId, responseTimerName, responseSharerName);
                    } else {
                        Log.e(TAG, "Cannot accept timer - no auth token received");
                        showActionResultNotification(context, "Accept Failed", 
                                "Unable to authenticate. Please try again.", false);
                    }
                    
                    // Unregister this receiver
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                }
            }
        };
        
        // Register the receiver
        LocalBroadcastManager.getInstance(context).registerReceiver(
            authTokenReceiver, 
            new IntentFilter("notification-action-auth-response")
        );
        
        // Set a timeout to unregister the receiver if no response
        new Handler().postDelayed(() -> {
            try {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(authTokenReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was already unregistered
            }
        }, 10000); // 10 second timeout
    }
    
    private void refreshSharedTimersAndAccept(Context context, String authToken, String timerId, String timerName, String sharerName) {
        Log.i(TAG, "Refreshing shared timers and then accepting: " + timerId);
        
        // Get the Application context from the BroadcastReceiver context
        Application application = (Application) context.getApplicationContext();
        
        // Move ViewModel creation to main thread to avoid threading issues
        new Handler(Looper.getMainLooper()).post(() -> {
            // Use the existing SharedTimerViewModel to refresh and accept
            SharedTimerViewModel viewModel = new ViewModelProvider.AndroidViewModelFactory(application)
                    .create(SharedTimerViewModel.class);
            
            // First refresh shared timers to get the latest data from backend
            // This will fetch the timer data and store it in the local database
            // Remove "Bearer " prefix if it exists since SharedTimerRepository will add it
            String cleanAuthToken = authToken;
            if (authToken.startsWith("Bearer ")) {
                cleanAuthToken = authToken.substring(7);
            }
            
            // Use callback-based refresh to ensure the timer is available before accepting
            viewModel.refreshSharedTimersWithTokenAndCallback(cleanAuthToken, () -> {
                // This callback runs after the refresh completes (success or failure)
                viewModel.acceptSharedTimer(timerId, new SharedTimerViewModel.SharedTimerAcceptCallback() {
                    @Override
                    public void onAcceptSuccess(String acceptedTimerName, String acceptedSharerName) {
                        showActionResultNotification(context, "Timer Accepted",
                                "You're now sharing '" + acceptedTimerName + "' with " + acceptedSharerName, true);
                    }

                    @Override
                    public void onAcceptFailed(String errorMessage) {
                        Log.e(TAG, "Failed to accept timer: " + errorMessage);
                        showActionResultNotification(context, "Accept Failed",
                                "Failed to accept timer: " + errorMessage, false);
                    }
                });
            });
        });
    }
    
    private void storeSharedTimersAndAccept(Context context, List<io.jhoyt.bubbletimer.Timer> sharedTimers, String timerId, String timerName, String sharerName) {
        // This method is no longer needed - we're using the repository method instead
    }
    
    private void handleDeclineTimer(Context context, String timerId, String timerName, String sharerName) {
        Log.i(TAG, "Handling decline timer: " + timerId);
        
        // Get auth token and call the existing reject API endpoint
        Amplify.Auth.fetchAuthSession(authSession -> {
            AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) authSession;
            String idToken = cognitoAuthSession.getUserPoolTokensResult().getValue().getIdToken();
            
            if (idToken == null || idToken.isEmpty()) {
                Log.e(TAG, "Cannot decline timer - no auth token");
                showActionResultNotification(context, "Decline Failed", 
                        "Authentication error", false);
                return;
            }
            
            // Call the existing reject shared timer API endpoint
            ApiService.RejectTimerRequest request = new ApiService.RejectTimerRequest(timerId);
            apiService.rejectSharedTimer("Bearer " + idToken, request)
                    .enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Log.i(TAG, "Successfully declined timer: " + timerId);
                                showActionResultNotification(context, "Timer Declined", 
                                        "You declined the invitation for '" + timerName + "'", true);
                            } else {
                                Log.e(TAG, "Failed to decline timer: " + response.code());
                                showActionResultNotification(context, "Decline Failed", 
                                        "Failed to decline timer invitation", false);
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            Log.e(TAG, "Error declining timer", t);
                            showActionResultNotification(context, "Decline Failed", 
                                    "Network error while declining timer", false);
                        }
                    });
            
        }, error -> {
            Log.e(TAG, "Error fetching auth session for decline", error);
            showActionResultNotification(context, "Decline Failed", 
                    "Authentication error", false);
        });
    }
    
    private void showActionResultNotification(Context context, String title, String message, boolean isSuccess) {
        // Create a simple notification to show the result
        android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        android.app.NotificationChannel resultChannel = new android.app.NotificationChannel(
                "action_results",
                "Action Results",
                android.app.NotificationManager.IMPORTANCE_HIGH
        );
        resultChannel.setDescription("Results of notification actions");
        resultChannel.enableVibration(true);
        resultChannel.setShowBadge(true);
        resultChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(resultChannel);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "action_results")
                .setSmallIcon(io.jhoyt.bubbletimer.R.drawable.bubble_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false);
        
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
