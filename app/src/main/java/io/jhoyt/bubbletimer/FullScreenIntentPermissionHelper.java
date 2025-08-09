package io.jhoyt.bubbletimer;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Helper class for managing full-screen intent permissions on Android 14+
 */
public class FullScreenIntentPermissionHelper {
    private static final String TAG = "FullScreenIntentPermissionHelper";

    /**
     * Check if the app has permission to use full-screen intents
     */
    public static boolean hasFullScreenIntentPermission(Context context) {
        Log.d(TAG, "Checking full-screen intent permission...");
        Log.d(TAG, "Android Version: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "Android Release: " + Build.VERSION.RELEASE);
        
        if (Build.VERSION.SDK_INT >= 34) { // Android 14+
            try {
                NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    boolean canUse = notificationManager.canUseFullScreenIntent();
                    Log.i(TAG, "Full-screen intent permission status (Android 14+): " + canUse);
                    
                    if (!canUse) {
                        Log.w(TAG, "❌ Full-screen intent permission DENIED");
                        Log.w(TAG, "User must grant permission in: Settings > Apps > Special App Access > Full-Screen Intents");
                    } else {
                        Log.i(TAG, "✅ Full-screen intent permission GRANTED");
                    }
                    
                    return canUse;
                } else {
                    Log.e(TAG, "NotificationManager is null!");
                    return false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking full-screen intent permission", e);
                return false;
            }
        } else {
            // For Android versions before 14, permission is granted automatically
            Log.d(TAG, "✅ Android version < 14, full-screen intent permission granted automatically");
            return true;
        }
    }

    /**
     * Request full-screen intent permission from the user by opening system settings
     */
    public static void requestFullScreenIntentPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 34) { // Android 14+
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.i(TAG, "Opened full-screen intent permission settings");
                Log.i(TAG, "🎯 QUICK GUIDE: Find 'Bubble Timer' in the list and toggle the switch ON");
            } catch (Exception e) {
                Log.e(TAG, "Failed to open full-screen intent permission settings", e);
                // Fallback: Open general app settings
                openAppSettings(context);
            }
        } else {
            Log.d(TAG, "Full-screen intent permission not required for Android version < 14");
        }
    }

    /**
     * Fallback method to open general app settings
     */
    private static void openAppSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "Opened general app settings as fallback");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open app settings", e);
        }
    }

    /**
     * Check if we should show a permission explanation to the user
     */
    public static boolean shouldShowPermissionExplanation(Context context) {
        return Build.VERSION.SDK_INT >= 34 && !hasFullScreenIntentPermission(context);
    }

    /**
     * Get a user-friendly explanation of why the permission is needed
     */
    public static String getPermissionExplanation() {
        return "To show timer alarms over the lock screen, this app needs permission to display full-screen notifications. " +
               "This ensures you can see and dismiss alarms even when your device is locked.";
    }
}
