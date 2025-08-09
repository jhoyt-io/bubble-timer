# UX Improvements: Themes and Reactions

## Description
Implement comprehensive UX improvements including shared timer themes, emoji reactions, and enhanced interaction capabilities. This creates a more engaging and personalized collaborative timer experience with visual customization and social interaction features.

## Status
- [x] Not Started
- [ ] In Progress
- [ ] Blocked
- [ ] Completed

## Initial Prompt
User requested: "UX Improvements - themes for timer tabs (themes should be shared, too), reactions and other ways to interact on a timer"

This addresses the need for more engaging and personalized timer experiences, allowing users to customize appearance and interact socially with shared timers.

## Implementation Plan

### Phase 1: Shared Theme System
- **Theme Architecture**
  - Create `TimerTheme` entity with color schemes, gradients, and effects
  - Implement theme sharing via WebSocket (themes sync across devices)
  - Store theme preferences in database with timer association
  - Support predefined themes and custom color picker

- **Theme Components**
  - **Color Schemes**: Primary, secondary, accent colors for bubble and UI
  - **Visual Effects**: Gradient backgrounds, border styles, shadow effects
  - **Animations**: Pulse effects, rotation speeds, countdown animations
  - **Typography**: Font weights, sizes for timer display

### Phase 2: Theme Application System
- **Bubble Overlay Theming**
  - Modify `CircularMenuLayout` to apply theme colors and effects
  - Dynamic bubble colors based on timer theme
  - Themed countdown circle colors and animations
  - Custom background gradients and border effects

- **Main Activity Integration**
  - Apply themes to timer list items in `MainActivity`
  - Themed action buttons and UI elements
  - Consistent theme application across all timer views

### Phase 3: Emoji Reactions System
- **Reaction Infrastructure**
  - Create `TimerReaction` entity (emoji, user, timestamp, timerId)
  - Implement reaction WebSocket messages for real-time sync
  - Reaction overlay display on bubbles and main activity
  - Recent reactions list with user attribution

- **Reaction UI Components**
  - **Reaction Picker**: Emoji selection interface for quick reactions
  - **Reaction Display**: Show recent reactions on timer bubbles
  - **Reaction History**: View all reactions for a timer
  - **Quick React**: Tap-and-hold gesture for instant emoji reactions

### Phase 4: Enhanced Interaction Features
- **Timer Social Features**
  - **Comments**: Text messages attached to shared timers
  - **Status Updates**: Share what you're working on during timer
  - **Achievement Badges**: Streak counters, completion celebrations
  - **Timer Templates**: Save and share favorite timer configurations

- **Collaborative Controls**
  - **Pause Votes**: Collaborative pausing for shared timers
  - **Time Extensions**: Request/approve additional time
  - **Focus Mode**: Hide distractions during timer sessions
  - **Group Challenges**: Compete or collaborate on timer goals

### Phase 5: Customization & Settings
- **User Preferences**
  - Default theme selection per user
  - Reaction notification settings
  - Custom theme creation and sharing
  - Privacy controls for reactions and comments

- **Timer-Specific Settings**
  - Per-timer theme overrides
  - Reaction permissions (all users, owner only, etc.)
  - Social feature toggles (comments, reactions, status)

## Technical Requirements

### Database Schema Updates
```sql
-- Timer themes
CREATE TABLE timer_themes (
    id VARCHAR PRIMARY KEY,
    name VARCHAR,
    primary_color VARCHAR,
    secondary_color VARCHAR,
    accent_color VARCHAR,
    gradient_type VARCHAR,
    created_by VARCHAR,
    is_shared BOOLEAN
);

-- Timer reactions
CREATE TABLE timer_reactions (
    id VARCHAR PRIMARY KEY,
    timer_id VARCHAR,
    user_id VARCHAR,
    emoji VARCHAR,
    timestamp DATETIME,
    reaction_type VARCHAR
);

-- Timer comments (future)
CREATE TABLE timer_comments (
    id VARCHAR PRIMARY KEY,
    timer_id VARCHAR,
    user_id VARCHAR,
    message TEXT,
    timestamp DATETIME
);
```

