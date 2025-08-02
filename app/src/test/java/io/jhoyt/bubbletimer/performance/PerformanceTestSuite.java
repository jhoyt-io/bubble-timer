package io.jhoyt.bubbletimer.performance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.WebsocketManager;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import io.jhoyt.bubbletimer.util.TestDataFactory;

/**
 * Comprehensive performance test suite for Bubble Timer application.
 * Tests stress handling, memory management, and performance benchmarks.
 */
@RunWith(JUnit4.class)
public class PerformanceTestSuite {

    private WebsocketManager websocketManager;

    @Before
    public void setUp() {
        // Initialize WebSocket manager without database dependency
        websocketManager = new WebsocketManager(null, new okhttp3.OkHttpClient());
    }

    /**
     * Comprehensive performance benchmark test.
     * Tests multiple performance aspects in a single test.
     */
    @Test
    public void testComprehensivePerformanceBenchmark() throws InterruptedException {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        // Test 1: Connection performance
        long connectionStart = System.currentTimeMillis();
        websocketManager.connectIfNeeded();
        long connectionTime = System.currentTimeMillis() - connectionStart;
        metrics.addMetric("connection_time", connectionTime);
        
        // Test 2: Timer creation performance
        long creationStart = System.currentTimeMillis();
        List<Timer> timers = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Timer timer = TestDataFactory.createTestTimer(
                "user" + i,
                "Performance Timer " + i,
                Duration.ofSeconds(30)
            );
            timers.add(timer);
            assertNotNull("Timer should be created", timer);
        }
        long creationTime = System.currentTimeMillis() - creationStart;
        metrics.addMetric("timer_creation_time", creationTime);
        
        // Test 3: Disconnection performance
        long disconnectStart = System.currentTimeMillis();
        websocketManager.close();
        long disconnectTime = System.currentTimeMillis() - disconnectStart;
        metrics.addMetric("disconnect_time", disconnectTime);
        
        // Verify performance targets
        assertTrue("Connection should be fast (<100ms): " + connectionTime + "ms", 
                  connectionTime < 100);
        assertTrue("Timer creation should be fast (<500ms): " + creationTime + "ms", 
                  creationTime < 500);
        assertTrue("Disconnection should be fast (<50ms): " + disconnectTime + "ms", 
                  disconnectTime < 50);
        
