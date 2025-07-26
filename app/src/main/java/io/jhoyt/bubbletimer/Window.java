package io.jhoyt.bubbletimer;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Window {
    private static final int SMALL_BUBBLE_RADIUS = 80;
    private static final int LITTLE_BUBBLE_RADIUS = 120;
    private static final int BIG_BUBBLE_RADIUS = 200;
    private static final int DISMISS_THRESHOLD_PIXELS = 50;  // Distance threshold for considering timer "pulled to dismiss"

    private final WindowManager.LayoutParams params;
    private final WindowManager windowManager;
    private final RelativeLayout view;
    private final LayoutInflater layoutInflater;
    private WindowManager.LayoutParams dismissCircleParams;
    private RelativeLayout dismissCircleView;
    private DismissCircleView dismissCircle;

    // Debug overlay
    private WindowManager.LayoutParams debugParams;
    private RelativeLayout debugView;
    private TextView debugText;
    private boolean isDebugModeEnabled = false;  // Track if debug mode is enabled through circle menu

    private final TimerView timer;
    private Boolean isOpen;
    private final String userId;

    // Touch-related state
    private float origdx, origdy;
    private float dx, dy;
    private int lastx, lasty;
    private boolean dragging = false;
    private DismissCircleView.DismissCircle pulledToDismissCircle = null;

    public interface BubbleEventListener {
        void onBubbleDismiss(Timer timer);
        void onBubbleClick(Timer timer);
        void onTimerUpdated(Timer timer);
        void onTimerStopped(Timer timer);
    }

    private BubbleEventListener bubbleEventListener;

    public Window(Context context, Boolean expanded, String userId) {
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.userId = userId;

        if (expanded) {
            this.view = (RelativeLayout) layoutInflater.inflate(R.layout.expanded_popup_window, null);
        } else {
            this.view = (RelativeLayout) layoutInflater.inflate(R.layout.popup_window, null);
        }

        this.timer = this.view.findViewById(R.id.timer);
        this.isOpen = false;

        // Get usable screen dimensions (excluding navigation bar)
        int[] usableDimensions = getUsableScreenDimensions(this.view);
        int usableWidth = usableDimensions[0];
        int usableHeight = usableDimensions[1];
        Log.d("Window", "Usable width: " + usableWidth + ", usable height: " + usableHeight);
        this.timer.setScreenDimensions(usableWidth, usableHeight);

        // Setup dismiss circle window
        this.dismissCircleView = (RelativeLayout) layoutInflater.inflate(R.layout.dismiss_circle_window, null);
        this.dismissCircle = this.dismissCircleView.findViewById(R.id.dismissCircle);
        Log.d("Window", "Setting dismissCircle dimensions: width=" + usableWidth + ", height=" + usableHeight);
        this.dismissCircle.setScreenDimensions(usableWidth, usableHeight);


        // Ensure both overlays are truly full screen and use the same params
        this.dismissCircleParams = new WindowManager.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT,
                TYPE_APPLICATION_OVERLAY,
                FLAG_NOT_FOCUSABLE |
                        FLAG_LAYOUT_IN_SCREEN |
                        FLAG_LAYOUT_NO_LIMITS |
                        FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        this.dismissCircleParams.gravity = Gravity.TOP | Gravity.START;

        // Restore timer overlay to WRAP_CONTENT and Gravity.CENTER
        this.params = new WindowManager.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT,
                TYPE_APPLICATION_OVERLAY,
                FLAG_NOT_FOCUSABLE |
                        FLAG_LAYOUT_IN_SCREEN |
                        FLAG_LAYOUT_NO_LIMITS |
                        FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        if (expanded) {
            timer.setSmallMode(false);
            timer.setExpandedMode(true);
            
            // Wait for view to be measured
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            
            // Calculate center position
            params.x = (usableWidth - view.getMeasuredWidth()) / 2;
            params.y = (usableHeight - view.getMeasuredHeight()) / 2;
            
            Log.d("Window", "Expanded bubble initial position: [" + params.x + "," + params.y + "]");
            Log.d("Window", "Screen dimensions: [" + usableWidth + "," + usableHeight + "]");
            Log.d("Window", "View dimensions: [" + view.getMeasuredWidth() + "," + view.getMeasuredHeight() + "]");
        } else {
            timer.setSmallMode(true);
            timer.setExpandedMode(false);

            params.y = (int) ((double)usableHeight * 0.1); // Move down by 10% of screen height
        }

        // Set the current user ID
        timer.setCurrentUserId(userId);

        this.timer.setOnModeChangeListener(new TimerView.OnModeChangeListener() {
            @Override
            public void onModeChanged(boolean isSmallMode) {
                Log.d("Window", "Mode changed to small mode: " + isSmallMode);
                if (isDebugModeEnabled && debugView.getParent() == null) {
                    showDebugOverlay();
                }
            }
        });

        /*
        this.timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });
         */

        // TODO: Move into TimerView
        // Maybe TODON'T? Tried and got hung up trying to get things out of params
        this.timer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                Window.this.windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                int height = displayMetrics.heightPixels;
                int width = displayMetrics.widthPixels;

                int buttonPressed = timer.getButtonAtPoint(event.getX(), event.getY());
                boolean isTouchInside = buttonPressed != -1 || timer.isPointInMainCircle(event.getX(), event.getY());

                Log.d("Window", "OnTouch: start");
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!isTouchInside) {
                            return false;
                        }

                        Log.d("Window", "view: [" + view.getX() + "," + view.getY() + "]");
                        Log.d("Window", "event: [" + event.getRawX() + "," + event.getRawY() + "]");
                        Log.d("Window", "event: [" + event.getX() + "," + event.getY() + "]");

                        dx = view.getX() - event.getRawX();
                        dy = view.getY() - event.getRawY();
                        Log.d("Window", "dx/dy: [" + dx + "," + dy + "]");

                        // Update debug overlay only if debug mode is enabled
                        if (isDebugModeEnabled) {
                            StringBuilder touchDebugInfo = new StringBuilder();
                            touchDebugInfo.append("=== Touch Debug ===\n");
                            touchDebugInfo.append("View Position: [").append(view.getX()).append(",").append(view.getY()).append("]\n");
                            touchDebugInfo.append("Raw Event: [").append(event.getRawX()).append(",").append(event.getRawY()).append("]\n");
                            touchDebugInfo.append("Local Event: [").append(event.getX()).append(",").append(event.getY()).append("]\n");
                            touchDebugInfo.append("dx/dy: [").append(dx).append(",").append(dy).append("]\n");
                            updateDebugText(touchDebugInfo.toString());
                        }

                        dragging = true;
                        pulledToDismissCircle = null;
                        timer.setDragging(true);
                        // Only show dismiss circle for non-expanded bubbles
                        if (timer.isSmallMode()) {
                            showDismissCircle();
                        }
                        Log.d("Window", "Set dragging to true");
                        break;

                    case MotionEvent.ACTION_MOVE:
                        // Only update debug text for move events if debug mode is enabled
                        if (isDebugModeEnabled) {
                            StringBuilder moveDebugInfo = new StringBuilder();
                            moveDebugInfo.append("=== Move Debug ===\n");
                            moveDebugInfo.append("Raw Position: [").append(event.getRawX()).append(",").append(event.getRawY()).append("]\n");
                            moveDebugInfo.append("Dragging: ").append(dragging).append("\n");
                            moveDebugInfo.append("Button Pressed: ").append(buttonPressed).append("\n");
                            moveDebugInfo.append("Expanded Mode: ").append(timer.isExpandedMode()).append("\n");
                            updateDebugText(moveDebugInfo.toString());
                        }

                        if (!dragging || buttonPressed != -1 || timer.isExpandedMode()) {
                            return false;
                        }

                        if (params.gravity == Gravity.CENTER) {
                            params.gravity = Gravity.NO_GRAVITY;
                            origdx = dx;
                            origdy = dy;
                        }

                        // Calculate new position
                        float newX = event.getRawX() - origdx;
                        float newY = event.getRawY() - origdy;

                        // Check proximity to all dismiss circles
                        DismissCircleView.DismissCircle nearest = dismissCircle.getNearestDismissCircle(event.getRawX(), event.getRawY());
                        if (nearest != null) {
                            params.gravity = Gravity.TOP | Gravity.START;
                            newX = nearest.centerX - (view.getWidth() / 2);
                            newY = nearest.centerY - (view.getHeight() / 2);
                            newX = Math.max(0, Math.min(newX, width- view.getWidth()));
                            newY = Math.max(0, Math.min(newY, height- view.getHeight()));
                            float centerX = newX + (view.getWidth() / 2);
                            float centerY = newY + (view.getHeight() / 2);
                            float dx = centerX - nearest.centerX;
                            float dy = centerY - nearest.centerY;
                            float distance = (float) Math.sqrt(dx * dx + dy * dy);
                            pulledToDismissCircle = (distance < DISMISS_THRESHOLD_PIXELS) ? nearest : null;
                        } else {
                            newX = event.getRawX() + origdx - (view.getWidth() / 2);
                            newY = event.getRawY() + origdy - (view.getHeight() / 2);
                            params.flags |= FLAG_LAYOUT_NO_LIMITS;
                            pulledToDismissCircle = null;
                        }

                        params.x = (int) newX;
                        params.y = (int) newY;

                        Log.d("Window", "event: [" + event.getRawX() + "," + event.getRawY() + "]");
                        Log.d("Window", "Move: [" + params.x + "," + params.y + "]");
                        params.setCanPlayMoveAnimation(true);
                        windowManager.updateViewLayout(view, params);

                        Log.d("Window", "OnTouch: setX/Y");
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        Log.d("Window", "ACTION_CANCEL received");
                        cleanupDragState();
                        return true;

                    case MotionEvent.ACTION_UP:
                        Log.d("Window", "ACTION_UP - event: [" + event.getRawX() + "," + event.getRawY() + "]");
                        Log.d("Window", "ACTION_UP - dx/dy: [" + dx + "," + dy + "]");

                        if (!dragging) {
                            return false;
                        }

                        cleanupDragState();

                        // Handle share menu state
                        if (timer.isInShareMenu()) {
                            if (buttonPressed == 100) { // Back button
                                timer.hideShareMenu();
                                return true;
                            }
                            Set<String> sharedWith = new HashSet<>(timer.getSharedWith());
                            if (buttonPressed == 101) {
                                // Toggle share with Alice
                                if (sharedWith.contains("ouchthathoyt")) {
                                    sharedWith.remove("ouchthathoyt");
                                    timer.setSharedWith(sharedWith);
                                } else {
                                    timer.shareWith("ouchthathoyt");
                                }
                                timer.refreshMenuLayout();
                                bubbleEventListener.onTimerUpdated(timer.getTimer());
                                return true;
                            }
                            if (buttonPressed == 102) {
                                // Toggle share with Bob
                                if (sharedWith.contains("jill")) {
                                    sharedWith.remove("jill");
                                    timer.setSharedWith(sharedWith);
                                } else {
                                    timer.shareWith("jill");
                                }
                                timer.refreshMenuLayout();
                                bubbleEventListener.onTimerUpdated(timer.getTimer());
                                return true;
                            }
                            if (buttonPressed == 103) {
                                // Toggle share with Carol
                                if (sharedWith.contains("tester")) {
                                    sharedWith.remove("tester");
                                    timer.setSharedWith(sharedWith);
                                } else {
                                    timer.shareWith("tester");
                                }
                                timer.refreshMenuLayout();
                                bubbleEventListener.onTimerUpdated(timer.getTimer());
                                return true;
                            }
                            // Ignore other touches in share menu
                            return false;
                        }

                        // Handle dismiss logic for any circle
                        if (pulledToDismissCircle != null) {
                            if (pulledToDismissCircle.type == DismissCircleView.DismissType.STOP) {
                                // Stop timer and dismiss
                                params.x = lastx;
                                params.y = lasty;
                                params.setCanPlayMoveAnimation(true);
                                windowManager.updateViewLayout(view, params);
                                bubbleEventListener.onTimerStopped(timer.getTimer());
                                Window.this.close();
                                return true;
                            } else if (pulledToDismissCircle.type == DismissCircleView.DismissType.DISMISS) {
                                // Just dismiss bubble, do not stop timer
                                params.x = lastx;
                                params.y = lasty;
                                params.setCanPlayMoveAnimation(true);
                                windowManager.updateViewLayout(view, params);
                                bubbleEventListener.onBubbleDismiss(timer.getTimer());
                                Window.this.close();
                                return true;
                            }
                        }

                        // Handle button presses
                        if (buttonPressed >= 0 && buttonPressed <= 5) {  // Updated to include debug button
                            Log.d("Window", "ACTION_UP - Button " + buttonPressed + " pressed");
                            switch (buttonPressed) {
                                case 0:
                                    timer.addTime(Duration.ofMinutes(1));
                                    bubbleEventListener.onTimerUpdated(timer.getTimer());
                                    return true;
                                case 1:
                                    if (timer.isPaused()) {
                                        timer.unpause();
                                    } else {
                                        timer.pause();
                                    }
                                    bubbleEventListener.onTimerUpdated(timer.getTimer());
                                    return true;
                                case 2:
                                    Window.this.close();
                                    return true;
                                case 3:
                                    Intent intent = new Intent(context, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    context.startActivity(intent);
                                    return true;
                                case 4:
                                    timer.showShareMenu();
                                    return true;
                                case 5:
                                    toggleDebugOverlay();
                                    return true;
                            }
                        }

                        // Check for dismissal before checking if touch is inside
                        if (shouldDismissTimer()) {
                            dismissTimer();
                            return true;
                        }

                        if (!isTouchInside) {
                            Log.d("Window", "ACTION_UP - Touch not inside, returning false");
                            return false;
                        }

                        // Check for click (no drag)
                        Log.d("Window", "=== Click Check Debug ===");
                        Log.d("Window", "Raw X/Y: [" + event.getRawX() + "," + event.getRawY() + "]");
                        Log.d("Window", "Initial position: [" + (0 - dx) + "," + (0 - dy) + "]");
                        Log.d("Window", "Is click: " + (event.getRawX() == (0 - dx) && event.getRawY() == (0 - dy)));
                        
                        if (event.getRawX() == (0 - dx) && event.getRawY() == (0 - dy)) {
                            Log.d("Window", "ACTION_UP - CLICK DETECTED");
                            bubbleEventListener.onBubbleClick(timer.getTimer());
                            return true;
                        }

                        Log.d("Window", "ACTION_UP - " + height);

                        // If not dismissing, snap to sides
                        if (timer.isSmallMode()) {
                            int overlap = timer.getWidth() / 5;

                            if (event.getRawX() < (width / 2.0)) {
                                params.x = 0 - overlap;
                            } else {
                                params.x = width - timer.getWidth() + overlap;
                            }
                        }

                        params.setCanPlayMoveAnimation(true);
                        windowManager.updateViewLayout(view, params);

                        lastx = params.x;
                        lasty = params.y;

                        return true;

                    default:
                        Log.d("Window", "OnTouch: default case - false");
                        return false;
                }

                Log.d("Window", "OnTouch: end");
                return true;
            }
        });

        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    public void invalidate() {
        TimerView timerView = (TimerView) view.getChildAt(0);
        timerView.invalidate();
    }

    public Boolean isOpen() {
        return this.isOpen;
    }

    public void open(Timer timer, BubbleEventListener onCloseListener) {
        this.isOpen = true;
        this.bubbleEventListener = onCloseListener;
        try {
            // check if view is already inflated or present
            if (view.getWindowToken() == null &&
                    view.getParent() == null) {

                this.timer.setTimer(timer);

                windowManager.addView(view, params);
                // Restore debug overlay state if it was previously visible
                if (isDebugModeEnabled) {
                    showDebugOverlay();
                }
            }
        } catch (Exception e) {
            Log.e("Error while opening: ", e.toString());
        }
    }

    public void close() {
        this.isOpen = false;
        try {
            // TODO if it doesn't work maybe switch to getting the system service again?
            view.invalidate();
            windowManager.removeView(view);
            if (dismissCircleView.getParent() != null) {
                windowManager.removeView(dismissCircleView);
            }
            // Always remove debug overlay when closing
            if (debugView.getParent() != null) {
                windowManager.removeView(debugView);
            }
        } catch (Exception e) {
            Log.e("Error while closing: ", e.toString());
        }
    }

    private void showDismissCircle() {
        try {
            if (dismissCircleView.getParent() == null) {
                windowManager.addView(dismissCircleView, dismissCircleParams);
            }
        } catch (Exception e) {
            Log.e("Error showing dismiss circle: ", e.toString());
        }
    }

    private void hideDismissCircle() {
        try {
            if (dismissCircleView.getParent() != null) {
                windowManager.removeView(dismissCircleView);
            }
        } catch (Exception e) {
            Log.e("Error hiding dismiss circle: ", e.toString());
        }
    }

    public void showDebugOverlay() {
        Log.d("Window", "showDebugOverlay called - debug mode: " + isDebugModeEnabled);
        if (!isDebugModeEnabled) {
            Log.d("Window", "Debug mode not enabled, not showing overlay");
            return;
        }
        // First ensure any existing overlay is removed
        if (debugView.getParent() != null) {
            try {
                windowManager.removeView(debugView);
                Log.d("Window", "Removed existing debug overlay");
            } catch (Exception e) {
                Log.e("Window", "Error removing existing debug overlay: " + e.toString());
            }
        }

        try {
            debugParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    TYPE_APPLICATION_OVERLAY,
                    FLAG_NOT_FOCUSABLE |
                            FLAG_LAYOUT_IN_SCREEN |
                            FLAG_LAYOUT_NO_LIMITS |
                            FLAG_NOT_TOUCH_MODAL |
                            FLAG_NOT_TOUCHABLE |
                            FLAG_LAYOUT_INSET_DECOR |
                            FLAG_LAYOUT_IN_OVERSCAN,
                    PixelFormat.TRANSLUCENT
            );
            debugParams.gravity = Gravity.TOP | Gravity.START;
            debugParams.type = TYPE_APPLICATION_OVERLAY;
            // Set a higher z-order to ensure it stays on top
            debugParams.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR;
            // Ensure debug overlay stays on top of other windows
            debugParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            debugParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            windowManager.addView(debugView, debugParams);
            Log.d("Window", "Debug overlay shown");
            // Set initial debug text
            updateDebugText("Debug overlay enabled\nClick debug button to toggle");
        } catch (Exception e) {
            Log.e("Window", "Error showing debug overlay: " + e.toString());
        }
    }

    public void hideDebugOverlay() {
        Log.d("Window", "hideDebugOverlay called - debug mode: " + isDebugModeEnabled);
        if (debugView.getParent() != null) {
            try {
                windowManager.removeView(debugView);
                Log.d("Window", "Debug overlay hidden");
            } catch (Exception e) {
                Log.e("Window", "Error hiding debug overlay: " + e.toString());
            }
        } else {
            Log.d("Window", "Debug overlay already hidden");
        }
    }

    public void updateDebugText(String text) {
        Log.d("Window", "updateDebugText called - debug mode: " + isDebugModeEnabled + ", debugText null: " + (debugText == null) + ", debugView parent: " + (debugView.getParent() != null));
        if (isDebugModeEnabled && debugText != null) {
            if (debugView.getParent() == null) {
                Log.d("Window", "Debug view has no parent, showing overlay");
                showDebugOverlay();
            }
            
            final String finalText = text;  // Create final copy for the runnable
            debugView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("Window", "Running debug text update on UI thread");
                    debugText.setText(finalText);
                    debugText.postInvalidate();
                    debugView.postInvalidate();
                    try {
                        windowManager.updateViewLayout(debugView, debugParams);
                        Log.d("Window", "Debug overlay layout updated");
                    } catch (Exception e) {
                        Log.e("Window", "Error updating debug overlay layout: " + e.toString());
                        // If we get an error, try to show the overlay again
                        showDebugOverlay();
                    }
                    Log.d("Window", "Debug text updated on UI thread: " + finalText);
                }
            });
        }
    }

    public void toggleDebugOverlay() {
        if (this.debugView == null || this.debugText == null) {
            // Setup debug window
            this.debugView = (RelativeLayout) layoutInflater.inflate(R.layout.debug_window, null);
            this.debugText = this.debugView.findViewById(R.id.debugText);
        }

        Log.d("Window", "Debug overlay initialized");
        Log.d("Window", "Toggling debug overlay, current debug mode: " + isDebugModeEnabled);
        
        // Send broadcast to service to toggle debug mode
        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "toggleDebugMode");
        LocalBroadcastManager.getInstance(view.getContext()).sendBroadcast(message);
        
        Log.d("Window", "Debug overlay toggled, new debug mode: " + isDebugModeEnabled);
        // Force an update to verify the state
        updateDebugText("Debug overlay toggled\nDebug mode: " + isDebugModeEnabled);
    }

    public TimerView getTimerView() {
        return this.timer;
    }

    /**
     * Checks if the timer should be dismissed based on its position relative to the dismiss circle.
     * @return true if the timer should be dismissed, false otherwise
     */
    private boolean shouldDismissTimer() {
        // This method is now only used for legacy fallback, but let's keep it for now
        DismissCircleView.DismissCircle nearest = dismissCircle.getNearestDismissCircle(
            params.x + (view.getWidth() / 2),
            params.y + (view.getHeight() / 2)
        );
        boolean isCurrentlyPulled = (nearest != null && nearest.type == DismissCircleView.DismissType.STOP);
        Log.d("Window", "=== Dismiss Check Debug ===");
        Log.d("Window", "Timer position: [" + params.x + "," + params.y + "]");
        Log.d("Window", "Timer center: [" + (params.x + (view.getWidth() / 2)) + "," + (params.y + (view.getHeight() / 2)) + "]");
        Log.d("Window", "Dismiss circle: [" + (nearest != null ? nearest.centerX : "null") + "," + (nearest != null ? nearest.centerY : "null") + "]");
        Log.d("Window", "Is currently pulled: " + isCurrentlyPulled);
        Log.d("Window", "Was pulled to dismiss: " + (pulledToDismissCircle != null ? pulledToDismissCircle.type : "null"));
        return isCurrentlyPulled || (pulledToDismissCircle != null && pulledToDismissCircle.type == DismissCircleView.DismissType.STOP);
    }

    /**
     * Dismisses the timer and notifies the listener.
     */
    private void dismissTimer() {
        Log.d("Window", "Dismissing timer - is currently pulled to dismiss circle");
        params.x = lastx;
        params.y = lasty;

        params.setCanPlayMoveAnimation(true);
        windowManager.updateViewLayout(view, params);

        bubbleEventListener.onBubbleDismiss(timer.getTimer());

        Window.this.close();
    }

    private void cleanupDragState() {
        dragging = false;
        timer.setDragging(false);
        hideDismissCircle();
        Log.d("Window", "Cleaned up drag state");
    }

    // Utility method to get usable screen dimensions (excluding navigation bar/system insets)
    private int[] getUsableScreenDimensions(View anchorView) {
        int width = 0;
        int height = 0;

        WindowInsets insets = anchorView.getRootWindowInsets();
        if (insets != null) {
            width = anchorView.getContext().getResources().getDisplayMetrics().widthPixels;
            height = anchorView.getContext().getResources().getDisplayMetrics().heightPixels - insets.getInsets(WindowInsets.Type.systemBars()).bottom;
        } else {
            width = anchorView.getContext().getResources().getDisplayMetrics().widthPixels;
            height = anchorView.getContext().getResources().getDisplayMetrics().heightPixels;
        }

        return new int[]{width, height};
    }

    public void setDebugMode(boolean enabled) {
        isDebugModeEnabled = enabled;
        if (isDebugModeEnabled) {
            if (this.debugView == null || this.debugText == null) {
                // Setup debug window
                this.debugView = (RelativeLayout) layoutInflater.inflate(R.layout.debug_window, null);
                this.debugText = this.debugView.findViewById(R.id.debugText);
            }
            showDebugOverlay();
        } else {
            hideDebugOverlay();
        }
    }
}
