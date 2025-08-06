package io.jhoyt.bubbletimer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

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
        boolean selected = isSelected.apply(text);
        Log.d("CircularMenuButton", "Drawing button '" + text + "' - selected: " + selected);
        if (selected) {
            bgPaint.setColor(android.graphics.Color.parseColor("#FF00FF")); // Bright magenta for debugging
            Log.d("CircularMenuButton", "Setting button '" + text + "' to magenta (selected)");
        }
        canvas.drawCircle(centerX, centerY, radius - 10, bgPaint);
        if (icon != null) {
            canvas.drawBitmap(icon, null, iconRect, iconPaint);
        }
        if (text != null) {
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(android.graphics.Color.BLACK);
            textPaint.setTextAlign(Paint.Align.CENTER);
            
            // Calculate appropriate text size based on text length and button radius
            float baseTextSize = radius / 2.5f;
            float maxTextWidth = radius * 1.6f; // Leave some margin
            float textSize = baseTextSize;
            
            // Scale down text size if it's too long
            textPaint.setTextSize(textSize);
            float textWidth = textPaint.measureText(text);
            if (textWidth > maxTextWidth) {
                textSize = (maxTextWidth / textWidth) * textSize;
                textPaint.setTextSize(textSize);
            }
            
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