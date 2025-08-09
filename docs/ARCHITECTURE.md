# Bubble Timer Architecture

## Core Data Flow
```
MainActivity ←→ ActiveTimerViewModel ←→ ActiveTimerRepository ←→ Room Database
    ↓                    ↓                           ↓
ForegroundService ←→ WebsocketManager ←→ Backend API ←→ Other Devices
    ↓
OverlayWindowFactory → OverlayWindow → TimerView/CircularMenuLayout
```

## Key Components & Responsibilities

### **ActiveTimerViewModel** (UI State Management)
- **Purpose**: Central state for UI, coordinates timer operations
- **Key Methods**: `startNewTimer()`, `stopTimer()`, `observeForever()`
- **Communication**: Broadcasts to ForegroundService via `LocalBroadcastManager`
- **Hilt Integration**: `@HiltViewModel` with `@ApplicationContext` injection

### **ForegroundService** (Background Orchestrator)
- **Purpose**: Overlay lifecycle, WebSocket coordination, persistent notifications
- **Key Responsibilities**:
  - Observes `activeTimerRepository.getAllActiveTimers()`
  - Manages `windowsByTimerId` (overlay windows)
  - Handles WebSocket connection based on shared timer presence
  - Processes broadcasts from ViewModel (`timerUpdated`, `showOverlay`, etc.)
- **Critical Pattern**: Always update `currentUserId` on existing windows when auth changes

### **WebsocketManager** (Real-time Sync)
- **Purpose**: Bi-directional sync for shared timers
- **Message Types**: `updateTimer`, `stopTimer`, `activeTimerList`, `acknowledge`
- **Connection Logic**: Auto-connects when shared timers exist, disconnects when none
- **Gotchas**: Emulator vs device timeout differences, ping/pong handling

### **OverlayWindow System** (Bubble UI)
- **OverlayWindowFactory**: Creates `IOverlayWindow` instances
- **OverlayWindow**: Individual bubble management, touch handling
- **TimerView/CircularMenuLayout**: Visual rendering, user interactions
- **Critical**: Must call `timerView.setCurrentUserId()` for shared-by display

### **Data Layer** (Persistence & Domain)
- **ActiveTimer** (Room Entity): Database representation with `sharedWithString` (#~# delimited)
- **Timer** (Domain Object): Business logic, shared with WebSocket
- **TimerConverter**: Bidirectional conversion, handles field mapping
- **SharedTimerRepository**: Invitation management, acceptance flow

## Database Schema

### ActiveTimer (Room Entity)
```java
id: String (UUID)
userId: String (creator)
name: String
totalDuration: Duration (PT format)
remainingDurationWhenPaused: Duration (null = running)
timerEnd: LocalDateTime (null = paused)
sharedWithString: String (#~# delimited)
sharedBy: String (original sharer, different from userId for accepted timers)
tagsString: String
```

### SharedTimer (Invitations)
```java
id: String
sharedBy: String
name: String
totalDuration: Duration
sharedWith: String (#~# delimited)
```

## WebSocket Message Patterns

### Outgoing (Client → Server)
```json
{
  "action": "sendmessage",
  "data": {
    "type": "updateTimer|stopTimer",
    "shareWith": ["user1", "user2"],
    "timer": { /* Timer object */ }
  }
}
```

### Incoming (Server → Client)
```json
{
  "type": "updateTimer|stopTimer|activeTimerList",
  "timer": { /* Timer object */ },
  "timers": [ /* Array for activeTimerList */ ]
}
```

## Critical Data Flow Patterns

### Timer Updates (Local → Remote)
1. User interaction → `ActiveTimerViewModel.updateTimer()`
2. ViewModel updates repository → broadcasts `"timerUpdated"` to ForegroundService
3. ForegroundService → `WebsocketManager.sendTimerUpdate()`
4. WebSocket → Server → Other clients

### Shared Timer Updates (Remote → Local)
1. WebSocket receives message → `ForegroundService.onMessage()`
2. ForegroundService → Repository update
3. Repository change → ViewModel observer → UI update
4. Repository change → ForegroundService observer → Overlay update

### Overlay Lifecycle
1. Timer starts → Repository insert → ForegroundService observer
2. If `isOverlayShown` → Create `OverlayWindow` → Store in `windowsByTimerId`
3. Timer stops → Repository delete → Observer cleanup → Window close

## Feature Extension Points

### 🔔 **Notifications & Wake-up (✅ IMPLEMENTED)**
- **Current**: Full-screen alarm system with wake capabilities
- **Components**:
  - `TimerAlarmActivity` - Full-screen alarm display over lock screen
  - `FullScreenIntentPermissionHelper` - Android 14+ permission management
  - `ForegroundService.launchTimerAlarmActivity()` - Smart alarm launch logic
  - `TimerView.MODE_ALARM` - Clean alarm display without radial menu
  - Auto-hide/show overlay system during alarms

### 🔄 **Stopwatch Support (Planned)**
- **Reusable Components**: WebsocketManager, OverlayWindow system, sharing infrastructure
- **New Entities**: `StopwatchTimer` (elapsed time vs countdown)
- **Pattern**: Follow Timer → ActiveTimer → SharedTimer model

### 👥 **Friend List System (Planned)**
- **Current**: Hardcoded users in `CircularMenuLayout`
- **Extension Points**:
  - New: `Friend` entity, `FriendRepository`
  - Modify: `CircularMenuLayout` to use dynamic friend list
  - WebSocket: Add friend request/accept message types

### 🎨 **UX Improvements (Planned)**
- **Customization**: Theme system in SharedPreferences
- **Radial Menu**: `CircularMenuLayout` already supports dynamic buttons
- **Overlay Effects**: Extend `OverlayWindow` rendering system

## Common Development Gotchas

### String Delimiters
- **Correct**: Use `#~#` for `sharedWithString` (consistent across all components)
- **Wrong**: Commas (conflicts with JSON serialization)

### User ID Management
- **Pattern**: Always ensure timer creator is in `sharedWith` set
- **Protection**: `Timer.setSharedWith()` and `removeSharing()` enforce creator inclusion
- **Overlay Bug**: Must call `timerView.setCurrentUserId()` when creating OverlayWindow

### Duration Handling
- **Correct**: Use `java.time.Duration` objects throughout
- **Wrong**: Separate minutes/seconds integers (causes precision loss)

### WebSocket Connection Management
- **Pattern**: Auto-connect only when shared timers exist
- **Timing**: Requires auth token before connection attempt
- **Error Handling**: Different timeouts for emulator vs device

### Hilt Dependency Injection
- **ViewModels**: Use `@HiltViewModel` + `@ApplicationContext` for Context
- **Activities**: Add `@AndroidEntryPoint`
- **Services**: Hilt integration in ForegroundService

## Testing Strategies
- **Unit Tests**: ViewModel business logic, TimerConverter
- **Integration Tests**: Repository + Database interactions
- **Manual Testing**: Overlay interactions, WebSocket sync across devices
- **Common Test Data**: Use TestDataFactory for consistent Timer objects
