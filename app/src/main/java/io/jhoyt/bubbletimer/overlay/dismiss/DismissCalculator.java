package io.jhoyt.bubbletimer.overlay.dismiss;

import io.jhoyt.bubbletimer.core.constants.OverlayConstants;

/**
 * Pure logic class for dismiss circle calculations, extracted from DismissCircleView.java
 * This handles the math for determining if a bubble should be dismissed based on position.
 */
public class DismissCalculator {
    
    /**
     * Calculate the distance between two points.
     * 
     * Extracted from DismissCircleView.getNearestDismissCircle() method.
     * 
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @return The distance between the points
     */
    public static float calculateDistance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Determine if a position is within dismiss threshold of a dismiss circle.
     * 
     * @param bubbleX X coordinate of bubble center
     * @param bubbleY Y coordinate of bubble center  
     * @param dismissCircleX X coordinate of dismiss circle center
     * @param dismissCircleY Y coordinate of dismiss circle center
     * @return true if within dismiss threshold, false otherwise
     */
    public static boolean isWithinDismissThreshold(float bubbleX, float bubbleY, 
                                                  float dismissCircleX, float dismissCircleY) {
        float distance = calculateDistance(bubbleX, bubbleY, dismissCircleX, dismissCircleY);
        return distance < OverlayConstants.DISMISS_CIRCLE_PULL_THRESHOLD;
    }
    
    /**
     * Determine if a position is within the smaller dismiss action threshold.
     * 
     * @param bubbleX X coordinate of bubble center
     * @param bubbleY Y coordinate of bubble center
     * @param dismissCircleX X coordinate of dismiss circle center
     * @param dismissCircleY Y coordinate of dismiss circle center
     * @return true if close enough to trigger dismiss, false otherwise
     */
    public static boolean isWithinDismissActionThreshold(float bubbleX, float bubbleY,
                                                        float dismissCircleX, float dismissCircleY) {
        float distance = calculateDistance(bubbleX, bubbleY, dismissCircleX, dismissCircleY);
        return distance < OverlayConstants.DISMISS_THRESHOLD_PIXELS;
    }
    
    /**
     * Calculate the center position of a bubble based on its corner position.
     * 
     * Extracted from Window.java shouldDismissTimer() method logic.
     * 
     * @param cornerX X coordinate of bubble's top-left corner
     * @param cornerY Y coordinate of bubble's top-left corner
     * @param bubbleWidth Width of the bubble
     * @param bubbleHeight Height of the bubble
     * @return int array with [centerX, centerY]
     */
    public static int[] calculateBubbleCenter(int cornerX, int cornerY, int bubbleWidth, int bubbleHeight) {
        if (bubbleWidth <= 0 || bubbleHeight <= 0) {
            throw new IllegalArgumentException("Bubble dimensions must be positive");
        }
        
        int centerX = cornerX + (bubbleWidth / 2);
        int centerY = cornerY + (bubbleHeight / 2);
        return new int[]{centerX, centerY};
    }
    
    /**
     * Calculate the position where bubble should be pulled towards dismiss circle.
     * 
     * @param bubbleX Current bubble X position
     * @param bubbleY Current bubble Y position
     * @param dismissCircleX Dismiss circle X center
     * @param dismissCircleY Dismiss circle Y center
     * @param bubbleWidth Bubble width for centering calculation
     * @param bubbleHeight Bubble height for centering calculation
     * @param screenWidth Screen width for boundary checking
     * @param screenHeight Screen height for boundary checking
     * @return int array with [newX, newY] position
     */
    public static int[] calculatePullToPosition(float bubbleX, float bubbleY,
                                               float dismissCircleX, float dismissCircleY,
                                               int bubbleWidth, int bubbleHeight,
                                               int screenWidth, int screenHeight) {
        if (bubbleWidth <= 0 || bubbleHeight <= 0) {
            throw new IllegalArgumentException("Bubble dimensions must be positive");
        }
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("Screen dimensions must be positive");
        }
        
        // Calculate new position centering bubble on dismiss circle
        int newX = (int) (dismissCircleX - (bubbleWidth / 2));
        int newY = (int) (dismissCircleY - (bubbleHeight / 2));
        
        // Ensure bubble stays within screen bounds
        newX = Math.max(0, Math.min(newX, screenWidth - bubbleWidth));
        newY = Math.max(0, Math.min(newY, screenHeight - bubbleHeight));
        
        return new int[]{newX, newY};
    }
}
