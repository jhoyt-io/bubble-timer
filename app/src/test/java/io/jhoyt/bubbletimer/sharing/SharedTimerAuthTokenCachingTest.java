package io.jhoyt.bubbletimer.sharing;

import android.app.Application;
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

import io.jhoyt.bubbletimer.db.AppDatabase;
import io.jhoyt.bubbletimer.db.SharedTimerRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class SharedTimerAuthTokenCachingTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private SharedTimerRepository repository;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        
        repository = new SharedTimerRepository((Application) context);
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void testCacheAuthToken_StoresTokenCorrectly() {
        // Given: A valid auth token
        String testToken = "test-auth-token-123";
        
        // When: Cache the token
        repository.cacheAuthToken(testToken);
        
        // Then: Token should be retrievable
        String cachedToken = repository.getCachedAuthToken();
        assertEquals("Cached token should match original", testToken, cachedToken);
    }

    @Test
    public void testCacheAuthToken_NullToken_DoesNotCache() {
        // Given: A null auth token
        String nullToken = null;
        
        // When: Try to cache null token
        repository.cacheAuthToken(nullToken);
        
        // Then: No token should be cached
        String cachedToken = repository.getCachedAuthToken();
        assertNull("Null token should not be cached", cachedToken);
    }

    @Test
    public void testCacheAuthToken_EmptyToken_DoesNotCache() {
        // Given: An empty auth token
        String emptyToken = "";
        
        // When: Try to cache empty token
        repository.cacheAuthToken(emptyToken);
        
        // Then: No token should be cached
        String cachedToken = repository.getCachedAuthToken();
        assertNull("Empty token should not be cached", cachedToken);
    }

    @Test
    public void testClearCachedAuthToken_RemovesToken() {
        // Given: A cached auth token
        String testToken = "test-auth-token-123";
        repository.cacheAuthToken(testToken);
        
        // Verify token is cached
        assertNotNull("Token should be cached initially", repository.getCachedAuthToken());
        
        // When: Clear the cached token
        repository.clearCachedAuthToken();
        
        // Then: Token should be removed
        String cachedToken = repository.getCachedAuthToken();
        assertNull("Cached token should be cleared", cachedToken);
    }

    @Test
    public void testGetCachedAuthToken_InitiallyNull() {
        // Given: No cached token
        
        // When: Get cached token
        String cachedToken = repository.getCachedAuthToken();
        
        // Then: Should be null
        assertNull("Initially no token should be cached", cachedToken);
    }

    @Test
    public void testCacheAuthToken_OverwritesExistingToken() {
        // Given: An existing cached token
        String firstToken = "first-token-123";
        repository.cacheAuthToken(firstToken);
        
        // Verify first token is cached
        assertEquals("First token should be cached", firstToken, repository.getCachedAuthToken());
        
        // When: Cache a different token
        String secondToken = "second-token-456";
        repository.cacheAuthToken(secondToken);
        
        // Then: Second token should replace first token
        String cachedToken = repository.getCachedAuthToken();
        assertEquals("Second token should replace first token", secondToken, cachedToken);
    }
}
