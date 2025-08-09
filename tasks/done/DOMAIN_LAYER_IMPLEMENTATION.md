# Domain Layer Implementation Plan

## Overview

This document outlines the implementation of Phase 1.1: Domain Layer Implementation. The domain layer will introduce clean architecture principles with proper separation of concerns, making the codebase more maintainable, testable, and flexible.

## Goals

1. **Introduce Clean Architecture**: Create a proper domain layer with use cases
2. **Improve Testability**: Domain logic should be easily unit testable
3. **Reduce Coupling**: Separate business logic from Android framework dependencies
4. **Enhance Maintainability**: Clear boundaries and responsibilities
5. **Prepare for Future**: Set foundation for data and presentation layer improvements

## Current State Analysis

### Existing Issues
- Business logic mixed with Android framework code
- Direct database access from ViewModels and Activities
- No clear separation between domain and data layers
- Difficult to test business logic in isolation
- Tight coupling between components

### Target State
- Pure domain entities and business logic
- Use cases that orchestrate business operations
- Repository interfaces defining data contracts
- Domain exceptions for error handling
- Framework-agnostic domain layer

## Implementation Strategy

### Phase 1.1.1: Domain Entities (Week 1)

#### **Create Domain Entities**
**Goal**: Define pure domain entities without Android dependencies

**Files to create**:
```
app/src/main/java/io/jhoyt/bubbletimer/domain/entities/
├── Timer.java
├── ActiveTimer.java
├── SharedTimer.java
├── User.java
└── TimerState.java
```

**Key Principles**:
- No Android imports (no `android.*` packages)
- Pure Java/Kotlin objects
- Immutable where possible
- Rich domain methods
- No database annotations

**Example Implementation**:
```java
// Domain entity - no Android dependencies
public class Timer {
    private final String id;
    private final String name;
    private final String userId;
    private final Duration totalDuration;
    private final Duration remainingDuration;
    private final LocalDateTime endTime;
    private final Set<String> sharedWith;
    private final Set<String> tags;
    private final TimerState state;

    // Constructor, getters, equals, hashCode
    
    // Domain methods
    public boolean isPaused() { return state == TimerState.PAUSED; }
    public boolean isRunning() { return state == TimerState.RUNNING; }
    public boolean isExpired() { return remainingDuration.isNegative() || remainingDuration.isZero(); }
    public Duration getElapsedTime() { return totalDuration.minus(remainingDuration); }
    public boolean isShared() { return !sharedWith.isEmpty(); }
    
    // Factory methods
    public static Timer create(String name, String userId, Duration duration, Set<String> tags) {
        return new Timer(
            UUID.randomUUID().toString(),
            name,
            userId,
            duration,
            duration,
            LocalDateTime.now().plus(duration),
            new HashSet<>(),
            tags,
            TimerState.RUNNING
        );
    }
    
    public Timer pause() {
        return new Timer(
            id, name, userId, totalDuration,
            getRemainingDuration(),
            null, // endTime becomes null when paused
            sharedWith, tags, TimerState.PAUSED
        );
    }
    
    public Timer resume() {
        return new Timer(
            id, name, userId, totalDuration,
            null, // remainingDuration becomes null when running
            LocalDateTime.now().plus(remainingDuration),
            sharedWith, tags, TimerState.RUNNING
        );
    }
}
```

#### **Create Domain Enums**
```java
public enum TimerState {
    RUNNING, PAUSED, EXPIRED, STOPPED
}

public enum ShareStatus {
    PENDING, ACCEPTED, REJECTED
}
```

### Phase 1.1.2: Repository Interfaces (Week 1)

#### **Define Repository Contracts**
**Goal**: Create interfaces that define data access contracts

**Files to create**:
```
app/src/main/java/io/jhoyt/bubbletimer/domain/repositories/
├── TimerRepository.java
├── ActiveTimerRepository.java
├── SharedTimerRepository.java
└── UserRepository.java
```

**Key Principles**:
- Framework-agnostic interfaces
- Return domain entities, not data models
- Define clear contracts
- Include error handling

