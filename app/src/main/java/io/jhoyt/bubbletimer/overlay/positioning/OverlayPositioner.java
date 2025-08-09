package io.jhoyt.bubbletimer.overlay.positioning;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;

import io.jhoyt.bubbletimer.TimerView;

/**
 * Centralized overlay positioning logic, extracted from Window.java positioning calculations.
 * 
 * This class handles:
 * - Initial overlay positioning (center, expanded mode, etc.)
 * - Position validation and boundary checking
 * - Coordinate system transformations
 * - Screen-relative positioning calculations
 */
public class OverlayPositioner {
    
    private static final String TAG = "OverlayPositioner";
    
    // Dependencies
    private final Context context;
    
    // Screen dimensions
    private int usableWidth = 0;
    private int usableHeight = 0;
    
    public OverlayPositioner(Context context) {
        this.context = context;
        updateScreenDimensions();
    }
    
    /**
     * Calculate initial position for a new overlay window.
     * Extracted from Window.java constructor positioning logic.
     * 
     * @param view The overlay view to position
     * @param timerView The timer view for size calculations
     * @param expanded Whether this is an expanded bubble
     * @return WindowManager.LayoutParams with calculated position
     */
    public WindowManager.LayoutParams calculateInitialPosition(View view, TimerView timerView, boolean expanded) {
        updateScreenDimensions();
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, 
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        );
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        
        if (expanded) {
            // Position expanded bubble in center of screen
            params.x = calculateCenterX(view);
            params.y = calculateCenterY(view);
            
            timerView.setSmallMode(false);
            timerView.setExpandedMode(true);
            
            android.util.Log.d(TAG, "Expanded bubble initial position: [" + params.x + "," + params.y + "]");
        } else {
            // Position small bubble towards top-left with some offset
            params.x = 0; // Will snap to side on first release
            params.y = (int) (usableHeight * 0.1); // 10% down from top
            
            timerView.setSmallMode(true);
            timerView.setExpandedMode(false);
            
            android.util.Log.d(TAG, "Small bubble initial position: [" + params.x + "," + params.y + "]");
        }
        
