# Bubble Timer On-Demand WebSocket Connection Implementation Plan

## Overview

Change the Bubble Timer app from an always-connected WebSocket model to an on-demand connection model based on shared timers.

## Current State

- **WebSocket Connection**: Always-on via ForegroundService
- **Sharing Model**: Timers have a `sharedWith` field containing usernames
- **Backend**: Stores timers in DynamoDB and manages WebSocket connections
- **Sharing UI**: Hardcoded friend names in TimerView

## New Model Flow

1. **Initial State**: No WebSocket connection, no shared timers
2. **Sharing Initiated**: User shares a timer → WebSocket connection established
3. **Timer Received**: Recipient gets notification → Can accept/reject shared timer
4. **Active Sharing**: Both users connected, receiving real-time updates
5. **Timer Ends**: Check if other shared timers exist → Disconnect if none

## Implementation Changes

### Phase 1: Android App Changes Only

#### 1.1 New SharedTimerManager (`SharedTimerManager.java`)
- Track which timers are shared and manage connection lifecycle
- Observe active timers to determine when to connect/disconnect
- Handle sharing workflow (share, accept, reject)
- Provide interface for shared timer events

#### 1.2 Updated WebsocketManager (`WebsocketManager.java`)
- Remove automatic connection on initialize
- Add `connectIfNeeded()` method for on-demand connection
- Add new message sending methods for sharing workflow
- Add handlers for new message types

#### 1.3 Updated ForegroundService (`ForegroundService.java`)
- Integrate SharedTimerManager
- Remove automatic WebSocket connection on startup
- Add handling for sharing commands
- Add SharedTimerListener implementation

### Phase 2: Connection Lifecycle Management

#### 2.1 Connection Logic
- **Connect when**:
  - User shares a timer
  - User accepts a shared timer
- **Disconnect when**:
  - All shared timers end
  - User rejects all shared timers
  - User unshares all timers

#### 2.2 State Tracking
- Track which timers are shared using existing `sharedWith` field
- Monitor connection state per user
- Determine connection needs based on shared timer state

## Benefits

1. **Reduced Resource Usage**: No unnecessary WebSocket connections
2. **Better User Experience**: Clear sharing workflow with accept/reject
3. **Improved Scalability**: Fewer active connections
4. **Enhanced Privacy**: Users control when they're connected
5. **Minimal Changes**: Leverages existing data structures
6. **Simpler Implementation**: No backend changes required

## Testing Plan

### Android App Testing
1. Test connection establishment when sharing
2. Test connection termination when no shared timers
3. Test shared timer notification handling
4. Test accept/reject workflow

### Integration Testing
1. End-to-end sharing workflow
2. Multiple users sharing timers
3. Connection stability during sharing
4. Error handling and recovery

## Migration Strategy

### Phase 1: App Deployment
1. Deploy app with new sharing model
2. Test with existing users
3. Monitor connection behavior

### Phase 2: UI Enhancement
1. Add notification system
2. Enhance sharing UI
3. Add user feedback mechanisms

## Backend Architecture (No Changes Required)

The existing backend already supports the on-demand connection model perfectly:

### Current Backend Capabilities
- **WebSocket Handler**: Maintains connections and forwards messages
- **Timer Storage**: DynamoDB stores timers with `sharedWith` field
- **Message Routing**: Uses existing `sharedWith` field to route messages
- **Connection Management**: Clients manage their own connections

### Why No Backend Changes Are Needed
1. **Message Relay System**: Backend is already a stateless message relay
2. **Existing Routing Logic**: `sharedWith` field already handles message routing
3. **Client-Side Connection Management**: Clients decide when to connect/disconnect
4. **No Connection Lifecycle Logic**: Backend doesn't need to track shared timer state

## Implementation Flow

```
1. User shares timer → Client connects to WebSocket
2. Backend receives message → Routes to recipient via existing sharedWith logic
3. Recipient receives message → Client connects to WebSocket if not already connected
4. Timer ends → Client checks local shared timer state → Disconnects if none remain
```

## Alternatives Worth Mentioning

### Alternative 1: New DynamoDB Table for Invitations
- **Approach**: Create a `SharedTimerInvitations` table to track pending invitations
- **Pros**: Clear invitation workflow, better tracking of accept/reject states
- **Cons**: More complex, requires schema changes, additional data management
- **When to consider**: If you need detailed invitation tracking or complex sharing workflows

### Alternative 2: Push Notifications for Shared Timers
- **Approach**: Use push notifications to notify users of shared timers instead of WebSocket
- **Pros**: Works even when app is closed, more reliable delivery
- **Cons**: Requires push notification infrastructure, less real-time
- **When to consider**: If you want to notify users when app is not active

### Alternative 3: Hybrid Approach with Connection Pooling
- **Approach**: Maintain a small pool of connections for active users, scale based on sharing
- **Pros**: Better resource utilization, faster response times
- **Cons**: More complex connection management, higher resource usage
- **When to consider**: If you have many users with frequent sharing activity

### Alternative 4: Server-Sent Events (SSE) Instead of WebSocket
- **Approach**: Use SSE for one-way communication from server to client
- **Pros**: Simpler than WebSocket, works through proxies
- **Cons**: One-way communication only, less suitable for real-time updates
- **When to consider**: If you only need server-to-client communication

## Recommended Approach

The **on-demand WebSocket connection model** using existing data structures is recommended because:

1. **Minimal Changes**: Leverages existing `sharedWith` field
2. **Simple Implementation**: No backend changes required
3. **Clear Benefits**: Significant resource savings with simple logic
4. **Backward Compatible**: Works with existing timer sharing model
5. **Scalable**: Reduces connection overhead for inactive users
6. **Fast Deployment**: Only Android app changes needed

This approach provides the core benefits of reduced resource usage and better user experience while minimizing implementation complexity and risk. 