        // Print metrics
        metrics.printMetrics();
    }

    /**
     * Test system behavior under sustained load.
     * Verifies the system maintains performance over extended periods.
     */
    @Test
    public void testSustainedLoadPerformance() throws InterruptedException {
        final int SUSTAINED_OPERATIONS = 500;
        final int BATCH_SIZE = 25;
        
        List<Long> batchTimes = new ArrayList<>();
        long totalStart = System.currentTimeMillis();
        
        // Connect
        websocketManager.connectIfNeeded();
        
        // Perform sustained operations in batches
        for (int batch = 0; batch < SUSTAINED_OPERATIONS / BATCH_SIZE; batch++) {
            long batchStart = System.currentTimeMillis();
            
            for (int i = 0; i < BATCH_SIZE; i++) {
                int index = batch * BATCH_SIZE + i;
                Timer timer = TestDataFactory.createTestTimer(
                    "user" + index,
                    "Sustained Timer " + index,
                    Duration.ofSeconds(25)
                );
                assertNotNull("Timer should be created", timer);
            }
            
            long batchTime = System.currentTimeMillis() - batchStart;
            batchTimes.add(batchTime);
            
            // Small delay between batches
            Thread.sleep(10);
        }
        
        long totalTime = System.currentTimeMillis() - totalStart;
        websocketManager.close();
        
        // Verify performance consistency
        long firstBatchTime = batchTimes.get(0);
        long lastBatchTime = batchTimes.get(batchTimes.size() - 1);
        double performanceRatio = (double) lastBatchTime / firstBatchTime;
        
        assertTrue("Performance should remain consistent: " + performanceRatio, 
                  performanceRatio < 2.0); // Should not degrade by more than 100%
        
        // Verify total performance
        double operationsPerSecond = (double) SUSTAINED_OPERATIONS / (totalTime / 1000.0);
        assertTrue("Should handle at least 10 operations per second: " + operationsPerSecond, 
                  operationsPerSecond >= 10.0);
    }

    /**
     * Test memory efficiency during high-load operations.
     * Verifies memory usage remains reasonable under stress.
     */
    @Test
    public void testMemoryEfficiencyUnderLoad() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        
        // Get baseline memory
        System.gc();
        Thread.sleep(1000);
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform high-load operations
        websocketManager.connectIfNeeded();
        
        List<Timer> timers = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            Timer timer = TestDataFactory.createTestTimer(
                "user" + i,
                "Memory Efficiency Timer " + i,
                Duration.ofSeconds(45)
            );
            timers.add(timer);
            assertNotNull("Timer should be created", timer);
        }
        
        // Measure memory during load
        long peakMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = peakMemory - baselineMemory;
        
        // Disconnect and cleanup
        websocketManager.close();
        timers.clear();
        
        // Force garbage collection
        System.gc();
        Thread.sleep(2000);
        
        // Measure final memory
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long finalIncrease = finalMemory - baselineMemory;
        
        // Verify memory efficiency
        assertTrue("Peak memory should be reasonable (<15MB): " + 
                  (memoryIncrease / 1024 / 1024) + "MB", 
                  memoryIncrease < 15 * 1024 * 1024);
        
        assertTrue("Final memory should be close to baseline (<5MB): " + 
                  (finalIncrease / 1024 / 1024) + "MB", 
                  finalIncrease < 5 * 1024 * 1024);
    }

    /**
     * Test concurrent user simulation.
     * Simulates multiple users performing operations simultaneously.
     */
    @Test
    public void testConcurrentUserSimulation() throws InterruptedException {
        final int USER_COUNT = 10;
        final int OPERATIONS_PER_USER = 15;
        final CountDownLatch latch = new CountDownLatch(USER_COUNT);
        
        List<Long> userTimes = new ArrayList<>();
        long simulationStart = System.currentTimeMillis();
        
        // Simulate concurrent users
        for (int userIndex = 0; userIndex < USER_COUNT; userIndex++) {
            final int userId = userIndex;
            new Thread(() -> {
                try {
                    long userStart = System.currentTimeMillis();
                    
                    // Each user performs operations
                    for (int op = 0; op < OPERATIONS_PER_USER; op++) {
                        Timer timer = TestDataFactory.createTestTimer(
                            "user" + userId,
                            "Concurrent Timer " + userId + "_" + op,
                            Duration.ofSeconds(30)
                        );
                        assertNotNull("Timer should be created", timer);
                        
                        // Simulate user thinking time
                        Thread.sleep(5);
                    }
                    
                    long userTime = System.currentTimeMillis() - userStart;
                    synchronized (userTimes) {
                        userTimes.add(userTime);
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // Wait for all users to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - simulationStart;
        
        assertTrue("Concurrent simulation should complete", completed);
        
        // Verify performance
        long totalOperations = USER_COUNT * OPERATIONS_PER_USER;
        double operationsPerSecond = (double) totalOperations / (totalTime / 1000.0);
        
        assertTrue("Should handle concurrent operations efficiently: " + operationsPerSecond + " ops/sec", 
                  operationsPerSecond >= 5.0);
        
        // Verify all users completed successfully
        assertEquals("All users should complete", USER_COUNT, userTimes.size());
    }

    /**
     * Test resource cleanup efficiency.
     * Verifies that resources are cleaned up quickly and completely.
     */
    @Test
    public void testResourceCleanupEfficiency() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        
        // Create resources
        websocketManager.connectIfNeeded();
        List<Timer> timers = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            Timer timer = TestDataFactory.createTestTimer(
                "user" + i,
                "Cleanup Timer " + i,
                Duration.ofSeconds(35)
            );
            timers.add(timer);
            assertNotNull("Timer should be created", timer);
        }
        
        // Measure cleanup time
        long cleanupStart = System.currentTimeMillis();
        
        // Disconnect
        websocketManager.close();
        
        // Clear timers
        timers.clear();
        
        long cleanupTime = System.currentTimeMillis() - cleanupStart;
        
        // Force garbage collection
        System.gc();
        Thread.sleep(1000);
        
        // Verify cleanup performance
        assertTrue("Cleanup should be fast (<200ms): " + cleanupTime + "ms", 
                  cleanupTime < 200);
        
        // Verify memory cleanup
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        assertTrue("Memory should be reasonable after cleanup: " + 
                  (finalMemory / 1024 / 1024) + "MB", 
                  finalMemory < 50 * 1024 * 1024); // Less than 50MB
    }

    // Helper class for performance metrics
    private static class PerformanceMetrics {
        private List<Metric> metrics = new ArrayList<>();
        
        public void addMetric(String name, long value) {
            metrics.add(new Metric(name, value));
        }
        
        public void printMetrics() {
            System.out.println("=== Performance Metrics ===");
            for (Metric metric : metrics) {
                System.out.printf("%s: %dms%n", metric.name, metric.value);
            }
            System.out.println("==========================");
        }
        
        private static class Metric {
            String name;
            long value;
            
            Metric(String name, long value) {
                this.name = name;
                this.value = value;
            }
        }
    }
} 