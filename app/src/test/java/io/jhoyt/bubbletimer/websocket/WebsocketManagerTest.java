package io.jhoyt.bubbletimer.websocket;

import org.junit.Test;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.WebsocketManager;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import okhttp3.OkHttpClient;

public class WebsocketManagerTest {

    @Test
    public void testWebsocketManagerCreation() {
        // This test verifies that WebsocketManager can be instantiated
        // In a real test with Hilt, we would use @HiltAndroidTest and @Inject
        ActiveTimerRepository mockRepository = null; // Would be mocked in real test
        OkHttpClient mockClient = new OkHttpClient();
        
        WebsocketManager manager = new WebsocketManager(mockRepository, mockClient);
        assertNotNull("WebsocketManager should be created", manager);
    }

    @Test
    public void testConnectionStateEnum() {
        // Test that all connection states are available
        WebsocketManager.ConnectionState[] states = WebsocketManager.ConnectionState.values();
        assertEquals("Should have 4 connection states", 4, states.length);
        
        // Verify specific states exist
        assertNotNull("DISCONNECTED state should exist", 
            WebsocketManager.ConnectionState.valueOf("DISCONNECTED"));
        assertNotNull("CONNECTING state should exist", 
            WebsocketManager.ConnectionState.valueOf("CONNECTING"));
        assertNotNull("CONNECTED state should exist", 
            WebsocketManager.ConnectionState.valueOf("CONNECTED"));
        assertNotNull("RECONNECTING state should exist", 
            WebsocketManager.ConnectionState.valueOf("RECONNECTING"));
    }
} 