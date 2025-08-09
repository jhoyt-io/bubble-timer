package io.jhoyt.bubbletimer.overlay.debug;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.jhoyt.bubbletimer.R;

/**
 * Manages debug overlay functionality, extracted from Window.java debug logic.
 * 
 * This class handles:
 * - Creating and managing debug overlay windows
 * - Updating debug information display
 * - Showing/hiding debug overlay based on debug mode
 * - Formatting debug information for display
 */
public class DebugOverlayManager {
    
    private static final String TAG = "DebugOverlayManager";
    
    // Dependencies
    private final Context context;
    private final WindowManager windowManager;
    private final LayoutInflater layoutInflater;
    
    // Debug overlay components
    private RelativeLayout debugView;
    private TextView debugText;
    private WindowManager.LayoutParams debugParams;
    
    // State
    private boolean isDebugModeEnabled = false;
    private boolean isDebugOverlayVisible = false;
    
    public DebugOverlayManager(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        initializeDebugOverlay();
    }
    
    /**
     * Initialize debug overlay components.
     * Extracted from Window.java debug overlay setup.
     */
    private void initializeDebugOverlay() {
        try {
            // Setup debug window layout
            debugView = (RelativeLayout) layoutInflater.inflate(R.layout.debug_window, null);
            debugText = debugView.findViewById(R.id.debugText);
            
            // Create layout parameters for debug overlay
            debugParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN,
                android.graphics.PixelFormat.TRANSLUCENT
            );
            
            debugParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            debugParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            
            // Set additional flags for proper display
            debugParams.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR;
            
            Log.d(TAG, "Debug overlay initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing debug overlay: " + e.toString());
        }
    }
    
    /**
     * Toggle debug mode on/off.
     * 
     * @return true if debug mode is now enabled, false if disabled
     */
    public boolean toggleDebugMode() {
        isDebugModeEnabled = !isDebugModeEnabled;
        
        Log.d(TAG, "Debug mode toggled: " + isDebugModeEnabled);
        
        if (isDebugModeEnabled) {
            showDebugOverlay();
            updateDebugText("Debug Mode: ENABLED\nTouch debug info will appear here...");
        } else {
            hideDebugOverlay();
        }
        
        return isDebugModeEnabled;
    }
    
    /**
     * Set debug mode state.
     * 
     * @param enabled true to enable debug mode, false to disable
     */
    public void setDebugMode(boolean enabled) {
        if (isDebugModeEnabled == enabled) {
            return; // No change needed
        }
        
        isDebugModeEnabled = enabled;
        Log.d(TAG, "Debug mode set to: " + enabled);
        
        if (enabled) {
            showDebugOverlay();
            updateDebugText("Debug Mode: ENABLED");
        } else {
            hideDebugOverlay();
        }
    }
    
    /**
     * Check if debug mode is currently enabled.
     * 
     * @return true if debug mode is enabled, false otherwise
     */
    public boolean isDebugModeEnabled() {
        return isDebugModeEnabled;
    }
    
    /**
     * Update debug information display.
     * 
     * @param debugInfo The debug information to display
     */
    public void updateDebugText(String debugInfo) {
        if (!isDebugModeEnabled || debugText == null) {
            return;
        }
        
        // Format debug info with timestamp
        String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        String formattedInfo = "[" + timestamp + "]\n" + debugInfo;
        
        // Update on UI thread
        if (debugText.getHandler() != null) {
            debugText.getHandler().post(() -> {
                debugText.setText(formattedInfo);
                Log.d(TAG, "Debug text updated: " + debugInfo.substring(0, Math.min(50, debugInfo.length())));
            });
        } else {
            debugText.setText(formattedInfo);
        }
    }
    
    /**
     * Format touch event information for debug display.
     * 
     * @param action Touch action (e.g., "ACTION_DOWN", "ACTION_MOVE")
     * @param rawX Raw X coordinate
     * @param rawY Raw Y coordinate
     * @param localX Local X coordinate
     * @param localY Local Y coordinate
     * @param deltaX Delta X from initial touch
     * @param deltaY Delta Y from initial touch
     * @return Formatted debug string
     */
    public String formatTouchDebugInfo(String action, float rawX, float rawY, 
                                     float localX, float localY, 
                                     float deltaX, float deltaY) {
        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("=== ").append(action).append(" ===\n");
        debugInfo.append("Raw: [").append(String.format("%.1f", rawX))
                 .append(",").append(String.format("%.1f", rawY)).append("]\n");
        debugInfo.append("Local: [").append(String.format("%.1f", localX))
                 .append(",").append(String.format("%.1f", localY)).append("]\n");
        debugInfo.append("Delta: [").append(String.format("%.1f", deltaX))
                 .append(",").append(String.format("%.1f", deltaY)).append("]\n");
        debugInfo.append("Distance: ").append(String.format("%.1f", 
                Math.sqrt(deltaX * deltaX + deltaY * deltaY))).append("px\n");
        
        return debugInfo.toString();
    }
    
    /**
     * Format overlay positioning information for debug display.
     * 
     * @param overlayX Current overlay X position
     * @param overlayY Current overlay Y position
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     * @param viewWidth Overlay view width
     * @param viewHeight Overlay view height
     * @return Formatted debug string
     */
    public String formatPositionDebugInfo(int overlayX, int overlayY, 
                                        int screenWidth, int screenHeight,
                                        int viewWidth, int viewHeight) {
        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("=== Position Info ===\n");
        debugInfo.append("Overlay: [").append(overlayX).append(",").append(overlayY).append("]\n");
        debugInfo.append("Screen: ").append(screenWidth).append("x").append(screenHeight).append("\n");
        debugInfo.append("View: ").append(viewWidth).append("x").append(viewHeight).append("\n");
        
        // Calculate center position
        int centerX = overlayX + (viewWidth / 2);
        int centerY = overlayY + (viewHeight / 2);
        debugInfo.append("Center: [").append(centerX).append(",").append(centerY).append("]\n");
        
        return debugInfo.toString();
    }
    
    /**
     * Show the debug overlay.
     */
    private void showDebugOverlay() {
        if (!isDebugModeEnabled) {
            Log.d(TAG, "Debug mode not enabled, not showing overlay");
            return;
        }
        
        if (isDebugOverlayVisible) {
            Log.d(TAG, "Debug overlay already visible");
            return;
        }
        
        // Remove any existing overlay first
        hideDebugOverlay();
        
        try {
            if (debugView != null && debugView.getParent() == null) {
                windowManager.addView(debugView, debugParams);
                isDebugOverlayVisible = true;
                Log.d(TAG, "Debug overlay shown");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing debug overlay: " + e.toString());
        }
    }
    
    /**
     * Hide the debug overlay.
     */
    private void hideDebugOverlay() {
        if (!isDebugOverlayVisible) {
            return;
        }
        
        try {
            if (debugView != null && debugView.getParent() != null) {
                windowManager.removeView(debugView);
                isDebugOverlayVisible = false;
                Log.d(TAG, "Debug overlay hidden");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding debug overlay: " + e.toString());
        }
    }
    
    /**
     * Check if debug overlay is currently visible.
     * 
     * @return true if debug overlay is showing, false otherwise
     */
    public boolean isDebugOverlayVisible() {
        return isDebugOverlayVisible;
    }
    
    /**
     * Cleanup debug overlay manager.
     * Should be called when the main overlay is being destroyed.
     */
    public void cleanup() {
        hideDebugOverlay();
        
        // Clean up references
        debugView = null;
        debugText = null;
        debugParams = null;
        
        Log.d(TAG, "Debug overlay manager cleaned up");
    }
}
