# Bubble Timer

A collaborative timer app with real-time sharing and overlay bubbles.

## 🚀 Quick Start for Developers

**New to this codebase?** Start here for fast context:

1. **📖 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Core data flows and component responsibilities
2. **🎬 [docs/demo-video.mov](docs/demo-video.mov)** - Visual walkthrough of key features
3. **📸 [docs/screenshots/](docs/screenshots/)** - Key UI states and workflows
4. **⚡ [docs/QUICK_REFERENCE.md](docs/QUICK_REFERENCE.md)** - Debugging commands and common patterns

## 📁 Documentation

All development documentation is in the [`docs/`](docs/) directory:

- **[docs/README.md](docs/README.md)** - Documentation overview and navigation
- **[docs/FEATURE_EXTENSION_PATTERNS.md](docs/FEATURE_EXTENSION_PATTERNS.md)** - How to add new features
- **[tasks/active/](tasks/active/)** - Current work in progress

## 🏗 Architecture Overview

```
MainActivity ←→ ActiveTimerViewModel ←→ ActiveTimerRepository ←→ Database
    ↓                    ↓                           ↓
ForegroundService ←→ WebsocketManager ←→ Backend ←→ Other Devices
    ↓
OverlayWindows (bubble UI)
```

## 🔧 Development

### Building
```bash
./gradlew assembleDebug
```

### Testing
```bash
./gradlew test
./scripts/run-tests.sh  # Custom test runner
```

### Common Tasks
- **Adding features**: See `docs/FEATURE_EXTENSION_PATTERNS.md`
- **Debugging WebSocket**: See `docs/QUICK_REFERENCE.md`
- **Understanding overlays**: See `docs/ARCHITECTURE.md`

## 📱 Key Features

- **Real-time timer sharing** between users
- **Overlay bubble interface** for background timer access
- **WebSocket synchronization** for live updates
- **Collaborative timer management** with acknowledgments

---

**💡 Pro tip**: The Cursor rules in `.cursorrules` automatically guide AI assistants to read the documentation first, making development more efficient.
