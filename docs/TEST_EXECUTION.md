# Bubble Timer Test Execution Guide

## Overview

This document provides comprehensive guidance on executing different types of tests in the Bubble Timer project. The testing strategy is organized into categories based on execution speed and scope.

## Test Categories

### 1. Unit Tests (Fast - <30 seconds)
**Purpose**: Test individual components in isolation
**Scope**: Core business logic, utilities, model classes
**Execution Time**: <30 seconds

**Test Classes**:
- `core.TimerModelTest` - Timer model functionality
- `util.TestDataFactoryTest` - Test data utilities
- `di.DependencyInjectionTest` - DI configuration
- `websocket.WebsocketManagerOnDemandTest` - On-demand connection
- `websocket.WebsocketManagerMessageTest` - Message handling
- `service.ForegroundServiceTest` - Service integration
- `sharing.SharedTimerPersistenceTest` - Timer sharing
- `repository.ActiveTimerRepositoryTest` - Repository operations

### 2. Integration Tests (Medium - <2 minutes)
**Purpose**: Test component interactions and data flow
**Scope**: Database operations, WebSocket integration
**Execution Time**: <2 minutes

**Test Classes**:
- `integration.DatabaseIntegrationTest` - Database operations
- `websocket.WebsocketManagerConnectionTest` - Connection management

### 3. End-to-End Tests (Slow - >2 minutes)
**Purpose**: Test complete workflows and user scenarios
**Scope**: Full application workflows, service lifecycles
**Execution Time**: >2 minutes

**Test Classes**:
- Complete sharing workflow tests
- Service lifecycle tests
- Connection state transition tests

## Execution Commands

### Using Gradle Tasks

```bash
# Run all tests
./gradlew test

# Run unit tests only (fast)
./gradlew unitTests

# Run integration tests only (medium)
./gradlew integrationTests

# Run fast tests only (core functionality)
./gradlew fastTests

# Run WebSocket tests only
./gradlew websocketTests

# Run service tests only
./gradlew serviceTests

# Run database tests only
./gradlew databaseTests

# Run tests with coverage
./gradlew coverageTests

# Run performance tests
./gradlew performanceTests

# Measure test execution time
./gradlew measureTestTime

# Show test categories
./gradlew testCategories
```

### Using Test Script

```bash
# Make script executable (first time only)
chmod +x scripts/run-tests.sh

# Run unit tests only
./scripts/run-tests.sh unit

# Run integration tests only
./scripts/run-tests.sh integration

# Run all tests
./scripts/run-tests.sh all

# Run tests with coverage
./scripts/run-tests.sh coverage

# Clean build and run tests
./scripts/run-tests.sh clean

# Run tests with debug output
./scripts/run-tests.sh debug

# Show help
./scripts/run-tests.sh help
```

### Using Direct Gradle Commands

```bash
# Run specific test class
./gradlew testDebugUnitTest --tests="io.jhoyt.bubbletimer.core.TimerModelTest"

# Run tests in specific package
./gradlew testDebugUnitTest --tests="io.jhoyt.bubbletimer.core.*"

# Run tests with specific pattern
./gradlew testDebugUnitTest --tests="*Test"

# Run tests with debug output
./gradlew testDebugUnitTest --info --stacktrace

# Run tests with coverage
./gradlew testDebugUnitTest --tests="io.jhoyt.bubbletimer.*"
```

## Test Configuration

### JVM Options
- **Heap Size**: 2GB maximum
- **Metaspace**: 512MB maximum
- **Parallel Execution**: Enabled
- **Timeout**: 5 minutes per test

### Test Logging
- **Events**: passed, skipped, failed, standardOut, standardError
- **Exceptions**: Full stack traces
- **Causes**: Show exception causes
- **Format**: Full exception details

### Performance Targets
- **Unit Tests**: <30 seconds
- **Integration Tests**: <2 minutes
- **All Tests**: <5 minutes
- **Memory Usage**: No memory leaks

## Continuous Integration

### GitHub Actions Workflow
The project includes a comprehensive CI/CD pipeline:

```yaml
# .github/workflows/test.yml
name: Bubble Timer Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
      - name: Set up JDK 17
      - name: Set up Android SDK
      - name: Run unit tests
      - name: Run integration tests
      - name: Build debug APK
      - name: Upload test results
      - name: Upload APK

  test-coverage:
    runs-on: ubuntu-latest
    needs: test
    steps:
      - name: Run tests with coverage
      - name: Upload coverage report

  test-performance:
    runs-on: ubuntu-latest
    needs: test
    steps:
      - name: Run performance tests
```

### CI Features
- **Parallel Execution**: Multiple test jobs run in parallel
- **Caching**: Gradle and Android SDK caching
- **Artifacts**: Test results and APK uploads
- **Coverage**: Coverage report generation
- **Performance**: Test execution time measurement

## Test Reports

### HTML Reports
Location: `app/build/reports/tests/testDebugUnitTest/index.html`

### Coverage Reports
Location: `app/build/reports/tests/testDebugUnitTest/`

### Test Results
Location: `app/build/reports/tests/`

## Best Practices

### 1. Test Organization
- Group tests by functionality
- Use descriptive test names
- Follow AAA pattern (Arrange, Act, Assert)

### 2. Test Execution
- Run unit tests frequently during development
- Run integration tests before commits
- Run full test suite before releases

### 3. Performance
- Monitor test execution times
- Optimize slow tests
- Use parallel execution when possible

### 4. Maintenance
- Keep tests up to date with code changes
- Remove obsolete tests
- Refactor tests for better maintainability

## Troubleshooting

### Common Issues

#### 1. Test Timeout
```bash
# Increase timeout for specific test
./gradlew testDebugUnitTest --tests="SlowTest" -Dorg.gradle.internal.http.connectionTimeout=300000
```

#### 2. Memory Issues
```bash
# Increase heap size
./gradlew testDebugUnitTest -Xmx4g
```

#### 3. Parallel Execution Issues
```bash
# Disable parallel execution
./gradlew testDebugUnitTest -Dorg.gradle.parallel=false
```

#### 4. Test Filtering Issues
```bash
# Debug test filtering
./gradlew testDebugUnitTest --info --tests="*Test"
```

### Debug Commands

```bash
# Run tests with debug output
./gradlew testDebugUnitTest --info --stacktrace

# Run specific test with debug
./gradlew testDebugUnitTest --tests="SpecificTest" --info

# Check test configuration
./gradlew testDebugUnitTest --dry-run

# Show test dependencies
./gradlew testDebugUnitTest --scan
```

## Success Metrics

### Coverage Goals
- **Unit Tests**: 80%+ line coverage for core business logic
- **Integration Tests**: 70%+ coverage for data flow
- **End-to-End Tests**: Critical path coverage

### Performance Goals
- **Test Execution**: Unit tests complete in <30 seconds
- **Integration Tests**: Complete in <2 minutes
- **Memory Usage**: No memory leaks in connection lifecycle

### Quality Goals
- **Zero Critical Bugs**: All critical paths tested
- **Reliable Tests**: <1% flaky tests
- **Maintainable**: Clear test structure and documentation

## Conclusion

This test execution strategy provides a comprehensive approach to testing the Bubble Timer application. By organizing tests into categories and providing multiple execution methods, developers can efficiently run the appropriate tests for their current needs while maintaining high code quality and reliability. 