package io.jhoyt.bubbletimer.di;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import io.jhoyt.bubbletimer.WebsocketManager;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import okhttp3.OkHttpClient;

@Module
@InstallIn(SingletonComponent.class)
public class WebsocketModule {

    @Provides
    public OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .hostnameVerifier((hostname, session) -> {
                    // Allow connections to our WebSocket endpoint even with IP-based URLs
                    if (hostname.equals("3.208.157.175") || 
                        hostname.equals("3.92.248.202") || 
                        hostname.equals("52.0.126.253") ||
                        hostname.equals("zc4ahryh1l.execute-api.us-east-1.amazonaws.com")) {
                        return true;
                    }
                    // For other hosts, use default verification
                    return javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session);
                })
                .build();
    }

    @Provides
    public WebsocketManager provideWebsocketManager(
            ActiveTimerRepository activeTimerRepository,
            OkHttpClient okHttpClient
    ) {
        return new WebsocketManager(activeTimerRepository, okHttpClient);
    }
} 