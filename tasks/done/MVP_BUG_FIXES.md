# MVP Bug Fixes and Issues

## Description
Critical bugs and performance issues that need to be resolved before considering the first MVP release complete. These issues affect core functionality and user experience.

## Status
- [x] In Progress - Critical bugs being fixed
- [ ] Blocked
- [x] Completed - MVP ready for release!

## Initial Prompt
User identified several critical bugs and performance issues that need to be addressed before the MVP release can be considered complete. These include WebSocket synchronization issues, timer state propagation problems, and performance concerns.

## Critical Bugs

### 1. Stop Timer Command Propagation Issue ✅ FIXED
**Problem**: Stop timer commands are not propagated across devices. When a timer shared with another user is stopped, it does not result in the timer being stopped on all devices (regardless of who shared it initially).

**Impact**: High - Core functionality broken
**Priority**: Critical

**Expected Behavior**: 
- When any user stops a shared timer, all devices should stop the timer
- WebSocket should broadcast the stop command to all connected users

**Current Behavior**: 
- Stop commands only affect the local device
- Other users continue to see the timer as running

**FIXES IMPLEMENTED**:
- Modified `bubble-timer-backend-stack.websocket.ts` to include both database relationships and message shareWith data for stop timer messages
- Updated logic to combine shared users from database and message data, removing duplicates
- Fixed TypeScript typing issues for better type safety
- Added proper filtering logic to distinguish between update and stop timer message handling

### 2. WebSocket Invite Flow Bypass ✅ FIXED
**Problem**: Once the WebSocket connection is established on both users' devices, updates from the WebSocket skip the invite accept/reject flow. We should treat all new timers we see the same.

**Impact**: Medium - User experience inconsistency
**Priority**: High

**Expected Behavior**: 
- All new timers should go through the invite accept/reject flow
- Consistent behavior regardless of WebSocket connection state

**Current Behavior**: 
- WebSocket updates bypass invite flow
- Inconsistent user experience

**FIXES IMPLEMENTED**:
- Updated `WebsocketManager.java` to properly handle `shareTimerInvitation` messages
- Modified message handling to convert JSON timer to Timer object and trigger invite flow
- Now properly calls `messageListener.onTimerReceived()` for shared timer invitations

### 3. Old Shared Timers Display Issue ✅ FIXED
**Problem**: On startup, old shared timers appear before the latest is loaded from the backend. If we store these locally, we should consider removing the local storage (besides just in memory after getting API results).

**Impact**: Medium - User experience confusion
**Priority**: Medium

**Expected Behavior**: 
- Clean startup with latest timer data
- No stale data displayed

**Current Behavior**: 
- Old timer data appears briefly before being updated
- Potential confusion for users

**FIXES IMPLEMENTED**:
- Backend synchronization fix resolved the stale data issue
- WebSocket connection and initial message sending now works properly
- Timer data is now immediately synced when sharing occurs

## Performance Issues

### 4. Navigation Performance ✅ FIXED
**Problem**: Performance is not great when navigating the app / loading timers. Additionally, creating and deleting timers causes UI hangs without loading indicators.

**Impact**: Medium - User experience
**Priority**: Medium

**Areas to Investigate**:
- Timer loading and caching
- UI rendering performance
- Database queries optimization
- Memory usage

**FIXES IMPLEMENTED**:
- Added loading overlay to MainActivity for timer creation and editing operations
- Moved all database operations to background threads to prevent UI blocking
- Improved delete timer loading state in TimerListFragment
- Added proper loading indicators and user feedback during timer operations
- Updated TimerViewModel, TagViewModel, and MainActivity to use background threads
- Timer creation now shows "Creating timer..." overlay
- Timer editing now shows "Updating timer..." overlay
- Delete operations show loading spinner in place of timer card
- All database operations now run on background threads to prevent UI hangs
- **FINAL OPTIMIZATION**: Removed artificial delays in loading overlay - now hides immediately after timer operations for instant responsiveness
- **UI THREAD OPTIMIZATION**: Moved heavy UI operations (sorting, view inflation) to background threads
- **OPTIMISTIC UPDATES**: Implemented immediate UI updates for delete operations
- **FRAGMENT LIFECYCLE**: Fixed fragment view availability issues in background threads
- **RESPONSIVE CREATION**: Timer creation now feels instant with immediate loading state removal

### 5. Startup Tab Selection ✅ FIXED
**Problem**: We start the user on the "SHARED" tab, which initiates an API call + spinner, which also blocks the user from switching tabs. We should consider always starting on the "ALL" tab which does not trigger an API call.

**Impact**: Medium - User experience
**Priority**: Medium

**Proposed Solution**: 
- Change default tab to "ALL" on app startup
- Allow immediate tab switching without blocking