**Example Implementation**:
```java
public interface TimerRepository {
    // Query methods
    List<Timer> getAllTimers();
    List<Timer> getTimersByTag(String tag);
    Timer getTimerById(String id);
    List<Timer> getActiveTimers();
    
    // Command methods
    void saveTimer(Timer timer);
    void updateTimer(Timer timer);
    void deleteTimer(String id);
    void deleteAllTimers();
    
    // Specialized methods
    void startTimer(String id);
    void stopTimer(String id);
    void pauseTimer(String id);
    void resumeTimer(String id);
    
    // Observable methods (for reactive programming)
    Observable<List<Timer>> observeAllTimers();
    Observable<List<Timer>> observeActiveTimers();
    Observable<Timer> observeTimerById(String id);
}
```

### Phase 1.1.3: Use Cases (Week 2)

#### **Create Business Logic Use Cases**
**Goal**: Implement business logic in dedicated use case classes

**Files to create**:
```
app/src/main/java/io/jhoyt/bubbletimer/domain/usecases/
├── timer/
│   ├── StartTimerUseCase.java
│   ├── StopTimerUseCase.java
│   ├── PauseTimerUseCase.java
│   ├── ResumeTimerUseCase.java
│   ├── GetActiveTimersUseCase.java
│   ├── GetTimerByIdUseCase.java
│   └── UpdateTimerUseCase.java
├── sharing/
│   ├── ShareTimerUseCase.java
│   ├── AcceptSharedTimerUseCase.java
│   ├── RejectSharedTimerUseCase.java
│   └── GetSharedTimersUseCase.java
└── user/
    ├── GetCurrentUserUseCase.java
    └── UpdateUserPreferencesUseCase.java
```

**Key Principles**:
- Single responsibility per use case
- Inject dependencies (repositories)
- Handle business rules and validation
- Return domain entities or results
- Include error handling

**Example Implementation**:
```java
public class StartTimerUseCase {
    private final TimerRepository timerRepository;
    private final ActiveTimerRepository activeTimerRepository;
    
    @Inject
    public StartTimerUseCase(TimerRepository timerRepository, 
                           ActiveTimerRepository activeTimerRepository) {
        this.timerRepository = timerRepository;
        this.activeTimerRepository = activeTimerRepository;
    }
    
    public Result<Timer> execute(String name, Duration duration, Set<String> tags, String userId) {
        try {
            // Validate inputs
            if (name == null || name.trim().isEmpty()) {
                return Result.failure(new ValidationException("Timer name cannot be empty"));
            }
            if (duration == null || duration.isNegative() || duration.isZero()) {
                return Result.failure(new ValidationException("Timer duration must be positive"));
            }
            if (userId == null || userId.trim().isEmpty()) {
                return Result.failure(new ValidationException("User ID cannot be empty"));
            }
            
            // Create domain entity
            Timer timer = Timer.create(name, userId, duration, tags);
            
            // Save to repositories
            timerRepository.saveTimer(timer);
            activeTimerRepository.addActiveTimer(timer);
            
            return Result.success(timer);
        } catch (Exception e) {
            return Result.failure(new TimerException("Failed to start timer", e));
        }
    }
}

public class PauseTimerUseCase {
    private final TimerRepository timerRepository;
    private final ActiveTimerRepository activeTimerRepository;
    
    @Inject
    public PauseTimerUseCase(TimerRepository timerRepository, 
                           ActiveTimerRepository activeTimerRepository) {
        this.timerRepository = timerRepository;
        this.activeTimerRepository = activeTimerRepository;
    }
    
    public Result<Timer> execute(String timerId) {
        try {
            // Get current timer
            Timer currentTimer = timerRepository.getTimerById(timerId);
            if (currentTimer == null) {
                return Result.failure(new TimerNotFoundException("Timer not found: " + timerId));
            }
            
            // Check if already paused
            if (currentTimer.isPaused()) {
                return Result.failure(new InvalidTimerStateException("Timer is already paused"));
            }
            
            // Pause the timer
            Timer pausedTimer = currentTimer.pause();
            
            // Update repositories
            timerRepository.updateTimer(pausedTimer);
            activeTimerRepository.updateActiveTimer(pausedTimer);
            
            return Result.success(pausedTimer);
        } catch (Exception e) {
            return Result.failure(new TimerException("Failed to pause timer", e));
        }
    }
}
```

### Phase 1.1.4: Domain Exceptions (Week 2)

#### **Create Domain-Specific Exceptions**
**Goal**: Define clear error types for domain operations

**Files to create**:
```
app/src/main/java/io/jhoyt/bubbletimer/domain/exceptions/
├── TimerException.java
├── TimerNotFoundException.java
├── InvalidTimerStateException.java
├── ValidationException.java
├── NetworkException.java
├── AuthenticationException.java
└── DomainException.java
```

