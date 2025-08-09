package io.jhoyt.bubbletimer.utils;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TimerSharingValidatorTest {

    @Test
    public void testEnsureCreatorIncluded_WithNullSharedWith() {
        Set<String> result = TimerSharingValidator.ensureCreatorIncluded(null, "creator123");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains("creator123"));
    }

    @Test
    public void testEnsureCreatorIncluded_WithEmptySharedWith() {
        Set<String> sharedWith = new HashSet<>();
        Set<String> result = TimerSharingValidator.ensureCreatorIncluded(sharedWith, "creator123");
        assertEquals(1, result.size());
        assertTrue(result.contains("creator123"));
    }

    @Test
    public void testEnsureCreatorIncluded_WithCreatorAlreadyIncluded() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "creator123", "user2"));
        Set<String> result = TimerSharingValidator.ensureCreatorIncluded(sharedWith, "creator123");
        assertEquals(3, result.size());
        assertTrue(result.contains("creator123"));
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testEnsureCreatorIncluded_WithCreatorNotIncluded() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        Set<String> result = TimerSharingValidator.ensureCreatorIncluded(sharedWith, "creator123");
        assertEquals(3, result.size());
        assertTrue(result.contains("creator123"));
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testEnsureCreatorIncluded_WithNullCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        Set<String> result = TimerSharingValidator.ensureCreatorIncluded(sharedWith, null);
        assertEquals(2, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testEnsureCreatorIncluded_WithEmptyCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        Set<String> result = TimerSharingValidator.ensureCreatorIncluded(sharedWith, "");
        assertEquals(2, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testEnsureCreatorIncluded_WithWhitespaceCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        Set<String> result = TimerSharingValidator.ensureCreatorIncluded(sharedWith, "   ");
        assertEquals(2, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testEnsureCreatorIncluded_WithTrimmedCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        Set<String> result = TimerSharingValidator.ensureCreatorIncluded(sharedWith, "  creator123  ");
        assertEquals(3, result.size());
        assertTrue(result.contains("creator123"));
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testIsValidSharedWithSet_WithValidSet() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "creator123", "user2"));
        assertTrue(TimerSharingValidator.isValidSharedWithSet(sharedWith, "creator123"));
    }

    @Test
    public void testIsValidSharedWithSet_WithNullSet() {
        assertFalse(TimerSharingValidator.isValidSharedWithSet(null, "creator123"));
    }

    @Test
    public void testIsValidSharedWithSet_WithNullValues() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", null, "user2"));
        assertFalse(TimerSharingValidator.isValidSharedWithSet(sharedWith, "creator123"));
    }

    @Test
    public void testIsValidSharedWithSet_WithEmptyValues() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "", "user2"));
        assertFalse(TimerSharingValidator.isValidSharedWithSet(sharedWith, "creator123"));
    }

    @Test
    public void testIsValidSharedWithSet_WithWhitespaceValues() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "   ", "user2"));
        assertFalse(TimerSharingValidator.isValidSharedWithSet(sharedWith, "creator123"));
    }

    @Test
    public void testIsValidSharedWithSet_WithDuplicates() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user1", "user2"));
        assertFalse(TimerSharingValidator.isValidSharedWithSet(sharedWith, "creator123"));
    }

    @Test
    public void testIsValidSharedWithSet_WithCaseInsensitiveDuplicates() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "USER1", "user2"));
        assertFalse(TimerSharingValidator.isValidSharedWithSet(sharedWith, "creator123"));
    }

    @Test
    public void testIsValidSharedWithSet_WithCreatorNotIncluded() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        assertFalse(TimerSharingValidator.isValidSharedWithSet(sharedWith, "creator123"));
    }

    @Test
    public void testIsValidSharedWithSet_WithEmptySet() {
        Set<String> sharedWith = new HashSet<>();
        assertTrue(TimerSharingValidator.isValidSharedWithSet(sharedWith, "creator123"));
    }

    @Test
    public void testIsValidSharedWithSet_WithNullCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        assertTrue(TimerSharingValidator.isValidSharedWithSet(sharedWith, null));
    }

    @Test
    public void testCleanSharedWithSet_WithValidSet() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2", "user3"));
        Set<String> result = TimerSharingValidator.cleanSharedWithSet(sharedWith);
        assertEquals(3, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
        assertTrue(result.contains("user3"));
    }

    @Test
    public void testCleanSharedWithSet_WithNullSet() {
        Set<String> result = TimerSharingValidator.cleanSharedWithSet(null);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testCleanSharedWithSet_WithNullValues() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", null, "user2"));
        Set<String> result = TimerSharingValidator.cleanSharedWithSet(sharedWith);
        assertEquals(2, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testCleanSharedWithSet_WithEmptyValues() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "", "user2"));
        Set<String> result = TimerSharingValidator.cleanSharedWithSet(sharedWith);
        assertEquals(2, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testCleanSharedWithSet_WithWhitespaceValues() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "   ", "user2"));
        Set<String> result = TimerSharingValidator.cleanSharedWithSet(sharedWith);
        assertEquals(2, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testCleanSharedWithSet_WithTrimmedValues() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("  user1  ", "user2", "  user3  "));
        Set<String> result = TimerSharingValidator.cleanSharedWithSet(sharedWith);
        assertEquals(3, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
        assertTrue(result.contains("user3"));
    }

    @Test
    public void testIsValidUserId_WithValidUserId() {
        assertTrue(TimerSharingValidator.isValidUserId("user123"));
        assertTrue(TimerSharingValidator.isValidUserId("user-123"));
        assertTrue(TimerSharingValidator.isValidUserId("user_123"));
    }

    @Test
    public void testIsValidUserId_WithNullUserId() {
        assertFalse(TimerSharingValidator.isValidUserId(null));
    }

    @Test
    public void testIsValidUserId_WithEmptyUserId() {
        assertFalse(TimerSharingValidator.isValidUserId(""));
    }

    @Test
    public void testIsValidUserId_WithWhitespaceUserId() {
        assertFalse(TimerSharingValidator.isValidUserId("   "));
    }

    @Test
    public void testIsTimerShared_WithSharedTimer() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("creator123", "user1", "user2"));
        assertTrue(TimerSharingValidator.isTimerShared(sharedWith, "creator123"));
    }

    @Test
    public void testIsTimerShared_WithOnlyCreator() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("creator123"));
        assertFalse(TimerSharingValidator.isTimerShared(sharedWith, "creator123"));
    }

    @Test
    public void testIsTimerShared_WithEmptySet() {
        Set<String> sharedWith = new HashSet<>();
        assertFalse(TimerSharingValidator.isTimerShared(sharedWith, "creator123"));
    }

    @Test
    public void testIsTimerShared_WithNullSet() {
        assertFalse(TimerSharingValidator.isTimerShared(null, "creator123"));
    }

    @Test
    public void testIsTimerShared_WithNullCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        assertTrue(TimerSharingValidator.isTimerShared(sharedWith, null));
    }

    @Test
    public void testIsTimerShared_WithEmptyCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        assertTrue(TimerSharingValidator.isTimerShared(sharedWith, ""));
    }

    @Test
    public void testGetSharedUsersExcludingCreator_WithMultipleUsers() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("creator123", "user1", "user2"));
        Set<String> result = TimerSharingValidator.getSharedUsersExcludingCreator(sharedWith, "creator123");
        assertEquals(2, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
        assertFalse(result.contains("creator123"));
    }

    @Test
    public void testGetSharedUsersExcludingCreator_WithOnlyCreator() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("creator123"));
        Set<String> result = TimerSharingValidator.getSharedUsersExcludingCreator(sharedWith, "creator123");
        assertEquals(0, result.size());
    }

    @Test
    public void testGetSharedUsersExcludingCreator_WithEmptySet() {
        Set<String> sharedWith = new HashSet<>();
        Set<String> result = TimerSharingValidator.getSharedUsersExcludingCreator(sharedWith, "creator123");
        assertEquals(0, result.size());
    }

    @Test
    public void testGetSharedUsersExcludingCreator_WithNullSet() {
        Set<String> result = TimerSharingValidator.getSharedUsersExcludingCreator(null, "creator123");
        assertEquals(0, result.size());
    }

    @Test
    public void testGetSharedUsersExcludingCreator_WithNullCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        Set<String> result = TimerSharingValidator.getSharedUsersExcludingCreator(sharedWith, null);
        assertEquals(2, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testGetSharedUsersExcludingCreator_WithEmptyCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        Set<String> result = TimerSharingValidator.getSharedUsersExcludingCreator(sharedWith, "");
        assertEquals(2, result.size());
        assertTrue(result.contains("user1"));
        assertTrue(result.contains("user2"));
    }

    @Test
    public void testLogSharedWithState_DoesNotThrowException() {
        // This test ensures the logging method doesn't throw exceptions
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "creator123", "user2"));
        try {
            TimerSharingValidator.logSharedWithState(sharedWith, "creator123", "Test Context");
            // If we get here, no exception was thrown
            assertTrue(true);
        } catch (Exception e) {
            fail("logSharedWithState threw an exception: " + e.getMessage());
        }
    }

    @Test
    public void testLogSharedWithState_WithNullSet() {
        try {
            TimerSharingValidator.logSharedWithState(null, "creator123", "Test Context");
            assertTrue(true);
        } catch (Exception e) {
            fail("logSharedWithState threw an exception with null set: " + e.getMessage());
        }
    }

    @Test
    public void testLogSharedWithState_WithNullCreatorId() {
        Set<String> sharedWith = new HashSet<>(Arrays.asList("user1", "user2"));
        try {
            TimerSharingValidator.logSharedWithState(sharedWith, null, "Test Context");
            assertTrue(true);
        } catch (Exception e) {
            fail("logSharedWithState threw an exception with null creator ID: " + e.getMessage());
        }
    }
}
