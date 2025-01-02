package io.jhoyt.bubbletimer;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import java.time.Duration;

public class Window {
    private static final int SMALL_BUBBLE_RADIUS = 80;
    private static final int LITTLE_BUBBLE_RADIUS = 120;
    private static final int BIG_BUBBLE_RADIUS = 200;

    private final WindowManager.LayoutParams params;
    private final WindowManager windowManager;
    private final RelativeLayout view;
    private final LayoutInflater layoutInflater;

    private final TimerView timer;
    private Boolean isOpen;

    public interface BubbleEventListener {
        void onBubbleDismiss(Timer timer);
        void onBubbleClick(Timer timer);
        void onTimerUpdated(Timer timer);
        void onTimerStopped(Timer timer);
    }

    private BubbleEventListener bubbleEventListener;

    public Window(Context context, Boolean expanded) {
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (expanded) {
            this.view = (RelativeLayout) layoutInflater.inflate(R.layout.expanded_popup_window, null);
        } else {
            this.view = (RelativeLayout) layoutInflater.inflate(R.layout.popup_window, null);
        }

        this.timer = this.view.findViewById(R.id.timer);
        this.isOpen = false;

        if (expanded) {
            timer.setSmallMode(false);
            timer.setExpandedMode(true);
        } else {
            timer.setSmallMode(true);
            timer.setExpandedMode(false);
        }

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
            float origdx, origdy;
            float dx, dy;
            int lastx, lasty;
            boolean dragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                Window.this.windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                int height = displayMetrics.heightPixels;
                int width = displayMetrics.widthPixels;

                int bigCircleCenterX;
                int bigCircleCenterY;
                if (timer.isSmallMode()) {
                    bigCircleCenterX = LITTLE_BUBBLE_RADIUS;
                    bigCircleCenterY = LITTLE_BUBBLE_RADIUS;
                } else {
                    bigCircleCenterX = SMALL_BUBBLE_RADIUS * 2 + BIG_BUBBLE_RADIUS;
                    bigCircleCenterY = SMALL_BUBBLE_RADIUS * 2 + BIG_BUBBLE_RADIUS;
                }

                int xDiff = Math.abs((int)event.getX() - bigCircleCenterX);
                int yDiff = Math.abs((int)event.getY() - bigCircleCenterY);

                Log.d("Window", "xdiff,ydiff: [" + xDiff + "," + yDiff + "]");

                boolean isTouchInside = false;
                int buttonPressed = -1;
                if (timer.isSmallMode()) {
                    /*
                    params.width = SMALL_BUBBLE_RADIUS * 2;
                    params.height = SMALL_BUBBLE_RADIUS * 2;
                    windowManager.updateViewLayout(view, params);
                     */

                    if ((xDiff * xDiff + yDiff * yDiff) < (124 * 124)) {
                        isTouchInside = true;
                    }
                } else {
                    /*
                    params.width = BIG_BUBBLE_RADIUS * 2 + SMALL_BUBBLE_RADIUS * 4;
                    params.height = BIG_BUBBLE_RADIUS * 2 + SMALL_BUBBLE_RADIUS * 4;
                    windowManager.updateViewLayout(view, params);
                     */

                    if ((xDiff * xDiff + yDiff * yDiff) < (BIG_BUBBLE_RADIUS * BIG_BUBBLE_RADIUS)) {
                        isTouchInside = true;
                    }

                    // button 0 - right
                    int littleCircleCenterX = bigCircleCenterX + BIG_BUBBLE_RADIUS + SMALL_BUBBLE_RADIUS;
                    int littleCircleCenterY = bigCircleCenterY;

                    xDiff = Math.abs((int)event.getX() - littleCircleCenterX);
                    yDiff = Math.abs((int)event.getY() - littleCircleCenterY);

                    if ((xDiff * xDiff + yDiff * yDiff) < (SMALL_BUBBLE_RADIUS * SMALL_BUBBLE_RADIUS)) {
                        isTouchInside = true;
                        buttonPressed = 0;
                    }

                    // button 1 - bottom
                    littleCircleCenterX = bigCircleCenterX;
                    littleCircleCenterY = bigCircleCenterY + BIG_BUBBLE_RADIUS + SMALL_BUBBLE_RADIUS;

                    xDiff = Math.abs((int)event.getX() - littleCircleCenterX);
                    yDiff = Math.abs((int)event.getY() - littleCircleCenterY);

                    if ((xDiff * xDiff + yDiff * yDiff) < (SMALL_BUBBLE_RADIUS * SMALL_BUBBLE_RADIUS)) {
                        isTouchInside = true;
                        buttonPressed = 1;
                    }

                    // button 2 - left
                    littleCircleCenterX = bigCircleCenterX - BIG_BUBBLE_RADIUS - SMALL_BUBBLE_RADIUS;
                    littleCircleCenterY = bigCircleCenterY;

                    xDiff = Math.abs((int)event.getX() - littleCircleCenterX);
                    yDiff = Math.abs((int)event.getY() - littleCircleCenterY);

                    if ((xDiff * xDiff + yDiff * yDiff) < (SMALL_BUBBLE_RADIUS * SMALL_BUBBLE_RADIUS)) {
                        isTouchInside = true;
                        buttonPressed = 2;
                    }

                    // button 3 - top
                    littleCircleCenterX = bigCircleCenterX;
                    littleCircleCenterY = bigCircleCenterY - BIG_BUBBLE_RADIUS - SMALL_BUBBLE_RADIUS;

                    xDiff = Math.abs((int)event.getX() - littleCircleCenterX);
                    yDiff = Math.abs((int)event.getY() - littleCircleCenterY);

                    if ((xDiff * xDiff + yDiff * yDiff) < (SMALL_BUBBLE_RADIUS * SMALL_BUBBLE_RADIUS)) {
                        isTouchInside = true;
                        buttonPressed = 3;
                    }

                    // button 4 - top-right
                    littleCircleCenterX = bigCircleCenterX + (int) (BIG_BUBBLE_RADIUS * .707f) + (int) (SMALL_BUBBLE_RADIUS * .707f);
                    littleCircleCenterY = bigCircleCenterY - (int) (BIG_BUBBLE_RADIUS * .707f) - (int) (SMALL_BUBBLE_RADIUS * .707f);

                    xDiff = Math.abs((int)event.getX() - littleCircleCenterX);
                    yDiff = Math.abs((int)event.getY() - littleCircleCenterY);

                    if ((xDiff * xDiff + yDiff * yDiff) < (SMALL_BUBBLE_RADIUS * SMALL_BUBBLE_RADIUS)) {
                        isTouchInside = true;
                        buttonPressed = 4;
                    }
                }

                Log.d("Window", "OnTouch: start");
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!isTouchInside) {
                            return false;
                        }

