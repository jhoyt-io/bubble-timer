package io.jhoyt.bubbletimer.overlay.positioning;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowInsets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeast;

/**
 * Tests for ScreenDimensionsCalculator using both pure logic and Android framework mocking
 */
@RunWith(RobolectricTestRunner.class)
public class ScreenDimensionsCalculatorTest {
    
    @Mock
    private View mockView;
    @Mock
    private Context mockContext;
    @Mock
    private Resources mockResources;
    @Mock
    private DisplayMetrics mockDisplayMetrics;
    @Mock
    private WindowInsets mockWindowInsets;
    // Using a real Insets object since it's a final class that's hard to mock
    
    @Test
    public void testCalculateUsableDimensions_PureLogic() {
        // Test the pure calculation logic without Android dependencies
        int[] dimensions = ScreenDimensionsCalculator.calculateUsableDimensions(1080, 1920, 120);
        
        assertEquals(1080, dimensions[0]); // Width unchanged
        assertEquals(1800, dimensions[1]); // Height minus system bars: 1920 - 120 = 1800
    }
    
    @Test
    public void testCalculateUsableDimensions_NoSystemBars() {
        int[] dimensions = ScreenDimensionsCalculator.calculateUsableDimensions(1080, 1920, 0);
        
        assertEquals(1080, dimensions[0]);
        assertEquals(1920, dimensions[1]); // No reduction in height
    }
    
    @Test
    public void testCalculateUsableDimensions_TabletSize() {
        int[] dimensions = ScreenDimensionsCalculator.calculateUsableDimensions(1920, 1200, 100);
        
        assertEquals(1920, dimensions[0]);
        assertEquals(1100, dimensions[1]); // 1200 - 100 = 1100
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateUsableDimensions_ZeroWidth() {
        ScreenDimensionsCalculator.calculateUsableDimensions(0, 1920, 120);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateUsableDimensions_NegativeHeight() {
        ScreenDimensionsCalculator.calculateUsableDimensions(1080, -100, 120);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateUsableDimensions_NegativeSystemBars() {
        ScreenDimensionsCalculator.calculateUsableDimensions(1080, 1920, -50);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetUsableScreenDimensions_NullView() {
        ScreenDimensionsCalculator.getUsableScreenDimensions(null);
    }
    
    @Test
    public void testGetUsableScreenDimensions_WithWindowInsets() {
        // Setup mocks
        MockitoAnnotations.openMocks(this);
        
        // Mock the view and context chain
        when(mockView.getContext()).thenReturn(mockContext);
        when(mockView.getRootWindowInsets()).thenReturn(mockWindowInsets);
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        
        // Setup display metrics
        mockDisplayMetrics.widthPixels = 1080;
        mockDisplayMetrics.heightPixels = 1920;
        
        // Setup window insets - create a real insets object
        android.graphics.Insets realInsets = android.graphics.Insets.of(0, 0, 0, 120);
        when(mockWindowInsets.getInsets(WindowInsets.Type.systemBars())).thenReturn(realInsets);
        
        // Test the method
        int[] dimensions = ScreenDimensionsCalculator.getUsableScreenDimensions(mockView);
        
        assertEquals(1080, dimensions[0]);
        assertEquals(1800, dimensions[1]); // 1920 - 120 = 1800
        
        // Verify key interactions (allowing for internal implementation details)
        verify(mockView, atLeast(1)).getContext();
        verify(mockView).getRootWindowInsets();
        verify(mockContext, atLeast(1)).getResources();
        verify(mockResources, atLeast(1)).getDisplayMetrics();
        verify(mockWindowInsets).getInsets(WindowInsets.Type.systemBars());
    }
    
    @Test
    public void testGetUsableScreenDimensions_NoWindowInsets() {
        // Setup mocks for fallback case
        MockitoAnnotations.openMocks(this);
        
        when(mockView.getContext()).thenReturn(mockContext);
        when(mockView.getRootWindowInsets()).thenReturn(null); // No insets available
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        
        mockDisplayMetrics.widthPixels = 1080;
        mockDisplayMetrics.heightPixels = 1920;
        
        // Test the fallback path
        int[] dimensions = ScreenDimensionsCalculator.getUsableScreenDimensions(mockView);
        
        assertEquals(1080, dimensions[0]);
        assertEquals(1920, dimensions[1]); // Full height when no insets
        
        // Verify fallback path was taken
        verify(mockView, atLeast(1)).getContext(); // Called in fallback path
        verify(mockView).getRootWindowInsets();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testGetUsableScreenDimensions_NullContext() {
        MockitoAnnotations.openMocks(this);
        when(mockView.getContext()).thenReturn(null);
        
        ScreenDimensionsCalculator.getUsableScreenDimensions(mockView);
    }
    
    // Test realistic scenarios based on actual device configurations
    @Test
    public void testRealWorldScenarios_CommonPhoneSizes() {
        // iPhone-like dimensions
        int[] iPhone = ScreenDimensionsCalculator.calculateUsableDimensions(1170, 2532, 134);
        assertEquals(1170, iPhone[0]);
        assertEquals(2398, iPhone[1]);
        
        // Samsung Galaxy-like dimensions
        int[] galaxy = ScreenDimensionsCalculator.calculateUsableDimensions(1440, 3200, 160);
        assertEquals(1440, galaxy[0]);
        assertEquals(3040, galaxy[1]);
        
        // Pixel-like dimensions
        int[] pixel = ScreenDimensionsCalculator.calculateUsableDimensions(1080, 2400, 120);
        assertEquals(1080, pixel[0]);
        assertEquals(2280, pixel[1]);
    }
    
    @Test
    public void testRealWorldScenarios_TabletSizes() {
        // iPad-like dimensions
        int[] iPad = ScreenDimensionsCalculator.calculateUsableDimensions(1668, 2388, 80);
        assertEquals(1668, iPad[0]);
        assertEquals(2308, iPad[1]);
        
        // Android tablet
        int[] androidTablet = ScreenDimensionsCalculator.calculateUsableDimensions(1920, 1200, 100);
        assertEquals(1920, androidTablet[0]);
        assertEquals(1100, androidTablet[1]);
    }
}
