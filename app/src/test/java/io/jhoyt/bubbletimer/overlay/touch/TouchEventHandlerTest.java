package io.jhoyt.bubbletimer.overlay.touch;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.TimerView;
import io.jhoyt.bubbletimer.overlay.dismiss.DismissCircleManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TouchEventHandler
 * Tests the extracted touch handling logic from Window.java
 */
@RunWith(RobolectricTestRunner.class)
public class TouchEventHandlerTest {
    
    @Mock private WindowManager mockWindowManager;
    @Mock private WindowManager.LayoutParams mockLayoutParams;
    @Mock private View mockOverlayView;
    @Mock private TimerView mockTimerView;
    @Mock private DismissCircleManager mockDismissCircleManager;
    @Mock private TouchEventHandler.TouchEventListener mockEventListener;
    @Mock private Timer mockTimer;
    
    private TouchEventHandler touchEventHandler;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup basic mock behaviors
        when(mockTimerView.getTimer()).thenReturn(mockTimer);
        when(mockTimerView.getWidth()).thenReturn(160);
        when(mockOverlayView.getWidth()).thenReturn(160);
        when(mockOverlayView.getHeight()).thenReturn(160);
        when(mockOverlayView.getContext()).thenReturn(org.robolectric.RuntimeEnvironment.getApplication());
        
