package io.jhoyt.bubbletimer.websocket;

import org.junit.Test;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.WebsocketManager;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import okhttp3.OkHttpClient;

public class WebsocketManagerConnectionTest {

    @Test
    public void testConnectionStateEnum() {
        // Test that connection states are properly defined
        WebsocketManager.ConnectionState[] states = WebsocketManager.ConnectionState.values();
        assertEquals("Should have exactly 4 connection states", 4, states.length);
        
        // Verify all expected states exist
        assertNotNull("DISCONNECTED state should exist", 
            WebsocketManager.ConnectionState.valueOf("DISCONNECTED"));
        assertNotNull("CONNECTING state should exist", 
            WebsocketManager.ConnectionState.valueOf("CONNECTING"));
        assertNotNull("CONNECTED state should exist", 
            WebsocketManager.ConnectionState.valueOf("CONNECTED"));
        assertNotNull("RECONNECTING state should exist", 
            WebsocketManager.ConnectionState.valueOf("RECONNECTING"));
    }

    @Test
    public void testWebsocketManagerCreation() {
        // Test that WebsocketManager can be instantiated
        ActiveTimerRepository mockRepository = null; // Would be mocked in real test
        OkHttpClient mockClient = new OkHttpClient();
        
        WebsocketManager manager = new WebsocketManager(mockRepository, mockClient);
        assertNotNull("WebsocketManager should be created", manager);
    }

    @Test
    public void testConstructorWithNullRepository() {
        // Test that constructor handles null repository gracefully
        OkHttpClient mockClient = new OkHttpClient();
        WebsocketManager manager = new WebsocketManager(null, mockClient);
        assertNotNull("WebsocketManager should be created even with null repository", manager);
    }

    @Test
    public void testConstructorWithNullClient() {
        // Test that constructor handles null client gracefully
        ActiveTimerRepository mockRepository = null;
        WebsocketManager manager = new WebsocketManager(mockRepository, null);
        assertNotNull("WebsocketManager should be created even with null client", manager);
    }

    @Test
    public void testInitialConnectionState() {
        // Test that the initial connection state is DISCONNECTED
        ActiveTimerRepository mockRepository = null;
        OkHttpClient mockClient = new OkHttpClient();
        WebsocketManager manager = new WebsocketManager(mockRepository, mockClient);
        
        assertEquals("Initial connection state should be DISCONNECTED", 
            WebsocketManager.ConnectionState.DISCONNECTED, 
            manager.getConnectionState());
    }
} 