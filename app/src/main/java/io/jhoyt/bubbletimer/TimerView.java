package io.jhoyt.bubbletimer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.util.Set;
import java.util.HashSet;

public class TimerView extends View {
    private static final float SMALL_CIRCLE_RADIUS = 120.0f;
    private static final float LARGE_CIRCLE_RADIUS = 200.0f;
    private static final float BUTTON_RADIUS = 80.0f;
    private static final float DISMISS_CIRCLE_RADIUS = 80.0f;
    private static final float DISMISS_CIRCLE_PULL_THRESHOLD = 150.0f;
    private static final float DISMISS_CIRCLE_BOTTOM_MARGIN = 200.0f; // Increased margin from bottom

    public static final int MODE_AUTO = -1;
    public static final int MODE_OVERLAY = 0;
    public static final int MODE_LIST_ITEM = 1;

    public interface OnModeChangeListener {
        void onModeChanged(boolean isSmallMode);
    }

    private OnModeChangeListener modeChangeListener;

    private int layoutMode = MODE_AUTO;
    private int screenWidth = 0;
    private int screenHeight = 0;

    private Paint textPaint;
    private Paint redTextPaint;
    private Paint arcPaint;
    private Paint circlePaint;
    private Paint dismissCirclePaint;
    private Paint dismissCircleTextPaint;

    private boolean isSmallMode = false;
    private boolean isExpandedMode = false;
    private boolean isDragging = false;

    private Bitmap bubbleBitmap;
    private Bitmap pauseBitmap;
    private Bitmap playBitmap;
    private Bitmap plusOneMinBitmap;
    private Bitmap closeBitmap;
    private Bitmap shareBitmap;
    private Bitmap debugBitmap;

    private Timer timer;
    private String currentUserId;
    private long lastUpdateTime = 0;
    private static final long UPDATE_THRESHOLD = 1000; // 1 second

    private CircularMenuLayout menuLayout;

    private boolean inShareMenu = false;
    private static final int BUTTON_ID_BACK = 100;
    private static final int BUTTON_ID_FRIEND_1 = 101;
    private static final int BUTTON_ID_FRIEND_2 = 102;
    private static final int BUTTON_ID_FRIEND_3 = 103;
    private static final String[] FRIEND_NAMES = {"ouchthathoyt", "jill", "tester"};

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.timer = null;
        this.currentUserId = null;

        this.textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.textPaint.setColor(Color.BLACK);
        this.textPaint.setTextAlign(Paint.Align.CENTER);

        this.redTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.redTextPaint.setColor(Color.RED);
        this.redTextPaint.setTextAlign(Paint.Align.CENTER);

        this.arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.arcPaint.setColor(Color.BLUE);
        this.arcPaint.setStyle(Paint.Style.STROKE);
        this.arcPaint.setStrokeWidth(20.0f);

        this.circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.circlePaint.setColor(Color.WHITE);
        this.circlePaint.setStyle(Paint.Style.FILL);

        this.dismissCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.dismissCirclePaint.setColor(Color.RED);
        this.dismissCirclePaint.setStyle(Paint.Style.FILL);
        this.dismissCirclePaint.setAlpha(180);

        this.dismissCircleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.dismissCircleTextPaint.setColor(Color.WHITE);
        this.dismissCircleTextPaint.setTextAlign(Paint.Align.CENTER);
        this.dismissCircleTextPaint.setTextSize(60.0f);

