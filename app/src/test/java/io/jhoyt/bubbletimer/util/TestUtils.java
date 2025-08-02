package io.jhoyt.bubbletimer.util;

import org.junit.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Utility class for common test operations like time advancement and condition waiting.
 */
public class TestUtils {

    /**
     * Wait for a condition to become true with timeout
     * @param condition The condition to wait for
     * @param timeoutMs Timeout in milliseconds
     * @param message Error message if condition doesn't become true
     */
    public static void waitForCondition(BooleanSupplier condition, long timeoutMs, String message) {
        long startTime = System.currentTimeMillis();
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                Assert.fail(message + " (timeout after " + timeoutMs + "ms)");
            }
            try {
                Thread.sleep(10); // Small delay to avoid busy waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Assert.fail("Test interrupted while waiting for condition: " + message);
            }
        }
    }

    /**
     * Wait for a condition to become true with default timeout
     * @param condition The condition to wait for
     * @param message Error message if condition doesn't become true
     */
    public static void waitForCondition(BooleanSupplier condition, String message) {
        waitForCondition(condition, 5000, message); // 5 second default timeout
    }

    /**
     * Wait for a condition to become true with timeout
     * @param condition The condition to wait for
     * @param timeoutMs Timeout in milliseconds
     */
    public static void waitForCondition(BooleanSupplier condition, long timeoutMs) {
        waitForCondition(condition, timeoutMs, "Condition not met within timeout");
    }

    /**
     * Wait for a condition to become true with default timeout
     * @param condition The condition to wait for
     */
    public static void waitForCondition(BooleanSupplier condition) {
        waitForCondition(condition, 5000, "Condition not met within timeout");
    }

    /**
     * Sleep for a specified duration
     * @param duration Duration to sleep
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }

    /**
     * Sleep for a specified number of milliseconds
     * @param milliseconds Milliseconds to sleep
     */
    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }

    /**
     * Sleep for a short duration (100ms)
     */
    public static void sleepShort() {
        sleep(100);
    }

    /**
     * Sleep for a medium duration (500ms)
     */
    public static void sleepMedium() {
        sleep(500);
    }

    /**
     * Sleep for a long duration (1000ms)
     */
    public static void sleepLong() {
        sleep(1000);
    }

    /**
     * Wait for a CountDownLatch with timeout
     * @param latch The CountDownLatch to wait for
     * @param timeoutMs Timeout in milliseconds
     * @param message Error message if latch doesn't count down
     */
    public static void waitForLatch(CountDownLatch latch, long timeoutMs, String message) {
        try {
            boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!completed) {
                Assert.fail(message + " (timeout after " + timeoutMs + "ms)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assert.fail("Test interrupted while waiting for latch: " + message);
        }
    }

    /**
     * Wait for a CountDownLatch with default timeout
     * @param latch The CountDownLatch to wait for
     * @param message Error message if latch doesn't count down
     */
    public static void waitForLatch(CountDownLatch latch, String message) {
        waitForLatch(latch, 5000, message); // 5 second default timeout
    }

    /**
     * Wait for a CountDownLatch with default timeout
     * @param latch The CountDownLatch to wait for
     */
    public static void waitForLatch(CountDownLatch latch) {
        waitForLatch(latch, 5000, "Latch not counted down within timeout");
    }

    /**
     * Assert that a condition is true within a timeout
     * @param condition The condition to check
     * @param timeoutMs Timeout in milliseconds
     * @param message Error message if condition is not true
     */
    public static void assertConditionWithinTimeout(BooleanSupplier condition, long timeoutMs, String message) {
        waitForCondition(condition, timeoutMs, message);
    }

    /**
     * Assert that a condition is true within default timeout
     * @param condition The condition to check
     * @param message Error message if condition is not true
     */
    public static void assertConditionWithinTimeout(BooleanSupplier condition, String message) {
        waitForCondition(condition, 5000, message);
    }

    /**
     * Assert that a condition is true within default timeout
     * @param condition The condition to check
     */
    public static void assertConditionWithinTimeout(BooleanSupplier condition) {
        waitForCondition(condition, 5000, "Condition not met within timeout");
    }

    /**
     * Create a timestamp for testing
     * @return Current timestamp
     */
    public static LocalDateTime createTestTimestamp() {
        return LocalDateTime.now();
    }

    /**
     * Create a timestamp in the past
     * @param hoursAgo Hours ago
     * @return Timestamp in the past
     */
    public static LocalDateTime createPastTimestamp(int hoursAgo) {
        return LocalDateTime.now().minusHours(hoursAgo);
    }

    /**
     * Create a timestamp in the future
     * @param hoursAhead Hours ahead
     * @return Timestamp in the future
     */
    public static LocalDateTime createFutureTimestamp(int hoursAhead) {
        return LocalDateTime.now().plusHours(hoursAhead);
    }

    /**
     * Measure execution time of a runnable
     * @param runnable The runnable to measure
     * @return Execution time in milliseconds
     */
    public static long measureExecutionTime(Runnable runnable) {
        long startTime = System.currentTimeMillis();
        runnable.run();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    /**
     * Assert that execution time is within expected range
     * @param runnable The runnable to measure
     * @param expectedMaxMs Expected maximum execution time in milliseconds
     * @param message Error message if execution time exceeds limit
     */
    public static void assertExecutionTimeWithinLimit(Runnable runnable, long expectedMaxMs, String message) {
        long executionTime = measureExecutionTime(runnable);
        if (executionTime > expectedMaxMs) {
            Assert.fail(message + " (execution time: " + executionTime + "ms, expected max: " + expectedMaxMs + "ms)");
        }
    }

    /**
     * Assert that execution time is within expected range
     * @param runnable The runnable to measure
     * @param expectedMaxMs Expected maximum execution time in milliseconds
     */
    public static void assertExecutionTimeWithinLimit(Runnable runnable, long expectedMaxMs) {
        assertExecutionTimeWithinLimit(runnable, expectedMaxMs, "Execution time exceeded limit");
    }

    /**
     * Retry an operation multiple times until it succeeds
     * @param operation The operation to retry
     * @param maxAttempts Maximum number of attempts
     * @param delayMs Delay between attempts in milliseconds
     * @param message Error message if all attempts fail
     */
    public static void retryOperation(Runnable operation, int maxAttempts, long delayMs, String message) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                operation.run();
                return; // Success
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    sleep(delayMs);
                }
            }
        }
        Assert.fail(message + " (failed after " + maxAttempts + " attempts). Last exception: " + lastException);
    }

    /**
     * Retry an operation multiple times until it succeeds
     * @param operation The operation to retry
     * @param maxAttempts Maximum number of attempts
     * @param delayMs Delay between attempts in milliseconds
     */
    public static void retryOperation(Runnable operation, int maxAttempts, long delayMs) {
        retryOperation(operation, maxAttempts, delayMs, "Operation failed after retries");
    }

    /**
     * Retry an operation with default settings
     * @param operation The operation to retry
     */
    public static void retryOperation(Runnable operation) {
        retryOperation(operation, 3, 100, "Operation failed after retries");
    }

    /**
     * Create a test duration for short operations
     * @return Short duration (1 second)
     */
    public static Duration createShortTestDuration() {
        return Duration.ofSeconds(1);
    }

    /**
     * Create a test duration for medium operations
     * @return Medium duration (5 seconds)
     */
    public static Duration createMediumTestDuration() {
        return Duration.ofSeconds(5);
    }

    /**
     * Create a test duration for long operations
     * @return Long duration (10 seconds)
     */
    public static Duration createLongTestDuration() {
        return Duration.ofSeconds(10);
    }

    /**
     * Assert that two durations are approximately equal (within tolerance)
     * @param expected Expected duration
     * @param actual Actual duration
     * @param toleranceMs Tolerance in milliseconds
     * @param message Error message if durations are not equal
     */
    public static void assertDurationApproximatelyEqual(Duration expected, Duration actual, long toleranceMs, String message) {
        long difference = Math.abs(expected.toMillis() - actual.toMillis());
        if (difference > toleranceMs) {
            Assert.fail(message + " (expected: " + expected + ", actual: " + actual + ", difference: " + difference + "ms)");
        }
    }

    /**
     * Assert that two durations are approximately equal (within default tolerance)
     * @param expected Expected duration
     * @param actual Actual duration
     * @param message Error message if durations are not equal
     */
    public static void assertDurationApproximatelyEqual(Duration expected, Duration actual, String message) {
        assertDurationApproximatelyEqual(expected, actual, 100, message); // 100ms default tolerance
    }

    /**
     * Assert that two durations are approximately equal (within default tolerance)
     * @param expected Expected duration
     * @param actual Actual duration
     */
    public static void assertDurationApproximatelyEqual(Duration expected, Duration actual) {
        assertDurationApproximatelyEqual(expected, actual, 100, "Durations are not approximately equal");
    }
} 