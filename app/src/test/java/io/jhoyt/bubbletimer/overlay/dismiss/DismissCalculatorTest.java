package io.jhoyt.bubbletimer.overlay.dismiss;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for DismissCalculator
 * Tests the pure logic extracted from DismissCircleView.java and Window.java
 */
public class DismissCalculatorTest {
    
    // Test constants
    private static final float DELTA = 0.001f; // For float comparisons
    
    @Test
    public void testCalculateDistance_BasicCases() {
        // Distance between same point should be 0
        assertEquals(0f, DismissCalculator.calculateDistance(100f, 200f, 100f, 200f), DELTA);
        
        // Horizontal distance
        assertEquals(50f, DismissCalculator.calculateDistance(100f, 200f, 150f, 200f), DELTA);
        
        // Vertical distance  
        assertEquals(30f, DismissCalculator.calculateDistance(100f, 200f, 100f, 230f), DELTA);
        
        // Diagonal distance (3-4-5 triangle)
        assertEquals(5f, DismissCalculator.calculateDistance(0f, 0f, 3f, 4f), DELTA);
    }
    
    @Test
    public void testCalculateDistance_NegativeCoordinates() {
        assertEquals(5f, DismissCalculator.calculateDistance(-3f, -4f, 0f, 0f), DELTA);
        assertEquals(10f, DismissCalculator.calculateDistance(-5f, 0f, 5f, 0f), DELTA);
    }
    
    @Test
    public void testIsWithinDismissThreshold() {
        float dismissX = 500f;
        float dismissY = 300f;
        
        // Within threshold (distance < 200)
        assertTrue(DismissCalculator.isWithinDismissThreshold(510f, 310f, dismissX, dismissY));
        assertTrue(DismissCalculator.isWithinDismissThreshold(450f, 250f, dismissX, dismissY));
        
        // Exactly at threshold boundary  
        float boundaryX = dismissX + 200f; // Distance = 200
        assertFalse(DismissCalculator.isWithinDismissThreshold(boundaryX, dismissY, dismissX, dismissY));
        
        // Outside threshold
        assertFalse(DismissCalculator.isWithinDismissThreshold(750f, 300f, dismissX, dismissY));
    }
    
    @Test
    public void testIsWithinDismissActionThreshold() {
        float dismissX = 500f;
        float dismissY = 300f;
        
        // Within action threshold (distance < 50)
        assertTrue(DismissCalculator.isWithinDismissActionThreshold(520f, 320f, dismissX, dismissY));
        assertTrue(DismissCalculator.isWithinDismissActionThreshold(480f, 280f, dismissX, dismissY));
        
        // Exactly at action threshold boundary
        float boundaryX = dismissX + 50f; // Distance = 50
        assertFalse(DismissCalculator.isWithinDismissActionThreshold(boundaryX, dismissY, dismissX, dismissY));
        
        // Outside action threshold but within pull threshold
        assertFalse(DismissCalculator.isWithinDismissActionThreshold(600f, 300f, dismissX, dismissY));
    }
    
    @Test
    public void testCalculateBubbleCenter() {
        // Basic case
        int[] center = DismissCalculator.calculateBubbleCenter(100, 200, 160, 120);
        assertEquals(180, center[0]); // 100 + 160/2
        assertEquals(260, center[1]); // 200 + 120/2
        
        // Zero corner position
        center = DismissCalculator.calculateBubbleCenter(0, 0, 200, 200);
        assertEquals(100, center[0]);
        assertEquals(100, center[1]);
        
        // Odd dimensions
        center = DismissCalculator.calculateBubbleCenter(50, 75, 101, 201);
        assertEquals(100, center[0]); // 50 + 101/2 = 50 + 50
        assertEquals(175, center[1]); // 75 + 201/2 = 75 + 100
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBubbleCenter_ZeroWidth() {
        DismissCalculator.calculateBubbleCenter(100, 200, 0, 120);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateBubbleCenter_NegativeHeight() {
        DismissCalculator.calculateBubbleCenter(100, 200, 160, -50);
    }
    
    @Test
    public void testCalculatePullToPosition() {
        // Test pulling bubble to center on dismiss circle
        int[] position = DismissCalculator.calculatePullToPosition(
            100f, 200f,        // Current bubble position
            500f, 300f,        // Dismiss circle center
            160, 120,          // Bubble dimensions
            1080, 1920         // Screen dimensions
        );
        
        // Should center bubble on dismiss circle
        assertEquals(420, position[0]); // 500 - 160/2 = 420
        assertEquals(240, position[1]); // 300 - 120/2 = 240
    }
    
    @Test
    public void testCalculatePullToPosition_BoundaryClipping() {
        // Test bubble gets clipped to screen boundaries
        int[] position = DismissCalculator.calculatePullToPosition(
            100f, 200f,        // Current bubble position
            50f, 50f,          // Dismiss circle near top-left corner
            160, 120,          // Bubble dimensions
            1080, 1920         // Screen dimensions
        );
        
        // Should be clipped to screen boundaries
        assertEquals(0, position[0]); // Would be -30, clipped to 0
        assertEquals(0, position[1]); // Would be -10, clipped to 0
    }
    
    @Test
    public void testCalculatePullToPosition_RightBottomBoundary() {
        // Test bubble gets clipped to right/bottom boundaries
        int[] position = DismissCalculator.calculatePullToPosition(
            100f, 200f,        // Current bubble position
            1050f, 1900f,      // Dismiss circle near bottom-right
            160, 120,          // Bubble dimensions
            1080, 1920         // Screen dimensions
        );
        
        // Should be clipped to screen boundaries
        assertEquals(920, position[0]); // 1080 - 160 = 920
        assertEquals(1800, position[1]); // 1920 - 120 = 1800
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculatePullToPosition_ZeroBubbleWidth() {
        DismissCalculator.calculatePullToPosition(100f, 200f, 500f, 300f, 0, 120, 1080, 1920);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculatePullToPosition_ZeroScreenWidth() {
        DismissCalculator.calculatePullToPosition(100f, 200f, 500f, 300f, 160, 120, 0, 1920);
    }
    
    // Test real-world scenarios based on actual Window.java logic
    @Test
    public void testRealWorldScenario_DismissCheck() {
        // Simulate the logic from Window.java shouldDismissTimer()
        int bubbleCornerX = 400;
        int bubbleCornerY = 600;
        int bubbleWidth = 160;
        int bubbleHeight = 160;
        
        // Calculate bubble center
        int[] center = DismissCalculator.calculateBubbleCenter(bubbleCornerX, bubbleCornerY, bubbleWidth, bubbleHeight);
        
        // Test against dismiss circle positions
        float dismissCircleX = 540f; // Screen center
        float dismissCircleY = 1800f; // Bottom of screen
        
        // Check if bubble should be dismissed
        boolean shouldDismiss = DismissCalculator.isWithinDismissActionThreshold(
            center[0], center[1], dismissCircleX, dismissCircleY);
        
        // Distance should be much larger than threshold, so false
        assertFalse(shouldDismiss);
    }
}
