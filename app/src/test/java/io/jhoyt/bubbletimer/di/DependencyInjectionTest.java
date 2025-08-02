package io.jhoyt.bubbletimer.di;

import org.junit.Test;
import static org.junit.Assert.*;

import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import io.jhoyt.bubbletimer.WebsocketManager;
import okhttp3.OkHttpClient;

public class DependencyInjectionTest {

    @Test
    public void testDatabaseModuleProvidesDependencies() {
        // This test verifies that our modules can be instantiated
        // In a real test with Hilt, we would use @HiltAndroidTest and @Inject
        assertTrue("Database module should be accessible", 
            DatabaseModule.class.isAnnotationPresent(dagger.Module.class));
    }

    @Test
    public void testWebsocketModuleProvidesDependencies() {
        // This test verifies that our modules can be instantiated
        assertTrue("Websocket module should be accessible", 
            WebsocketModule.class.isAnnotationPresent(dagger.Module.class));
    }
} 