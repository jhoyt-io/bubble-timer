package io.jhoyt.bubbletimer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.util.Set;

public class TimerView extends View {
    private static final float arcWidth = 20.0f;
    private static final float circleRadius = 200.0f;
    private static final RectF arcOval =
                new RectF(arcWidth, arcWidth,
                  circleRadius * 2 - arcWidth, circleRadius * 2 - arcWidth);

    private Paint textPaint;
    private Paint redTextPaint;
    private Paint arcPaint;
    private Paint circlePaint;

    private boolean isSmallMode = false;
    private boolean isExpandedMode = false;

    private Bitmap bubbleBitmap;
    private Bitmap pauseBitmap;
    private Bitmap playBitmap;
    private Bitmap plusOneMinBitmap;
    private Bitmap closeBitmap;
    private Bitmap shareBitmap;
    private RectF littleBubbleIconRect = new RectF(0.0f, 0.0f, 0.0f, 0.0f);

    private Timer timer;
    private long lastUpdateTime = 0;
    private static final long UPDATE_THRESHOLD = 1000; // 1 second

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.timer = null; //new Timer();

        this.textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.textPaint.setColor(Color.BLACK);
        this.textPaint.setTextAlign(Paint.Align.CENTER);

        this.redTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.redTextPaint.setColor(Color.RED);
        this.redTextPaint.setTextAlign(Paint.Align.CENTER);

        this.arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.arcPaint.setColor(Color.BLUE);
        this.arcPaint.setDither(true);
        this.arcPaint.setStyle(Paint.Style.STROKE);
        this.arcPaint.setStrokeJoin(Paint.Join.ROUND);
        this.arcPaint.setStrokeCap(Paint.Cap.ROUND);
        this.arcPaint.setPathEffect(new CornerPathEffect(arcWidth));
        this.arcPaint.setStrokeWidth(arcWidth);

        this.circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.circlePaint.setColor(Color.WHITE);
        this.circlePaint.setDither(true);
        this.circlePaint.setStyle(Paint.Style.FILL);

