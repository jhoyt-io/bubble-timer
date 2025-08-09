package io.jhoyt.bubbletimer.di.data;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;
import io.jhoyt.bubbletimer.domain.repositories.UserRepository;
import io.jhoyt.bubbletimer.domain.repositories.SharedTimerRepository;
import io.jhoyt.bubbletimer.data.repositories.RoomTimerRepository;
import io.jhoyt.bubbletimer.data.repositories.RoomUserRepository;
import io.jhoyt.bubbletimer.data.repositories.RoomSharedTimerRepository;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import io.jhoyt.bubbletimer.db.TimerDao;
import io.jhoyt.bubbletimer.data.converters.DomainTimerConverter;

import javax.inject.Singleton;
import java.util.List;
import io.jhoyt.bubbletimer.domain.entities.SharedTimer;
import io.jhoyt.bubbletimer.domain.entities.ShareStatus;
import io.jhoyt.bubbletimer.domain.entities.User;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository.Observable;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository.Observer;

/**
 * Hilt module for data layer dependency injection.
 * This module provides repository implementations and their dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DataModule {
    
    /**
     * Provide TimerRepository implementation
     * @param activeTimerRepository ActiveTimer repository
     * @param timerDao Timer DAO
     * @param timerConverter Timer converter
     * @return TimerRepository implementation
     */
    @Provides
    @Singleton
    public TimerRepository provideTimerRepository(ActiveTimerRepository activeTimerRepository,
                                               TimerDao timerDao) {
        return new RoomTimerRepository(activeTimerRepository, timerDao);
    }
    
    /**
     * Provide UserRepository implementation
     * @return RoomUserRepository implementation
     */
    @Provides
    @Singleton
    public UserRepository provideUserRepository() {
        return new RoomUserRepository();
    }
    
    /**
     * Provide SharedTimerRepository implementation
     * @return RoomSharedTimerRepository implementation
     */
    @Provides
    @Singleton
    public SharedTimerRepository provideSharedTimerRepository() {
        return new RoomSharedTimerRepository();
    }
    
    /**
     * Provide TimerConverter
     * @return TimerConverter instance
     */

    

}
