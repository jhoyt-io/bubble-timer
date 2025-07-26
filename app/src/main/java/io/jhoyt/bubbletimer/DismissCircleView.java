package io.jhoyt.bubbletimer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import android.graphics.drawable.Drawable;

public class DismissCircleView extends View {
    public static final float DISMISS_CIRCLE_RADIUS = 80.0f;
    private static final float DISMISS_CIRCLE_PULL_THRESHOLD = 200.0f;
    private static final float DISMISS_CIRCLE_PULL_STRENGTH = 0.5f;

    public enum DismissType { STOP, DISMISS }

    public static class DismissCircle {
        public float centerX;
        public float centerY;
        public Drawable icon;
        public DismissType type;
        public DismissCircle(float centerX, float centerY, Drawable icon, DismissType type) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.icon = icon;
            this.type = type;
        }
    }

    private Paint circlePaint;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private List<DismissCircle> circles = new ArrayList<>();

    public DismissCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.circlePaint.setColor(context.getResources().getColor(android.R.color.darker_gray, context.getTheme()));
        this.circlePaint.setStyle(Paint.Style.FILL);
        this.circlePaint.setAlpha(180);
    }

    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        setupDefaultCircles(getContext());
        invalidate();
    }

    private void setupDefaultCircles(Context context) {
        circles.clear();
        // Top circle (DISMISS)
        Drawable xIcon = null;
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(60.0f);
        // We'll draw the X manually, so icon is null
        circles.add(new DismissCircle(screenWidth / 2.0f, 200.0f + DISMISS_CIRCLE_RADIUS, null, DismissType.DISMISS));
        // Bottom circle (STOP)
        Drawable stopIcon = context.getResources().getDrawable(R.drawable.stop, context.getTheme());
        circles.add(new DismissCircle(screenWidth / 2.0f, screenHeight - 200.0f - DISMISS_CIRCLE_RADIUS, stopIcon, DismissType.STOP));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(60.0f);
        for (DismissCircle circle : circles) {
            // Draw background
            canvas.drawCircle(circle.centerX, circle.centerY, DISMISS_CIRCLE_RADIUS + 4, bgPaint);
            // Draw main circle
            canvas.drawCircle(circle.centerX, circle.centerY, DISMISS_CIRCLE_RADIUS, circlePaint);
            if (circle.type == DismissType.DISMISS) {
                // Draw X manually
                canvas.drawText("×", circle.centerX, circle.centerY + textPaint.getTextSize()/3, textPaint);
            } else if (circle.type == DismissType.STOP && circle.icon != null) {
                int iconSize = (int)(DISMISS_CIRCLE_RADIUS * 1.2f);
                int left = (int)(circle.centerX - iconSize/2);
                int top = (int)(circle.centerY - iconSize/2);
                int right = (int)(circle.centerX + iconSize/2);
                int bottom = (int)(circle.centerY + iconSize/2);
                circle.icon.setBounds(left, top, right, bottom);
                circle.icon.draw(canvas);
            }
        }
    }

    public DismissCircle getNearestDismissCircle(float x, float y) {
        DismissCircle nearest = null;
        float minDist = Float.MAX_VALUE;
        for (DismissCircle circle : circles) {
            float dx = x - circle.centerX;
            float dy = y - circle.centerY;
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            if (dist < DISMISS_CIRCLE_PULL_THRESHOLD && dist < minDist) {
                minDist = dist;
                nearest = circle;
            }
        }
        return nearest;
    }

    public List<DismissCircle> getCircles() {
        return circles;
    }
} 