        return params;
    }
    
    /**
     * Calculate center X position for a view on screen.
     * 
     * @param view The view to center
     * @return X coordinate for centering the view
     */
    public int calculateCenterX(View view) {
        updateScreenDimensions();
        
        // Ensure view is measured
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        
        return (usableWidth - view.getMeasuredWidth()) / 2;
    }
    
    /**
     * Calculate center Y position for a view on screen.
     * 
     * @param view The view to center
     * @return Y coordinate for centering the view
     */
    public int calculateCenterY(View view) {
        updateScreenDimensions();
        
        // Ensure view is measured
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        
        return (usableHeight - view.getMeasuredHeight()) / 2;
    }
    
    /**
     * Validate and constrain a position to screen boundaries.
     * 
     * @param x Desired X position
     * @param y Desired Y position
     * @param viewWidth Width of the view
     * @param viewHeight Height of the view
     * @return int array with [validX, validY] constrained to screen
     */
    public int[] constrainToScreen(int x, int y, int viewWidth, int viewHeight) {
        updateScreenDimensions();
        
        int constrainedX = Math.max(0, Math.min(x, usableWidth - viewWidth));
        int constrainedY = Math.max(0, Math.min(y, usableHeight - viewHeight));
        
        return new int[]{constrainedX, constrainedY};
    }
    
    /**
     * Calculate position for smooth drag movement.
     * 
     * @param touchX Current touch X coordinate
     * @param touchY Current touch Y coordinate
     * @param initialTouchX Initial touch X coordinate
     * @param initialTouchY Initial touch Y coordinate
     * @param viewWidth Width of the overlay view
     * @param viewHeight Height of the overlay view
     * @return int array with [newX, newY] for overlay position
     */
    public int[] calculateDragPosition(float touchX, float touchY, 
                                     float initialTouchX, float initialTouchY,
                                     int viewWidth, int viewHeight) {
        
        // Calculate offset from touch point to view center
        float deltaX = touchX - initialTouchX;
        float deltaY = touchY - initialTouchY;
        
        // Calculate new position (centered on touch point)
        int newX = (int) (touchX + deltaX - (viewWidth / 2));
        int newY = (int) (touchY + deltaY - (viewHeight / 2));
        
        // Constrain to screen boundaries
        return constrainToScreen(newX, newY, viewWidth, viewHeight);
    }
    
    /**
     * Check if a position is within screen boundaries.
     * 
     * @param x X position to check
     * @param y Y position to check
     * @param viewWidth Width of the view
     * @param viewHeight Height of the view
     * @return true if position is valid, false if outside screen
     */
    public boolean isPositionValid(int x, int y, int viewWidth, int viewHeight) {
        updateScreenDimensions();
        
        return x >= 0 && y >= 0 && 
               (x + viewWidth) <= usableWidth && 
               (y + viewHeight) <= usableHeight;
    }
    
    /**
     * Get the current usable screen width.
     * 
     * @return Usable screen width in pixels
     */
    public int getUsableWidth() {
        updateScreenDimensions();
        return usableWidth;
    }
    
    /**
     * Get the current usable screen height.
     * 
     * @return Usable screen height in pixels
     */
    public int getUsableHeight() {
        updateScreenDimensions();
        return usableHeight;
    }
    
    /**
     * Update screen dimensions from current display metrics.
     * Handles device rotation and screen size changes.
     */
    public void updateScreenDimensions() {
        android.util.DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        
        // For now, use simple display metrics
        // TODO: Integrate with ScreenDimensionsCalculator for system bar handling
        int newWidth = displayMetrics.widthPixels;
        int newHeight = displayMetrics.heightPixels;
        
        if (newWidth != usableWidth || newHeight != usableHeight) {
            usableWidth = newWidth;
            usableHeight = newHeight;
            android.util.Log.d(TAG, "Screen dimensions updated: " + usableWidth + "x" + usableHeight);
        }
    }
    
    /**
     * Convert from screen coordinates to overlay-relative coordinates.
     * 
     * @param screenX X coordinate in screen space
     * @param screenY Y coordinate in screen space
     * @param overlayX Current overlay X position
     * @param overlayY Current overlay Y position
     * @return int array with [relativeX, relativeY]
     */
    public int[] screenToOverlayCoordinates(float screenX, float screenY, int overlayX, int overlayY) {
        int relativeX = (int) (screenX - overlayX);
        int relativeY = (int) (screenY - overlayY);
        return new int[]{relativeX, relativeY};
    }
    
    /**
     * Convert from overlay-relative coordinates to screen coordinates.
     * 
     * @param relativeX X coordinate relative to overlay
     * @param relativeY Y coordinate relative to overlay
     * @param overlayX Current overlay X position
     * @param overlayY Current overlay Y position
     * @return int array with [screenX, screenY]
     */
    public int[] overlayToScreenCoordinates(int relativeX, int relativeY, int overlayX, int overlayY) {
        int screenX = relativeX + overlayX;
        int screenY = relativeY + overlayY;
        return new int[]{screenX, screenY};
    }
    
    /**
     * Calculate distance from overlay center to a screen position.
     * 
     * @param screenX X coordinate in screen space
     * @param screenY Y coordinate in screen space
     * @param overlayX Current overlay X position
     * @param overlayY Current overlay Y position
     * @param viewWidth Width of overlay view
     * @param viewHeight Height of overlay view
     * @return Distance in pixels
     */
    public float calculateDistanceFromCenter(float screenX, float screenY, 
                                           int overlayX, int overlayY,
                                           int viewWidth, int viewHeight) {
        float centerX = overlayX + (viewWidth / 2.0f);
        float centerY = overlayY + (viewHeight / 2.0f);
        
        float deltaX = screenX - centerX;
        float deltaY = screenY - centerY;
        
        return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}
