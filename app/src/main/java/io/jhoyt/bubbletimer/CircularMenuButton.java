package io.jhoyt.bubbletimer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.function.Function;

public class CircularMenuButton {
    private final float centerX;
    private final float centerY;
    private final float radius;
    private final Bitmap icon;
    private final RectF iconRect;
    private final Paint circlePaint;
    private final Paint iconPaint;
    private final int buttonId;
    private final String text;
    private final Function<String, Boolean> isSelected;

    public CircularMenuButton(float centerX, float centerY, float radius, Bitmap icon, int buttonId) {
        this(centerX, centerY, radius, icon, buttonId, null, friend -> false);
    }

    public CircularMenuButton(float centerX, float centerY, float radius, Bitmap icon, int buttonId, String text, Function<String, Boolean> isSelected) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.icon = icon;
        this.buttonId = buttonId;
        this.text = text;
        this.isSelected = isSelected;
        this.iconRect = new RectF(
            centerX - radius / 1.6f,
            centerY - radius / 1.6f,
            centerX + radius / 1.6f,
            centerY + radius / 1.6f
        );
        this.circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.circlePaint.setColor(android.graphics.Color.WHITE);
        this.circlePaint.setStyle(Paint.Style.FILL);
        this.iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void draw(Canvas canvas) {
        Paint bgPaint = new Paint(circlePaint);
        if (isSelected.apply(null)) {
            bgPaint.setColor(android.graphics.Color.parseColor("#FF00FF")); // Bright magenta for debugging
        }
        canvas.drawCircle(centerX, centerY, radius - 10, bgPaint);
        if (icon != null) {
            canvas.drawBitmap(icon, null, iconRect, iconPaint);
        }
        if (text != null) {
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(android.graphics.Color.BLACK);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(radius / 2.5f);
            canvas.drawText(text, centerX, centerY + (radius / 4.0f), textPaint);
        }
    }

    public boolean isPointInside(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        return (dx * dx + dy * dy) < (radius * radius);
    }

    public int getButtonId() {
        return buttonId;
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getRadius() {
        return radius;
    }
} 