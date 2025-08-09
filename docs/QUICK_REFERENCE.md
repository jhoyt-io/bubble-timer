# Quick Reference Guide

## 🚀 Getting Started Checklist
1. Read `ARCHITECTURE.md` for data flows
2. Watch `demo-video.mov` or review `screenshots/` for visual context
3. Check `tasks/active/` for current work
4. Run tests: `./gradlew test`

## 🔧 Key File Locations

### Core Logic
- `ActiveTimerViewModel.java` - UI state management
- `ForegroundService.java` - Background orchestration  
- `WebsocketManager.java` - Real-time sync
- `TimerConverter.java` - Data mapping between layers

### UI Components
- `MainActivity.java` - Main app screen
- `OverlayWindow.java` - Individual bubble management
- `CircularMenuLayout.java` - Bubble rendering & interactions
- `TouchEventHandler.java` - Gesture processing

### Data Layer
- `ActiveTimerRepository.java` - Timer persistence
- `SharedTimerRepository.java` - Invitation management
- `db/ActiveTimer.java` - Database entity
- `domain/entities/Timer.java` - Business object

## 🐛 Debugging Commands

### WebSocket Issues
```bash
# Check connection status
adb logcat | grep "WebsocketManager.*CONNECTED"

# Monitor ping/pong
adb logcat | grep "ping\|pong"

# Watch message flow
adb logcat | grep "Sending\|Received.*timer"
```

### Overlay Problems
```bash
# Check currentUserId setting
adb logcat | grep "setCurrentUserId\|currentUserId"

# Monitor window lifecycle
adb logcat | grep "OverlayWindow.*open\|close"

# Watch shared-by display
adb logcat | grep "Drawing shared by\|Not drawing shared by"
```

### Database Sync Issues
```bash
# Repository changes
adb logcat | grep "Repository observer\|timers:"

# Broadcast communication
adb logcat | grep "Broadcast received\|timerUpdated"
```

## 🔍 Common Data Patterns

### Timer State Transitions
```
Created → Started → Running → (Paused ↔ Running) → Stopped/Expired
```

### Sharing Flow
```
Owner starts timer → Sends invitation → Recipient accepts → Real-time sync begins
```

### Overlay Lifecycle  
```
Timer created → Service observer → Window created → (if overlayShown) → Window.open()
```

## 📊 Database Quick Queries

### Check Active Timers
```sql
SELECT name, userId, sharedWithString, timerEnd FROM ActiveTimer;
```

### Check Pending Invitations
```sql
SELECT name, sharedBy, sharedWith FROM SharedTimer;
```

## 🌐 WebSocket Message Types

| Type | Direction | Purpose |
|------|-----------|---------|
| `updateTimer` | Bi-directional | Timer state changes |
| `stopTimer` | Bi-directional | Timer deletion |
| `activeTimerList` | Incoming | Bulk timer sync |
| `acknowledge` | Outgoing | Confirm receipt |

## ⚡ Performance Tips

### Memory Management
- OverlayWindows are cached in `windowsByTimerId`
- WebSocket auto-disconnects when no shared timers
- Repository observers use `observeForever()` - ensure cleanup

### Battery Optimization
- ForegroundService only runs when timers active
- Overlay rendering optimized for small circles
- WebSocket ping interval: 30s (emulator), 15s (device)

## 🛠 Extension Patterns

### Adding New Timer Properties
1. Update `Timer.java` (domain)
2. Update `ActiveTimer.java` (database) + migration
3. Update `TimerConverter.java` mapping
4. Update UI components as needed

### Adding New WebSocket Messages
1. Add message type to `WebsocketManager`
2. Handle in `ForegroundService.onMessage()`
3. Update UI via repository or broadcast

### Adding New Overlay Features
1. Modify `OverlayWindow` for behavior
2. Update `CircularMenuLayout` for display
3. Handle user input in `TouchEventHandler`

## 🎯 Feature Flags & Configuration

### Current Settings
- Overlay implementation: New modular system (feature flags removed)
- WebSocket timeouts: Adaptive based on emulator detection
- Text truncation: 10 chars → 8 chars + "..." for small bubbles

### Hardcoded Values to Eventually Configure
- Friend list in `CircularMenuLayout`
- Color scheme in overlay rendering
- Bubble sizes and positioning
- WebSocket ping intervals
