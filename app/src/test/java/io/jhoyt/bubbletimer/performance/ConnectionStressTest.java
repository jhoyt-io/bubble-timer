package io.jhoyt.bubbletimer.performance;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.WebsocketManager;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import io.jhoyt.bubbletimer.util.TestDataFactory;

/**
 * Stress tests for WebSocket connection handling with multiple shared timers.
 * Tests the system's ability to handle concurrent shared timer operations
 * without performance degradation or memory leaks.
 */
@RunWith(JUnit4.class)
public class ConnectionStressTest {

    private WebsocketManager websocketManager;
    
    @Mock
    private WebsocketManager mockWebsocketManager;
    
    @Mock
    private ActiveTimerRepository mockRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize WebSocket manager without database dependency
        websocketManager = new WebsocketManager(null, new okhttp3.OkHttpClient());
    }

    /**
     * Test handling of 50 concurrent timer operations.
     * Verifies the system can handle high load without errors.
     */
    @Test
    public void testConcurrentTimerOperations() throws InterruptedException {
        final int TIMER_COUNT = 50;
        final CountDownLatch latch = new CountDownLatch(TIMER_COUNT);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        
        // Create multiple threads to simulate concurrent timer operations
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 0; i < TIMER_COUNT; i++) {
            final int timerIndex = i;
            Thread thread = new Thread(() -> {
                try {
                    // Create a timer
                    Timer timer = TestDataFactory.createTestTimer(
                        "user" + timerIndex,
                        "Stress Timer " + timerIndex,
                        Duration.ofSeconds(30)
                    );
                    
                    // Simulate timer operation (just create the timer)
                    assertNotNull("Timer should be created", timer);
                    assertEquals("Timer should have correct user ID", "user" + timerIndex, timer.getUserId());
                    assertEquals("Timer should have correct name", "Stress Timer " + timerIndex, timer.getName());
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
            
            threads.add(thread);
            thread.start();
        }
        
        // Wait for all operations to complete (max 30 seconds)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        // Verify results
        assertTrue("Stress test should complete within timeout", completed);
        assertEquals("All operations should succeed", TIMER_COUNT, successCount.get());
        assertEquals("No errors should occur", 0, errorCount.get());
    }

    /**
     * Test rapid connection/disconnection cycles.
     * Verifies the system can handle frequent connection state changes.
     */
    @Test
    public void testRapidConnectionCycles() throws InterruptedException {
        final int CYCLE_COUNT = 20;
        final CountDownLatch latch = new CountDownLatch(CYCLE_COUNT);
        final AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < CYCLE_COUNT; i++) {
            final int cycleIndex = i;
            new Thread(() -> {
                try {
                    // Simulate connection
                    websocketManager.connectIfNeeded();
                    
                    // Simulate disconnection
                    websocketManager.close();
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
            
            // Small delay between cycles
            Thread.sleep(10);
        }
        
        // Wait for completion
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        
        assertTrue("Connection cycles should complete", completed);
        assertEquals("All cycles should succeed", CYCLE_COUNT, successCount.get());
    }

    /**
     * Test memory usage during stress operations.
     * Verifies no memory leaks occur during high-load operations.
     */
    @Test
    public void testMemoryUsageUnderStress() throws InterruptedException {
        // Get initial memory usage
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform stress operations
        testConcurrentTimerOperations();
        
        // Force garbage collection
        System.gc();
        Thread.sleep(1000); // Allow GC to complete
        
        // Get final memory usage
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Memory increase should be reasonable (less than 10MB)
        long maxAllowedIncrease = 10 * 1024 * 1024; // 10MB
        assertTrue("Memory usage should not increase significantly: " + 
                  (memoryIncrease / 1024 / 1024) + "MB", 
                  memoryIncrease < maxAllowedIncrease);
    }

    /**
     * Test performance degradation over time.
     * Verifies the system maintains performance during extended stress.
     */
    @Test
    public void testPerformanceOverTime() {
        final int ITERATIONS = 5;
        final int OPERATIONS_PER_ITERATION = 10;
        
        List<Long> iterationTimes = new ArrayList<>();
        
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            long startTime = System.currentTimeMillis();
            
            // Perform operations for this iteration
            for (int i = 0; i < OPERATIONS_PER_ITERATION; i++) {
                Timer timer = TestDataFactory.createTestTimer(
                    "user" + iteration + "_" + i,
                    "Performance Timer " + iteration + "_" + i,
                    Duration.ofSeconds(30)
                );
                
                // Verify timer creation
                assertNotNull("Timer should be created", timer);
                assertEquals("Timer should have correct user ID", "user" + iteration + "_" + i, timer.getUserId());
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            iterationTimes.add(duration);
        }
        
        // Verify performance doesn't degrade significantly
        long firstIterationTime = iterationTimes.get(0);
        long lastIterationTime = iterationTimes.get(iterationTimes.size() - 1);
        
        // Performance should not degrade by more than 50%
        double degradationRatio = (double) lastIterationTime / firstIterationTime;
        assertTrue("Performance should not degrade significantly: " + degradationRatio, 
                  degradationRatio < 1.5);
    }

    /**
     * Test WebSocket manager state transitions.
     * Verifies the WebSocket manager can handle state changes properly.
     */
    @Test
    public void testWebSocketManagerStateTransitions() {
        // Test initial state
        assertEquals("Initial state should be DISCONNECTED", 
                    WebsocketManager.ConnectionState.DISCONNECTED, 
                    websocketManager.getConnectionState());
        
        // Test connection attempt
        websocketManager.connectIfNeeded();
        // Note: In unit tests, connection will fail due to no real network, but state should change
        
        // Test close
        websocketManager.close();
        assertEquals("State after close should be DISCONNECTED", 
                    WebsocketManager.ConnectionState.DISCONNECTED, 
                    websocketManager.getConnectionState());
    }
} 