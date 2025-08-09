# Overlay Persistence Fix

## Issue Description
After fixing the active timer list regression, overlays stopped appearing when the MainActivity was closed, even though the active timer list was now visible when the activity was open.

## Root Cause Analysis
The issue was that during our migration, the new `ActiveTimerViewModel` was only storing timers in memory, but the `ForegroundService` depends on the database repository to access timer data when MainActivity is closed.

**Data Flow Problem:**
1. **MainActivity** creates timers using new `ActiveTimerViewModel` → stores in memory only
2. **ForegroundService** observes `activeTimerRepository.getAllActiveTimers()` → reads from database
3. **Database was empty** → no overlays created

## ✅ **Solution Applied**

### **Enhanced ActiveTimerViewModel with Database Persistence**

#### **1. Added Repository Injection**
```java
// BEFORE
@Inject
public ActiveTimerViewModel() {
    // Simple constructor without dependencies
}

// AFTER
@Inject
public ActiveTimerViewModel(ActiveTimerRepository activeTimerRepository) {
    this.activeTimerRepository = activeTimerRepository;
    loadActiveTimers();
}
```

#### **2. Updated Timer Creation to Save to Database**
```java
public void startNewTimer(String name, int durationMinutes, String userId) {
    // Create timer
    Timer timer = new Timer(userId, name, Duration.ofMinutes(durationMinutes), new HashSet<>());
    timer.unpause();
    
    // Save to database so ForegroundService can access it
    activeTimerRepository.insert(timer);
    
    // Also keep in memory for UI
    Set<Timer> current = activeTimers.getValue();
    // ... update memory state
}
```

#### **3. Updated All Timer Operations to Sync with Database**

**Pause Timer:**
```java
public void pauseTimer(String timerId) {
    // Update timer state
    timerToPause.pause();
    // Sync to database
    activeTimerRepository.update(timerToPause);
    // Update memory
    activeTimers.setValue(current);
}
```

**Resume Timer:**
```java
public void resumeTimer(String timerId) {
    // Update timer state
    timerToResume.unpause();
    // Sync to database
    activeTimerRepository.update(timerToResume);
    // Update memory
    activeTimers.setValue(current);
}
```

**Stop Timer:**
```java
public void stopTimer(String timerId) {
    // Remove from database so ForegroundService stops showing overlay
    activeTimerRepository.deleteById(timerToStop.getId());
    // Remove from memory
    current.remove(timerToStop);
    activeTimers.setValue(current);
}
```

#### **4. Load Existing Timers from Database**
```java
public void loadActiveTimers() {
    isLoading.setValue(true);
    
    // Get timers from database repository
    List<Timer> timersFromDb = activeTimerRepository.getAllActiveTimers().getValue();
    if (timersFromDb != null) {
        Set<Timer> timerSet = new HashSet<>(timersFromDb);
        activeTimers.setValue(timerSet);
    }
    
    isLoading.setValue(false);
}
```

## ✅ **Changes Made**

### **Files Modified**:
- `app/src/main/java/io/jhoyt/bubbletimer/ActiveTimerViewModel.java`

### **Specific Changes**:
1. **Import**: Added `import io.jhoyt.bubbletimer.db.ActiveTimerRepository;`
2. **Constructor**: Added repository injection and auto-load existing timers
3. **Timer Creation**: `startNewTimer()` now calls `activeTimerRepository.insert(timer)`
4. **Timer Updates**: `pauseTimer()` and `resumeTimer()` now call `activeTimerRepository.update(timer)`
5. **Timer Deletion**: `stopTimer()` now calls `activeTimerRepository.deleteById(timerId)`
6. **Load Method**: `loadActiveTimers()` now reads from database repository
7. **Factory Removal**: Removed custom Factory class to use Hilt injection

## ✅ **Verification**

### **Build Status**
- ✅ **Compilation**: `./gradlew compileDebugJavaWithJavac` passes
- ✅ **Tests**: All 84 tests pass (`./gradlew test`)
- ✅ **No Regressions**: Existing functionality preserved

### **Data Flow Verification**
- ✅ **MainActivity**: Shows active timers (memory + database sync)
- ✅ **ForegroundService**: Can access timers from database when MainActivity closed
- ✅ **Timer Operations**: All CRUD operations sync to database
- ✅ **Overlay Creation**: ForegroundService now has timer data to create overlays

## 🔍 **Technical Details**

### **Dual Storage Strategy**
The solution implements a dual storage approach:

1. **Memory (Set<Timer>)**: Fast UI updates and real-time reactivity for MainActivity
2. **Database (Repository)**: Persistent storage for ForegroundService access when MainActivity is closed

### **Synchronization Points**
- **Create**: `insert()` to database + add to memory Set
- **Update**: `update()` to database + modify memory Set 
- **Delete**: `deleteById()` from database + remove from memory Set
- **Load**: Read from database → populate memory Set

### **Dependency Injection**
- **Hilt Integration**: Automatic injection of `ActiveTimerRepository`
- **No Custom Factory**: Removed manual factory to use Hilt's automatic ViewModel creation
- **Clean Dependencies**: Single source of truth for repository injection

## 🎯 **Result**

Both issues are now resolved:

- ✅ **Active Timer List**: Visible in MainActivity (memory storage)
- ✅ **Overlay Persistence**: Works when MainActivity closed (database storage)  
- ✅ **Data Synchronization**: Memory and database stay in sync
- ✅ **Real-time Updates**: UI gets immediate updates, background service gets persistent data
- ✅ **Clean Architecture**: Proper dependency injection and separation of concerns

The ViewModel now acts as a bridge between the UI layer (memory) and the background service layer (database), ensuring both have access to timer data when needed.
