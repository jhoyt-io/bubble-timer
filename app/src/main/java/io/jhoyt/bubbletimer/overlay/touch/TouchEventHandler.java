package io.jhoyt.bubbletimer.overlay.touch;

import android.util.Log;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import io.jhoyt.bubbletimer.DismissCircleView;
import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.TimerView;
import io.jhoyt.bubbletimer.overlay.dismiss.DismissCalculator;
import io.jhoyt.bubbletimer.overlay.dismiss.DismissCircleManager;
import io.jhoyt.bubbletimer.overlay.positioning.SnapToSideCalculator;
import io.jhoyt.bubbletimer.MainActivity;
import io.jhoyt.bubbletimer.service.TimerSharingService;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * Handles touch events for overlay windows, extracted from Window.java 270-line touch listener.
 * 
 * This class manages the complex touch logic including:
 * - Touch down/move/up event processing
 * - Drag vs click detection
 * - Button press handling
 * - Dismiss circle interactions
 * - Snap-to-side behavior
 * - Share menu interactions
 */
public class TouchEventHandler implements View.OnTouchListener {
    
    private static final String TAG = "TouchEventHandler";
    
    // Dependencies
    private final WindowManager windowManager;
    private final WindowManager.LayoutParams layoutParams;
    private final View overlayView;
    private final TimerView timerView;
    private final DismissCircleManager dismissCircleManager;
    private final TouchEventListener eventListener;
    
    // Touch state
    private TouchEventState currentTouchState;
    private DismissCircleView.DismissCircle pulledToDismissCircle = null;
    private boolean isDebugModeEnabled = false;
    // Offset between view position and touch point captured on ACTION_DOWN
    private float downDx = 0f;
    private float downDy = 0f;
    
    // Screen dimensions cache
    private int screenWidth = 0;
    private int screenHeight = 0;
    
    public interface TouchEventListener {
        void onBubbleClick(Timer timer);
        void onBubbleDismiss(Timer timer);
        void onTimerStopped(Timer timer);
        void onTimerUpdated(Timer timer);
        void onDebugInfoUpdate(String debugInfo);
        void onDragStateChanged(boolean isDragging);
        void onDebugModeChanged(boolean enabled);
    }
    
