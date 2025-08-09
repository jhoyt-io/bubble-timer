# WebSocket Implementation Guide

## Overview

The WebSocket implementation handles real-time communication between the Android frontend and AWS backend for timer sharing and synchronization.

## Message Formats

### Outgoing Messages (Client → Server)

#### Update Timer
```json
{
  "action": "sendmessage",
  "data": {
    "type": "updateTimer",
    "shareWith": ["user1", "user2"],
    "timer": {
      "id": "timer-uuid",
      "userId": "creator-user-id",
      "name": "Timer Name",
      "totalDuration": "PT30M",
      "remainingDuration": "PT15M",
      "timerEnd": "2024-01-01T12:00:00"
    }
  }
}
```

#### Stop Timer
```json
{
  "action": "sendmessage",
  "data": {
    "type": "stopTimer",
    "timerId": "timer-uuid",
    "shareWith": ["user1", "user2"]
  }
}
```

#### Ping
```json
{
  "action": "sendmessage",
  "data": {
    "type": "ping",
    "timestamp": "2024-01-01T12:00:00.000Z"
  }
}
```

### Incoming Messages (Server → Client)

#### Timer Update
```json
{
  "type": "updateTimer",
  "messageId": "msg-uuid",
  "timer": {
    "id": "timer-uuid",
    "userId": "creator-user-id",
    "name": "Timer Name",
    "totalDuration": "PT30M",
    "remainingDuration": "PT15M",
    "timerEnd": "2024-01-01T12:00:00"
  }
}
```

#### Timer Stop
```json
{
  "type": "stopTimer",
  "messageId": "msg-uuid",
  "timerId": "timer-uuid"
}
```

#### Pong Response
```json
{
  "type": "pong",
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

## Critical Implementation Patterns

### 1. Message Field Mapping

**IMPORTANT**: Different message types use different field structures:

```typescript
// CORRECT: Handle different message formats appropriately
const timerId = data.type === 'stopTimer' ? data.timerId : (data.timerId || data.timer?.id);
```

- **`stopTimer`**: Uses `data.timerId` directly
- **`updateTimer`**: Uses `data.timer.id` (nested in timer object)

### 2. Broadcasting Pattern

The correct broadcasting pattern is:

1. **Send to user's own connections first** (with messageId)
2. **Then broadcast to shared users** (fire-and-forget)

```typescript
// CORRECT: Send to user's connections first
await sendDataToUser(userId, deviceId, messageWithId);

// CORRECT: Fire-and-forget for shared users
currentSharedUsers.forEach((userName: string) => {
    sendDataToUser(userName, senderDeviceId, data).catch((error) => {
        // Handle error but don't await
    });
});
```

### 3. Connection Cleanup

**Simple is better**: Clean up connections on any error, not just specific error types.

```typescript
// CORRECT: Simple connection cleanup
} catch (error) {
    await updateConnection({
        userId: cognitoUserName,
        deviceId: connection.deviceId,
        connectionId: undefined,
    });
}
```

## Error Handling

### Connection Failures
- **Clean up on any error**: Don't try to detect specific error types
- **Log the error**: Include connection details for debugging
- **Continue processing**: Don't let one failed connection break the broadcast

### Message Validation
- **Validate early**: Check message structure before processing
- **Provide clear errors**: Return meaningful error messages to the client
- **Log validation failures**: Track what messages are being rejected

## Performance Considerations

### Fire-and-Forget Broadcasting
- **Don't await shared user broadcasts**: Use `.forEach()` with `.catch()`
- **Parallel processing**: Send to multiple users simultaneously
- **Error isolation**: One failed broadcast shouldn't affect others

### Connection Management
- **Clean up stale connections**: Remove connections that fail to receive messages
- **Monitor connection health**: Track connection success/failure rates
- **Limit reconnection attempts**: Prevent infinite reconnection loops

## Testing

### Unit Tests
- **Test message parsing**: Verify different message formats are handled correctly
- **Test broadcasting logic**: Ensure messages are sent to the right users
- **Test error handling**: Verify connection cleanup works properly

### Integration Tests
- **Test end-to-end flows**: Verify timer updates propagate across users
- **Test connection scenarios**: Test with multiple devices and users
- **Test error scenarios**: Test with network failures and invalid messages

## Monitoring

### Key Metrics
- **Message success rate**: Track successful vs failed message deliveries
- **Connection health**: Monitor connection establishment and cleanup
- **Error rates**: Track validation failures and processing errors

### Logging
- **Structured logging**: Use consistent log format with relevant metadata
- **Context information**: Include `timerId`, `messageType`, `userId` in logs
- **Error details**: Log full error information for debugging

## Common Pitfalls

### 1. Over-Engineering Error Handling
❌ **Wrong**: Complex error type detection
```typescript
const isStaleConnection = error.message.includes('ECONNREFUSED') || 
                         error.message.includes('GoneException');
```

✅ **Correct**: Simple cleanup on any error
```typescript
// Clean up on any connection failure
await updateConnection({...});
```

### 2. Breaking Message Broadcasting Order
❌ **Wrong**: Only sending to shared users
```typescript
await handleTimerSharing(data, deviceId, timerLogger);
```

✅ **Correct**: Send to user's connections first, then shared users
```typescript
await sendDataToUser(userId, deviceId, messageWithId);
await handleTimerSharing(data, deviceId, timerLogger);
```

### 3. Awaiting Fire-and-Forget Operations
❌ **Wrong**: Awaiting shared user broadcasts
```typescript
await Promise.allSettled(sendPromises);
```

✅ **Correct**: Fire-and-forget with error handling
```typescript
currentSharedUsers.forEach((userName: string) => {
    sendDataToUser(userName, senderDeviceId, data).catch((error) => {
        // Handle error but don't await
    });
});
```

## Remember

The WebSocket implementation is critical for real-time timer sharing. **Preserve working behavior** and make **minimal, focused changes**. When in doubt, compare with the original working implementation.