### WebSocket Message Extensions
- **Theme sync**: `themeUpdated`, `themeShared`
- **Reactions**: `reactionAdded`, `reactionRemoved`
- **Social features**: `commentAdded`, `statusUpdated`

### UI Component Updates
- **CircularMenuLayout**: Dynamic theming and reaction overlays
- **MainActivity**: Themed timer list items
- **ReactionPicker**: Emoji selection component
- **ThemeSelector**: Color picker and theme management

## User Experience Flows

### Theme Sharing Flow
1. User customizes timer theme (colors, effects)
2. Theme automatically syncs to all shared timer participants
3. All users see timer with updated theme in real-time
4. Theme preferences saved for future timer creation

### Reaction Flow
1. User sees shared timer (bubble or main activity)
2. Tap-and-hold or dedicated reaction button
3. Emoji picker appears with recently used and popular reactions
4. Selected emoji appears on timer with user attribution
5. Reaction syncs to all shared timer participants via WebSocket

### Social Interaction Flow
1. User working on shared "Study Session" timer
2. Adds 🔥 reaction to show motivation
3. Other users see reaction and add their own (👍, 💪)
4. Quick status update: "Working on math homework"
5. Timer becomes more engaging and collaborative

## Integration Points

### Existing Systems
- **WebsocketManager**: Enhanced for theme and reaction sync
- **CircularMenuLayout**: Dynamic theming and reaction display
- **TimerConverter**: Handle theme and reaction data mapping
- **ForegroundService**: Coordinate social feature notifications

### New Components
- **ThemeManager**: Theme application and sync coordination
- **ReactionManager**: Emoji reaction handling and display
- **SocialInteractionManager**: Comments, status, collaborative features
- **CustomizationRepository**: Theme and preference persistence

## Design Considerations

### Performance
- **Efficient rendering**: Minimize redraws for theme changes
- **Memory management**: Cache themes and reactions appropriately
- **Network optimization**: Batch reaction updates, compress theme data

### Accessibility
- **Color contrast**: Ensure readable text on custom theme backgrounds
- **Screen readers**: Proper descriptions for reactions and themes
- **Motor accessibility**: Large touch targets for reaction selection

### Privacy & Control
- **Reaction permissions**: Control who can react to your timers
- **Theme sharing**: Option to keep themes private
- **Social feature toggles**: Disable reactions/comments per timer

## Testing Strategy

### Unit Tests
- `ThemeManager` color application and validation
- `ReactionManager` emoji handling and sync logic
- Theme data serialization and WebSocket transmission

### Integration Tests
- End-to-end theme sharing across devices
- Reaction sync and display coordination
- Theme persistence and restoration

### Visual Testing
- Theme application across different screen sizes
- Color contrast and accessibility compliance
- Reaction display in various bubble states

### Manual Testing
- Cross-device theme synchronization
- Reaction performance under high usage
- Custom theme creation and sharing workflow

## Notes / Updates
- [2025-01-08] - Initial task created based on user requirements
- Should integrate with existing overlay system for seamless experience
- Consider gamification elements (streaks, achievements) for engagement
- Plan for A/B testing different reaction UI approaches

## Related Files
- `app/src/main/java/io/jhoyt/bubbletimer/CircularMenuLayout.java` - Theme application target
- `app/src/main/java/io/jhoyt/bubbletimer/WebsocketManager.java` - Social feature sync
- `app/src/main/java/io/jhoyt/bubbletimer/MainActivity.java` - Theme integration
- `docs/FEATURE_EXTENSION_PATTERNS.md` - UX customization patterns
- `docs/ARCHITECTURE.md` - Component integration guidelines