        this.bubbleBitmap = BitmapFactory.decodeResource(getResources(), R.raw.bubble_logo);
        this.playBitmap = BitmapFactory.decodeResource(getResources(), R.raw.play);
        this.pauseBitmap = BitmapFactory.decodeResource(getResources(), R.raw.pause);
        this.plusOneMinBitmap = BitmapFactory.decodeResource(getResources(), R.raw.plus_one_min);
        this.closeBitmap = BitmapFactory.decodeResource(getResources(), R.raw.bubble_x);
        this.shareBitmap = BitmapFactory.decodeResource(getResources(), R.raw.bubble_share);
    }

    public Timer getTimer() {
        return this.timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
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
        this.isSmallMode = isSmallMode;
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

    public Set<String> getTags() {
        return this.timer.getTags();
    }

    public void setTags(Set<String> tags) {
        this.timer.setTags(tags);
    }

    public boolean needsUpdate() {
        if (timer == null || timer.isPaused()) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= UPDATE_THRESHOLD) {
            lastUpdateTime = currentTime;
            return true;
        }
        return false;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (timer == null) {
            return;
        }
        
        // Update last update time when drawing
        lastUpdateTime = System.currentTimeMillis();
        
        Duration remaining = this.timer.getRemainingDuration();
        long remainingSeconds = remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
        String durationText = DurationUtil.getFormattedDuration(remaining);

        float paintCircleRadius = 0.0f;
        float paintArcWidth = 0.0f;
        float textHeight = 0.0f;
        float littleCircleRadius = 80.0f;
        float centerX;
        float centerY;
        if (isSmallMode) {
            paintCircleRadius = 120.0f;
            centerX = paintCircleRadius;
            centerY = paintCircleRadius;
            paintArcWidth = 20.0f;
            textHeight = 60.0f;
            arcOval.left = centerX - paintCircleRadius + paintArcWidth;
            arcOval.top = centerY - paintCircleRadius + paintArcWidth;
            arcOval.right = centerX + paintCircleRadius - paintArcWidth;
            arcOval.bottom = centerY + paintCircleRadius - paintArcWidth;
            arcPaint.setStrokeWidth(paintArcWidth);
            textPaint.setTextSize(textHeight - (durationText.length() / 6 * 20.f));
            redTextPaint.setTextSize(textHeight);
        } else {
            // LARGE
            paintCircleRadius = 200.0f;
            paintArcWidth = 20.0f;
            textHeight = 100.0f;

            if (isExpandedMode) {
                littleCircleRadius = 80.0f;
            } else {
                littleCircleRadius = 0.0f;
            }

            centerX = littleCircleRadius * 2.0f + circleRadius;
            centerY = littleCircleRadius * 2.0f + circleRadius;
            arcOval.left = centerX - paintCircleRadius + paintArcWidth;
            arcOval.top = centerY - paintCircleRadius + paintArcWidth;
            arcOval.right = centerX + paintCircleRadius - paintArcWidth;
            arcOval.bottom = centerY + paintCircleRadius - paintArcWidth;
            arcPaint.setStrokeWidth(paintArcWidth);
            textPaint.setTextSize(textHeight - (durationText.length() / 7 * 20.0f));
            redTextPaint.setTextSize(textHeight);
        }

        float remainingDuration = (float) remaining.getSeconds() + (remaining.getNano() / 1000000000.0f);
        float totalDuration = (float) this.timer.getTotalDuration().getSeconds();
        float sweepAngle = 360.0f * Math.max(remainingDuration, 0.0f) / totalDuration;

        canvas.drawCircle(centerX, centerY, paintCircleRadius, this.circlePaint);
        canvas.drawArc(
                arcOval,
                -90.0f, sweepAngle,
                false,
                this.arcPaint
        );

        if (remainingSeconds > 0) {
            canvas.drawText(durationText, centerX, centerY + (textHeight / 4.0f), this.textPaint);
        } else {
            if (remaining.getNano() > 500000000) {
                canvas.drawText(durationText, centerX, centerY + (textHeight / 4.0f), this.redTextPaint);
            }
        }

        if (!isSmallMode) {
            String name = this.timer.getName();
            this.textPaint.setTextSize(textHeight / 2.0f - Math.min(30.0f, Math.max(0, (name.length() - 10)) * 3.0f));
            canvas.drawText(name, centerX, centerY - (circleRadius / 2.5f), this.textPaint);
            this.textPaint.setTextSize(textHeight);

            // right
            float littleBubbleCenterX = centerX + paintCircleRadius + littleCircleRadius;
            float littleBubbleCenterY = centerY;
            canvas.drawCircle(
                    littleBubbleCenterX,
                    littleBubbleCenterY,
                    littleCircleRadius - 10, this.circlePaint);
            littleBubbleIconRect.left = littleBubbleCenterX - littleCircleRadius / 1.6f;
            littleBubbleIconRect.top = littleBubbleCenterY - littleCircleRadius / 1.6f;
            littleBubbleIconRect.right = littleBubbleCenterX + littleCircleRadius / 1.6f;
            littleBubbleIconRect.bottom = littleBubbleCenterY + littleCircleRadius / 1.6f;
            canvas.drawBitmap(plusOneMinBitmap,
                    null,
                    littleBubbleIconRect,
                    textPaint);

            // left
            littleBubbleCenterX = centerX - paintCircleRadius - littleCircleRadius;
            littleBubbleCenterY = centerY;
            canvas.drawCircle(
                    littleBubbleCenterX,
                    littleBubbleCenterY,
                    littleCircleRadius - 10, this.circlePaint);
            littleBubbleIconRect.left = littleBubbleCenterX - littleCircleRadius / 1.6f;
            littleBubbleIconRect.top = littleBubbleCenterY - littleCircleRadius / 1.6f;
            littleBubbleIconRect.right = littleBubbleCenterX + littleCircleRadius / 1.6f;
            littleBubbleIconRect.bottom = littleBubbleCenterY + littleCircleRadius / 1.6f;
            canvas.drawBitmap(closeBitmap,
                    null,
                    littleBubbleIconRect,
                    textPaint);

            // bottom
            littleBubbleCenterX = centerX;
            littleBubbleCenterY = centerY + paintCircleRadius + littleCircleRadius;
            canvas.drawCircle(
                    littleBubbleCenterX,
                    littleBubbleCenterY,
                    littleCircleRadius - 10, this.circlePaint);
            littleBubbleIconRect.left = littleBubbleCenterX - littleCircleRadius / 1.6f;
            littleBubbleIconRect.top = littleBubbleCenterY - littleCircleRadius / 1.6f;
            littleBubbleIconRect.right = littleBubbleCenterX + littleCircleRadius / 1.6f;
            littleBubbleIconRect.bottom = littleBubbleCenterY + littleCircleRadius / 1.6f;
            if (timer.isPaused()) {
                canvas.drawBitmap(playBitmap,
                        null,
                        littleBubbleIconRect,
                        textPaint);
            } else {
                canvas.drawBitmap(pauseBitmap,
                        null,
                        littleBubbleIconRect,
                        textPaint);
            }

            // top
            littleBubbleCenterX = centerX;
            littleBubbleCenterY = centerY - paintCircleRadius - littleCircleRadius;
            canvas.drawCircle(
                    littleBubbleCenterX,
                    littleBubbleCenterY,
                    littleCircleRadius - 10, this.circlePaint);
            littleBubbleIconRect.left = littleBubbleCenterX - littleCircleRadius / 1.6f;
            littleBubbleIconRect.top = littleBubbleCenterY - littleCircleRadius / 1.6f;
            littleBubbleIconRect.right = littleBubbleCenterX + littleCircleRadius / 1.6f;
            littleBubbleIconRect.bottom = littleBubbleCenterY + littleCircleRadius / 1.6f;
            canvas.drawBitmap(bubbleBitmap,
                    null,
                    littleBubbleIconRect,
                    textPaint);

            // top-right
            littleBubbleCenterX = centerX + paintCircleRadius * 0.707f + littleCircleRadius * 0.707f ;
            littleBubbleCenterY = centerY - paintCircleRadius * 0.707f - littleCircleRadius * 0.707f ;
            canvas.drawCircle(
                    littleBubbleCenterX,
                    littleBubbleCenterY,
                    littleCircleRadius - 10, this.circlePaint);
            littleBubbleIconRect.left = littleBubbleCenterX - littleCircleRadius / 1.6f;
            littleBubbleIconRect.top = littleBubbleCenterY - littleCircleRadius / 1.6f;
            littleBubbleIconRect.right = littleBubbleCenterX + littleCircleRadius / 1.6f;
            littleBubbleIconRect.bottom = littleBubbleCenterY + littleCircleRadius / 1.6f;
            canvas.drawBitmap(shareBitmap,
                    null,
                    littleBubbleIconRect,
                    textPaint);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