**Example Implementation**:
```java
public abstract class DomainException extends Exception {
    private final String errorCode;
    
    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

public class TimerNotFoundException extends DomainException {
    public TimerNotFoundException(String message) {
        super(message, "TIMER_NOT_FOUND");
    }
}

public class InvalidTimerStateException extends DomainException {
    public InvalidTimerStateException(String message) {
        super(message, "INVALID_TIMER_STATE");
    }
}
```

### Phase 1.1.5: Result Pattern (Week 2)

#### **Implement Result Pattern for Error Handling**
**Goal**: Provide consistent error handling across use cases

**Files to create**:
```
app/src/main/java/io/jhoyt/bubbletimer/domain/core/
├── Result.java
├── Either.java
└── ValidationResult.java
```

**Example Implementation**:
```java
public class Result<T> {
    private final T data;
    private final Exception error;
    private final boolean isSuccess;
    
    private Result(T data, Exception error, boolean isSuccess) {
        this.data = data;
        this.error = error;
        this.isSuccess = isSuccess;
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(data, null, true);
    }
    
    public static <T> Result<T> failure(Exception error) {
        return new Result<>(null, error, false);
    }
    
    public boolean isSuccess() {
        return isSuccess;
    }
    
    public boolean isFailure() {
        return !isSuccess;
    }
    
    public T getData() {
        if (!isSuccess) {
            throw new IllegalStateException("Cannot get data from failed result");
        }
        return data;
    }
    
    public Exception getError() {
        if (isSuccess) {
            throw new IllegalStateException("Cannot get error from successful result");
        }
        return error;
    }
    
    public T getDataOrThrow() throws Exception {
        if (isSuccess) {
            return data;
        } else {
            throw error;
        }
    }
    
    public <R> Result<R> map(Function<T, R> mapper) {
        if (isSuccess) {
            return Result.success(mapper.apply(data));
        } else {
            return Result.failure(error);
        }
    }
    
    public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        if (isSuccess) {
            return mapper.apply(data);
        } else {
            return Result.failure(error);
        }
    }
}
```

## Migration Strategy

### Step 1: Create Domain Layer (Weeks 1-2)
1. Create domain entities without Android dependencies
2. Define repository interfaces
3. Implement use cases with business logic
4. Add domain exceptions and result pattern

### Step 2: Update Existing Code (Week 3)
1. Update ViewModels to use use cases instead of direct repository access
2. Update Activities to use use cases for business operations
3. Maintain backward compatibility during transition

### Step 3: Testing (Week 4)
1. Create unit tests for all use cases
2. Test domain entities and business logic
3. Verify error handling and edge cases
4. Ensure no regressions in existing functionality

## Benefits

### **1. Improved Testability**
- Domain logic can be tested without Android framework
- Use cases can be unit tested in isolation
- Clear separation of concerns

### **2. Better Maintainability**
- Business logic is centralized in use cases
- Clear boundaries between layers
- Easier to understand and modify

### **3. Enhanced Flexibility**
- Domain layer is framework-agnostic
- Easy to swap implementations
- Clear contracts between layers

### **4. Reduced Coupling**
- ViewModels no longer directly access repositories
- Business logic is separated from UI logic
- Dependencies are clearly defined

## Success Criteria

### **Code Quality**
- [ ] Domain entities have no Android dependencies
- [ ] All use cases are unit testable
- [ ] Repository interfaces are clearly defined
- [ ] Error handling is consistent across use cases

### **Test Coverage**
- [ ] >90% test coverage for domain layer
- [ ] All use cases have unit tests
- [ ] Edge cases and error scenarios are tested
- [ ] No regressions in existing functionality

### **Integration**
- [ ] Existing ViewModels updated to use use cases
- [ ] All existing functionality continues to work
- [ ] Performance is maintained or improved
- [ ] No breaking changes to public APIs

## Next Steps

1. **Review this plan** with the development team
2. **Set up development environment** for domain layer
3. **Begin implementation** with domain entities
4. **Create comprehensive test suite** for domain logic
5. **Gradually migrate** existing code to use domain layer

## Timeline

- **Week 1**: Domain entities and repository interfaces
- **Week 2**: Use cases and domain exceptions
- **Week 3**: Integration with existing code
- **Week 4**: Testing and validation

This domain layer implementation will provide a solid foundation for the subsequent data and presentation layer improvements while maintaining all existing functionality.
