package io.jhoyt.bubbletimer.integration;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.db.ActiveTimer;
import io.jhoyt.bubbletimer.db.ActiveTimerDao;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import io.jhoyt.bubbletimer.db.AppDatabase;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DatabaseIntegrationTest {

    private AppDatabase database;
    private ActiveTimerDao dao;
    private ActiveTimerRepository repository;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries() // For testing only
                .build();
        
        dao = database.activeTimerDao();
        repository = new ActiveTimerRepository(context);
    }

    @After
    public void closeDb() {
        database.close();
    }

    @Test
    public void testInsertAndRetrieveTimer() {
        // Create test timer
        long currentTime = System.currentTimeMillis();
        ActiveTimer timer = new ActiveTimer("test-id", "Test Timer", 300000L, 150000L, currentTime + 300000);
        
        // Insert timer
        dao.insertActiveTimer(timer);
        
        // Retrieve timer
        ActiveTimer retrieved = dao.getActiveTimerById("test-id");
        
        // Verify
        assertNotNull("Retrieved timer should not be null", retrieved);
        assertEquals("ID should match", "test-id", retrieved.getId());
        assertEquals("Name should match", "Test Timer", retrieved.getName());
        assertEquals("Total duration should match", 300000L, retrieved.getTotalDuration());
        assertEquals("Remaining duration should match", 150000L, retrieved.getRemainingDuration());
        assertEquals("Timer end should match", currentTime + 300000, retrieved.getTimerEnd());
    }

    @Test
    public void testUpdateTimer() {
        // Create and insert timer
        long currentTime = System.currentTimeMillis();
        ActiveTimer timer = new ActiveTimer("test-id", "Original Name", 300000L, 150000L, currentTime + 300000);
        dao.insertActiveTimer(timer);
        
        // Update timer
        ActiveTimer updatedTimer = new ActiveTimer("test-id", "Updated Name", 300000L, 100000L, currentTime + 200000);
        dao.updateActiveTimer(updatedTimer);
        
        // Retrieve updated timer
        ActiveTimer retrieved = dao.getActiveTimerById("test-id");
        
        // Verify
        assertNotNull("Retrieved timer should not be null", retrieved);
        assertEquals("Name should be updated", "Updated Name", retrieved.getName());
        assertEquals("Remaining duration should be updated", 100000L, retrieved.getRemainingDuration());
        assertEquals("Timer end should be updated", currentTime + 200000, retrieved.getTimerEnd());
    }

    @Test
    public void testDeleteTimer() {
        // Create and insert timer
        ActiveTimer timer = new ActiveTimer("test-id", "Test Timer", 300000L, 150000L, System.currentTimeMillis() + 300000);
        dao.insertActiveTimer(timer);
        
        // Verify timer exists
        ActiveTimer retrieved = dao.getActiveTimerById("test-id");
        assertNotNull("Timer should exist before deletion", retrieved);
        
        // Delete timer
        dao.deleteById("test-id");
        
        // Verify timer is deleted
        ActiveTimer deleted = dao.getActiveTimerById("test-id");
        assertNull("Timer should be null after deletion", deleted);
    }

    @Test
    public void testGetAllActiveTimers() {
        // Create multiple timers
        long currentTime = System.currentTimeMillis();
        ActiveTimer timer1 = new ActiveTimer("timer1", "Timer 1", 300000L, 150000L, currentTime + 300000);
        ActiveTimer timer2 = new ActiveTimer("timer2", "Timer 2", 600000L, 300000L, currentTime + 600000);
        ActiveTimer timer3 = new ActiveTimer("timer3", "Timer 3", 900000L, 450000L, currentTime + 900000);
        
        // Insert timers
        dao.insertActiveTimer(timer1);
        dao.insertActiveTimer(timer2);
        dao.insertActiveTimer(timer3);
        
        // Retrieve all timers
        List<ActiveTimer> allTimers = dao.getAllActiveTimers();
        
        // Verify
        assertNotNull("Timer list should not be null", allTimers);
        assertEquals("Should have 3 timers", 3, allTimers.size());
        
        // Verify all timers are present
        boolean foundTimer1 = false, foundTimer2 = false, foundTimer3 = false;
        for (ActiveTimer timer : allTimers) {
            if ("timer1".equals(timer.getId())) foundTimer1 = true;
            if ("timer2".equals(timer.getId())) foundTimer2 = true;
            if ("timer3".equals(timer.getId())) foundTimer3 = true;
        }
        
        assertTrue("Should find timer1", foundTimer1);
        assertTrue("Should find timer2", foundTimer2);
        assertTrue("Should find timer3", foundTimer3);
    }

    @Test
    public void testGetActiveTimerCount() {
        // Initially should be 0
        int initialCount = dao.getActiveTimerCount();
        assertEquals("Initial count should be 0", 0, initialCount);
        
        // Add a timer
        ActiveTimer timer = new ActiveTimer("test-id", "Test Timer", 300000L, 150000L, System.currentTimeMillis() + 300000);
        dao.insertActiveTimer(timer);
        
        // Count should be 1
        int countAfterInsert = dao.getActiveTimerCount();
        assertEquals("Count should be 1 after insert", 1, countAfterInsert);
        
        // Delete the timer
        dao.deleteById("test-id");
        
        // Count should be 0 again
        int countAfterDelete = dao.getActiveTimerCount();
        assertEquals("Count should be 0 after delete", 0, countAfterDelete);
    }

    @Test
    public void testDeleteAllActiveTimers() {
        // Create multiple timers
        ActiveTimer timer1 = new ActiveTimer("timer1", "Timer 1", 300000L, 150000L, System.currentTimeMillis() + 300000);
        ActiveTimer timer2 = new ActiveTimer("timer2", "Timer 2", 600000L, 300000L, System.currentTimeMillis() + 600000);
        
        // Insert timers
        dao.insertActiveTimer(timer1);
        dao.insertActiveTimer(timer2);
        
        // Verify timers exist
        assertEquals("Should have 2 timers", 2, dao.getActiveTimerCount());
        
        // Delete all timers
        dao.deleteAllActiveTimers();
        
        // Verify all timers are deleted
        assertEquals("Should have 0 timers after delete all", 0, dao.getActiveTimerCount());
        assertTrue("Timer list should be empty", dao.getAllActiveTimers().isEmpty());
    }

    @Test
    public void testDatabaseSchema() {
        // Test that the database schema is correct by inserting and retrieving data
        long currentTime = System.currentTimeMillis();
        ActiveTimer timer = new ActiveTimer("schema-test", "Schema Test", 300000L, 150000L, currentTime + 300000);
        
        // Insert should not throw exceptions
        dao.insertActiveTimer(timer);
        
        // Retrieve should work
        ActiveTimer retrieved = dao.getActiveTimerById("schema-test");
        assertNotNull("Retrieved timer should not be null", retrieved);
        
        // All fields should be preserved
        assertEquals("ID should be preserved", "schema-test", retrieved.getId());
        assertEquals("Name should be preserved", "Schema Test", retrieved.getName());
        assertEquals("Total duration should be preserved", 300000L, retrieved.getTotalDuration());
        assertEquals("Remaining duration should be preserved", 150000L, retrieved.getRemainingDuration());
        assertEquals("Timer end should be preserved", currentTime + 300000, retrieved.getTimerEnd());
    }
} 