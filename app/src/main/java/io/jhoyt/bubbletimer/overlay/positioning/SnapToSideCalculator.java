package io.jhoyt.bubbletimer.overlay.positioning;

import io.jhoyt.bubbletimer.core.constants.OverlayConstants;

/**
 * Pure logic class for snap-to-side calculations, extracted from Window.java
 * This handles the math for determining where the bubble should snap when released.
 */
public class SnapToSideCalculator {
    
    /**
     * Calculate the X position for snapping to left or right side of screen.
     * 
     * Extracted from Window.java touch handler logic:
     * Lines 428-436: snap to sides with overlap calculation
     * 
     * @param touchX The X coordinate where the touch was released
     * @param screenWidth The width of the screen
     * @param bubbleWidth The width of the bubble
     * @return The X position where the bubble should snap
     */
    public static int calculateSnapX(float touchX, int screenWidth, int bubbleWidth) {
        if (screenWidth <= 0) {
            throw new IllegalArgumentException("Screen width must be positive");
        }
        if (bubbleWidth <= 0) {
            throw new IllegalArgumentException("Bubble width must be positive");
        }
        
        int overlap = (int) (bubbleWidth * OverlayConstants.SNAP_OVERLAP_RATIO);
        
        if (touchX < (screenWidth / 2.0)) {
            // Snap to left side with overlap
            return 0 - overlap;
        } else {
            // Snap to right side with overlap
            return screenWidth - bubbleWidth + overlap;
        }
    }
    
    /**
     * Determine which side of the screen a touch position is on.
     * 
     * @param touchX The X coordinate of the touch
     * @param screenWidth The width of the screen
     * @return true if left side, false if right side
     */
    public static boolean isLeftSide(float touchX, int screenWidth) {
        if (screenWidth <= 0) {
            throw new IllegalArgumentException("Screen width must be positive");
        }
        
        return touchX < (screenWidth / 2.0);
    }
    
    /**
     * Calculate the overlap amount based on bubble width.
     * 
     * @param bubbleWidth The width of the bubble
     * @return The overlap amount in pixels
     */
    public static int calculateOverlap(int bubbleWidth) {
        if (bubbleWidth <= 0) {
            throw new IllegalArgumentException("Bubble width must be positive");
        }
        
        return (int) (bubbleWidth * OverlayConstants.SNAP_OVERLAP_RATIO);
    }
}
