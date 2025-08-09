package io.jhoyt.bubbletimer.overlay.touch;

/**
 * Value class to represent touch event state, extracted from Window.java touch handling logic.
 * This replaces the scattered float variables (dx, dy, origdx, origdy, etc.) with a structured approach.
 */
public class TouchEventState {
    
    // Touch event types
    public enum TouchType {
        NONE,
        POTENTIAL_CLICK,  // Touch down but no movement yet
        DRAGGING,         // Moving with touch down
        CLICK,            // Touch up with no movement
        DRAG_RELEASE      // Touch up after movement
    }
    
    private final float initialX;
    private final float initialY;
    private final float currentX;
    private final float currentY;
    private final float deltaX;
    private final float deltaY;
    private final TouchType touchType;
    private final boolean isDragging;
    
    // Constructor for initial touch down
    public TouchEventState(float initialX, float initialY) {
        this.initialX = initialX;
        this.initialY = initialY;
        this.currentX = initialX;
        this.currentY = initialY;
        this.deltaX = 0;
        this.deltaY = 0;
        this.touchType = TouchType.POTENTIAL_CLICK;
        this.isDragging = false;
    }
    
    // Constructor for touch move/up events
    public TouchEventState(float initialX, float initialY, float currentX, float currentY, 
                          TouchType touchType, boolean isDragging) {
        this.initialX = initialX;
        this.initialY = initialY;
        this.currentX = currentX;
        this.currentY = currentY;
        this.deltaX = currentX - initialX;
        this.deltaY = currentY - initialY;
        this.touchType = touchType;
        this.isDragging = isDragging;
    }
    
    /**
     * Create a new state for touch movement.
     */
    public TouchEventState withMovement(float newX, float newY) {
        boolean hasMovement = Math.abs(newX - initialX) > 0 || Math.abs(newY - initialY) > 0;
        TouchType newType = hasMovement ? TouchType.DRAGGING : TouchType.POTENTIAL_CLICK;
        return new TouchEventState(initialX, initialY, newX, newY, newType, hasMovement);
    }
    
    /**
     * Create a new state for touch release.
     */
    public TouchEventState withRelease(float releaseX, float releaseY) {
        boolean isClick = (releaseX == initialX && releaseY == initialY);
        TouchType newType = isClick ? TouchType.CLICK : TouchType.DRAG_RELEASE;
        return new TouchEventState(initialX, initialY, releaseX, releaseY, newType, !isClick);
    }
    
    /**
     * Check if this represents a click (no movement from initial position).
     * Extracted from Window.java click detection logic.
     */
    public boolean isClick() {
        return touchType == TouchType.CLICK || 
               (currentX == initialX && currentY == initialY);
    }
    
    /**
     * Check if this represents a drag operation.
     */
    public boolean isDrag() {
        return touchType == TouchType.DRAGGING || touchType == TouchType.DRAG_RELEASE;
    }
    
    /**
     * Get the total distance moved from initial position.
     */
    public float getTotalDistance() {
        return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
    
    // Getters
    public float getInitialX() { return initialX; }
    public float getInitialY() { return initialY; }
    public float getCurrentX() { return currentX; }
    public float getCurrentY() { return currentY; }
    public float getDeltaX() { return deltaX; }
    public float getDeltaY() { return deltaY; }
    public TouchType getTouchType() { return touchType; }
    public boolean isDragging() { return isDragging; }
    
    @Override
    public String toString() {
        return String.format("TouchEventState{type=%s, initial=[%.1f,%.1f], current=[%.1f,%.1f], delta=[%.1f,%.1f]}", 
                touchType, initialX, initialY, currentX, currentY, deltaX, deltaY);
    }
}
