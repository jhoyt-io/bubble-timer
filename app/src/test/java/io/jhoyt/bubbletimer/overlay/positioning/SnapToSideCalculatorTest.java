package io.jhoyt.bubbletimer.overlay.positioning;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SnapToSideCalculator
 * Tests the pure logic extracted from Window.java touch handling
 */
public class SnapToSideCalculatorTest {
    
    // Test data based on common Android screen sizes
    private static final int SCREEN_WIDTH_PHONE = 1080;
    private static final int SCREEN_WIDTH_TABLET = 1920;
    private static final int BUBBLE_WIDTH = 160; // Typical bubble size
    
    @Test
    public void testCalculateSnapX_LeftSide_Phone() {
        // Touch on left side of phone screen
        float touchX = 400f;
        
        int snapX = SnapToSideCalculator.calculateSnapX(touchX, SCREEN_WIDTH_PHONE, BUBBLE_WIDTH);
        
        // Should snap to left with overlap: 0 - (160 * 0.2) = -32
        assertEquals(-32, snapX);
    }
    
    @Test
    public void testCalculateSnapX_RightSide_Phone() {
        // Touch on right side of phone screen
        float touchX = 700f;
        
        int snapX = SnapToSideCalculator.calculateSnapX(touchX, SCREEN_WIDTH_PHONE, BUBBLE_WIDTH);
        
        // Should snap to right: 1080 - 160 + 32 = 952
        assertEquals(952, snapX);
    }
    
    @Test
    public void testCalculateSnapX_ExactCenter_GoesToRight() {
        // Touch exactly at center
        float touchX = SCREEN_WIDTH_PHONE / 2.0f;
        
        int snapX = SnapToSideCalculator.calculateSnapX(touchX, SCREEN_WIDTH_PHONE, BUBBLE_WIDTH);
        
        // At exact center (540), should go to right side
        assertEquals(952, snapX);
    }
    
    @Test
    public void testCalculateSnapX_JustLeftOfCenter() {
        // Touch just left of center
        float touchX = (SCREEN_WIDTH_PHONE / 2.0f) - 1;
        
        int snapX = SnapToSideCalculator.calculateSnapX(touchX, SCREEN_WIDTH_PHONE, BUBBLE_WIDTH);
        
        // Should snap to left
        assertEquals(-32, snapX);
    }
    
    @Test
    public void testCalculateSnapX_Tablet() {
        // Test with tablet dimensions
        float touchX = 800f; // Left side of tablet
        
        int snapX = SnapToSideCalculator.calculateSnapX(touchX, SCREEN_WIDTH_TABLET, BUBBLE_WIDTH);
        
        // Should snap to left since 800 < 960 (tablet center)
        assertEquals(-32, snapX);
    }
    
    @Test
    public void testIsLeftSide_VariousPositions() {
        assertTrue(SnapToSideCalculator.isLeftSide(100f, SCREEN_WIDTH_PHONE));
        assertTrue(SnapToSideCalculator.isLeftSide(539f, SCREEN_WIDTH_PHONE)); // Just left of center
        assertFalse(SnapToSideCalculator.isLeftSide(540f, SCREEN_WIDTH_PHONE)); // At center
        assertFalse(SnapToSideCalculator.isLeftSide(600f, SCREEN_WIDTH_PHONE));
        assertFalse(SnapToSideCalculator.isLeftSide(1000f, SCREEN_WIDTH_PHONE));
    }
    
    @Test
    public void testCalculateOverlap() {
        assertEquals(32, SnapToSideCalculator.calculateOverlap(160));
        assertEquals(24, SnapToSideCalculator.calculateOverlap(120));
        assertEquals(40, SnapToSideCalculator.calculateOverlap(200));
        assertEquals(16, SnapToSideCalculator.calculateOverlap(80));
    }
    
    // Edge cases and error conditions
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateSnapX_ZeroScreenWidth() {
        SnapToSideCalculator.calculateSnapX(100f, 0, BUBBLE_WIDTH);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateSnapX_NegativeScreenWidth() {
        SnapToSideCalculator.calculateSnapX(100f, -100, BUBBLE_WIDTH);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateSnapX_ZeroBubbleWidth() {
        SnapToSideCalculator.calculateSnapX(100f, SCREEN_WIDTH_PHONE, 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateSnapX_NegativeBubbleWidth() {
        SnapToSideCalculator.calculateSnapX(100f, SCREEN_WIDTH_PHONE, -50);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testIsLeftSide_ZeroScreenWidth() {
        SnapToSideCalculator.isLeftSide(100f, 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateOverlap_ZeroBubbleWidth() {
        SnapToSideCalculator.calculateOverlap(0);
    }
    
    // Test extreme values
    @Test
    public void testCalculateSnapX_VerySmallBubble() {
        int snapX = SnapToSideCalculator.calculateSnapX(100f, SCREEN_WIDTH_PHONE, 10);
        assertEquals(-2, snapX); // 10 * 0.2 = 2
    }
    
    @Test
    public void testCalculateSnapX_VeryLargeBubble() {
        int snapX = SnapToSideCalculator.calculateSnapX(100f, SCREEN_WIDTH_PHONE, 500);
        assertEquals(-100, snapX); // 500 * 0.2 = 100
    }
}
