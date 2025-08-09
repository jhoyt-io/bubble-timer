# Feature Extension Patterns

This guide shows how to extend the Bubble Timer architecture for common feature types, with specific focus on planned features.

## 🔔 Notification & Alert Systems

### Current State
- Basic persistent notification in `ForegroundService.updateNotification()`
- No wake-up or full-screen alerts
- No push notifications

### Extension Pattern: Enhanced Notifications

#### 1. Countdown Circle in Notification
```java
// In ForegroundService.updateNotification()
RemoteViews customNotification = new RemoteViews(packageName, R.layout.notification_countdown);
// Use Canvas to draw progress circle
// Update notification with custom view
```

#### 2. Full-Screen Wake-Up
```java
// New class: ScreenWakeManager
public class ScreenWakeManager {
    private PowerManager.WakeLock wakeLock;
    private KeyguardManager.KeyguardLock keyguardLock;
    
    public void wakeScreen() {
        // Acquire wake lock
        // Start full-screen activity with FLAG_TURN_SCREEN_ON
    }
}
```

#### 3. Push Notifications for Sharing
```java
// Extend WebsocketManager to handle push triggers
case "shareRequest":
    NotificationManager.sendPushNotification(data);
    
// Add to ForegroundService
private void handleShareRequest(JsonObject data) {
    // Create notification with accept/decline actions
    // Use PendingIntent to handle responses
}
```

### Integration Points
- **WebSocket**: Add `timerExpired`, `shareRequest` message types
- **ForegroundService**: Extend broadcast receiver for notification actions
- **Database**: Add `NotificationSettings` table for user preferences

---

## 🔄 Timer-like Feature Extensions (Stopwatch)

### Reusable Infrastructure
- ✅ **WebsocketManager**: Message routing, connection management
- ✅ **OverlayWindow system**: Bubble display, touch handling
- ✅ **ForegroundService**: Background coordination
- ✅ **Repository pattern**: Data persistence

### Extension Pattern: Stopwatch Support

#### 1. New Domain Object
```java
public class StopwatchTimer {
    private String id;
    private String userId;
    private String name;
    private LocalDateTime startTime;  // Key difference: start time vs end time
    private Duration elapsedWhenPaused;
    private boolean isRunning;
    private Set<String> sharedWith;
}
```

#### 2. Database Entity
```java
@Entity(tableName = "active_stopwatch")
public class ActiveStopwatch {
    // Mirror StopwatchTimer structure
    // Use same #~# delimiter pattern for sharedWith
}
```

#### 3. Converter Pattern
```java
public class StopwatchConverter {
    // Follow TimerConverter pattern
    public static StopwatchTimer fromActiveStopwatch(ActiveStopwatch entity) { }
    public static ActiveStopwatch toActiveStopwatch(StopwatchTimer domain) { }
}
```

#### 4. WebSocket Integration
```java
// In WebsocketManager
case "updateStopwatch":
    StopwatchTimer stopwatch = gson.fromJson(timer, StopwatchTimer.class);
    // Follow same sharing pattern as timers
```

#### 5. UI Adaptation
```java
// In CircularMenuLayout
if (timer instanceof StopwatchTimer) {
    // Render elapsed time instead of countdown
    // Different visual indicators (counting up vs down)
}
```

---

## 👥 Friend List & Social Features

### Current State
- Hardcoded friend list in `CircularMenuLayout`
- No friend management UI
- No persistence

### Extension Pattern: Dynamic Friend System

#### 1. Data Layer
```java
@Entity(tableName = "friends")
public class Friend {
    @PrimaryKey String userId;
    String displayName;
    String avatarUrl;
    int shareFrequency;  // For priority ordering
    boolean isFavorite;
    LocalDateTime lastSharedAt;
}

@Dao
public interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY isFavorite DESC, shareFrequency DESC")
    LiveData<List<Friend>> getFriendsOrderedByPriority();
}
```

#### 2. Repository
```java
public class FriendRepository {
    public LiveData<List<Friend>> getOrderedFriends() { }
    public void incrementShareFrequency(String userId) { }
    public void addFriend(String userId, String displayName) { }
}
```

#### 3. UI Integration
```java
// In CircularMenuLayout
public void setFriendList(List<Friend> friends) {
    this.friends = friends;
    // Rebuild button layout
    invalidate();
}

// In ViewModel
friendRepository.getOrderedFriends().observe(this, friends -> {
    circularMenuLayout.setFriendList(friends);
});
```

