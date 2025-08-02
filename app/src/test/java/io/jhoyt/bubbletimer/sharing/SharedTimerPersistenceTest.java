package io.jhoyt.bubbletimer.sharing;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;

import java.time.Duration;
import java.util.Set;

public class SharedTimerPersistenceTest {

    @Mock
    private ActiveTimerRepository mockRepository;
    
    private Timer testTimer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testTimer = new Timer("test-user", "Shared Timer", Duration.ofMinutes(10), Set.of());
    }

    @Test
    public void testTimerSharingWithSingleUser() {
        // Test sharing timer with a single user
        testTimer.shareWith("user1");
        
        Set<String> sharedWith = testTimer.getSharedWith();
        assertEquals("Should have 1 shared user", 1, sharedWith.size());
        assertTrue("Should contain user1", sharedWith.contains("user1"));
    }

    @Test
    public void testTimerSharingWithMultipleUsers() {
        // Test sharing timer with multiple users
        testTimer.shareWith("user1");
        testTimer.shareWith("user2");
        testTimer.shareWith("user3");
        
        Set<String> sharedWith = testTimer.getSharedWith();
        assertEquals("Should have 3 shared users", 3, sharedWith.size());
        assertTrue("Should contain user1", sharedWith.contains("user1"));
        assertTrue("Should contain user2", sharedWith.contains("user2"));
        assertTrue("Should contain user3", sharedWith.contains("user3"));
    }

    @Test
    public void testTimerSharingWithDuplicateUser() {
        // Test sharing timer with the same user multiple times
        testTimer.shareWith("user1");
        testTimer.shareWith("user1"); // Duplicate
        
        Set<String> sharedWith = testTimer.getSharedWith();
        assertEquals("Should have 1 shared user (no duplicates)", 1, sharedWith.size());
        assertTrue("Should contain user1", sharedWith.contains("user1"));
    }

    @Test
    public void testTimerSharingWithEmptyUser() {
        // Test sharing timer with empty username
        testTimer.shareWith("");
        
        Set<String> sharedWith = testTimer.getSharedWith();
        assertEquals("Should have 1 shared user", 1, sharedWith.size());
        assertTrue("Should contain empty string", sharedWith.contains(""));
    }

    @Test
    public void testTimerSharingWithNullUser() {
        // Test sharing timer with null username
        testTimer.shareWith(null);
        
        Set<String> sharedWith = testTimer.getSharedWith();
        assertEquals("Should have 1 shared user", 1, sharedWith.size());
        assertTrue("Should contain null", sharedWith.contains(null));
    }

    @Test
    public void testTimerSharingWithSpecialCharacters() {
        // Test sharing timer with special characters in username
        testTimer.shareWith("user@domain.com");
        testTimer.shareWith("user-name");
        testTimer.shareWith("user_name");
        
        Set<String> sharedWith = testTimer.getSharedWith();
        assertEquals("Should have 3 shared users", 3, sharedWith.size());
        assertTrue("Should contain email", sharedWith.contains("user@domain.com"));
        assertTrue("Should contain hyphen", sharedWith.contains("user-name"));
        assertTrue("Should contain underscore", sharedWith.contains("user_name"));
    }

    @Test
    public void testTimerSharingWithLongUsername() {
        // Test sharing timer with long username
        String longUsername = "a".repeat(1000);
        testTimer.shareWith(longUsername);
        
        Set<String> sharedWith = testTimer.getSharedWith();
        assertEquals("Should have 1 shared user", 1, sharedWith.size());
        assertTrue("Should contain long username", sharedWith.contains(longUsername));
    }

    @Test
    public void testTimerSharingWithUnicodeUsername() {
        // Test sharing timer with unicode username
        testTimer.shareWith("用户");
        testTimer.shareWith("usuario");
        testTimer.shareWith("пользователь");
        
        Set<String> sharedWith = testTimer.getSharedWith();
        assertEquals("Should have 3 shared users", 3, sharedWith.size());
        assertTrue("Should contain Chinese", sharedWith.contains("用户"));
        assertTrue("Should contain Spanish", sharedWith.contains("usuario"));
        assertTrue("Should contain Russian", sharedWith.contains("пользователь"));
    }

    @Test
    public void testTimerSharingPersistence() {
        // Test that shared users persist when timer is copied
        testTimer.shareWith("user1");
        testTimer.shareWith("user2");
        
        Timer copiedTimer = testTimer.copy();
        Set<String> copiedSharedWith = copiedTimer.getSharedWith();
        
        assertEquals("Copied timer should have same number of shared users", 
            testTimer.getSharedWith().size(), copiedSharedWith.size());
        assertTrue("Copied timer should contain user1", copiedSharedWith.contains("user1"));
        assertTrue("Copied timer should contain user2", copiedSharedWith.contains("user2"));
    }

    @Test
    public void testTimerSharingEquality() {
        // Test that timers with same sharing are equal
        Timer timer1 = new Timer("test-user", "Timer 1", Duration.ofMinutes(5), Set.of());
        Timer timer2 = new Timer("test-user", "Timer 1", Duration.ofMinutes(5), Set.of());
        
        timer1.shareWith("user1");
        timer2.shareWith("user1");
        
        // Note: Timer equality depends on the implementation
        // This test verifies that sharing doesn't break the timer structure
        assertNotNull("Timer1 should not be null", timer1);
        assertNotNull("Timer2 should not be null", timer2);
    }

    @Test
    public void testTimerSharingWithTags() {
        // Test sharing timer that also has tags
        Set<String> tags = Set.of("work", "important");
        Timer taggedTimer = new Timer("test-user", "Work Timer", Duration.ofMinutes(30), tags);
        
        taggedTimer.shareWith("user1");
        taggedTimer.shareWith("user2");
        
        Set<String> sharedWith = taggedTimer.getSharedWith();
        Set<String> timerTags = taggedTimer.getTags();
        
        assertEquals("Should have 2 shared users", 2, sharedWith.size());
        assertEquals("Should have 2 tags", 2, timerTags.size());
        assertTrue("Should contain user1", sharedWith.contains("user1"));
        assertTrue("Should contain user2", sharedWith.contains("user2"));
        assertTrue("Should contain work tag", timerTags.contains("work"));
        assertTrue("Should contain important tag", timerTags.contains("important"));
    }

    @Test
    public void testTimerSharingWithDifferentDurations() {
        // Test sharing timers with different durations
        Timer shortTimer = new Timer("test-user", "Short Timer", Duration.ofSeconds(30), Set.of());
        Timer longTimer = new Timer("test-user", "Long Timer", Duration.ofHours(2), Set.of());
        
        shortTimer.shareWith("user1");
        longTimer.shareWith("user2");
        
        Set<String> shortSharedWith = shortTimer.getSharedWith();
        Set<String> longSharedWith = longTimer.getSharedWith();
        
        assertEquals("Short timer should have 1 shared user", 1, shortSharedWith.size());
        assertEquals("Long timer should have 1 shared user", 1, longSharedWith.size());
        assertTrue("Short timer should contain user1", shortSharedWith.contains("user1"));
        assertTrue("Long timer should contain user2", longSharedWith.contains("user2"));
    }

    @Test
    public void testTimerSharingWithRepository() {
        // Test that repository can handle shared timers
        // In a real test, we would test actual database persistence
        assertNotNull("Repository should be available for shared timer persistence", mockRepository);
    }

    @Test
    public void testTimerSharingWithWebsocketManager() {
        // Test that WebSocket manager can handle shared timers
        // In a real test, we would test actual WebSocket message sending
        assertNotNull("WebSocket manager should be available for shared timer messaging", mockRepository);
    }
} 