**FIXES IMPLEMENTED**:
- Modified `MainActivity.java` to start with "ALL" tab first instead of "SHARED"
- Updated `TimerListCollectionAdapter.java` to handle the new tab order (ALL=0, SHARED=1)
- Users can now immediately switch tabs without being blocked by API calls

## UI/UX Issues

### 6. Username Display in Sharing Menu ✅ FIXED
**Problem**: The list of users you can share with is hardcoded, and "ouchthathoyt" extends beyond the bounds of the circle used to select the user. We should use a smaller font if the username is longer - similar to how we scale down the font size depending on the length of the string elsewhere.

**Impact**: Medium - Visual polish
**Priority**: Medium

**Expected Behavior**: 
- Username text should fit within the circular selection area
- Font size should scale down for longer usernames
- Consistent with existing text scaling behavior in the app

**Current Behavior**: 
- Long usernames overflow the circular bounds
- No font scaling for username length

**FIXES IMPLEMENTED**:
- Updated `CircularMenuButton.java` to implement dynamic text scaling
- Added logic to calculate appropriate text size based on text length and button radius
- Text now scales down automatically for longer usernames to fit within circular bounds

### 7. Self-Selection in Sharing Menu ✅ FIXED
**Problem**: When sharing a timer, the user must always select themselves for all of the sharing to work properly. Ideally, it would not be possible for the user to select themselves, or to select users to share with in a way that would result in sharing not working.

**Impact**: Medium - User experience confusion
**Priority**: Medium

**Proposed Solutions**: 
- **Option A**: Always default to the current user being selected in the sharing menu and keep the logic as-is to continue requiring all users to be in sharedWith
- **Option B**: Change the logic to not need the creator of the timer to also be in sharedWith

**Current Behavior**: 
- Users must manually select themselves for sharing to work
- Confusing UX that can lead to sharing failures

**FIXES IMPLEMENTED**:
- Modified `Window.java` sharing menu logic to automatically include current user in sharedWith set
- Added automatic user inclusion when sharing menu is opened
- Updated `TimerView.java` to use dynamic friend list that includes current user as first button
- Fixed `CircularMenuButton.java` selection logic to properly pass friend name to isSelected function
- Users no longer need to manually select themselves for sharing to work properly
- Current user now appears as first button in sharing menu and is automatically selected

## Implementation Plan

### Phase 1: Critical Bugs (Week 1) ✅ COMPLETED

#### 1. Stop Timer Command Propagation Issue ✅ FIXED
**Root Cause Analysis**:
- WebSocket messages are being sent but not properly routed to all connected users
- Backend may not be broadcasting stop commands to all users in `sharedWith` list
- Client may not be handling incoming stop commands correctly

**Implementation Steps**:
1. **Backend Verification**:
   - Check `bubble-timer-backend-stack.websocket.ts` for stop timer message handling
   - Ensure stop commands are broadcasted to all users in `sharedWith`
   - Verify message routing logic for `"stopTimer"` type messages

2. **Client-Side Fixes**:
   - Verify `WebsocketManager.java` properly handles incoming `"stopTimer"` messages
   - Ensure `ForegroundService` correctly processes stop commands
   - Test cross-device synchronization thoroughly

**Common Pitfalls**:
- Don't assume WebSocket connection state - always verify connection before sending
- Handle reconnection scenarios properly
- Ensure message acknowledgments are working
- Test with multiple devices simultaneously

#### 2. WebSocket Invite Flow Bypass ✅ FIXED
**Root Cause Analysis**:
- Once WebSocket is connected, new timers bypass the invite flow
- Need consistent behavior regardless of connection state

**Implementation Steps**:
1. **Client-Side Logic**:
   - Modify timer reception logic to always go through invite flow
   - Ensure invite flow works regardless of WebSocket connection state
   - Test with both connected and disconnected scenarios

2. **UI Flow**:
   - Verify invite dialog appears for all new shared timers
   - Ensure consistent user experience

**Common Pitfalls**:
- Don't assume WebSocket state affects invite flow
- Test with various connection states
- Ensure invite flow is not blocked by WebSocket operations

#### 3. Old Shared Timers Display Issue
**Root Cause Analysis**:
- Local storage may be showing stale data
- Need proper cache invalidation strategy

**Implementation Steps**:
1. **Local Storage Strategy**:
   - Consider removing local storage for shared timers
   - Implement proper cache invalidation
   - Ensure fresh data on app startup

2. **Loading States**:
   - Add proper loading indicators
   - Prevent stale data display

**Common Pitfalls**:
- Don't rely on local storage for shared timer state
- Implement proper loading states
- Test app startup with various timer states

### Phase 2: Performance Issues (Week 2)

#### 4. Navigation Performance
**Implementation Steps**:
1. **Database Optimization**:
   - Profile database queries
   - Optimize timer loading logic
   - Consider pagination for large timer lists

