# Push Notifications and Acknowledgment Model

## Description
Implement push notifications for timer invitations and establish an acknowledgment-based model for shared timer lifecycle management. This replaces the current model where any timer update re-adds it to the shared timer query, requiring backend improvements for proper invitation and completion workflows.

## Status
- [x] Not Started
- [ ] In Progress
- [ ] Blocked
- [ ] Completed

## Initial Prompt
User requested: "Push notifications for timer invitations - this should also include the acknowledgement model, since we don't want the current model where any update to a timer adds it back to the shared timer query (this is mostly a backend limitation, but once we have a better backend flow available we'll need to integrate it in the app)"

This addresses the need for proper invitation notifications and a more sophisticated shared timer lifecycle that requires explicit acknowledgment rather than immediate deletion.

## Implementation Plan

### Phase 1: Push Notification Infrastructure
- **Integrate Firebase Cloud Messaging (FCM)**
  - Add FCM dependency to app-level `build.gradle`
  - Configure Firebase project and add `google-services.json`
  - Implement `FirebaseMessagingService` for push notification handling

- **Create `NotificationManager` class**
  - Handle different notification types (invitations, timer updates, acknowledgments)
  - Manage notification channels for different priority levels
  - Provide rich notifications with action buttons (Accept/Decline)

### Phase 2: Invitation Notification System
- **Timer Sharing Notifications**
  - Push notification when timer is shared with user
  - Rich notification with timer details (name, duration, shared by)
  - Action buttons: "Accept", "Decline", "View"
  - Deep linking to app for invitation management

- **Update WebSocket Message Types**
  - Add `timerInvitation` message type for sharing notifications
  - Add `invitationResponse` message type for accept/decline responses
  - Modify backend API to trigger push notifications for invitations

### Phase 3: Acknowledgment Model Implementation
- **New Timer States**
  - `RUNNING` - Timer actively counting down
  - `PAUSED` - Timer temporarily stopped
  - `EXPIRED_PENDING_ACK` - Timer finished, awaiting acknowledgment
  - `ACKNOWLEDGED` - All users have acknowledged completion
  - `CANCELLED` - Timer manually stopped before completion

- **Database Schema Updates**
  - Add `state` field to `ActiveTimer` entity
  - Add `acknowledgedBy` field (delimited string of user IDs)
  - Add `completedAt` and `completedBy` timestamps
  - Create database migration for new fields

### Phase 4: Backend Integration Requirements
- **Enhanced Timer Lifecycle API**
  - Separate endpoints for timer completion vs acknowledgment
  - Invitation tracking with accept/decline responses
  - Proper state management preventing re-addition to queries
  - User preference management for notification settings

- **WebSocket Message Updates**
  - `timerExpired` - Natural timer completion (time reached zero)
  - `timerStopped` - Manual timer cancellation
  - `timerAcknowledged` - User acknowledged completed timer
  - `invitationSent` - Timer shared with user
  - `invitationAccepted` - User accepted shared timer
  - `invitationDeclined` - User declined shared timer

### Phase 5: UI Integration
- **Enhanced Shared Timer List**
  - Show invitation status (Pending, Accepted, Declined)
  - Display timer state for completed timers awaiting acknowledgment
  - "Acknowledge" button for expired timers
  - Visual indicators for different timer states

- **Notification UI**
  - In-app notification management
  - Push notification history
  - Notification preferences and settings

## Technical Requirements

### Dependencies
- `Firebase Cloud Messaging (FCM)` - Push notification delivery
- `Firebase Analytics` - Notification engagement tracking
- `Room Database` - Enhanced schema for acknowledgment model

### Permissions
- `INTERNET` - Network communication (already granted)
- `WAKE_LOCK` - For notification processing (already granted)
- `VIBRATE` - Notification alerts (already granted)

### Backend Requirements
- **Enhanced timer lifecycle endpoints**
  - `POST /timers/{id}/complete` - Mark timer as expired
  - `POST /timers/{id}/acknowledge` - User acknowledges completion
  - `POST /timers/{id}/invite` - Send invitation with push notification
  - `POST /invitations/{id}/respond` - Accept/decline invitation

- **Push notification triggers**
  - Timer invitation sent → FCM push to recipient
  - Timer expires → FCM push to all shared users
  - Invitation response → FCM push to timer owner

## Integration Points

### Existing Systems
- **WebsocketManager**: Enhanced message types for acknowledgment model
- **SharedTimerRepository**: Updated invitation and completion workflows
- **ForegroundService**: Integration with notification display and acknowledgment
- **TimerConverter**: Handle new timer states and acknowledgment fields

### New Components
- **FirebaseMessagingService**: Push notification reception and handling
- **NotificationManager**: Rich notification creation and management
- **InvitationManager**: Handle invitation workflow and user responses
- **AcknowledgmentManager**: Track and coordinate timer acknowledgments

## User Experience Flow

### Timer Invitation
1. User A shares timer with User B
2. Backend sends FCM push to User B's device
3. User B receives rich notification with Accept/Decline actions
4. User B responds → Backend updates invitation status → WebSocket sync

### Timer Completion with Acknowledgment
1. Timer reaches zero on any device
2. All shared users receive push notification: "Timer 'Work Focus' completed"
3. Timer state becomes `EXPIRED_PENDING_ACK` on all devices
4. Each user must tap "Acknowledge" button
5. Timer fully deleted only when all users have acknowledged

### Benefits
- **No accidental re-activation**: Timers don't re-appear in shared queries
- **Explicit user control**: Users must actively acknowledge completion
- **Better coordination**: All users know when others have seen the completion
- **Reduced notification spam**: Controlled push notification delivery

## Testing Strategy

### Unit Tests
- `NotificationManager` notification creation and channel management
- `AcknowledgmentManager` state tracking and completion logic
- Firebase messaging service message handling

### Integration Tests
- End-to-end invitation flow with push notifications
- Timer completion and acknowledgment across multiple devices
- WebSocket message coordination with notification system

### Manual Testing
- Test push notifications on various Android versions
- Cross-device acknowledgment coordination
- Notification interaction testing (Accept/Decline actions)

## Notes / Updates
- [2025-01-08] - Initial task created based on user requirements
- Backend improvements required before full implementation
- Should coordinate with wake screen feature for timer completion alerts
- Consider user preferences for notification frequency and types

## Related Files
- `app/src/main/java/io/jhoyt/bubbletimer/WebsocketManager.java` - Message type extensions
- `app/src/main/java/io/jhoyt/bubbletimer/db/SharedTimerRepository.java` - Invitation workflow
- `app/src/main/java/io/jhoyt/bubbletimer/ForegroundService.java` - Notification coordination
- `docs/FEATURE_EXTENSION_PATTERNS.md` - Notification system patterns
- `docs/ARCHITECTURE.md` - Component integration guidelines
