# Simplified Shared Timer Management

## Problem
The `SharedTimerManager` was adding unnecessary complexity with LiveData setup issues and websocket connection debugging difficulties. The `ForegroundService` was already successfully managing active timers and websocket connections via `WebsocketManager`.

## Solution
Removed `SharedTimerManager` entirely and integrated shared timer logic directly into `ForegroundService` using existing working patterns.

## Changes Made

### 1. Removed SharedTimerManager
- Deleted `SharedTimerManager.java` file
- Removed all SharedTimerManager dependencies from `ForegroundService`
- Removed SharedTimerManager references from `WebsocketManager`

### 2. Integrated Shared Timer Logic into ForegroundService
- Added simple `HashSet` tracking for shared timer IDs:
  - `sharedTimerIds` - all timers that are shared
  - `pendingInvitationIds` - timers pending acceptance
  - `activeSharedTimerIds` - actively shared timers
- Added `updateSharedTimerTracking()` method that processes timer changes from the existing repository observer
- Added `checkWebsocketConnectionNeeds()` method that manages websocket connections based on shared timer state

### 3. Simplified WebsocketManager
- Removed SharedTimerManager dependency and related methods
- Replaced SharedTimerManager notifications with simple logging
- Updated comments to reflect that connection management is now handled by ForegroundService

## Benefits

1. **Simplified Architecture**: Removed an entire layer of complexity
2. **Working Patterns**: Uses the existing successful patterns from ForegroundService
3. **No LiveData Issues**: Eliminates the LiveData setup problems that were causing debugging difficulties
4. **On-Demand Connection**: Still maintains the on-demand websocket connection behavior
5. **Easier Debugging**: Fewer moving parts and clearer data flow

## How It Works

1. **Timer Changes**: The existing repository observer in ForegroundService triggers `updateSharedTimerTracking()`
2. **Shared Timer Detection**: Checks each timer's `sharedWith` field to identify shared timers
3. **Connection Management**: `checkWebsocketConnectionNeeds()` determines if websocket connection is needed
4. **On-Demand Mode**: Only connects when there are shared timers, disconnects when none exist

## Testing
The simplified approach should be much easier to test and debug since:
- All logic is in one place (ForegroundService)
- Uses existing working patterns
- No complex LiveData interactions
- Clear logging for debugging

## Next Steps
After confirming this works, we can:
1. Add more sophisticated shared timer state tracking if needed
2. Optimize the connection logic further
3. Add better error handling and retry logic
4. Consider extracting shared timer logic into a separate class if it grows too large 