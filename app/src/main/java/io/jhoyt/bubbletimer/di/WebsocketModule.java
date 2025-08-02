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