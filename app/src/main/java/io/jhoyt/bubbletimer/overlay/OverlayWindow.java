package io.jhoyt.bubbletimer.overlay;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import io.jhoyt.bubbletimer.R;
import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.TimerView;
import io.jhoyt.bubbletimer.overlay.debug.DebugOverlayManager;
import io.jhoyt.bubbletimer.overlay.dismiss.DismissCircleManager;
import io.jhoyt.bubbletimer.overlay.positioning.OverlayPositioner;
import io.jhoyt.bubbletimer.overlay.positioning.ScreenDimensionsCalculator;
import io.jhoyt.bubbletimer.overlay.touch.TouchEventHandler;

/**
 * Simplified overlay window implementation that replaces the monolithic Window.java.
 * 
 * This class integrates all the extracted components:
 * - TouchEventHandler: Handles all touch interactions
 * - DismissCircleManager: Manages dismiss circle lifecycle
 * - OverlayPositioner: Handles positioning calculations
 * - DebugOverlayManager: Manages debug overlay functionality
 * 
 * The design follows single responsibility principle with clear component boundaries.
 */
public class OverlayWindow implements TouchEventHandler.TouchEventListener {
    
    private static final String TAG = "OverlayWindow";
    
    // Dependencies
    private final Context context;
    private final WindowManager windowManager;
    private final LayoutInflater layoutInflater;
    private final String userId;
    private final boolean expanded;
    
    // Layout components
    private RelativeLayout overlayView;
    private TimerView timerView;
    private WindowManager.LayoutParams layoutParams;
    
    // Component managers
    private TouchEventHandler touchEventHandler;
    private DismissCircleManager dismissCircleManager;
    private OverlayPositioner overlayPositioner;
    private DebugOverlayManager debugOverlayManager;
    
    // State
    private boolean isOpen = false;
    private BubbleEventListener bubbleEventListener;
    
    // Event listener interface (same as original Window.java)
    public interface BubbleEventListener {
        void onBubbleDismiss(Timer timer);
        void onBubbleClick(Timer timer);
        void onTimerUpdated(Timer timer);
        void onTimerStopped(Timer timer);
    }
    
    public OverlayWindow(Context context, Boolean expanded, String userId) {
        this.context = context;
        this.userId = userId;
        this.expanded = expanded != null && expanded;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        Log.d(TAG, "OverlayWindow constructor called with userId: " + userId + ", expanded: " + expanded);
        
        initializeComponents(this.expanded);
    }
    
    /**
     * Initialize all overlay components and managers.
     */
    private void initializeComponents(Boolean expanded) {
        // Initialize overlay view and timer
        initializeOverlayView(expanded);
        
        // Initialize component managers
        initializeComponentManagers();
        
        // Setup touch handling
        timerView.setOnTouchListener(touchEventHandler);
        
        Log.d(TAG, "All components initialized");
    }
    
    /**
     * Initialize the main overlay view and timer component.
     */
    private void initializeOverlayView(Boolean expanded) {
        // Inflate appropriate layout
        if (expanded) {
            overlayView = (RelativeLayout) layoutInflater.inflate(R.layout.expanded_popup_window, null);
        } else {
            overlayView = (RelativeLayout) layoutInflater.inflate(R.layout.popup_window, null);
        }
        
        // Get timer view reference
        timerView = overlayView.findViewById(R.id.timer);
        
        // Calculate and set screen dimensions
        int[] usableDimensions = ScreenDimensionsCalculator.getUsableScreenDimensions(overlayView);
        int usableWidth = usableDimensions[0];
        int usableHeight = usableDimensions[1];
        Log.d(TAG, "Usable dimensions: " + usableWidth + "x" + usableHeight);
        
        timerView.setScreenDimensions(usableWidth, usableHeight);
        
        // Set the current user ID on the timer view
        timerView.setCurrentUserId(userId);
        Log.d(TAG, "Set currentUserId on TimerView: " + userId);
        
        Log.d(TAG, "Overlay view initialized: expanded=" + expanded);
    }
    
    /**
     * Initialize all component managers.
     */
    private void initializeComponentManagers() {
        // Initialize positioning manager
        overlayPositioner = new OverlayPositioner(context);
        
        // Initialize dismiss circle manager
        dismissCircleManager = new DismissCircleManager(context, windowManager);
        
        // Initialize debug overlay manager
        debugOverlayManager = new DebugOverlayManager(context, windowManager);
        
        // Calculate initial layout parameters using the correct expanded flag
        layoutParams = overlayPositioner.calculateInitialPosition(overlayView, timerView, expanded);
        
        // Initialize touch event handler
        touchEventHandler = new TouchEventHandler(
            windowManager,
            layoutParams,
            overlayView,
            timerView,
            dismissCircleManager,
            this // TouchEventListener
        );
        
        Log.d(TAG, "Component managers initialized");
    }
    
