package io.jhoyt.bubbletimer.overlay;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Unit tests for OverlayWindowFactory
 * Tests the simplified overlay creation functionality
 */
@RunWith(RobolectricTestRunner.class)
public class OverlayWindowFactoryTest {
    
    private Context context;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }
    
    @Test
    public void testCreateOverlayWindow_ReturnsValidInstance() {
        // Test creating an overlay window
        OverlayWindowFactory.IOverlayWindow window = OverlayWindowFactory.createOverlayWindow(context, false, "testUser");
        
        assertNotNull("Factory should return a valid overlay window", window);
        assertFalse("New overlay window should not be open initially", window.isOpen());
    }
    
    @Test
    public void testCreateExpandedOverlayWindow_ReturnsValidInstance() {
        // Test creating an expanded overlay window
        OverlayWindowFactory.IOverlayWindow window = OverlayWindowFactory.createOverlayWindow(context, true, "testUser");
        
        assertNotNull("Factory should return a valid expanded overlay window", window);
        assertFalse("New expanded overlay window should not be open initially", window.isOpen());
    }
    
    @Test
    public void testOverlayWindowHasTimerView() {
        // Test that overlay window has a timer view
        OverlayWindowFactory.IOverlayWindow window = OverlayWindowFactory.createOverlayWindow(context, false, "testUser");
        
        assertNotNull("Overlay window should have a timer view", window.getTimerView());
    }
}