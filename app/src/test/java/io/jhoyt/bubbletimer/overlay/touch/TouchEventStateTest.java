package io.jhoyt.bubbletimer.overlay.touch;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for TouchEventState
 * Tests the touch state management extracted from Window.java touch handling
 */
public class TouchEventStateTest {
    
    private static final float DELTA = 0.001f; // For float comparisons
    
    @Test
    public void testInitialTouchDown() {
        TouchEventState state = new TouchEventState(100f, 200f);
        
        assertEquals(100f, state.getInitialX(), DELTA);
        assertEquals(200f, state.getInitialY(), DELTA);
        assertEquals(100f, state.getCurrentX(), DELTA);
        assertEquals(200f, state.getCurrentY(), DELTA);
        assertEquals(0f, state.getDeltaX(), DELTA);
        assertEquals(0f, state.getDeltaY(), DELTA);
        assertEquals(TouchEventState.TouchType.POTENTIAL_CLICK, state.getTouchType());
        assertFalse(state.isDragging());
        assertTrue(state.isClick()); // No movement = click
        assertFalse(state.isDrag());
    }
    
    @Test
    public void testWithMovement_NoDelta() {
        TouchEventState initial = new TouchEventState(100f, 200f);
        TouchEventState moved = initial.withMovement(100f, 200f);
        
        assertEquals(TouchEventState.TouchType.POTENTIAL_CLICK, moved.getTouchType());
        assertFalse(moved.isDragging());
        assertTrue(moved.isClick());
        assertEquals(0f, moved.getTotalDistance(), DELTA);
    }
    
    @Test
    public void testWithMovement_HasDelta() {
        TouchEventState initial = new TouchEventState(100f, 200f);
        TouchEventState moved = initial.withMovement(150f, 250f);
        
        assertEquals(150f, moved.getCurrentX(), DELTA);
        assertEquals(250f, moved.getCurrentY(), DELTA);
        assertEquals(50f, moved.getDeltaX(), DELTA);
        assertEquals(50f, moved.getDeltaY(), DELTA);
        assertEquals(TouchEventState.TouchType.DRAGGING, moved.getTouchType());
        assertTrue(moved.isDragging());
        assertFalse(moved.isClick());
        assertTrue(moved.isDrag());
    }
    
    @Test
    public void testWithRelease_Click() {
        TouchEventState initial = new TouchEventState(100f, 200f);
        TouchEventState released = initial.withRelease(100f, 200f);
        
        assertEquals(TouchEventState.TouchType.CLICK, released.getTouchType());
        assertFalse(released.isDragging());
        assertTrue(released.isClick());
        assertFalse(released.isDrag());
    }
    
    @Test
    public void testWithRelease_DragRelease() {
        TouchEventState initial = new TouchEventState(100f, 200f);
        TouchEventState released = initial.withRelease(150f, 250f);
        
        assertEquals(150f, released.getCurrentX(), DELTA);
        assertEquals(250f, released.getCurrentY(), DELTA);
        assertEquals(50f, released.getDeltaX(), DELTA);
        assertEquals(50f, released.getDeltaY(), DELTA);
        assertEquals(TouchEventState.TouchType.DRAG_RELEASE, released.getTouchType());
        assertTrue(released.isDragging());
        assertFalse(released.isClick());
        assertTrue(released.isDrag());
    }
    
    @Test
    public void testGetTotalDistance() {
        TouchEventState initial = new TouchEventState(0f, 0f);
        
        // 3-4-5 triangle
        TouchEventState moved = initial.withMovement(3f, 4f);
        assertEquals(5f, moved.getTotalDistance(), DELTA);
        
        // No movement
        TouchEventState stillSame = initial.withMovement(0f, 0f);
        assertEquals(0f, stillSame.getTotalDistance(), DELTA);
        
        // Diagonal movement
        TouchEventState diagonal = initial.withMovement(10f, 10f);
        assertEquals(Math.sqrt(200), diagonal.getTotalDistance(), DELTA);
    }
    
    @Test
    public void testComplexTouchSequence() {
        // Simulate a real touch sequence: down -> move -> move -> release
        TouchEventState down = new TouchEventState(100f, 200f);
        
        // First movement - should detect as drag
        TouchEventState firstMove = down.withMovement(105f, 205f);
        assertEquals(TouchEventState.TouchType.DRAGGING, firstMove.getTouchType());
        assertTrue(firstMove.isDrag());
        
        // Second movement - continuing drag
        TouchEventState secondMove = new TouchEventState(
            100f, 200f, 150f, 250f, 
            TouchEventState.TouchType.DRAGGING, true);
        assertTrue(secondMove.isDrag());
        assertEquals(50f, secondMove.getDeltaX(), DELTA);
        assertEquals(50f, secondMove.getDeltaY(), DELTA);
        
        // Release after drag
        TouchEventState release = down.withRelease(150f, 250f);
        assertEquals(TouchEventState.TouchType.DRAG_RELEASE, release.getTouchType());
        assertTrue(release.isDrag());
        assertFalse(release.isClick());
    }
    
    @Test
    public void testNegativeMovement() {
        TouchEventState initial = new TouchEventState(500f, 600f);
        TouchEventState moved = initial.withMovement(400f, 500f);
        
        assertEquals(-100f, moved.getDeltaX(), DELTA);
        assertEquals(-100f, moved.getDeltaY(), DELTA);
        assertEquals(TouchEventState.TouchType.DRAGGING, moved.getTouchType());
        assertTrue(moved.isDrag());
        
        // Total distance should be positive even with negative deltas
        assertEquals(Math.sqrt(20000), moved.getTotalDistance(), DELTA); // sqrt(100^2 + 100^2)
    }
    
    @Test
    public void testToString() {
        TouchEventState state = new TouchEventState(100f, 200f);
        TouchEventState moved = state.withMovement(150f, 250f);
        
        String str = moved.toString();
        assertTrue(str.contains("DRAGGING"));
        assertTrue(str.contains("100.0"));
        assertTrue(str.contains("200.0"));
        assertTrue(str.contains("150.0"));
        assertTrue(str.contains("250.0"));
        assertTrue(str.contains("50.0")); // Delta values
    }
    
    // Test edge cases that might occur in real Window.java scenarios
    @Test
    public void testRealWorldScenario_TinyMovement() {
        // Sometimes finger might move just 1 pixel due to imprecise touch
        TouchEventState initial = new TouchEventState(100f, 200f);
        TouchEventState tinyMove = initial.withMovement(101f, 200f);
        
        // Should be detected as drag, not click
        assertEquals(TouchEventState.TouchType.DRAGGING, tinyMove.getTouchType());
        assertTrue(tinyMove.isDrag());
        assertFalse(tinyMove.isClick());
        assertEquals(1f, tinyMove.getTotalDistance(), DELTA);
    }
    
    @Test
    public void testRealWorldScenario_ClickDetection() {
        // This tests the exact logic from Window.java line 419
        // if (event.getRawX() == (0 - dx) && event.getRawY() == (0 - dy))
        
        TouchEventState initial = new TouchEventState(100f, 200f);
        TouchEventState exactSame = initial.withRelease(100f, 200f);
        
        assertTrue(exactSame.isClick());
        assertEquals(TouchEventState.TouchType.CLICK, exactSame.getTouchType());
    }
}
