package io.jhoyt.bubbletimer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.jhoyt.bubbletimer.MainActivity;
import io.jhoyt.bubbletimer.R;

public class BubbleTimerFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "BubbleTimerFCM";
    private static final String CHANNEL_ID = "timer_invitations";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }
    
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.i(TAG, "New FCM token received: " + token);
        registerTokenWithBackend(token);
    }
    
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.i(TAG, "Message received from: " + remoteMessage.getFrom());
        Log.i(TAG, "Message data: " + remoteMessage.getData());
        
        // Check for timer invitation notification
        if (remoteMessage.getData().containsKey("timerId") && 
            remoteMessage.getData().containsKey("timerName") &&
            remoteMessage.getData().containsKey("sharerName")) {
            Log.i(TAG, "Processing timer invitation notification");
            handleTimerInvitation(remoteMessage);
        } else {
            Log.w(TAG, "Received message but missing required data for timer invitation");
        }
    }
    
    private void handleTimerInvitation(RemoteMessage remoteMessage) {
        String timerId = remoteMessage.getData().get("timerId");
        String timerName = remoteMessage.getData().get("timerName");
        String sharerName = remoteMessage.getData().get("sharerName");
        
        Log.i(TAG, "Handling timer invitation - Timer: " + timerName + ", ID: " + timerId + ", Shared by: " + sharerName);
        
        // Check if app is in foreground (this might affect notification display)
        boolean isAppInForeground = isAppInForeground();
        Log.i(TAG, "App in foreground: " + isAppInForeground);
        
        // Create main notification intent (taps notification body)
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("action", "view_invitation");
        mainIntent.putExtra("timerId", timerId);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Create Accept action intent
        Intent acceptIntent = new Intent(this, NotificationActionReceiver.class);
        acceptIntent.setAction("ACCEPT_TIMER");
        acceptIntent.putExtra("timerId", timerId);
        acceptIntent.putExtra("timerName", timerName);
        acceptIntent.putExtra("sharerName", sharerName);
        
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, 1, acceptIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Create Decline action intent
        Intent declineIntent = new Intent(this, NotificationActionReceiver.class);
        declineIntent.setAction("DECLINE_TIMER");
        declineIntent.putExtra("timerId", timerId);
        declineIntent.putExtra("timerName", timerName);
        declineIntent.putExtra("sharerName", sharerName);
        
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(this, 2, declineIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Build the notification with action buttons - make it more prominent
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.bubble_logo)
                .setContentTitle("Timer Invitation")
                .setContentText(sharerName + " shared '" + timerName + "' with you")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(sharerName + " shared '" + timerName + "' with you\nTap to view or use the buttons below to respond."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(true)
                .setOngoing(false)
                .setOnlyAlertOnce(false)
                .setContentIntent(mainPendingIntent)
                .addAction(R.drawable.bubble_logo, "Accept", acceptPendingIntent)
                .addAction(R.drawable.bubble_logo, "Decline", declinePendingIntent);
        
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Use timerId hash for unique notification ID
        int notificationId = Math.abs(timerId.hashCode());
        
        Log.i(TAG, "Creating notification with ID: " + notificationId);
        Log.i(TAG, "Notification channel: " + CHANNEL_ID);
        
        android.app.Notification notification = builder.build();
        Log.i(TAG, "Notification has " + notification.actions.length + " actions");
        Log.i(TAG, "Notification priority: " + notification.priority);
        Log.i(TAG, "Notification category: " + notification.category);
        
        notificationManager.notify(notificationId, notification);
        
        Log.i(TAG, "Timer invitation notification posted with ID: " + notificationId);
    }
    
    private boolean isAppInForeground() {
        android.app.ActivityManager activityManager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<android.app.ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        String packageName = getPackageName();
        for (android.app.ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && 
                appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Timer Invitations",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for timer sharing invitations");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.enableLights(true);
            channel.setLightColor(0xFF2196F3); // Blue color for timer invitations
            
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void registerTokenWithBackend(String token) {
        executor.execute(() -> {
            FcmTokenManager fcmTokenManager = new FcmTokenManager(this);
            fcmTokenManager.registerDeviceToken(token);
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