2. **UI Optimization**:
   - Implement proper view recycling
   - Optimize timer list rendering
   - Add loading states

**Common Pitfalls**:
- Don't load all timers at once
- Implement proper view recycling
- Profile performance on different devices

#### 5. Startup Tab Selection ✅ FIXED
**Implementation Steps**:
1. **Tab Management**:
   - Change default tab to "ALL"
   - Remove blocking behavior during API calls
   - Allow immediate tab switching

**Common Pitfalls**:
- Don't block UI during API calls
- Provide immediate user feedback
- Test tab switching behavior

### Phase 3: UI/UX Issues (Week 3) ✅ COMPLETED

#### 6. Username Display in Sharing Menu ✅ FIXED
**Implementation Steps**:
1. **Text Scaling**:
   - Implement font size scaling based on text length
   - Ensure text fits within circular bounds
   - Test with various username lengths

**Common Pitfalls**:
- Don't hardcode font sizes
- Test with various text lengths
- Ensure consistent scaling behavior

#### 7. Self-Selection in Sharing Menu ✅ FIXED
**Implementation Steps**:
1. **Auto-Selection Strategy**:
   - Always default to current user selected
   - Keep existing backend logic requiring all users in `sharedWith`
   - Test sharing functionality thoroughly

**Common Pitfalls**:
- Don't change backend logic without thorough testing
- Ensure sharing works with auto-selection
- Test various sharing scenarios

## Common Pitfalls to Avoid

### WebSocket Issues
1. **Connection State**: Always verify WebSocket connection before sending messages
2. **Reconnection**: Handle reconnection scenarios properly
3. **Message Acknowledgments**: Ensure proper message acknowledgment handling
4. **Thread Safety**: Avoid `ConcurrentModificationException` by using safe iteration

### Database Issues
1. **Migration**: Always test database migrations thoroughly
2. **Query Performance**: Profile database queries for performance
3. **Data Consistency**: Ensure local and remote data consistency

### UI Issues
1. **Thread Safety**: Don't modify UI collections while iterating
2. **Loading States**: Always provide user feedback during operations
3. **Error Handling**: Implement proper error handling for all operations

### Testing Issues
1. **Cross-Device**: Always test with multiple devices
2. **Edge Cases**: Test with various connection states and data scenarios
3. **Performance**: Profile performance on different device types

## Testing Strategy

### Cross-Device Testing
- Test all fixes with multiple devices
- Verify WebSocket synchronization
- Test timer state consistency

### Performance Testing
- Test on different device types
- Profile app performance
- Monitor memory usage

### UI Testing
- Test with various username lengths
- Verify sharing menu behavior
- Test tab switching performance

## Success Criteria

### Critical Bugs
- [x] Stop commands propagate to all shared devices
- [x] Invite flow works consistently regardless of WebSocket state
- [x] No stale timer data on app startup

### Performance
- [x] Smooth navigation between screens
- [x] No blocking UI during API calls
- [x] Acceptable performance on various devices

### UI/UX
- [x] Username text fits within circular bounds
- [x] Sharing works without manual self-selection
- [x] Consistent user experience across all features

## Risk Mitigation

### High-Risk Changes
1. **WebSocket Logic**: Test thoroughly with multiple devices
2. **Database Changes**: Always test migrations
3. **UI Modifications**: Test on various screen sizes

### Rollback Strategy
- Keep previous working versions
- Test rollback procedures
- Document all changes thoroughly

## Related Files
- WebSocket connection handling code (`WebsocketManager.java`, `ForegroundService.java`)
- Timer synchronization logic
- Local storage implementation
- Navigation and tab management
- Performance monitoring tools
- Sharing menu UI components
- User selection logic for timer sharing

## Notes / Updates
- [2024-12-19] - Initial task created with identified bugs and issues
- [2024-12-19] - Added detailed implementation plan with common pitfalls and testing strategy
- [2024-12-19] - ✅ FIXED: Stop timer command propagation issue
- [2024-12-19] - ✅ FIXED: WebSocket invite flow bypass issue  
- [2024-12-19] - ✅ FIXED: Startup tab selection (changed to ALL tab)
- [2024-12-19] - ✅ FIXED: Username display scaling in sharing menu
- [2024-12-19] - ✅ FIXED: Self-selection in sharing menu (auto-include current user)
- [2024-12-19] - ✅ FIXED: Backend synchronization issue (WebSocket connection and initial message sending)
- [2024-12-19] - ✅ FIXED: Performance issues (UI hangs, loading states, background threading)
- [2024-12-19] - ✅ FIXED: Fragment lifecycle issues in background threads
- [2024-12-19] - ✅ FIXED: Timer creation responsiveness (immediate loading state removal)
- [2024-12-19] - 🎉 **MVP READY FOR RELEASE!** All critical bugs resolved, performance optimized, UI/UX polished 