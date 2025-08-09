package io.jhoyt.bubbletimer.sharing;

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
import java.util.List;

import io.jhoyt.bubbletimer.db.AppDatabase;
import io.jhoyt.bubbletimer.db.SharedTimer;
import io.jhoyt.bubbletimer.db.SharedTimerDao;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class SharedTimerAggressiveDeletionTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private SharedTimerDao sharedTimerDao;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        
        sharedTimerDao = database.sharedTimerDao();
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void testDeleteAllSharedTimers_RemovesAllTimers() {
        // Given: Some existing shared timers in the database
        SharedTimer existingTimer1 = createTestSharedTimer("timer1", "Test Timer 1");
        SharedTimer existingTimer2 = createTestSharedTimer("timer2", "Test Timer 2");
        
        sharedTimerDao.insertAll(List.of(existingTimer1, existingTimer2));
        
        // Verify initial state
        List<SharedTimer> initialTimers = sharedTimerDao.getAllSharedTimersSync();
        assertEquals("Should have 2 initial timers", 2, initialTimers.size());
        
        // When: Delete all shared timers
        sharedTimerDao.deleteAll();
        
        // Then: Should have no timers after deletion
        List<SharedTimer> finalTimers = sharedTimerDao.getAllSharedTimersSync();
        assertEquals("Should have 0 timers after deletion", 0, finalTimers.size());
    }

    @Test
    public void testDeleteAllSharedTimers_EmptyDatabase_NoError() {
        // Given: Empty database
        List<SharedTimer> initialTimers = sharedTimerDao.getAllSharedTimersSync();
        assertEquals("Should have 0 initial timers", 0, initialTimers.size());
        
        // When: Delete all shared timers
        sharedTimerDao.deleteAll();
        
        // Then: Should still have no timers (no error)
        List<SharedTimer> finalTimers = sharedTimerDao.getAllSharedTimersSync();
        assertEquals("Should have 0 timers after deletion", 0, finalTimers.size());
    }

    @Test
    public void testDeleteAllSharedTimers_ThenInsertNew_WorksCorrectly() {
        // Given: Some existing shared timers in the database
        SharedTimer existingTimer1 = createTestSharedTimer("timer1", "Test Timer 1");
        SharedTimer existingTimer2 = createTestSharedTimer("timer2", "Test Timer 2");
        
        sharedTimerDao.insertAll(List.of(existingTimer1, existingTimer2));
        
        // Verify initial state
        List<SharedTimer> initialTimers = sharedTimerDao.getAllSharedTimersSync();
        assertEquals("Should have 2 initial timers", 2, initialTimers.size());
        
        // When: Delete all and insert new timer
        sharedTimerDao.deleteAll();
        SharedTimer newTimer = createTestSharedTimer("timer3", "New Timer");
        sharedTimerDao.insert(newTimer);
        
        // Then: Should have only the new timer
        List<SharedTimer> finalTimers = sharedTimerDao.getAllSharedTimersSync();
        assertEquals("Should have 1 timer after delete and insert", 1, finalTimers.size());
        assertEquals("Should have the new timer", "timer3", finalTimers.get(0).timerId);
    }

    private SharedTimer createTestSharedTimer(String timerId, String name) {
        SharedTimer timer = new SharedTimer();
        timer.timerId = timerId;
        timer.name = name;
        timer.userId = "test-user";
        timer.totalDuration = Duration.ofMinutes(5);
        timer.status = "PENDING";
        timer.sharedBy = "test-user";
        timer.sharedWith = "";
        timer.createdAt = java.time.LocalDateTime.now();
        return timer;
    }
}
