# Wake Screen on Timer Alarm

## Description
Implement a full-screen alert system that activates when a timer reaches zero. When the countdown completes and vibration begins, the system should wake the device screen and display a full-screen activity showing the timer in expanded mode with a prominent stop button for easy dismissal.

## Status
- [ ] Not Started
- [ ] In Progress
- [ ] Blocked
- [x] ✅ **COMPLETED**

## Initial Prompt
User requested: "Wake the screen on timer alarm - when the countdown reaches 0 and vibration begins, wake the screen and show a full screen activity that shows the timer in expanded mode and provides a large stop button."

This addresses the need for a better user experience when timers expire, ensuring users are immediately alerted and can easily dismiss the alarm.

## Implementation Plan

### Phase 1: Screen Wake Manager
- **Create `ScreenWakeManager` class**
  - Handle `PowerManager.WakeLock` for screen activation
  - Manage `KeyguardManager.KeyguardLock` for lock screen bypass
  - Provide safe wake/release methods with timeout protection

### Phase 2: Full-Screen Alarm Activity
- **Create `TimerAlarmActivity`**
  - Full-screen layout with `FLAG_TURN_SCREEN_ON` and `FLAG_DISMISS_KEYGUARD`
  - Display timer in expanded mode (reuse `CircularMenuLayout`)
  - Large, prominent stop button for easy dismissal
  - Handle timeout auto-dismiss (e.g., 30 seconds)

### Phase 3: Integration with Timer Expiration
- **Extend `ForegroundService` timer expiration logic**
  - Detect when countdown reaches zero
  - Trigger screen wake via `ScreenWakeManager`
  - Launch `TimerAlarmActivity` with timer data
  - Coordinate with existing vibration system

### Phase 4: Shared Timer Considerations
- **Multi-device coordination**
  - Ensure only the device that initiated the timer shows full-screen alert
  - Or implement setting to control per-device alarm preferences
  - Handle acknowledgment model integration

## Technical Requirements

### Permissions
- `WAKE_LOCK` - To wake the device screen
- `DISABLE_KEYGUARD` - To bypass lock screen (if needed)
- `SYSTEM_ALERT_WINDOW` - For overlay permissions (already granted)

### Key Components
- **ScreenWakeManager**: Centralized screen wake logic
- **TimerAlarmActivity**: Full-screen alarm interface
- **ForegroundService**: Timer expiration detection and coordination
- **Notification system**: Fallback for when full-screen fails

### Design Considerations
- **Battery optimization**: Respect device power management settings
- **Accessibility**: Large buttons, clear visual indicators
- **User control**: Settings to enable/disable full-screen alarms
- **Fallback handling**: Enhanced notification if full-screen fails

## Integration Points

### Existing Systems
- **ForegroundService**: Timer lifecycle and expiration detection
- **CircularMenuLayout**: Reuse for expanded timer display
- **WebsocketManager**: Coordinate shared timer alarm states
- **Notification system**: Enhanced notifications as fallback

### Future Features
- **Acknowledgment model**: Full-screen alarm fits naturally with timer acknowledgment
- **Custom alarm sounds**: Audio alert integration
- **Snooze functionality**: Temporary timer extension options

## Testing Strategy

### Unit Tests
- `ScreenWakeManager` wake/release logic
- `TimerAlarmActivity` lifecycle and timeout handling
- Timer expiration detection accuracy

### Integration Tests
- Full flow: Timer expiration → Screen wake → Full-screen display → Dismissal
- Multi-timer scenarios with different expiration times
- Shared timer coordination across devices

### Manual Testing
- Test on various devices with different lock screen settings
- Battery optimization interference testing
- Accessibility compliance verification

## Notes / Updates
- [2025-01-08] - Initial task created based on user requirements
- [2025-01-08] - **✅ IMPLEMENTATION COMPLETED AND TESTED**

### Final Implementation Details:
- ✅ **TimerAlarmActivity**: Full-screen activity with wake capabilities over lock screen
- ✅ **Permission System**: Auto-requests `USE_FULL_SCREEN_INTENT` permission on Android 14+
- ✅ **Smart Launch Logic**: Direct activity launch (no notification when successful)
- ✅ **Overlay Management**: Automatically hides/shows bubble overlays during alarm
- ✅ **Clean Alarm Display**: New `MODE_ALARM` showing large bubble without radial menu
- ✅ **Live Countdown**: Shows continuing elapsed time since expiration (-8s, -9s, etc.)
- ✅ **Shared Timer Logic**: ALL participants get full-screen alarm for easy vibration control
- ✅ **Auto-dismiss**: 30-second timeout with proper cleanup
- ✅ **Battery Optimization**: Pauses/resumes timer updates based on activity lifecycle
- ✅ **Robust Error Handling**: Fallback notifications if full-screen fails

## Implementation Files Created/Modified
- ✅ `TimerAlarmActivity.java` - **NEW**: Full-screen alarm activity
- ✅ `FullScreenIntentPermissionHelper.java` - **NEW**: Permission management utility
- ✅ `activity_timer_alarm.xml` - **NEW**: Alarm activity layout
- ✅ `button_stop_alarm.xml` - **NEW**: Stop button styling
- ✅ `ForegroundService.java` - **MODIFIED**: Alarm launch logic and overlay management
- ✅ `TimerView.java` - **MODIFIED**: Added `MODE_ALARM` for clean display
- ✅ `MainActivity.java` - **MODIFIED**: Permission request on app start
- ✅ `AndroidManifest.xml` - **MODIFIED**: Added permissions and activity declaration

## User Experience
**Before**: Timer expires → Device stays asleep or shows basic notification
**After**: Timer expires → Screen wakes → Full-screen alarm with live countdown → Clean dismissal

### Shared Timer Experience:
- **ALL participants** get full-screen alarm (not just creators)
- **Big stop button** makes it easy for anyone to stop vibration immediately
- **Synchronized dismissal** across all devices when anyone stops the timer
- **WebSocket coordination** ensures vibration stops and alarms dismiss on all devices instantly

The implementation successfully provides an iOS Clock app-like experience for timer alarms with excellent shared timer UX! 🎉