        touchEventHandler = new TouchEventHandler(
            mockWindowManager,
            mockLayoutParams,
            mockOverlayView,
            mockTimerView,
            mockDismissCircleManager,
            mockEventListener
        );
    }
    
    @Test
    public void testTouchDown_InsideTouch_ReturnsTrue() {
        // Setup
        MotionEvent downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(-1);
        when(mockTimerView.isPointInMainCircle(100f, 100f)).thenReturn(true);
        when(mockTimerView.isSmallMode()).thenReturn(true);
        
        // Execute
        boolean result = touchEventHandler.onTouch(mockOverlayView, downEvent);
        
        // Verify
        assertTrue(result);
        verify(mockTimerView).setDragging(true);
        verify(mockEventListener).onDragStateChanged(true);
        verify(mockDismissCircleManager).showDismissCircles();
        
        downEvent.recycle();
    }
    
    @Test
    public void testTouchDown_OutsideTouch_ReturnsFalse() {
        // Setup
        MotionEvent downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(-1);
        when(mockTimerView.isPointInMainCircle(100f, 100f)).thenReturn(false);
        
        // Execute
        boolean result = touchEventHandler.onTouch(mockOverlayView, downEvent);
        
        // Verify
        assertFalse(result);
        verify(mockTimerView, never()).setDragging(true);
        verify(mockEventListener, never()).onDragStateChanged(true);
        
        downEvent.recycle();
    }
    
    @Test
    public void testTouchMove_UpdatesPosition() {
        // Setup - simulate touch down first
        MotionEvent downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(-1);
        when(mockTimerView.isPointInMainCircle(100f, 100f)).thenReturn(true);
        touchEventHandler.onTouch(mockOverlayView, downEvent);
        
        // Move event
        MotionEvent moveEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 150f, 150f, 0);
        when(mockDismissCircleManager.getNearestDismissCircle(150f, 150f)).thenReturn(null);
        
        // Execute
        boolean result = touchEventHandler.onTouch(mockOverlayView, moveEvent);
        
        // Verify
        assertTrue(result);
        verify(mockWindowManager).updateViewLayout(mockOverlayView, mockLayoutParams);
        
        downEvent.recycle();
        moveEvent.recycle();
    }
    
    @Test
    public void testTouchUp_Click_CallsBubbleClick() {
        // Setup - simulate touch down
        MotionEvent downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(-1);
        when(mockTimerView.isPointInMainCircle(100f, 100f)).thenReturn(true);
        touchEventHandler.onTouch(mockOverlayView, downEvent);
        
        // Touch up at same position (click)
        MotionEvent upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 100f, 100f, 0);
        when(mockTimerView.isInShareMenu()).thenReturn(false);
        
        // Execute
        boolean result = touchEventHandler.onTouch(mockOverlayView, upEvent);
        
        // Verify
        assertTrue(result);
        verify(mockEventListener).onBubbleClick(mockTimer);
        verify(mockTimerView).setDragging(false);
        verify(mockDismissCircleManager).hideDismissCircles();
        
        downEvent.recycle();
        upEvent.recycle();
    }
    
    @Test
    public void testTouchUp_ButtonPress_HandlesCorrectly() {
        // Setup - simulate touch down
        MotionEvent downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(-1);
        when(mockTimerView.isPointInMainCircle(100f, 100f)).thenReturn(true);
        touchEventHandler.onTouch(mockOverlayView, downEvent);
        
        // Touch up on button 1 (play/pause)
        MotionEvent upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(1);
        when(mockTimerView.isInShareMenu()).thenReturn(false);
        when(mockTimerView.isPaused()).thenReturn(true);
        
        // Execute
        boolean result = touchEventHandler.onTouch(mockOverlayView, upEvent);
        
        // Verify
        assertTrue(result);
        verify(mockTimerView).unpause();
        verify(mockEventListener).onTimerUpdated(mockTimer);
        
        downEvent.recycle();
        upEvent.recycle();
    }
    
    @Test
    public void testTouchUp_SmallModeDrag_SnapsToSide() {
        // Setup - simulate touch down and move
        MotionEvent downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(-1);
        when(mockTimerView.isPointInMainCircle(100f, 100f)).thenReturn(true);
        touchEventHandler.onTouch(mockOverlayView, downEvent);
        
        MotionEvent moveEvent = MotionEvent.obtain(0, 50, MotionEvent.ACTION_MOVE, 150f, 150f, 0);
        touchEventHandler.onTouch(mockOverlayView, moveEvent);
        
        // Touch up after drag in small mode
        MotionEvent upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 200f, 200f, 0);
        when(mockTimerView.isInShareMenu()).thenReturn(false);
        when(mockTimerView.getButtonAtPoint(200f, 200f)).thenReturn(-1);
        when(mockTimerView.isPointInMainCircle(200f, 200f)).thenReturn(true);
        when(mockTimerView.isSmallMode()).thenReturn(true);
        
        // Execute
        boolean result = touchEventHandler.onTouch(mockOverlayView, upEvent);
        
        // Verify
        assertTrue(result);
        verify(mockWindowManager, atLeast(2)).updateViewLayout(mockOverlayView, mockLayoutParams);
        
        downEvent.recycle();
        moveEvent.recycle();
        upEvent.recycle();
    }
    
    @Test
    public void testTouchCancel_CleansUpState() {
        // Setup - simulate touch down
        MotionEvent downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(-1);
        when(mockTimerView.isPointInMainCircle(100f, 100f)).thenReturn(true);
        touchEventHandler.onTouch(mockOverlayView, downEvent);
        
        // Cancel event
        MotionEvent cancelEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_CANCEL, 100f, 100f, 0);
        
        // Execute
        boolean result = touchEventHandler.onTouch(mockOverlayView, cancelEvent);
        
        // Verify
        assertTrue(result);
        verify(mockTimerView).setDragging(false);
        verify(mockEventListener).onDragStateChanged(false);
        verify(mockDismissCircleManager).hideDismissCircles();
        
        downEvent.recycle();
        cancelEvent.recycle();
    }
    
    @Test
    public void testDebugMode_ToggleWorks() {
        // Initial state
        assertFalse(touchEventHandler.isDebugModeEnabled());
        
        // Enable debug mode
        touchEventHandler.setDebugMode(true);
        assertTrue(touchEventHandler.isDebugModeEnabled());
        
        // Disable debug mode
        touchEventHandler.setDebugMode(false);
        assertFalse(touchEventHandler.isDebugModeEnabled());
    }
    
    @Test
    public void testShareMenuInteraction() {
        // Setup - simulate touch down
        MotionEvent downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(-1);
        when(mockTimerView.isPointInMainCircle(100f, 100f)).thenReturn(true);
        touchEventHandler.onTouch(mockOverlayView, downEvent);
        
        // Touch up on back button in share menu
        MotionEvent upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 100f, 100f, 0);
        when(mockTimerView.getButtonAtPoint(100f, 100f)).thenReturn(100); // Back button
        when(mockTimerView.isInShareMenu()).thenReturn(true);
        
        // Execute
        boolean result = touchEventHandler.onTouch(mockOverlayView, upEvent);
        
        // Verify
        assertTrue(result);
        verify(mockTimerView).hideShareMenu();
        
        downEvent.recycle();
        upEvent.recycle();
    }
}
