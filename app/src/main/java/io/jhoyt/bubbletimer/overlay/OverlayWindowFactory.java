package io.jhoyt.bubbletimer.overlay;

import android.content.Context;
import android.util.Log;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.TimerView;

/**
 * Factory for creating overlay windows.
 * 
 * Creates new modular OverlayWindow implementations.
 */
public class OverlayWindowFactory {
    
    private static final String TAG = "OverlayWindowFactory";
    
    /**
     * Interface that overlay implementations must support.
     */
    public interface IOverlayWindow {
        void open(Timer timer, BubbleEventListener listener);
        void close();
        boolean isOpen();
        TimerView getTimerView();
        void setDebugMode(boolean enabled);
        void cleanup();
        void onTimerStopped(Timer timer);
    }
    
    /**
     * Event listener interface for bubble interactions.
     */
    public interface BubbleEventListener {
        void onBubbleDismiss(Timer timer);
        void onBubbleClick(Timer timer);
        void onTimerUpdated(Timer timer);
        void onTimerStopped(Timer timer);
    }
    
    /**
     * Adapter for new OverlayWindow to implement IOverlayWindow interface.
     */
    public static class OverlayWindowAdapter implements IOverlayWindow {
        private final OverlayWindow overlayWindow;
        
        public OverlayWindowAdapter(OverlayWindow overlayWindow) {
            this.overlayWindow = overlayWindow;
        }
        
        @Override
        public void open(Timer timer, BubbleEventListener listener) {
            // Convert to OverlayWindow.BubbleEventListener
            overlayWindow.setBubbleEventListener(new OverlayWindow.BubbleEventListener() {
                @Override
                public void onBubbleDismiss(Timer timer) {
                    listener.onBubbleDismiss(timer);
                }
                
                @Override
                public void onBubbleClick(Timer timer) {
                    listener.onBubbleClick(timer);
                }
                
                @Override
                public void onTimerUpdated(Timer timer) {
                    listener.onTimerUpdated(timer);
                }
                
                @Override
                public void onTimerStopped(Timer timer) {
                    listener.onTimerStopped(timer);
                }
            });
            
            overlayWindow.updateTimer(timer);
            overlayWindow.open();
        }
        
        @Override
        public void close() {
            overlayWindow.close();
        }
        
        @Override
        public boolean isOpen() {
            return overlayWindow.isOpen();
        }
        
        @Override
        public TimerView getTimerView() {
            return overlayWindow.getTimerView();
        }
        
        @Override
        public void setDebugMode(boolean enabled) {
            overlayWindow.setDebugMode(enabled);
        }
        
        @Override
        public void cleanup() {
            overlayWindow.cleanup();
        }
        
        @Override
        public void onTimerStopped(Timer timer) {
            overlayWindow.onTimerStopped(timer);
        }
    }
    
    /**
     * Create an overlay window using the new modular implementation.
     * 
     * @param context Application context
     * @param expanded Whether to create an expanded bubble
     * @param userId User ID for the overlay
     * @return IOverlayWindow instance
     */
    public static IOverlayWindow createOverlayWindow(Context context, Boolean expanded, String userId) {
        Log.i(TAG, "Creating OverlayWindow for user: " + userId);
        OverlayWindow newWindow = new OverlayWindow(context, expanded, userId);
        return new OverlayWindowAdapter(newWindow);
    }
}