    public TouchEventHandler(WindowManager windowManager, 
                           WindowManager.LayoutParams layoutParams,
                           View overlayView,
                           TimerView timerView,
                           DismissCircleManager dismissCircleManager,
                           TouchEventListener eventListener) {
        this.windowManager = windowManager;
        this.layoutParams = layoutParams;
        this.overlayView = overlayView;
        this.timerView = timerView;
        this.dismissCircleManager = dismissCircleManager;
        this.eventListener = eventListener;
        
        // Initialize screen dimensions
        updateScreenDimensions();
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Update screen dimensions if needed
        updateScreenDimensions();
        
        // Check if touch is inside our interactive area
        int buttonPressed = timerView.getButtonAtPoint(event.getX(), event.getY());
        boolean isTouchInside = buttonPressed != -1 || timerView.isPointInMainCircle(event.getX(), event.getY());
        
        Log.d(TAG, "OnTouch: action=" + event.getAction() + " inside=" + isTouchInside + " button=" + buttonPressed);
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleTouchDown(event, isTouchInside);
                
            case MotionEvent.ACTION_MOVE:
                return handleTouchMove(event);
                
            case MotionEvent.ACTION_UP:
                return handleTouchUp(event, isTouchInside, buttonPressed);
                
            case MotionEvent.ACTION_CANCEL:
                return handleTouchCancel();
                
            default:
                Log.d(TAG, "Unhandled touch action: " + event.getAction());
                return false;
        }
    }
    
    private boolean handleTouchDown(MotionEvent event, boolean isTouchInside) {
        if (!isTouchInside) {
            return false;
        }
        
        Log.d(TAG, "ACTION_DOWN: raw=[" + event.getRawX() + "," + event.getRawY() + "] " +
                   "local=[" + event.getX() + "," + event.getY() + "]");
        
        // Initialize touch state
        currentTouchState = new TouchEventState(event.getRawX(), event.getRawY());
        pulledToDismissCircle = null;
        
        // Capture offset so the bubble tracks finger exactly like legacy logic
        // Equivalent to: dx = view.getX() - event.getRawX(); dy = view.getY() - event.getRawY();
        downDx = layoutParams.x - event.getRawX();
        downDy = layoutParams.y - event.getRawY();
        
        // Set dragging state
        timerView.setDragging(true);
        eventListener.onDragStateChanged(true);
        
        // Show dismiss circles for small mode bubbles
        if (timerView.isSmallMode()) {
            dismissCircleManager.showDismissCircles();
        }
        
        // Update debug info if enabled
        if (isDebugModeEnabled) {
            updateDebugInfo("Touch Down", event);
        }
        
        return true;
    }
    
    private boolean handleTouchMove(MotionEvent event) {
        if (currentTouchState == null) {
            return false;
        }
        
        // Update touch state with movement
        currentTouchState = currentTouchState.withMovement(event.getRawX(), event.getRawY());
        
        Log.d(TAG, "ACTION_MOVE: raw=[" + event.getRawX() + "," + event.getRawY() + "] " +
                   "delta=[" + currentTouchState.getDeltaX() + "," + currentTouchState.getDeltaY() + "]");
        
        // Calculate new position for overlay
        float newX, newY;
        
        // Check if we're being pulled toward a dismiss circle
        DismissCircleView.DismissCircle nearest = dismissCircleManager.getNearestDismissCircle(
            event.getRawX(), event.getRawY());
            
        if (nearest != null) {
            // Calculate pull-to-dismiss position
            int[] pullPosition = DismissCalculator.calculatePullToPosition(
                // Use current bubble top-left, not finger position
                layoutParams.x, layoutParams.y,
                nearest.centerX, nearest.centerY,
                overlayView.getWidth(), overlayView.getHeight(),
                screenWidth, screenHeight
            );
            newX = pullPosition[0];
            newY = pullPosition[1];
            
            // Check if we're close enough to be "pulled" to dismiss
            int[] bubbleCenter = DismissCalculator.calculateBubbleCenter(
                (int)newX, (int)newY, overlayView.getWidth(), overlayView.getHeight());
            boolean withinThreshold = DismissCalculator.isWithinDismissActionThreshold(
                bubbleCenter[0], bubbleCenter[1], nearest.centerX, nearest.centerY);
            
            pulledToDismissCircle = withinThreshold ? nearest : null;
        } else {
            // Normal drag positioning using captured offsets (legacy-equivalent behavior)
            newX = event.getRawX() + downDx;
            newY = event.getRawY() + downDy;
            pulledToDismissCircle = null;
        }
        
        // Update overlay position
        layoutParams.x = (int) newX;
        layoutParams.y = (int) newY;
        windowManager.updateViewLayout(overlayView, layoutParams);
        
        // Update debug info if enabled
        if (isDebugModeEnabled) {
            updateDebugInfo("Touch Move", event);
        }
        
        return true;
    }
    
    private boolean handleTouchUp(MotionEvent event, boolean isTouchInside, int buttonPressed) {
        if (currentTouchState == null) {
            return false;
        }
        
        Log.d(TAG, "ACTION_UP: raw=[" + event.getRawX() + "," + event.getRawY() + "] " +
                   "button=" + buttonPressed + " inside=" + isTouchInside);
        
        // Update touch state with release
        currentTouchState = currentTouchState.withRelease(event.getRawX(), event.getRawY());
        
        // Check for dismissal based on circle type BEFORE cleanup
        if (pulledToDismissCircle != null) {
            // Check if we're still close to the dismiss circle
            int[] bubbleCenter = DismissCalculator.calculateBubbleCenter(
                layoutParams.x, layoutParams.y, 
                overlayView.getWidth(), overlayView.getHeight());
                
            boolean isNearDismissCircle = DismissCalculator.isWithinDismissActionThreshold(
                bubbleCenter[0], bubbleCenter[1], 
                pulledToDismissCircle.centerX, pulledToDismissCircle.centerY);
                
            Log.d(TAG, "Dismiss check: near=" + isNearDismissCircle + " type=" + pulledToDismissCircle.type);
                
            if (isNearDismissCircle) {
                if (pulledToDismissCircle.type == DismissCircleView.DismissType.STOP) {
                    // Stop timer and dismiss
                    Log.d(TAG, "Stopping timer and dismissing bubble");
                    eventListener.onTimerStopped(timerView.getTimer());
                } else {
                    // Just dismiss bubble, do not stop timer
                    Log.d(TAG, "Dismissing bubble (timer continues)");
                    eventListener.onBubbleDismiss(timerView.getTimer());
                }
                cleanupDragState();
                return true;
            }
        }
        
        // Cleanup drag state
        cleanupDragState();
        
        // Handle share menu interactions
        if (timerView.isInShareMenu()) {
            return handleShareMenuInteraction(buttonPressed);
        }
        
        // Handle regular button presses
        if (buttonPressed >= 0 && buttonPressed <= 5) {
            return handleButtonPress(buttonPressed);
        }
        
        // Validate touch is still inside
        if (!isTouchInside) {
            Log.d(TAG, "ACTION_UP - Touch not inside, returning false");
            return false;
        }
        
        // Check for click (no movement)
        if (currentTouchState.isClick()) {
            Log.d(TAG, "ACTION_UP - CLICK DETECTED");
            eventListener.onBubbleClick(timerView.getTimer());
            return true;
        }
        
        // Handle snap-to-side for small mode bubbles
        if (timerView.isSmallMode()) {
            int snapX = SnapToSideCalculator.calculateSnapX(
                event.getRawX(), screenWidth, timerView.getWidth());
            layoutParams.x = snapX;
        }
        
        // Apply final position
        windowManager.updateViewLayout(overlayView, layoutParams);
        
        return true;
    }
    
    private boolean handleTouchCancel() {
        Log.d(TAG, "ACTION_CANCEL received");
        cleanupDragState();
        currentTouchState = null;
        return true;
    }
    
    private boolean handleShareMenuInteraction(int buttonPressed) {
        if (buttonPressed == 100) { // Back button
            timerView.hideShareMenu();
            return true;
        }
        
        // Handle friend sharing buttons (101-105)
        if (buttonPressed >= 101 && buttonPressed <= 105) {
            String[] friendNames = TimerView.FRIEND_NAMES;
            int friendIndex = buttonPressed - 101;
            
            if (friendIndex < friendNames.length) {
                String friendName = friendNames[friendIndex];
                
                // Toggle sharing with this friend
                Set<String> currentSharedWith = new HashSet<>(timerView.getSharedWith());
                boolean isCurrentlyShared = currentSharedWith.contains(friendName);
                
                if (isCurrentlyShared) {
                    // Remove from sharing
                    currentSharedWith.remove(friendName);
                    timerView.setSharedWith(currentSharedWith);
                    Log.i(TAG, "Removed " + friendName + " from sharing");
                } else {
                    // Add to sharing - use new REST API
                    currentSharedWith.add(friendName);
                    timerView.setSharedWith(currentSharedWith);
                    Log.i(TAG, "Adding " + friendName + " to sharing via REST API");
                    
                    // Call the new REST API to share timer (triggers push notification)
                    shareTimerViaRestApi(timerView.getTimer().getId(), currentSharedWith);
                }
                
                timerView.refreshMenuLayout();
                eventListener.onTimerUpdated(timerView.getTimer());
                return true;
            }
        }
        
        return false;
    }
    
    private void shareTimerViaRestApi(String timerId, Set<String> sharedWith) {
        TimerSharingService sharingService = new TimerSharingService();
        
        // Get the timer data to send to backend
        Timer timer = timerView.getTimer();
        Map<String, Object> timerData = new HashMap<>();
        timerData.put("id", timer.getId());
        timerData.put("userId", timer.getUserId());
        timerData.put("name", timer.getName());
        timerData.put("totalDuration", timer.getTotalDuration().toString());
        if (timer.getRemainingDuration() != null) {
            timerData.put("remainingDuration", timer.getRemainingDuration().toString());
        }
        if (timer.getTimerEnd() != null) {
            timerData.put("endTime", timer.getTimerEnd().toString());
        }
        
        sharingService.shareTimerWithUsers(timerId, sharedWith, timerData, new TimerSharingService.SharingCallback() {
            @Override
            public void onSharingSuccess(List<String> successUsers, List<String> failedUsers) {
                Log.i(TAG, "Timer sharing via REST API successful");
                Log.i(TAG, "Success users: " + successUsers);
                Log.i(TAG, "Failed users: " + failedUsers);
                
                // Note: We don't need to update the timer locally since the backend
                // will handle the shared timer relationships. The local sharedWith
                // set is just for UI state management.
                
                // If there were failures, we could show a notification to the user
                if (!failedUsers.isEmpty()) {
                    Log.w(TAG, "Some users failed to receive timer sharing: " + failedUsers);
                    // TODO: Show user-friendly notification about failed shares
                }
            }
            
            @Override
            public void onSharingError(String error) {
                Log.e(TAG, "Timer sharing via REST API failed: " + error);
                
                // Revert the local sharedWith change since the API call failed
                Set<String> currentSharedWith = new HashSet<>(timerView.getSharedWith());
                // Find which user was just added and remove them
                for (String friendName : TimerView.FRIEND_NAMES) {
                    if (currentSharedWith.contains(friendName)) {
                        // This is a bit of a hack - we assume the last added user
                        // is the one that failed. In a more robust implementation,
                        // we'd track which user was just added.
                        Log.w(TAG, "Reverting share with " + friendName + " due to API failure");
                        currentSharedWith.remove(friendName);
                        timerView.setSharedWith(currentSharedWith);
                        timerView.refreshMenuLayout();
                        break;
                    }
                }
                
                // TODO: Show user-friendly error notification
            }
        });
    }
    
    private boolean handleButtonPress(int buttonPressed) {
        Log.d(TAG, "Button " + buttonPressed + " pressed");
        
        switch (buttonPressed) {
            case 0: // +1 minute
                timerView.addTime(java.time.Duration.ofMinutes(1));
                eventListener.onTimerUpdated(timerView.getTimer());
                timerView.invalidate();
                return true;
                
            case 1: // Play/pause
                try {
                    if (timerView.isPaused()) {
                        timerView.unpause();
                    } else {
                        timerView.pause();
                    }
                    eventListener.onTimerUpdated(timerView.getTimer());
                    timerView.invalidate();
                } catch (Exception e) {
                    Log.e(TAG, "Error toggling pause state: " + e.getMessage());
                }
                return true;
                
            case 2: // Close/stop
                eventListener.onBubbleDismiss(timerView.getTimer());
                return true;
                
            case 3: // Open main activity
                try {
                    Intent intent = new Intent(overlayView.getContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    overlayView.getContext().startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open MainActivity from overlay: " + e);
                }
                return true;
                
            case 4: // Share menu
                timerView.showShareMenu();
                return true;
                
            case 5: // Debug toggle
                toggleDebugMode();
                return true;
                
            default:
                Log.w(TAG, "Unknown button pressed: " + buttonPressed);
                return false;
        }
    }
    
    private boolean shouldDismissTimer() {
        if (pulledToDismissCircle == null) {
            return false;
        }
        
        // Check if we're still close to the dismiss circle
        int[] bubbleCenter = DismissCalculator.calculateBubbleCenter(
            layoutParams.x, layoutParams.y, 
            overlayView.getWidth(), overlayView.getHeight());
            
        boolean isNearDismissCircle = DismissCalculator.isWithinDismissActionThreshold(
            bubbleCenter[0], bubbleCenter[1], 
            pulledToDismissCircle.centerX, pulledToDismissCircle.centerY);
            
        Log.d(TAG, "Dismiss check: near=" + isNearDismissCircle + " type=" + pulledToDismissCircle.type);
        
        // Both DISMISS and STOP types should trigger dismissal
        return isNearDismissCircle;
    }
    
    private void cleanupDragState() {
        timerView.setDragging(false);
        eventListener.onDragStateChanged(false);
        dismissCircleManager.hideDismissCircles();
        pulledToDismissCircle = null;
        Log.d(TAG, "Cleaned up drag state");
    }
    
    private void updateScreenDimensions() {
        android.util.DisplayMetrics displayMetrics = overlayView.getContext()
            .getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
    }
    
    private void updateDebugInfo(String action, MotionEvent event) {
        if (currentTouchState == null) return;
        
        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("=== ").append(action).append(" Debug ===\n");
        debugInfo.append("Raw: [").append(event.getRawX()).append(",").append(event.getRawY()).append("]\n");
        debugInfo.append("Local: [").append(event.getX()).append(",").append(event.getY()).append("]\n");
        debugInfo.append("Delta: [").append(currentTouchState.getDeltaX()).append(",").append(currentTouchState.getDeltaY()).append("]\n");
        debugInfo.append("Type: ").append(currentTouchState.getTouchType()).append("\n");
        debugInfo.append("Distance: ").append(currentTouchState.getTotalDistance()).append("\n");
        
        eventListener.onDebugInfoUpdate(debugInfo.toString());
    }
    
    private void toggleDebugMode() {
        isDebugModeEnabled = !isDebugModeEnabled;
        Log.d(TAG, "Debug mode toggled: " + isDebugModeEnabled);
        eventListener.onDebugInfoUpdate("Debug mode: " + (isDebugModeEnabled ? "ON" : "OFF"));
        eventListener.onDebugModeChanged(isDebugModeEnabled);
    }
    
    public void setDebugMode(boolean enabled) {
        isDebugModeEnabled = enabled;
    }
    
    public boolean isDebugModeEnabled() {
        return isDebugModeEnabled;
    }
}