    // TouchEventHandler.TouchEventListener implementation
    @Override
    public void onBubbleClick(Timer timer) {
        Log.d(TAG, "Bubble clicked");
        if (bubbleEventListener != null) {
            bubbleEventListener.onBubbleClick(timer);
        }
    }
    
    @Override
    public void onBubbleDismiss(Timer timer) {
        Log.d(TAG, "Bubble dismissed");
        if (bubbleEventListener != null) {
            bubbleEventListener.onBubbleDismiss(timer);
        }
    }
    
    @Override
    public void onTimerStopped(Timer timer) {
        Log.d(TAG, "Timer stopped");
        if (bubbleEventListener != null) {
            bubbleEventListener.onTimerStopped(timer);
        }
    }
    
    @Override
    public void onTimerUpdated(Timer timer) {
        Log.d(TAG, "Timer updated");
        if (bubbleEventListener != null) {
            bubbleEventListener.onTimerUpdated(timer);
        }
    }
    
    @Override
    public void onDebugInfoUpdate(String debugInfo) {
        Log.d(TAG, "Debug info: " + debugInfo);
        if (debugOverlayManager != null) {
            debugOverlayManager.updateDebugText(debugInfo);
        }
    }
    
    @Override
    public void onDragStateChanged(boolean isDragging) {
        Log.d(TAG, "Drag state changed: " + isDragging);
        // Could be used for additional state management if needed
    }

    @Override
    public void onDebugModeChanged(boolean enabled) {
        if (debugOverlayManager != null) {
            debugOverlayManager.setDebugMode(enabled);
        }
    }
    
    /**
     * Open/show the overlay window.
     */
    public void open() {
        if (isOpen) {
            Log.d(TAG, "Overlay already open");
            return;
        }
        
        try {
            windowManager.addView(overlayView, layoutParams);
            isOpen = true;
            Log.d(TAG, "Overlay window opened");
        } catch (Exception e) {
            Log.e(TAG, "Error opening overlay window: " + e.toString());
        }
    }
    
    /**
     * Close/hide the overlay window.
     */
    public void close() {
        if (!isOpen) {
            Log.d(TAG, "Overlay already closed");
            return;
        }
        
        try {
            if (overlayView.getParent() != null) {
                windowManager.removeView(overlayView);
            }
            isOpen = false;
            Log.d(TAG, "Overlay window closed");
        } catch (Exception e) {
            Log.e(TAG, "Error closing overlay window: " + e.toString());
        }
    }
    
    /**
     * Update the timer associated with this overlay.
     */
    public void updateTimer(Timer timer) {
        if (timerView != null) {
            timerView.setTimer(timer);
            Log.d(TAG, "Timer updated");
        }
    }
    
    /**
     * Set the bubble event listener.
     */
    public void setBubbleEventListener(BubbleEventListener listener) {
        this.bubbleEventListener = listener;
        Log.d(TAG, "Bubble event listener set");
    }
    
    /**
     * Check if the overlay is currently open.
     */
    public boolean isOpen() {
        return isOpen;
    }
    
    /**
     * Get the timer view for direct access if needed.
     */
    public TimerView getTimerView() {
        return timerView;
    }
    
    /**
     * Toggle debug mode on/off.
     */
    public void setDebugMode(boolean enabled) {
        if (debugOverlayManager != null) {
            debugOverlayManager.setDebugMode(enabled);
        }
        if (touchEventHandler != null) {
            touchEventHandler.setDebugMode(enabled);
        }
        Log.d(TAG, "Debug mode set to: " + enabled);
    }
    
    /**
     * Check if debug mode is enabled.
     */
    public boolean isDebugModeEnabled() {
        return touchEventHandler != null ? touchEventHandler.isDebugModeEnabled() : false;
    }
    
    /**
     * Update screen dimensions when device orientation changes.
     */
    public void refreshLayout() {
        if (overlayPositioner != null) {
            overlayPositioner.updateScreenDimensions();
        }
        if (dismissCircleManager != null) {
            dismissCircleManager.refreshLayout();
        }
        
        // Update timer view dimensions
        if (timerView != null && overlayView != null) {
            int[] usableDimensions = ScreenDimensionsCalculator.getUsableScreenDimensions(overlayView);
            timerView.setScreenDimensions(usableDimensions[0], usableDimensions[1]);
        }
        
        Log.d(TAG, "Layout refreshed for orientation change");
    }
    
    /**
     * Cleanup overlay window and all component managers.
     * Should be called when the overlay is no longer needed.
     */
    public void cleanup() {
        close();
        
        // Cleanup component managers
        if (dismissCircleManager != null) {
            dismissCircleManager.cleanup();
        }
        if (debugOverlayManager != null) {
            debugOverlayManager.cleanup();
        }
        
        // Clear references
        touchEventHandler = null;
        dismissCircleManager = null;
        overlayPositioner = null;
        debugOverlayManager = null;
        bubbleEventListener = null;
        
        Log.d(TAG, "Overlay window cleaned up");
    }
}
