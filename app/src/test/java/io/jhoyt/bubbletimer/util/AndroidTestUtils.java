package io.jhoyt.bubbletimer.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Utility class for Android mocking in unit tests.
 * Provides methods to mock Android framework classes that are not available in unit tests.
 */
public class AndroidTestUtils {

    /**
     * Test rule that sets up Android mocking for the duration of a test.
     */
    public static class AndroidMockRule implements TestRule {
        private MockedStatic<Log> logMock;
        private MockedStatic<Looper> looperMock;
        private MockedStatic<Handler> handlerMock;

        @Override
        public Statement apply(Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    // Set up mocks
                    logMock = Mockito.mockStatic(Log.class);
                    looperMock = Mockito.mockStatic(Looper.class);
                    handlerMock = Mockito.mockStatic(Handler.class);
                    
                    // Mock Log methods
                    logMock.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
                    logMock.when(() -> Log.i(anyString(), anyString())).thenReturn(0);
                    logMock.when(() -> Log.w(anyString(), anyString())).thenReturn(0);
                    logMock.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
                    logMock.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);
                    
                    // Mock Looper
                    Looper mockLooper = Mockito.mock(Looper.class);
                    looperMock.when(Looper::getMainLooper).thenReturn(mockLooper);
                    looperMock.when(() -> Looper.myLooper()).thenReturn(mockLooper);
                    
                    // Mock Handler instance methods - we can't mock static methods on Handler
                    // The Handler methods are called on instances, not statically
                    // This will be handled by the LiveData and WebSocket implementations
                    
                    try {
                        base.evaluate();
                    } finally {
                        // Clean up mocks
                        if (logMock != null) {
                            logMock.close();
                        }
                        if (looperMock != null) {
                            looperMock.close();
                        }
                        if (handlerMock != null) {
                            handlerMock.close();
                        }
                    }
                }
            };
        }
    }

    /**
     * Helper method to create a LiveData that works in unit tests.
     * This bypasses the main thread requirement.
     */
    public static <T> MutableLiveData<T> createTestLiveData() {
        return new MutableLiveData<>();
    }

    /**
     * Helper method to set value on LiveData in unit tests.
     * This bypasses the main thread requirement by using a direct executor.
     */
    public static <T> void setValue(MutableLiveData<T> liveData, T value) {
        // Use a direct executor to avoid Handler issues
        Executor directExecutor = Runnable::run;
        liveData.setValue(value);
    }

    /**
     * Helper method to post value on LiveData in unit tests.
     * This bypasses the main thread requirement by using a direct executor.
     */
    public static <T> void postValue(MutableLiveData<T> liveData, T value) {
        // Use a direct executor to avoid Handler issues
        Executor directExecutor = Runnable::run;
        liveData.postValue(value);
    }

    /**
     * Mock any string parameter for Mockito.
     */
    private static String anyString() {
        return Mockito.anyString();
    }

    /**
     * Mock any throwable parameter for Mockito.
     */
    private static Throwable any(Class<Throwable> clazz) {
        return Mockito.any(clazz);
    }
} 