                        Log.d("Window", "view: [" + view.getX() + "," + view.getY() + "]");
                        Log.d("Window", "event: [" + event.getRawX() + "," + event.getRawY() + "]");
                        Log.d("Window", "event: [" + event.getX() + "," + event.getY() + "]");

                        // note: view.getX() and view.getY() are almost always 0?
                        dx = view.getX() - event.getRawX();
                        dy = view.getY() - event.getRawY();
                        Log.d("Window", "dx/dy: [" + dx + "," + dy + "]");

                        dragging = true;

                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (!dragging || buttonPressed != -1 || timer.isExpandedMode()) {
                            return false;
                        }

                        if (params.gravity == Gravity.CENTER) {
                            params.gravity = Gravity.NO_GRAVITY;
                            origdx = dx;
                            origdy = dy;
                        }
                        params.x = (int) (event.getRawX() + origdx);
                        params.y = (int) (event.getRawY() + origdy);

                        Log.d("Window", "event: [" + event.getRawX() + "," + event.getRawY() + "]");
                        Log.d("Window", "Move: [" + params.x + "," + params.y + "]");
                        params.setCanPlayMoveAnimation(true);
                        windowManager.updateViewLayout(view, params);

                        Log.d("Window", "OnTouch: setX/Y");
                        break;

                    case MotionEvent.ACTION_UP:
                        Log.d("Window", "ACTION_UP - event: [" + event.getRawX() + "," + event.getRawY() + "]");
                        Log.d("Window", "ACTION_UP - dx/dy: [" + dx + "," + dy + "]");

                        if (!dragging) {
                            return false;
                        }

                        dragging = false;


                        if (buttonPressed == 0) {
                            timer.addTime(Duration.ofMinutes(1));
                            bubbleEventListener.onTimerUpdated(timer.getTimer());
                            return true;
                        }

                        if (buttonPressed == 1) {
                            if (timer.isPaused()) {
                                timer.unpause();
                            } else {
                                timer.pause();
                            }
                            bubbleEventListener.onTimerUpdated(timer.getTimer());
                            return true;
                        }

                        if (buttonPressed == 2) {
                            Window.this.close();
                            return true;
                        }

                        if (buttonPressed == 3) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            context.startActivity(intent);
                            return true;
                        }

                        if (buttonPressed == 4) {
                            timer.shareWith("ouchthathoyt");
                            timer.shareWith("tester");
                            bubbleEventListener.onTimerUpdated(timer.getTimer());
                            return true;
                        }

                        if (!isTouchInside) {
                            return false;
                        }

                        if (event.getRawX() == (0 - dx) && event.getRawY() == (0 - dy)) {
                            Log.d("Window", "ACTION_UP - CLICK DETECTED");

                            bubbleEventListener.onBubbleClick(timer.getTimer());

                            return true;
                        }

                        Log.d("Window", "ACTION_UP - " + height);

                        if (event.getRawY() > ((float) height * 0.8f) &&
                            (event.getRawY() > width * 0.4f
                            && event.getRawX() < width * 0.6f)
                        ) {
                            params.x = lastx;
                            params.y = lasty;

                            params.setCanPlayMoveAnimation(true);
                            windowManager.updateViewLayout(view, params);

                            bubbleEventListener.onTimerStopped(timer.getTimer());
                            bubbleEventListener.onBubbleDismiss(timer.getTimer());

                            Window.this.close();
                            return true;
                        }

                        if (timer.isSmallMode()) {
                            if (event.getRawX() < (width / 2.0)) {
                                params.x = -(width / 2) + SMALL_BUBBLE_RADIUS;
                            } else {
                                params.x = (width / 2) - SMALL_BUBBLE_RADIUS;
                            }
                        } else {
                            if (event.getRawX() < (width / 2.0)) {
                                params.x = -(width / 2) + 160 + SMALL_BUBBLE_RADIUS * 2;
                            } else {
                                params.x = (width / 2) - 160 + SMALL_BUBBLE_RADIUS * 2;
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


        this.params = new WindowManager.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT,
                TYPE_APPLICATION_OVERLAY,
                FLAG_NOT_FOCUSABLE |
                        //FLAG_FULLSCREEN |
                        FLAG_LAYOUT_IN_SCREEN |
                        FLAG_LAYOUT_NO_LIMITS |
                        //FLAG_LAYOUT_INSET_DECOR |
                        FLAG_NOT_TOUCH_MODAL
                ,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;
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
        } catch (Exception e) {
            Log.e("Error while closing: ", e.toString());
        }
    }
}
