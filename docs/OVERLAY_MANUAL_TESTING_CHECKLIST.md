# Overlay Manual Testing Checklist

This checklist ensures comprehensive validation of overlay functionality before and after refactoring. Use this for regression testing to ensure no behavioral changes occur during the refactoring process.

## Test Environment Setup

### Devices
- [ ] Phone (Android 11+): Small screen, ~1080x2400
- [ ] Tablet (Android 11+): Large screen, ~1920x1200
- [ ] Emulator fallback if physical devices unavailable

### Pre-Test Setup
- [ ] Install current version of app
- [ ] Grant overlay permission (System Settings > Apps > Bubble Timer > Display over other apps)
- [ ] Have a timer ready to test with
- [ ] Clear any existing timers for consistent testing

## Core Overlay Functionality Tests

### 1. Overlay Display and Creation
- [ ] **Test**: Start a timer, verify overlay appears
- [ ] **Expected**: Circular timer overlay visible on screen
- [ ] **Test**: Overlay appears over other apps (open another app)
- [ ] **Expected**: Timer remains visible over other applications

### 2. Touch Event Handling

#### 2.1 Click Detection
- [ ] **Test**: Single tap on timer overlay
- [ ] **Expected**: Opens main activity
- [ ] **Test**: Single tap on button areas (if in expanded mode)
- [ ] **Expected**: Appropriate button action (pause/play, +1min, etc.)

#### 2.2 Drag Functionality
- [ ] **Test**: Long press and drag timer around screen
- [ ] **Expected**: Timer follows finger smoothly, no lag
- [ ] **Test**: Drag to all four corners of screen
- [ ] **Expected**: Timer can reach all areas, doesn't get stuck
- [ ] **Test**: Drag beyond screen edges
- [ ] **Expected**: Timer constrained to screen boundaries

#### 2.3 Snap-to-Side Behavior
- [ ] **Test**: Drag timer to left half of screen, release
- [ ] **Expected**: Timer snaps to left edge with slight overlap
- [ ] **Test**: Drag timer to right half of screen, release
- [ ] **Expected**: Timer snaps to right edge with slight overlap
- [ ] **Test**: Release timer exactly at screen center
- [ ] **Expected**: Consistent behavior (should go to right side)

### 3. Dismiss Circle Functionality

#### 3.1 Dismiss Circle Appearance
- [ ] **Test**: Start dragging timer
- [ ] **Expected**: Dismiss circles appear (typically at bottom of screen)
- [ ] **Test**: Stop dragging without dismissing
- [ ] **Expected**: Dismiss circles disappear

#### 3.2 Timer Dismissal
- [ ] **Test**: Drag timer to dismiss circle (close/stop icon)
- [ ] **Expected**: Timer visual feedback (pulled toward circle)
- [ ] **Test**: Release timer over dismiss circle
- [ ] **Expected**: Timer disappears, stops, and closes overlay
- [ ] **Test**: Drag near dismiss circle but release outside threshold
- [ ] **Expected**: Timer snaps back to side, continues running

### 4. Screen Orientation and Size Handling

#### 4.1 Rotation Testing
- [ ] **Test**: Rotate device while timer is displayed
- [ ] **Expected**: Timer repositions appropriately for new orientation
- [ ] **Test**: Drag timer in portrait, rotate to landscape
- [ ] **Expected**: Timer remains visible and functional

#### 4.2 Multi-Window/Split Screen
- [ ] **Test**: Enable split-screen mode with timer overlay
- [ ] **Expected**: Timer remains visible and functional
- [ ] **Test**: Drag timer in split-screen environment
- [ ] **Expected**: Normal drag and snap behavior

### 5. Edge Cases and Error Conditions

#### 5.1 Boundary Testing
- [ ] **Test**: Rapid tapping on timer (stress test)
- [ ] **Expected**: No crashes, consistent click detection
- [ ] **Test**: Rapid dragging back and forth
- [ ] **Expected**: Smooth tracking, no performance issues
- [ ] **Test**: Try to drag timer off-screen
- [ ] **Expected**: Timer constrained to visible area

#### 5.2 System Integration
- [ ] **Test**: Receive phone call while timer overlay active
- [ ] **Expected**: Timer remains functional, doesn't interfere
- [ ] **Test**: Pull down notification shade
- [ ] **Expected**: Timer visible above or below as appropriate
- [ ] **Test**: Open keyboard
- [ ] **Expected**: Timer repositions if necessary, remains accessible

### 6. Performance Testing

#### 6.1 Responsiveness
- [ ] **Test**: Time from touch to visual response
- [ ] **Expected**: < 16ms (60fps), no visible lag
- [ ] **Test**: Smooth animation during drag
- [ ] **Expected**: No stuttering or frame drops
- [ ] **Test**: Memory usage during extended overlay use
- [ ] **Expected**: No significant memory leaks

#### 6.2 Battery Impact
- [ ] **Test**: Battery usage with overlay active for 30 minutes
- [ ] **Expected**: Reasonable power consumption
- [ ] **Test**: CPU usage during drag operations
- [ ] **Expected**: Minimal CPU impact when not actively dragging

## Regression Testing Protocol

### Before Refactoring
1. Run complete checklist with current implementation
2. Document any existing issues or limitations
3. Record performance baselines
4. Take screenshots/videos of expected behavior

### After Each Refactoring Step
1. Run focused tests for modified functionality
2. Run subset of full checklist for critical paths
3. Compare performance against baselines
4. Document any behavioral changes

### Final Validation
1. Run complete checklist again
2. Compare results with pre-refactoring baseline
3. Ensure zero behavioral regression
4. Validate performance maintained or improved

## Issue Documentation Template

```
**Issue**: Brief description
**Test Case**: Which test case revealed the issue
**Expected**: What should happen
**Actual**: What actually happened
**Device**: Device/emulator used
**Steps to Reproduce**: 
1. Step 1
2. Step 2
3. Step 3
**Severity**: Critical/High/Medium/Low
**Regression**: Yes/No (was this working before?)
```

## Sign-off Criteria

For the refactoring to be considered successful:

- [ ] All test cases pass with identical behavior to baseline
- [ ] No new issues introduced
- [ ] Performance maintained or improved
- [ ] Code maintainability significantly improved
- [ ] Unit test coverage for extracted components > 90%

## Notes Section

Use this space to document any observations, edge cases discovered, or additional test scenarios that should be added to future testing.

---

**Last Updated**: [Date]  
**Tested By**: [Name]  
**Version**: [App Version]  
**Baseline Established**: [Date]
