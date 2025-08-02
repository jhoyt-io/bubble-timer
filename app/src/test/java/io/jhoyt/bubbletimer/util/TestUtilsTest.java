package io.jhoyt.bubbletimer.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestUtilsTest {

    @Test
    public void testWaitForCondition() {
        AtomicBoolean condition = new AtomicBoolean(false);
        
        // Start a thread that will set the condition to true after a short delay
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100);
                condition.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();
        
        // Wait for the condition to become true
        TestUtils.waitForCondition(condition::get, 1000, "Condition should become true");
        
        assertTrue("Condition should be true", condition.get());
    }

    @Test
    public void testWaitForConditionWithDefaultTimeout() {
        AtomicBoolean condition = new AtomicBoolean(false);
        
        // Start a thread that will set the condition to true after a short delay
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100);
                condition.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();
        
        // Wait for the condition to become true with default timeout
        TestUtils.waitForCondition(condition::get, "Condition should become true");
        
        assertTrue("Condition should be true", condition.get());
    }

    @Test
    public void testWaitForLatch() {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Start a thread that will count down the latch after a short delay
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();
        
        // Wait for the latch to count down
        TestUtils.waitForLatch(latch, 1000, "Latch should count down");
        
        assertEquals("Latch should be counted down", 0, latch.getCount());
    }

    @Test
    public void testWaitForLatchWithDefaultTimeout() {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Start a thread that will count down the latch after a short delay
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();
        
        // Wait for the latch to count down with default timeout
        TestUtils.waitForLatch(latch, "Latch should count down");
        
        assertEquals("Latch should be counted down", 0, latch.getCount());
    }

    @Test
    public void testSleep() {
        long startTime = System.currentTimeMillis();
        
        TestUtils.sleep(Duration.ofMillis(100));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("Should sleep for at least 100ms", duration >= 100);
    }

    @Test
    public void testSleepWithMilliseconds() {
        long startTime = System.currentTimeMillis();
        
        TestUtils.sleep(100);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("Should sleep for at least 100ms", duration >= 100);
    }

    @Test
    public void testSleepShort() {
        long startTime = System.currentTimeMillis();
        
        TestUtils.sleepShort();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("Should sleep for at least 100ms", duration >= 100);
    }

    @Test
    public void testSleepMedium() {
        long startTime = System.currentTimeMillis();
        
        TestUtils.sleepMedium();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("Should sleep for at least 500ms", duration >= 500);
    }

    @Test
    public void testSleepLong() {
        long startTime = System.currentTimeMillis();
        
        TestUtils.sleepLong();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("Should sleep for at least 1000ms", duration >= 1000);
    }

    @Test
    public void testAssertConditionWithinTimeout() {
        AtomicBoolean condition = new AtomicBoolean(false);
        
        // Start a thread that will set the condition to true after a short delay
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100);
                condition.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();
        
        // Assert that the condition becomes true within timeout
        TestUtils.assertConditionWithinTimeout(condition::get, 1000, "Condition should become true");
        
        assertTrue("Condition should be true", condition.get());
    }

    @Test
    public void testCreateTestTimestamp() {
        LocalDateTime timestamp = TestUtils.createTestTimestamp();
        
        assertNotNull("Timestamp should not be null", timestamp);
        assertTrue("Timestamp should be recent", 
            Duration.between(timestamp, LocalDateTime.now()).toSeconds() < 1);
    }

    @Test
    public void testCreatePastTimestamp() {
        LocalDateTime pastTimestamp = TestUtils.createPastTimestamp(2);
        LocalDateTime now = LocalDateTime.now();
        
        assertNotNull("Past timestamp should not be null", pastTimestamp);
        assertTrue("Should be in the past", pastTimestamp.isBefore(now));
        
        Duration difference = Duration.between(pastTimestamp, now);
        assertTrue("Should be approximately 2 hours ago", 
            difference.toHours() >= 1 && difference.toHours() <= 3);
    }

    @Test
    public void testCreateFutureTimestamp() {
        LocalDateTime futureTimestamp = TestUtils.createFutureTimestamp(2);
        LocalDateTime now = LocalDateTime.now();
        
        assertNotNull("Future timestamp should not be null", futureTimestamp);
        assertTrue("Should be in the future", futureTimestamp.isAfter(now));
        
        Duration difference = Duration.between(now, futureTimestamp);
        assertTrue("Should be approximately 2 hours ahead", 
            difference.toHours() >= 1 && difference.toHours() <= 3);
    }

    @Test
    public void testMeasureExecutionTime() {
        long executionTime = TestUtils.measureExecutionTime(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        assertTrue("Should measure execution time", executionTime > 0);
        assertTrue("Should be at least 100ms", executionTime >= 100);
    }

    @Test
    public void testAssertExecutionTimeWithinLimit() {
        TestUtils.assertExecutionTimeWithinLimit(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 200, "Execution time should be within limit");
    }

    @Test
    public void testRetryOperation() {
        AtomicBoolean success = new AtomicBoolean(false);
        int[] attempts = {0};
        
        TestUtils.retryOperation(() -> {
            attempts[0]++;
            if (attempts[0] < 3) {
                throw new RuntimeException("Simulated failure");
            }
            success.set(true);
        }, 3, 10, "Operation should succeed after retries");
        
        assertTrue("Operation should succeed", success.get());
        assertEquals("Should have made 3 attempts", 3, attempts[0]);
    }

    @Test
    public void testRetryOperationWithDefaultSettings() {
        AtomicBoolean success = new AtomicBoolean(false);
        int[] attempts = {0};
        
        TestUtils.retryOperation(() -> {
            attempts[0]++;
            if (attempts[0] < 2) {
                throw new RuntimeException("Simulated failure");
            }
            success.set(true);
        });
        
        assertTrue("Operation should succeed", success.get());
        assertEquals("Should have made 2 attempts", 2, attempts[0]);
    }

    @Test
    public void testCreateShortTestDuration() {
        Duration duration = TestUtils.createShortTestDuration();
        
        assertEquals("Should be 1 second", Duration.ofSeconds(1), duration);
    }

    @Test
    public void testCreateMediumTestDuration() {
        Duration duration = TestUtils.createMediumTestDuration();
        
        assertEquals("Should be 5 seconds", Duration.ofSeconds(5), duration);
    }

    @Test
    public void testCreateLongTestDuration() {
        Duration duration = TestUtils.createLongTestDuration();
        
        assertEquals("Should be 10 seconds", Duration.ofSeconds(10), duration);
    }

    @Test
    public void testAssertDurationApproximatelyEqual() {
        Duration expected = Duration.ofSeconds(5);
        Duration actual = Duration.ofSeconds(5);
        
        TestUtils.assertDurationApproximatelyEqual(expected, actual, 100, "Durations should be approximately equal");
    }

    @Test
    public void testAssertDurationApproximatelyEqualWithDefaultTolerance() {
        Duration expected = Duration.ofSeconds(5);
        Duration actual = Duration.ofSeconds(5);
        
        TestUtils.assertDurationApproximatelyEqual(expected, actual, "Durations should be approximately equal");
    }

    @Test
    public void testAssertDurationApproximatelyEqualWithDefaultMessage() {
        Duration expected = Duration.ofSeconds(5);
        Duration actual = Duration.ofSeconds(5);
        
        TestUtils.assertDurationApproximatelyEqual(expected, actual);
    }

    @Test
    public void testAssertDurationApproximatelyEqualWithinTolerance() {
        Duration expected = Duration.ofSeconds(5);
        Duration actual = Duration.ofSeconds(5).plusMillis(50); // 50ms difference
        
        TestUtils.assertDurationApproximatelyEqual(expected, actual, 100, "Durations should be approximately equal");
    }
} 