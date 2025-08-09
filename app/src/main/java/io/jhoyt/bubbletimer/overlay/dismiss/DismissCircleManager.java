package io.jhoyt.bubbletimer.overlay.dismiss;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import io.jhoyt.bubbletimer.DismissCircleView;
import io.jhoyt.bubbletimer.R;

/**
 * Manages the lifecycle of dismiss circles, extracted from Window.java dismiss circle logic.
 * 
 * This class handles:
 * - Creating and configuring dismiss circle overlays
 * - Showing/hiding dismiss circles during drag operations
 * - Managing WindowManager interactions for dismiss circles
 * - Setting up proper layout parameters for dismiss circles
 */
public class DismissCircleManager {
    
    private static final String TAG = "DismissCircleManager";
    
    // Dependencies
    private final WindowManager windowManager;
    private final LayoutInflater layoutInflater;
    private final Context context;
    
    // Dismiss circle components
    private RelativeLayout dismissCircleView;
    private DismissCircleView dismissCircle;
    private WindowManager.LayoutParams dismissCircleParams;
    
    // State
    private boolean isShowing = false;
    private int screenWidth = 0;
    private int screenHeight = 0;
    
    public DismissCircleManager(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        initializeDismissCircle();
    }
    
    /**
     * Initialize the dismiss circle view and layout parameters.
     * Extracted from Window.java constructor dismiss circle setup.
     */
    private void initializeDismissCircle() {
        // Setup dismiss circle window
        dismissCircleView = (RelativeLayout) layoutInflater.inflate(R.layout.dismiss_circle_window, null);
        dismissCircle = dismissCircleView.findViewById(R.id.dismissCircle);
        
        // Get screen dimensions for setup
        updateScreenDimensions();
        
        Log.d(TAG, "Setting dismissCircle dimensions: width=" + screenWidth + ", height=" + screenHeight);
        dismissCircle.setScreenDimensions(screenWidth, screenHeight);
        
        // Create layout parameters for dismiss circle overlay
        dismissCircleParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, 
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        );
        dismissCircleParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        
        Log.d(TAG, "Dismiss circle manager initialized");
    }
    
    /**
     * Show dismiss circles during drag operation.
     * Should be called when user starts dragging a timer bubble.
     */
    public void showDismissCircles() {
        if (isShowing) {
            Log.d(TAG, "Dismiss circles already showing");
            return;
        }
        
        try {
            // Ensure screen dimensions are current
            updateScreenDimensions();
            dismissCircle.setScreenDimensions(screenWidth, screenHeight);
            
            // Add dismiss circle overlay to window manager
            if (dismissCircleView.getParent() == null) {
                windowManager.addView(dismissCircleView, dismissCircleParams);
                isShowing = true;
                Log.d(TAG, "Dismiss circles shown");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing dismiss circles: " + e.toString());
        }
    }
    
    /**
     * Hide dismiss circles when drag operation ends.
     * Should be called when user stops dragging or releases touch.
     */
    public void hideDismissCircles() {
        if (!isShowing) {
            Log.d(TAG, "Dismiss circles already hidden");
            return;
        }
        
        try {
            if (dismissCircleView.getParent() != null) {
                windowManager.removeView(dismissCircleView);
                isShowing = false;
                Log.d(TAG, "Dismiss circles hidden");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding dismiss circles: " + e.toString());
        }
    }
    
    /**
     * Get the nearest dismiss circle to a given position.
     * 
     * @param x X coordinate to check
     * @param y Y coordinate to check
     * @return The nearest dismiss circle, or null if none are within range
     */
    public DismissCircleView.DismissCircle getNearestDismissCircle(float x, float y) {
        if (dismissCircle == null) {
            return null;
        }
        
        return dismissCircle.getNearestDismissCircle(x, y);
    }
    
    /**
     * Get the dismiss circle view for direct access if needed.
     * 
     * @return The DismissCircleView instance
     */
    public DismissCircleView getDismissCircleView() {
        return dismissCircle;
    }
    
    /**
     * Check if dismiss circles are currently visible.
     * 
     * @return true if dismiss circles are showing, false otherwise
     */
    public boolean isShowing() {
        return isShowing;
    }
    
    /**
     * Update screen dimensions when device orientation changes or for initialization.
     */
    public void updateScreenDimensions() {
        android.util.DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int newWidth = displayMetrics.widthPixels;
        int newHeight = displayMetrics.heightPixels;
        
        if (newWidth != screenWidth || newHeight != screenHeight) {
            screenWidth = newWidth;
            screenHeight = newHeight;
            
            if (dismissCircle != null) {
                dismissCircle.setScreenDimensions(screenWidth, screenHeight);
            }
            
            Log.d(TAG, "Screen dimensions updated: " + screenWidth + "x" + screenHeight);
        }
    }
    
    /**
     * Force refresh of dismiss circle layout.
     * Useful when screen orientation changes.
     */
    public void refreshLayout() {
        updateScreenDimensions();
        
        if (isShowing) {
            // Re-layout dismiss circles with new dimensions
            try {
                windowManager.updateViewLayout(dismissCircleView, dismissCircleParams);
                Log.d(TAG, "Dismiss circle layout refreshed");
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing dismiss circle layout: " + e.toString());
            }
        }
    }
    
    /**
     * Cleanup dismiss circle manager.
     * Should be called when the overlay window is being destroyed.
     */
    public void cleanup() {
        hideDismissCircles();
        
        // Clean up references
        dismissCircleView = null;
        dismissCircle = null;
        dismissCircleParams = null;
        
        Log.d(TAG, "Dismiss circle manager cleaned up");
    }
}
