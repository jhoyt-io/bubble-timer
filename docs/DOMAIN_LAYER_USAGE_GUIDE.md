# Domain Layer Usage Guide

## Overview

The domain layer provides a clean, framework-agnostic foundation for business logic in the Bubble Timer app. This guide explains how to use the domain layer components effectively.

## Table of Contents

1. [Domain Entities](#domain-entities)
2. [Use Cases](#use-cases)
3. [Result Pattern](#result-pattern)
4. [Error Handling](#error-handling)
5. [Dependency Injection](#dependency-injection)
6. [Testing](#testing)
7. [Migration Guide](#migration-guide)

## Domain Entities

### Timer Entity

The `Timer` entity represents a timer with rich business logic:

```java
// Create a new timer
Timer timer = Timer.create("Work Session", "user123", Duration.ofMinutes(25), Set.of("work", "focus"));

// Check timer state
if (timer.isRunning()) {
    Duration remaining = timer.getRemainingDuration();
    // Handle running timer
}

// State transitions
Timer pausedTimer = timer.pause();
Timer resumedTimer = pausedTimer.resume();
Timer stoppedTimer = timer.stop();

// Business operations
Timer sharedTimer = timer.shareWith("user456");
Timer taggedTimer = timer.addTag("important");
```

### TimerState Enum

```java
TimerState state = timer.getState();

switch (state) {
    case RUNNING:
        // Timer is actively counting down
        break;
    case PAUSED:
        // Timer is paused, can be resumed
        break;
    case EXPIRED:
        // Timer has finished
        break;
    case STOPPED:
        // Timer was manually stopped
        break;
}
```

## Use Cases

### StartTimerUseCase

Creates and starts a new timer with validation:

```java
@Inject
StartTimerUseCase startTimerUseCase;

// Start timer with full parameters
Result<Timer> result = startTimerUseCase.execute(
    "Work Session",           // name
    Duration.ofMinutes(25),   // duration
    Set.of("work", "focus"),  // tags
    "user123"                 // userId
);

// Start timer with minutes
Result<Timer> result = startTimerUseCase.execute("Quick Timer", 15, "user123");

// Start timer with seconds
Result<Timer> result = startTimerUseCase.execute("Short Timer", 300L, "user123");

// Handle result
result.onSuccess(timer -> {
    // Timer created successfully
    Log.d("Timer", "Created timer: " + timer.getName());
}).onFailure(error -> {
    // Handle error
    Log.e("Timer", "Failed to create timer: " + error.getMessage());
});
```

### PauseTimerUseCase

Pauses a running timer:

```java
@Inject
PauseTimerUseCase pauseTimerUseCase;

Result<Timer> result = pauseTimerUseCase.execute("timer123");

result.onSuccess(pausedTimer -> {
    // Timer paused successfully
    assert pausedTimer.isPaused();
    assert pausedTimer.getRemainingDurationWhenPaused() != null;
}).onFailure(error -> {
    if (error instanceof InvalidTimerStateException) {
        // Timer cannot be paused (already paused, expired, etc.)
    } else if (error instanceof TimerNotFoundException) {
        // Timer not found
    }
});
```

### ResumeTimerUseCase

Resumes a paused timer:

```java
@Inject
ResumeTimerUseCase resumeTimerUseCase;

Result<Timer> result = resumeTimerUseCase.execute("timer123");

result.onSuccess(resumedTimer -> {
    // Timer resumed successfully
    assert resumedTimer.isRunning();
}).onFailure(error -> {
    // Handle error
});
```

### StopTimerUseCase

Stops a timer from any state:

```java
@Inject
StopTimerUseCase stopTimerUseCase;

Result<Timer> result = stopTimerUseCase.execute("timer123");

result.onSuccess(stoppedTimer -> {
    // Timer stopped successfully
    assert stoppedTimer.isStopped();
}).onFailure(error -> {
    // Handle error
});
```

### GetActiveTimersUseCase

Retrieves all active timers:

```java
@Inject
GetActiveTimersUseCase getActiveTimersUseCase;

// Get all active timers
Result<List<Timer>> result = getActiveTimersUseCase.execute();

// Get active timers for specific user
Result<List<Timer>> result = getActiveTimersUseCase.execute("user123");

result.onSuccess(timers -> {
    // Process active timers
    for (Timer timer : timers) {
        if (timer.isRunning()) {
            // Handle running timer
        } else if (timer.isPaused()) {
            // Handle paused timer
        }
    }
}).onFailure(error -> {
    // Handle error
});
```

## Result Pattern

The `Result<T>` pattern provides functional error handling:

### Basic Usage

```java
// Success case
Result<Timer> success = Result.success(timer);
assert success.isSuccess();
assert !success.isFailure();
Timer data = success.getData();

// Failure case
Result<Timer> failure = Result.failure(new ValidationException("Invalid input"));
assert !failure.isSuccess();
assert failure.isFailure();
ValidationException error = failure.getError();
```

### Functional Composition

```java
Result<Timer> result = startTimerUseCase.execute("Work Timer", 25, "user123");

// Transform data
Result<String> timerName = result.map(Timer::getName);
Result<String> upperName = result.map(Timer::getName).map(String::toUpperCase);

// Chain operations
Result<String> chained = result
    .onSuccess(timer -> Log.d("Timer", "Created: " + timer.getName()))
    .map(Timer::getName)
    .onSuccess(name -> Log.d("Timer", "Name: " + name))
    .map(String::toUpperCase);

// Handle with defaults
Timer timer = result.getDataOrDefault(defaultTimer);
Timer timer = result.getDataOrDefault(() -> createDefaultTimer());
```

### Error Handling

```java
result.onSuccess(timer -> {
    // Handle success
    updateUI(timer);
}).onFailure(error -> {
    // Handle error
    if (error instanceof ValidationException) {
        showValidationError(error.getUserFriendlyMessage());
    } else if (error instanceof TimerNotFoundException) {
        showNotFoundError();
    } else {
        showGenericError(error.getUserFriendlyMessage());
    }
});
```

### Recoverability

```java
if (result.isRecoverable()) {
    // Error can be retried
    showRetryButton();
} else {
    // Error cannot be retried
    showPermanentError();
}
```

## Error Handling

### Domain Exceptions

```java
// Validation errors (recoverable)
ValidationException validationError = new ValidationException("Invalid input");

// Timer errors (recoverable by default)
TimerException timerError = new TimerException("Timer operation failed");

// Not found errors (non-recoverable)
TimerNotFoundException notFoundError = new TimerNotFoundException("timer123");

// State errors (non-recoverable)
InvalidTimerStateException stateError = new InvalidTimerStateException(
    TimerState.PAUSED, "resume"
);

// Network errors (recoverable)
NetworkException networkError = new NetworkException("Connection failed");

// Authentication errors (recoverable)
AuthenticationException authError = new AuthenticationException("Login required");
```

### Error Properties

```java
DomainException error = result.getError();

String errorCode = error.getErrorCode();        // Machine-readable code
String errorType = error.getErrorType();        // Exception class name
boolean recoverable = error.isRecoverable();    // Can be retried
String userMessage = error.getUserFriendlyMessage(); // UI message
```

## Dependency Injection

### Hilt Setup

```java
@Module
@InstallIn(SingletonComponent.class)
public class DomainModule {
    
    @Provides
    @Singleton
    public StartTimerUseCase provideStartTimerUseCase(TimerRepository timerRepository) {
        return new StartTimerUseCase(timerRepository);
    }
    
    @Provides
    @Singleton
    public PauseTimerUseCase providePauseTimerUseCase(TimerRepository timerRepository) {
        return new PauseTimerUseCase(timerRepository);
    }
    
    // ... other use cases
}
```

### ViewModel Integration

```java
@HiltViewModel
public class TimerViewModel extends ViewModel {
    
    private final StartTimerUseCase startTimerUseCase;
    private final PauseTimerUseCase pauseTimerUseCase;
    private final GetActiveTimersUseCase getActiveTimersUseCase;
    
    @Inject
    public TimerViewModel(StartTimerUseCase startTimerUseCase,
                        PauseTimerUseCase pauseTimerUseCase,
                        GetActiveTimersUseCase getActiveTimersUseCase) {
        this.startTimerUseCase = startTimerUseCase;
        this.pauseTimerUseCase = pauseTimerUseCase;
        this.getActiveTimersUseCase = getActiveTimersUseCase;
    }
    
    public void startTimer(String name, int minutes, String userId) {
        startTimerUseCase.execute(name, minutes, userId)
            .onSuccess(timer -> {
                // Update UI with new timer
                _timers.postValue(timer);
            })
            .onFailure(error -> {
                // Show error message
                _errorMessage.postValue(error.getUserFriendlyMessage());
            });
    }
}
```

## Testing

### Unit Testing Use Cases

```java
@RunWith(MockitoJUnitRunner.class)
public class StartTimerUseCaseTest {
    
    @Mock
    private TimerRepository timerRepository;
    
    private StartTimerUseCase startTimerUseCase;
    
    @Before
    public void setUp() {
        startTimerUseCase = new StartTimerUseCase(timerRepository);
    }
    
    @Test
    public void testExecute_ValidInput_ReturnsSuccess() {
        // Arrange
        String name = "Test Timer";
        Duration duration = Duration.ofMinutes(30);
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertTrue("Result should be successful", result.isSuccess());
        Timer timer = result.getData();
        assertEquals("Timer name should match", name, timer.getName());
        assertEquals("Timer duration should match", duration, timer.getTotalDuration());
        
        // Verify repository was called
        verify(timerRepository, times(1)).saveTimer(any(Timer.class));
    }
    
    @Test
    public void testExecute_EmptyName_ReturnsValidationError() {
        // Arrange
        String name = "";
        Duration duration = Duration.ofMinutes(30);
        String userId = "user123";
        
        // Act
        Result<Timer> result = startTimerUseCase.execute(name, duration, Set.of(), userId);
        
        // Assert
        assertFalse("Result should be a failure", result.isSuccess());
        assertTrue("Error should be ValidationException", result.getError() instanceof ValidationException);
        assertTrue("Error should be recoverable", result.isRecoverable());
    }
}
```

### Testing Result Pattern

```java
@Test
public void testResult_Chaining_ExecutesAllSteps() {
    // Arrange
    Timer timer = Timer.create("Test Timer", "user123", Duration.ofMinutes(30), Set.of());
    Result<Timer> result = Result.success(timer);
    AtomicInteger stepCount = new AtomicInteger(0);
    
    // Act
    Result<String> chainedResult = result
        .onSuccess(t -> stepCount.incrementAndGet())
        .map(Timer::getName)
        .onSuccess(name -> stepCount.incrementAndGet())
        .map(String::toUpperCase);
    
    // Assert
    assertTrue("Chained result should be successful", chainedResult.isSuccess());
    assertEquals("Chained data should be uppercase name", "TEST TIMER", chainedResult.getData());
    assertEquals("All steps should have been executed", 2, stepCount.get());
}
```

## Migration Guide

### From Direct Repository Access

**Before:**
```java
public class TimerViewModel extends ViewModel {
    private final TimerRepository timerRepository;
    
    public void startTimer(String name, int minutes) {
        Timer timer = new Timer(name, minutes);
        timerRepository.saveTimer(timer);
        // Handle success/failure manually
    }
}
```

**After:**
```java
@HiltViewModel
public class TimerViewModel extends ViewModel {
    private final StartTimerUseCase startTimerUseCase;
    
    @Inject
    public TimerViewModel(StartTimerUseCase startTimerUseCase) {
        this.startTimerUseCase = startTimerUseCase;
    }
    
    public void startTimer(String name, int minutes, String userId) {
        startTimerUseCase.execute(name, minutes, userId)
            .onSuccess(timer -> {
                // Handle success
                _timers.postValue(timer);
            })
            .onFailure(error -> {
                // Handle error
                _errorMessage.postValue(error.getUserFriendlyMessage());
            });
    }
}
```

### From Exception Handling

**Before:**
```java
try {
    Timer timer = timerRepository.getTimerById(id);
    timer.pause();
    timerRepository.updateTimer(timer);
} catch (Exception e) {
    // Handle generic exception
    Log.e("Timer", "Error pausing timer", e);
}
```

**After:**
```java
pauseTimerUseCase.execute(id)
    .onSuccess(pausedTimer -> {
        // Handle success
        updateUI(pausedTimer);
    })
    .onFailure(error -> {
        // Handle specific error types
        if (error instanceof InvalidTimerStateException) {
            showStateError();
        } else if (error instanceof TimerNotFoundException) {
            showNotFoundError();
        } else {
            showGenericError(error.getUserFriendlyMessage());
        }
    });
```

### From Manual Validation

**Before:**
```java
public void startTimer(String name, int minutes) {
    if (name == null || name.trim().isEmpty()) {
        showError("Name cannot be empty");
        return;
    }
    if (minutes <= 0) {
        showError("Duration must be positive");
        return;
    }
    // Create timer...
}
```

**After:**
```java
public void startTimer(String name, int minutes, String userId) {
    startTimerUseCase.execute(name, minutes, userId)
        .onSuccess(timer -> {
            // Timer created successfully
            showSuccess();
        })
        .onFailure(error -> {
            // Validation errors handled automatically
            showError(error.getUserFriendlyMessage());
        });
}
```

## Best Practices

### 1. Always Use Result Pattern

```java
// Good
Result<Timer> result = useCase.execute(params);
result.onSuccess(data -> handleSuccess(data))
      .onFailure(error -> handleError(error));

// Avoid
try {
    Timer timer = useCase.execute(params);
    handleSuccess(timer);
} catch (Exception e) {
    handleError(e);
}
```

### 2. Use Functional Composition

```java
// Good - Chain operations
Result<String> result = startTimerUseCase.execute(name, duration, tags, userId)
    .map(Timer::getName)
    .map(String::toUpperCase);

// Avoid - Multiple variables
Result<Timer> timerResult = startTimerUseCase.execute(name, duration, tags, userId);
if (timerResult.isSuccess()) {
    Timer timer = timerResult.getData();
    String name = timer.getName();
    String upperName = name.toUpperCase();
    // Use upperName...
}
```

### 3. Handle Specific Error Types

```java
result.onFailure(error -> {
    if (error instanceof ValidationException) {
        // Handle validation errors
        showValidationError(error.getUserFriendlyMessage());
    } else if (error instanceof TimerNotFoundException) {
        // Handle not found errors
        showNotFoundError();
    } else if (error instanceof NetworkException) {
        // Handle network errors
        showNetworkError();
    } else {
        // Handle generic errors
        showGenericError(error.getUserFriendlyMessage());
    }
});
```

### 4. Use Recoverability for UI Decisions

```java
if (result.isRecoverable()) {
    // Show retry button
    showRetryButton();
} else {
    // Show permanent error
    showPermanentError();
}
```

### 5. Test Both Success and Failure Cases

```java
@Test
public void testUseCase_SuccessCase() {
    // Test successful execution
    Result<Timer> result = useCase.execute(validParams);
    assertTrue(result.isSuccess());
    // Verify data and side effects
}

@Test
public void testUseCase_FailureCase() {
    // Test error conditions
    Result<Timer> result = useCase.execute(invalidParams);
    assertTrue(result.isFailure());
    assertTrue(result.getError() instanceof ValidationException);
    assertTrue(result.isRecoverable());
}
```

## Conclusion

The domain layer provides a robust foundation for business logic with:

- **Clean separation** of concerns
- **Functional error handling** with the Result pattern
- **Rich domain entities** with business logic
- **Comprehensive validation** and error handling
- **Easy testing** with mocked dependencies
- **Framework-agnostic design** for maximum flexibility

By following this guide, you can effectively use the domain layer to build maintainable, testable, and robust business logic for the Bubble Timer app.