        this.bubbleBitmap = BitmapFactory.decodeResource(getResources(), R.raw.bubble_logo);
        this.playBitmap = BitmapFactory.decodeResource(getResources(), R.raw.play);
        this.pauseBitmap = BitmapFactory.decodeResource(getResources(), R.raw.pause);
        this.plusOneMinBitmap = BitmapFactory.decodeResource(getResources(), R.raw.plus_one_min);
        this.closeBitmap = BitmapFactory.decodeResource(getResources(), R.raw.bubble_x);
        this.shareBitmap = BitmapFactory.decodeResource(getResources(), R.raw.bubble_share);
        // Convert vector drawable to bitmap
        android.graphics.drawable.Drawable drawable = getResources().getDrawable(R.drawable.debug, null);
        this.debugBitmap = android.graphics.Bitmap.createBitmap(24, 24, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(debugBitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
    }

    public Timer getTimer() {
        return this.timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void addTime(Duration duration) {
        this.timer.addTime(duration);
    }

    public boolean isPaused() {
        return this.timer.isPaused();
    }

    public void pause() {
        this.timer.pause();
    }

    public Duration getRemainingDuration() {
        return this.timer.getRemainingDuration();
    }

    public void unpause() {
        this.timer.unpause();
    }

    public void setSmallMode(boolean isSmallMode) {
        boolean oldMode = this.isSmallMode;
        this.isSmallMode = isSmallMode;
        if (oldMode != isSmallMode && modeChangeListener != null) {
            modeChangeListener.onModeChanged(isSmallMode);
        }
    }

    public void setExpandedMode(boolean isExpandedMode) {
        this.isExpandedMode = isExpandedMode;
    }

    public boolean isSmallMode() {
        return this.isSmallMode;
    }

    public boolean isExpandedMode() {
        return this.isExpandedMode;
    }

    public void shareWith(String username) {
        this.timer.shareWith(username);
    }

    public void setSharedWith(Set<String> sharedWith) {
        this.timer.setSharedWith(sharedWith);
    }

    public Set<String> getSharedWith() {
        return this.timer.getSharedWith();
    }

    public Set<String> getTags() {
        return this.timer.getTags();
    }

    public void setTags(Set<String> tags) {
        this.timer.setTags(tags);
    }

    public void setLayoutMode(int mode) {
        this.layoutMode = mode;
        this.menuLayout = null; // force re-setup
        invalidate();
    }

    private int getEffectiveLayoutMode() {
        if (layoutMode != MODE_AUTO) return layoutMode;
        // Auto-detect: if view is large, use overlay; if small, use list item
        int minDim = Math.min(getWidth(), getHeight());
        // Threshold: 300px (tweak as needed)
        return minDim > 300 ? MODE_OVERLAY : MODE_LIST_ITEM;
    }

    public void showShareMenu() {
        inShareMenu = true;
        menuLayout = null;
        invalidate();
    }

    public void hideShareMenu() {
        inShareMenu = false;
        menuLayout = null;
        invalidate();
    }

    public boolean isInShareMenu() {
        return inShareMenu;
    }

    public void refreshMenuLayout() {
        this.menuLayout = null;
        invalidate();
    }

    private void setupMenuLayout() {
        int mode = getEffectiveLayoutMode();
        float centerX, centerY, mainRadius, buttonRadius;
        if (mode == MODE_LIST_ITEM) {
            mainRadius = SMALL_CIRCLE_RADIUS;
            buttonRadius = 0;
            centerX = SMALL_CIRCLE_RADIUS;
            centerY = SMALL_CIRCLE_RADIUS;
            menuLayout = new CircularMenuLayout(centerX, centerY, mainRadius, buttonRadius, timer, currentUserId);
        } else if (inShareMenu) {
            centerX = getWidth() / 2.0f;
            centerY = getHeight() / 2.0f;
            mainRadius = LARGE_CIRCLE_RADIUS;
            buttonRadius = BUTTON_RADIUS;
            Set<String> sharedWith = timer.getSharedWith();
            menuLayout = new CircularMenuLayout(centerX, centerY, mainRadius, buttonRadius, timer, currentUserId);
            // Add center back button as a real button
            menuLayout.addButtonWithText("Back", BUTTON_ID_BACK, 0, 0, timer -> false);
            // Add friend buttons with text and selected state
            menuLayout.addButtonWithText(FRIEND_NAMES[0], BUTTON_ID_FRIEND_1, 0, mainRadius + buttonRadius, timer -> sharedWith.contains(FRIEND_NAMES[0]));
            menuLayout.addButtonWithText(FRIEND_NAMES[1], BUTTON_ID_FRIEND_2, 120, mainRadius + buttonRadius, timer -> sharedWith.contains(FRIEND_NAMES[1]));
            menuLayout.addButtonWithText(FRIEND_NAMES[2], BUTTON_ID_FRIEND_3, 240, mainRadius + buttonRadius, timer -> sharedWith.contains(FRIEND_NAMES[2]));
        } else {
            centerX = getWidth() / 2.0f;
            centerY = getHeight() / 2.0f;
            mainRadius = LARGE_CIRCLE_RADIUS;
            buttonRadius = BUTTON_RADIUS;
            menuLayout = new CircularMenuLayout(centerX, centerY, mainRadius, buttonRadius, timer, currentUserId);
            if (isExpandedMode) {
                menuLayout.addButton(plusOneMinBitmap, 0, 0);
                menuLayout.addButton(pauseBitmap, 1, 90);
                menuLayout.addButton(closeBitmap, 2, 180);
                menuLayout.addButton(bubbleBitmap, 3, 270);
                menuLayout.addButton(shareBitmap, 4, 315);
                menuLayout.addButton(debugBitmap, 5, 45);  // Add debug button at 45 degrees
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (timer == null) {
            return;
        }
        lastUpdateTime = System.currentTimeMillis();
        Duration remaining = this.timer.getRemainingDuration();
        String durationText = DurationUtil.getFormattedDuration(remaining);
        if (menuLayout == null) {
            setupMenuLayout();
        }
        float remainingDuration = (float) remaining.getSeconds() + (remaining.getNano() / 1000000000.0f);
        float totalDuration = (float) this.timer.getTotalDuration().getSeconds();
        float sweepAngle = 360.0f * Math.max(remainingDuration, 0.0f) / totalDuration;
        int mode = getEffectiveLayoutMode();
        float textSize = mode == MODE_LIST_ITEM ? 60.0f : 100.0f;
        textSize -= (durationText.length() / (mode == MODE_LIST_ITEM ? 6 : 7) * 20.0f);

        // Draw the main timer UI first
        if (inShareMenu) {
            // Draw back button in center
            float centerX = BUTTON_RADIUS * 2 + LARGE_CIRCLE_RADIUS;
            float centerY = BUTTON_RADIUS * 2 + LARGE_CIRCLE_RADIUS;
            float mainRadius = LARGE_CIRCLE_RADIUS;
            Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backPaint.setColor(Color.LTGRAY);
            canvas.drawCircle(centerX, centerY, mainRadius, backPaint);
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(80.0f);
            canvas.drawText("Back", centerX, centerY + 30.0f, textPaint);
            // Draw friend buttons
            menuLayout.drawButtons(canvas);
        } else {
            menuLayout.drawMainCircle(canvas, sweepAngle, durationText, timer.getName(), textSize);
            if (mode != MODE_LIST_ITEM) {
                menuLayout.drawButtons(canvas);
            }
        }
    }

    public int getButtonAtPoint(float x, float y) {
        if (menuLayout == null) {
            return -1;
        }
        return menuLayout.getButtonAtPoint(x, y);
    }

    public boolean isPointInMainCircle(float x, float y) {
        if (menuLayout == null) {
            return false;
        }
        return menuLayout.isPointInMainCircle(x, y);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
        invalidate();
    }

    public boolean isDragging() {
        return this.isDragging;
    }

    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        invalidate();
    }

    public void setOnModeChangeListener(OnModeChangeListener listener) {
        this.modeChangeListener = listener;
    }
}