#### 4. WebSocket Friend Requests
```java
// New message types
case "friendRequest":
    // Handle incoming friend request
case "friendAccept":
    // Handle friend acceptance
```

---

## 🎨 UX & Customization Systems

### Extension Pattern: Theme System

#### 1. Settings Storage
```java
public class ThemeSettings {
    public static final String BUBBLE_COLOR = "bubble_color";
    public static final String ACCENT_COLOR = "accent_color";
    
    private SharedPreferences prefs;
    
    public int getBubbleColor() {
        return prefs.getInt(BUBBLE_COLOR, Color.BLUE);
    }
}
```

#### 2. Overlay Customization
```java
// In CircularMenuLayout
private ThemeSettings themeSettings;

@Override
protected void onDraw(Canvas canvas) {
    int bubbleColor = themeSettings.getBubbleColor();
    paint.setColor(bubbleColor);
    // Apply throughout rendering
}
```

### Extension Pattern: Configurable Actions

#### 1. Action System
```java
public enum BubbleAction {
    STOP("stop", R.drawable.ic_stop),
    PAUSE("pause", R.drawable.ic_pause),
    SHARE("share", R.drawable.ic_share),
    EXTEND("extend", R.drawable.ic_add_time);
    
    public final String id;
    public final int iconRes;
}

public class ActionConfiguration {
    private List<BubbleAction> enabledActions;
    private BubbleAction dismissAction = BubbleAction.STOP;
}
```

#### 2. Dynamic Menu Building
```java
// In CircularMenuLayout
public void setAvailableActions(List<BubbleAction> actions) {
    // Rebuild circular menu with configured actions
    // Position icons around circle based on count
}
```

---

## 🔄 Acknowledgment Model for Shared Timers

### Current State: Immediate Deletion
- Timer stops → Immediately removed from all devices
- No indication of who stopped it
- No way to "snooze" or defer

### Extension Pattern: Acknowledgment System

#### 1. New Timer States
```java
public enum TimerState {
    RUNNING,
    PAUSED,
    EXPIRED_PENDING_ACK,  // New state
    ACKNOWLEDGED,
    STOPPED
}
```

#### 2. Database Changes
```java
// Add to ActiveTimer
private TimerState state;
private String stoppedBy;  // Who triggered the stop
private LocalDateTime stoppedAt;
private Set<String> acknowledgedBy;  // Who has acked (# delimited)
```

#### 3. WebSocket Messages
```java
// New message types
case "timerExpired":
    // Natural expiration (time ran out)
case "timerStopped": 
    // Manual stop by user
case "timerAcknowledged":
    // User acknowledged the stopped timer
```

#### 4. UI Changes
```java
// In OverlayWindow - different visual for expired vs stopped
if (timer.getState() == TimerState.EXPIRED_PENDING_ACK) {
    // Show pulsing red bubble with "Acknowledge" action
    // Show who stopped it: "Stopped by [username]"
}
```

#### 5. Cleanup Logic
```java
// In ForegroundService - only delete when all users have acked
private void cleanupAcknowledgedTimers() {
    activeTimers.stream()
        .filter(timer -> timer.allUsersHaveAcknowledged())
        .forEach(timer -> repository.deleteById(timer.getId()));
}
```

---

## 📐 Development Workflow for New Features

### 1. Planning Phase
- Review existing patterns in this document
- Identify reusable components vs new requirements
- Check if WebSocket messages need extension
- Plan database migrations

### 2. Implementation Order
1. **Domain objects** (business logic)
2. **Database entities** + migration
3. **Repository layer** (data access)
4. **WebSocket integration** (if needed)
5. **ViewModel updates** (state management)
6. **UI components** (visual representation)
7. **Tests** (unit → integration → manual)

### 3. Integration Checklist
- [ ] Hilt dependency injection configured
- [ ] WebSocket message handling (if applicable)
- [ ] Database migration tested
- [ ] TimerConverter pattern followed (if applicable)
- [ ] ForegroundService integration (if background work needed)
- [ ] Error handling and edge cases
- [ ] Tests written and passing

### 4. Common Integration Points
- **Repository observers**: Most features need to react to data changes
- **Broadcast system**: Communication between ViewModel and ForegroundService
- **Overlay system**: Visual representation in bubbles
- **WebSocket**: Sharing with other users
- **Notification system**: Background alerts
