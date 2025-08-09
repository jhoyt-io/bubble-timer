package io.jhoyt.bubbletimer.overlay.positioning;

import android.content.Context;
import android.view.View;
import android.view.WindowInsets;

/**
 * Pure logic class for calculating screen dimensions, extracted from Window.java
 * This handles the complex logic of determining usable screen space excluding system bars.
 */
public class ScreenDimensionsCalculator {
    
    /**
     * Calculate usable screen dimensions excluding navigation bar and system insets.
     * 
     * Extracted from Window.java getUsableScreenDimensions() method.
     * 
     * @param anchorView View to get context and window insets from
     * @return int array with [width, height] of usable screen space
     */
    public static int[] getUsableScreenDimensions(View anchorView) {
        if (anchorView == null) {
            throw new IllegalArgumentException("anchorView cannot be null");
        }
        
        Context context = anchorView.getContext();
        if (context == null) {
            throw new IllegalStateException("anchorView must have a valid context");
        }
        
        int width = 0;
        int height = 0;

        WindowInsets insets = anchorView.getRootWindowInsets();
        if (insets != null) {
            width = context.getResources().getDisplayMetrics().widthPixels;
            height = context.getResources().getDisplayMetrics().heightPixels 
                    - insets.getInsets(WindowInsets.Type.systemBars()).bottom;
        } else {
            // Fallback when insets are not available
            width = context.getResources().getDisplayMetrics().widthPixels;
            height = context.getResources().getDisplayMetrics().heightPixels;
        }

        return new int[]{width, height};
    }
    
    /**
     * Calculate screen dimensions from display metrics (for testing with mock context)
     * 
     * @param widthPixels Screen width in pixels
     * @param heightPixels Screen height in pixels
     * @param systemBarsBottom Height of system bars at bottom
     * @return int array with [width, usableHeight]
     */
    public static int[] calculateUsableDimensions(int widthPixels, int heightPixels, int systemBarsBottom) {
        if (widthPixels <= 0 || heightPixels <= 0) {
            throw new IllegalArgumentException("Screen dimensions must be positive");
        }
        if (systemBarsBottom < 0) {
            throw new IllegalArgumentException("System bars height cannot be negative");
        }
        
        int usableHeight = heightPixels - systemBarsBottom;
        return new int[]{widthPixels, usableHeight};
    }
}
