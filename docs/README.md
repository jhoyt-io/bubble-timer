# Bubble Timer Documentation

This directory contains focused documentation to get you productive quickly without burning through context tokens.

## 📖 Documentation Overview

### **🏗 [ARCHITECTURE.md](ARCHITECTURE.md)** ⭐ **Start Here**
- **Core data flows** and component responsibilities
- **Database schema** and WebSocket message patterns  
- **Critical gotchas** that cause bugs
- **Extension points** for planned features

### **⚡ [QUICK_REFERENCE.md](QUICK_REFERENCE.md)**
- **Key file locations** for different tasks
- **Debugging commands** for common issues
- **Data patterns** and performance tips
- **Configuration** settings and hardcoded values

### **🔧 [FEATURE_EXTENSION_PATTERNS.md](FEATURE_EXTENSION_PATTERNS.md)**
- **Specific patterns** for planned feature types
- **Reusable infrastructure** and integration points
- **Step-by-step guides** for notifications, stopwatch, friends, themes
- **Development workflow** and checklists

## 🎯 Quick Start for Different Tasks

### Adding a New Feature
1. Read `ARCHITECTURE.md` → Find relevant extension points
2. Check `FEATURE_EXTENSION_PATTERNS.md` → Follow the pattern for your feature type
3. Use `QUICK_REFERENCE.md` → Find key files and debugging commands

### Fixing a Bug
1. `QUICK_REFERENCE.md` → Use debugging commands to identify the issue
2. `ARCHITECTURE.md` → Understand the data flow involved
3. Check `../tasks/active/` → See if it's a known issue

### Understanding Data Flow
1. `ARCHITECTURE.md` → Core data flow diagrams
2. Key classes: `ActiveTimerViewModel` → `ForegroundService` → `WebsocketManager`
3. Common patterns: Repository observers, broadcast communication, overlay lifecycle

## 🚀 Planned Features (Context for Architecture)

### Near-term
- **Enhanced notifications**: Countdown circles, full-screen wake-up, push notifications
- **Acknowledgment model**: Shared timers require acknowledgment instead of immediate deletion
- **Stopwatch support**: Reuse sharing infrastructure for elapsed-time tracking

### Medium-term  
- **Friend list system**: Replace hardcoded users with dynamic friend management
- **UX improvements**: Configurable themes, radial menu redesign, custom actions
- **Advanced sharing**: Emoji reactions, group timers, timer templates

## 📁 Related Documentation

### Implementation Progress
- `../tasks/active/` - Current work in progress
- `../tasks/done/` - Completed major changes with summaries

### Code-specific Guides
- `DOMAIN_LAYER_USAGE_GUIDE.md` - Domain object patterns
- `NEW_OVERLAY_SETUP_COMPLETE.md` - Overlay system overview
- `OVERLAY_MANUAL_TESTING_CHECKLIST.md` - Testing procedures

### Visual Documentation
- `demo-video.mov` - Screen recording showing core features and interactions
- `screenshots/` - Key UI states and workflows:
  - `01-main-activity-with-timers.png` - Main screen with active timer list
  - `02-small-bubble-overlay.png` - Small bubble overlay with shared-by info
  - `03-expanded-bubble-menu.png` - Expanded bubble with circular menu
  - `04-sharing-flow.png` - Timer sharing and invitation workflow
  - `05-multi-device-sync.png` - Real-time sync across devices

## 💡 Documentation Philosophy

This documentation prioritizes:
- **High signal-to-noise ratio**: Maximum useful information per token
- **Extension-focused**: How to build on existing systems
- **Problem-oriented**: Organized around common development tasks
- **Context-efficient**: Quick reference without deep dives

The goal is to get you 80% of the context you need in 200-400 tokens instead of rediscovering the architecture every time.
