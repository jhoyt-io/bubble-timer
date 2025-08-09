package io.jhoyt.bubbletimer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CircularMenuLayout {
    private final float centerX;
    private final float centerY;
    private final float mainCircleRadius;
    private final float buttonRadius;
    private final float buttonDistance;
    private final List<CircularMenuButton> buttons;
    private final Paint circlePaint;
    private final Paint arcPaint;
    private final Paint textPaint;
    private final Paint redTextPaint;
    private final Timer timer;
    private final String currentUserId;

    public CircularMenuLayout(float centerX, float centerY, float mainCircleRadius, float buttonRadius, Timer timer, String currentUserId) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.mainCircleRadius = mainCircleRadius;
        this.buttonRadius = buttonRadius;
        this.buttonDistance = mainCircleRadius + buttonRadius;
        this.buttons = new ArrayList<>();
        this.timer = timer;
        this.currentUserId = currentUserId;

        this.circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.circlePaint.setColor(android.graphics.Color.WHITE);
        this.circlePaint.setStyle(Paint.Style.FILL);

        this.arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.arcPaint.setColor(android.graphics.Color.BLUE);
        this.arcPaint.setStyle(Paint.Style.STROKE);
        this.arcPaint.setStrokeWidth(20.0f);

        this.textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.textPaint.setColor(android.graphics.Color.BLACK);
        this.textPaint.setTextAlign(Paint.Align.CENTER);

        this.redTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.redTextPaint.setColor(android.graphics.Color.RED);
        this.redTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void addButton(Bitmap icon, int buttonId, float angle) {
        float buttonX = centerX + (float) (buttonDistance * Math.cos(Math.toRadians(angle)));
        float buttonY = centerY + (float) (buttonDistance * Math.sin(Math.toRadians(angle)));
        buttons.add(new CircularMenuButton(buttonX, buttonY, buttonRadius, icon, buttonId));
    }

    public void addButtonWithText(String text, int buttonId, float angle, float customRadius, Function<String, Boolean> isSelected) {
        float buttonX = centerX + (float) (customRadius * Math.cos(Math.toRadians(angle)));
        float buttonY = centerY + (float) (customRadius * Math.sin(Math.toRadians(angle)));
        buttons.add(new CircularMenuButton(buttonX, buttonY, buttonRadius, null, buttonId, text, isSelected));
    }

    public void drawMainCircle(Canvas canvas, float sweepAngle, String text, String name, float textSize) {
        // Draw main circle
        canvas.drawCircle(centerX, centerY, mainCircleRadius, circlePaint);

        // Draw arc
        RectF arcOval = new RectF(
            centerX - mainCircleRadius + 20,
            centerY - mainCircleRadius + 20,
            centerX + mainCircleRadius - 20,
            centerY + mainCircleRadius - 20
        );
        canvas.drawArc(arcOval, -90.0f, sweepAngle, false, arcPaint);

        // Draw text
        textPaint.setTextSize(textSize);
        canvas.drawText(text, centerX, centerY + (textSize / 4.0f), textPaint);

        if (name != null) {
            textPaint.setTextSize(textSize / 2.0f - Math.min(30.0f, Math.max(0, (name.length() - 10)) * 3.0f));
            canvas.drawText(name, centerX, centerY - (mainCircleRadius / 2.5f), textPaint);
        }

        // Draw originator info if timer is shared
        if (timer != null && currentUserId != null && timer.getSharedBy() != null && 
            !timer.getSharedBy().trim().isEmpty() && !timer.getSharedBy().equals(currentUserId)) {
            
            // Adjust text size and positioning based on circle size
            float originatorTextSize = textSize / 3.0f;
            float originatorY;
            
            if (mainCircleRadius < 150) { // Small bubble
                // For small bubbles, position text higher and make it slightly larger for better readability
                originatorTextSize = textSize / 3.5f; // Slightly larger text
                originatorY = centerY + (mainCircleRadius / 2.2f); // Higher position
            } else { // Large bubble
                originatorY = centerY + (mainCircleRadius / 2.5f);
            }
            
            textPaint.setTextSize(originatorTextSize);
            
            // Truncate long usernames for small bubbles to prevent text overflow
            String sharedByUser = timer.getSharedBy();
            if (mainCircleRadius < 150 && sharedByUser.length() > 10) {
                sharedByUser = sharedByUser.substring(0, 8) + "...";
            }
            String originatorText = "👤" + sharedByUser;
            
            // Debug logging
            android.util.Log.d("CircularMenuLayout", "Drawing shared by info:");
            android.util.Log.d("CircularMenuLayout", "  mainCircleRadius: " + mainCircleRadius);
            android.util.Log.d("CircularMenuLayout", "  centerX: " + centerX + ", centerY: " + centerY);
            android.util.Log.d("CircularMenuLayout", "  originatorY: " + originatorY);
            android.util.Log.d("CircularMenuLayout", "  originalUser: " + timer.getSharedBy());
            android.util.Log.d("CircularMenuLayout", "  truncatedUser: " + sharedByUser);
            android.util.Log.d("CircularMenuLayout", "  originatorText: " + originatorText);
            android.util.Log.d("CircularMenuLayout", "  textSize: " + originatorTextSize);
            
            canvas.drawText(originatorText, centerX, originatorY, textPaint);
        } else {
            // Debug logging for when not showing
            android.util.Log.d("CircularMenuLayout", "Not drawing shared by info:");
            android.util.Log.d("CircularMenuLayout", "  timer: " + (timer != null ? "not null" : "null"));
            android.util.Log.d("CircularMenuLayout", "  currentUserId: " + currentUserId);
            android.util.Log.d("CircularMenuLayout", "  timer.getSharedBy(): " + (timer != null ? timer.getSharedBy() : "N/A"));
            if (timer != null && timer.getSharedBy() != null && currentUserId != null) {
                android.util.Log.d("CircularMenuLayout", "  sharedBy.equals(currentUserId): " + timer.getSharedBy().equals(currentUserId));
            }
        }
    }

    public void drawButtons(Canvas canvas) {
        for (CircularMenuButton button : buttons) {
            button.draw(canvas);
        }
    }

    public int getButtonAtPoint(float x, float y) {
        for (CircularMenuButton button : buttons) {
            if (button.isPointInside(x, y)) {
                return button.getButtonId();
            }
        }
        return -1;
    }

    public boolean isPointInMainCircle(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        return (dx * dx + dy * dy) < (mainCircleRadius * mainCircleRadius);
    }
} 