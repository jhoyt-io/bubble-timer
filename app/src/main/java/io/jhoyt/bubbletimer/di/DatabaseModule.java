package io.jhoyt.bubbletimer.di;

import android.app.Application;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import io.jhoyt.bubbletimer.db.AppDatabase;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    public AppDatabase provideAppDatabase(Application application) {
        return AppDatabase.getDatabase(application);
    }

    @Provides
    public ActiveTimerRepository provideActiveTimerRepository(Application application) {
        return new ActiveTimerRepository(application);
    }
} 