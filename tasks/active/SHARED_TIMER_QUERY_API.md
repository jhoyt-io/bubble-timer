# Shared Timer Query API and UI Components

## Problem
Currently, the app only connects to the websocket when there are active timers with non-empty `sharedWith` sets. However, this approach has limitations:

1. **Manual Sharing Required**: Users must manually share a timer from both devices to establish connection
2. **No Discovery**: No way to discover existing shared timers without manual action
3. **No Real-time Updates**: Can't see shared timer invitations or updates without active connection

## Solution
Add API endpoints and UI components to query for shared timers and manage websocket connections proactively.

## Backend Changes Needed

### 1. New API Endpoints

#### GET `/api/timers/shared`
Query for timers shared with the current user or by the current user.

**Request:**
```
GET /api/timers/shared?userId={userId}
```

**Response:**
```json
{
  "sharedWithMe": [
    {
      "timerId": "timer-123",
      "sharerUserId": "user-456", 
      "sharerUsername": "alice",
      "timer": { /* timer object */ },
      "status": "pending" | "accepted" | "rejected"
    }
  ],
  "sharedByMe": [
    {
      "timerId": "timer-789",
      "sharedWithUserId": "user-101",
      "sharedWithUsername": "bob",
      "timer": { /* timer object */ },
      "status": "pending" | "accepted" | "rejected"
    }
  ]
}
```

#### POST `/api/timers/{timerId}/share`
Share a timer with specific users.

**Request:**
```json
{
  "sharedWith": ["user1", "user2"],
  "message": "Optional invitation message"
}
```

#### POST `/api/timers/{timerId}/accept`
Accept a shared timer invitation.

#### POST `/api/timers/{timerId}/reject`
Reject a shared timer invitation.

### 2. Database Schema Updates

#### Shared Timer Invitations Table
```sql
CREATE TABLE shared_timer_invitations (
  id UUID PRIMARY KEY,
  timer_id UUID NOT NULL,
  sharer_user_id UUID NOT NULL,
  shared_with_user_id UUID NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending, accepted, rejected
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  message TEXT,
  FOREIGN KEY (timer_id) REFERENCES timers(id),
  FOREIGN KEY (sharer_user_id) REFERENCES users(id),
  FOREIGN KEY (shared_with_user_id) REFERENCES users(id)
);
```

## Frontend Changes Needed

### 1. Shared Timer Management UI

#### Shared Timers Screen
- List of timers shared with current user
- List of timers shared by current user
- Accept/reject buttons for pending invitations
- Share timer functionality

#### Share Timer Dialog
- User search/selection
- Optional message field
- Share button

### 2. API Service Layer

#### SharedTimerApiService
```java
public interface SharedTimerApiService {
    // Query for shared timers
    Call<SharedTimersResponse> getSharedTimers(String userId);
    
    // Share a timer
    Call<Void> shareTimer(String timerId, ShareTimerRequest request);
    
    // Accept/reject invitations
    Call<Void> acceptSharedTimer(String timerId);
    Call<Void> rejectSharedTimer(String timerId);
}
```

### 3. ForegroundService Integration

#### Enhanced Connection Logic
```java
// Check for shared timers via API on app startup
private void checkForSharedTimersOnStartup() {
    // Query API for shared timers
    // Connect to websocket if any shared timers exist
    // Update local shared timer tracking
}

// Periodic refresh of shared timer state
private void refreshSharedTimerState() {
    // Query API periodically
    // Update local state
    // Manage websocket connection accordingly
}
```

### 4. Repository Layer Updates

#### SharedTimerRepository
```java
public class SharedTimerRepository {
    // Cache shared timer data
    // Sync with API
    // Provide LiveData for UI updates
}
```

## Implementation Plan

### Phase 1: Backend API
1. Create database schema for shared timer invitations
2. Implement API endpoints for querying shared timers
3. Add authentication and authorization
4. Test API endpoints

### Phase 2: Frontend API Integration
1. Create SharedTimerApiService
2. Implement SharedTimerRepository
3. Add API calls to ForegroundService
4. Test API integration

### Phase 3: UI Components
1. Create Shared Timers screen
2. Add Share Timer dialog
3. Integrate with existing timer management
4. Test UI functionality

### Phase 4: Enhanced Connection Logic
1. Update ForegroundService to query API on startup
2. Add periodic refresh of shared timer state
3. Improve websocket connection management
4. Test end-to-end functionality

## Benefits

1. **Proactive Connection**: App can connect to websocket immediately if shared timers exist
2. **Better UX**: Users can discover and manage shared timers without manual action
3. **Real-time Updates**: Proper invitation and acceptance flow
4. **Scalable**: API-based approach supports multiple users and devices

## Testing Strategy

1. **API Testing**: Unit tests for all endpoints
2. **Integration Testing**: Test API with real database
3. **UI Testing**: Test shared timer management screens
4. **End-to-End Testing**: Test complete sharing workflow
5. **Performance Testing**: Test with multiple users and timers 