package io.jhoyt.bubbletimer.performance;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.jhoyt.bubbletimer.domain.core.Result;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;
import io.jhoyt.bubbletimer.integration.TestTimerRepository;
import io.jhoyt.bubbletimer.domain.usecases.timer.StartTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.GetAllTimersUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.GetTimerByIdUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.DeleteTimerUseCase;

import static org.junit.Assert.*;

/**
 * Performance tests for timer operations.
 * Tests the performance characteristics of timer operations with large datasets.
 */
@RunWith(AndroidJUnit4.class)
public class TimerPerformanceTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TimerRepository timerRepository;
    private StartTimerUseCase startTimerUseCase;
    private GetAllTimersUseCase getAllTimersUseCase;
    private GetTimerByIdUseCase getTimerByIdUseCase;
    private DeleteTimerUseCase deleteTimerUseCase;

    @Before
    public void setUp() {
        // Create simple test repository
        timerRepository = new TestTimerRepository();
        
        // Initialize use cases
        startTimerUseCase = new StartTimerUseCase(timerRepository);
        getAllTimersUseCase = new GetAllTimersUseCase(timerRepository);
        getTimerByIdUseCase = new GetTimerByIdUseCase(timerRepository);
        deleteTimerUseCase = new DeleteTimerUseCase(timerRepository);
    }

    @Test
    public void testBulkTimerCreationPerformance() {
        int timerCount = 100;
        List<String> timerIds = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // Create timers in bulk
        for (int i = 0; i < timerCount; i++) {
            Result<Timer> result = startTimerUseCase.execute("Performance Timer " + i, 5, "test-user");
            assertTrue("Timer " + i + " should be created", result.isSuccess());
            timerIds.add(result.getData().getId());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Performance assertion: Should create 100 timers in under 5 seconds
        assertTrue("Bulk timer creation should complete within 5 seconds", duration < 5000);
        
        System.out.println("Created " + timerCount + " timers in " + duration + "ms");
        System.out.println("Average time per timer: " + (duration / (double) timerCount) + "ms");
    }

    @Test
    public void testLargeDatasetRetrievalPerformance() {
        int timerCount = 500;
        List<String> timerIds = new ArrayList<>();
        
        // Create a large dataset
        for (int i = 0; i < timerCount; i++) {
            Result<Timer> result = startTimerUseCase.execute("Large Dataset Timer " + i, 10, "test-user");
            assertTrue("Timer " + i + " should be created", result.isSuccess());
            timerIds.add(result.getData().getId());
        }
        
        // Test retrieval performance
        long startTime = System.currentTimeMillis();
        
        Result<List<Timer>> getAllResult = getAllTimersUseCase.execute();
        assertTrue("Get all timers should succeed", getAllResult.isSuccess());
        assertEquals("Should have " + timerCount + " timers", timerCount, getAllResult.getData().size());
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Performance assertion: Should retrieve 500 timers in under 2 seconds
        assertTrue("Large dataset retrieval should complete within 2 seconds", duration < 2000);
        
        System.out.println("Retrieved " + timerCount + " timers in " + duration + "ms");
        System.out.println("Average time per timer: " + (duration / (double) timerCount) + "ms");
    }

    @Test
    public void testIndividualTimerRetrievalPerformance() {
        int timerCount = 100;
        List<String> timerIds = new ArrayList<>();
        
        // Create timers
        for (int i = 0; i < timerCount; i++) {
            Result<Timer> result = startTimerUseCase.execute("Individual Timer " + i, 15, "test-user");
            assertTrue("Timer " + i + " should be created", result.isSuccess());
            timerIds.add(result.getData().getId());
        }
        
        // Test individual retrieval performance
        long startTime = System.currentTimeMillis();
        
        for (String timerId : timerIds) {
            Result<Timer> result = getTimerByIdUseCase.execute(timerId);
            assertTrue("Get timer should succeed", result.isSuccess());
            assertNotNull("Retrieved timer should not be null", result.getData());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Performance assertion: Should retrieve 100 individual timers in under 3 seconds
        assertTrue("Individual timer retrieval should complete within 3 seconds", duration < 3000);
        
        System.out.println("Retrieved " + timerCount + " individual timers in " + duration + "ms");
        System.out.println("Average time per timer: " + (duration / (double) timerCount) + "ms");
    }

    @Test
    public void testBulkTimerDeletionPerformance() {
        int timerCount = 200;
        List<String> timerIds = new ArrayList<>();
        
        // Create timers
        for (int i = 0; i < timerCount; i++) {
            Result<Timer> result = startTimerUseCase.execute("Delete Timer " + i, 20, "test-user");
            assertTrue("Timer " + i + " should be created", result.isSuccess());
            timerIds.add(result.getData().getId());
        }
        
        // Verify timers exist
        Result<List<Timer>> getAllResult = getAllTimersUseCase.execute();
        assertEquals("Should have " + timerCount + " timers before deletion", timerCount, getAllResult.getData().size());
        
        // Test bulk deletion performance
        long startTime = System.currentTimeMillis();
        
        for (String timerId : timerIds) {
            Result<Void> result = deleteTimerUseCase.execute(timerId);
            assertTrue("Delete timer should succeed", result.isSuccess());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Performance assertion: Should delete 200 timers in under 4 seconds
        assertTrue("Bulk timer deletion should complete within 4 seconds", duration < 4000);
        
        // Verify all timers are deleted
        Result<List<Timer>> getAllAfterResult = getAllTimersUseCase.execute();
        assertEquals("Should have 0 timers after deletion", 0, getAllAfterResult.getData().size());
        
        System.out.println("Deleted " + timerCount + " timers in " + duration + "ms");
        System.out.println("Average time per timer: " + (duration / (double) timerCount) + "ms");
    }

    @Test
    public void testMemoryUsageWithLargeDataset() {
        int timerCount = 1000;
        List<String> timerIds = new ArrayList<>();
        
        // Get initial memory usage
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create large dataset
        for (int i = 0; i < timerCount; i++) {
            Result<Timer> result = startTimerUseCase.execute("Memory Timer " + i, 25, "test-user");
            assertTrue("Timer " + i + " should be created", result.isSuccess());
            timerIds.add(result.getData().getId());
        }
        
        // Force garbage collection to get accurate memory measurement
        System.gc();
        
        long memoryAfterCreation = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfterCreation - initialMemory;
        
        // Performance assertion: Memory usage should be reasonable (under 50MB for 1000 timers)
        long maxMemoryMB = 50 * 1024 * 1024; // 50MB
        assertTrue("Memory usage should be under 50MB", memoryUsed < maxMemoryMB);
        
        System.out.println("Memory used for " + timerCount + " timers: " + (memoryUsed / 1024 / 1024) + "MB");
        System.out.println("Average memory per timer: " + (memoryUsed / (double) timerCount / 1024) + "KB");
    }

    @Test
    public void testConcurrentTimerOperationsPerformance() throws InterruptedException {
        int threadCount = 10;
        int timersPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // Create threads for concurrent operations
        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            final int threadId = threadIndex;
            Thread thread = new Thread(() -> {
                try {
                    // Each thread creates timers
                    for (int i = 0; i < timersPerThread; i++) {
                        Result<Timer> result = startTimerUseCase.execute(
                            "Concurrent Timer " + threadId + "-" + i, 30, "test-user-" + threadId);
                        assertTrue("Timer should be created", result.isSuccess());
                    }
                    latch.countDown();
                } catch (Exception e) {
                    fail("Thread " + threadId + " failed: " + e.getMessage());
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // Wait for all threads to complete
        assertTrue("All threads should complete within 10 seconds", latch.await(10, TimeUnit.SECONDS));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Performance assertion: Should handle concurrent operations in under 10 seconds
        assertTrue("Concurrent operations should complete within 10 seconds", duration < 10000);
        
        // Verify all timers were created
        Result<List<Timer>> getAllResult = getAllTimersUseCase.execute();
        assertEquals("Should have " + (threadCount * timersPerThread) + " timers", 
                   threadCount * timersPerThread, getAllResult.getData().size());
        
        System.out.println("Created " + (threadCount * timersPerThread) + " timers concurrently in " + duration + "ms");
        System.out.println("Average time per timer: " + (duration / (double) (threadCount * timersPerThread)) + "ms");
    }

    @Test
    public void testDatabaseQueryPerformance() {
        int timerCount = 300;
        List<String> timerIds = new ArrayList<>();
        
        // Create timers with different durations
        for (int i = 0; i < timerCount; i++) {
            int duration = (i % 10) + 1; // 1-10 minutes
            Result<Timer> result = startTimerUseCase.execute("Query Timer " + i, duration, "test-user");
            assertTrue("Timer " + i + " should be created", result.isSuccess());
            timerIds.add(result.getData().getId());
        }
        
        // Test query performance with different operations
        long startTime = System.currentTimeMillis();
        
        // Multiple getAllTimers operations
        for (int i = 0; i < 10; i++) {
            Result<List<Timer>> result = getAllTimersUseCase.execute();
            assertTrue("Get all timers should succeed", result.isSuccess());
            assertEquals("Should have " + timerCount + " timers", timerCount, result.getData().size());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Performance assertion: Should handle multiple queries efficiently
        assertTrue("Multiple queries should complete within 3 seconds", duration < 3000);
        
        System.out.println("Executed 10 getAllTimers queries in " + duration + "ms");
        System.out.println("Average time per query: " + (duration / 10.0) + "ms");
    }
}
