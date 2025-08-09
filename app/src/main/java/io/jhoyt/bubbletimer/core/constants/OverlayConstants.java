package io.jhoyt.bubbletimer.core.constants;

/**
 * Constants for overlay functionality extracted from Window.java
 * This centralizes hardcoded values to make them testable and maintainable.
 */
public final class OverlayConstants {
    
    // Bubble size constants
    public static final int SMALL_BUBBLE_RADIUS = 80;
    public static final int LITTLE_BUBBLE_RADIUS = 120;
    public static final int BIG_BUBBLE_RADIUS = 200;
    
    // Dismiss functionality
    public static final int DISMISS_THRESHOLD_PIXELS = 50;
    
    // Snap-to-side behavior
    public static final float SNAP_OVERLAP_RATIO = 0.2f; // bubble width / 5
    
    // Dismiss circle constants (from DismissCircleView)
    public static final float DISMISS_CIRCLE_RADIUS = 80.0f;
    public static final float DISMISS_CIRCLE_PULL_THRESHOLD = 200.0f;
    public static final float DISMISS_CIRCLE_PULL_STRENGTH = 0.5f;
    
    // Private constructor to prevent instantiation
    private OverlayConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
