package io.jhoyt.bubbletimer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

public class DismissCircleView extends View {
    public static final float DISMISS_CIRCLE_RADIUS = 80.0f;
    private static final float DISMISS_CIRCLE_BOTTOM_MARGIN = 200.0f;
    private static final float DISMISS_CIRCLE_PULL_THRESHOLD = 200.0f;
    private static final float DISMISS_CIRCLE_PULL_STRENGTH = 0.5f;

    private Paint circlePaint;
    private Paint textPaint;
    private int screenWidth = 0;
    private int screenHeight = 0;

    public DismissCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.circlePaint.setColor(context.getResources().getColor(android.R.color.darker_gray, context.getTheme()));
        this.circlePaint.setStyle(Paint.Style.FILL);
        this.circlePaint.setAlpha(180);

        this.textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.textPaint.setColor(Color.WHITE);
        this.textPaint.setTextAlign(Paint.Align.CENTER);
        this.textPaint.setTextSize(60.0f);
    }

    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight - DISMISS_CIRCLE_BOTTOM_MARGIN - DISMISS_CIRCLE_RADIUS;
        Log.d("DismissCircleView", "onDraw: screenWidth=" + screenWidth + ", screenHeight=" + screenHeight + ", centerY=" + centerY);
        
        // Draw a background circle first
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, DISMISS_CIRCLE_RADIUS + 4, bgPaint);
        
        // Draw the main circle
        canvas.drawCircle(centerX, centerY, DISMISS_CIRCLE_RADIUS, circlePaint);
        
        // Draw the X symbol
        canvas.drawText("×", centerX, centerY + textPaint.getTextSize()/3, textPaint);
    }

    public float[] getDismissCirclePosition() {
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight - DISMISS_CIRCLE_BOTTOM_MARGIN - DISMISS_CIRCLE_RADIUS;
        return new float[]{centerX, centerY};
    }

    public boolean isNearDismissCircle(float x, float y) {
        float[] dismissPos = getDismissCirclePosition();
        float dx = x - dismissPos[0];
        float dy = y - dismissPos[1];
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < DISMISS_CIRCLE_PULL_THRESHOLD;
    }

    public float[] getDismissCirclePullVector(float x, float y) {
        float[] dismissPos = getDismissCirclePosition();
        float dx = dismissPos[0] - x;
        float dy = dismissPos[1] - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance < DISMISS_CIRCLE_PULL_THRESHOLD) {
            float pullStrength = (1.0f - (distance / DISMISS_CIRCLE_PULL_THRESHOLD)) * DISMISS_CIRCLE_PULL_STRENGTH;
            return new float[]{dx * pullStrength, dy * pullStrength};
        }
        return new float[]{0, 0};
    }
} 