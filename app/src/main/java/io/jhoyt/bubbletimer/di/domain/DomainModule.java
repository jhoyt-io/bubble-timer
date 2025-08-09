package io.jhoyt.bubbletimer.di.domain;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;
import io.jhoyt.bubbletimer.domain.repositories.UserRepository;
import io.jhoyt.bubbletimer.domain.repositories.SharedTimerRepository;
import io.jhoyt.bubbletimer.domain.usecases.timer.StartTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.PauseTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.ResumeTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.StopTimerUseCase;
import io.jhoyt.bubbletimer.domain.usecases.timer.GetActiveTimersUseCase;

import javax.inject.Singleton;

/**
 * Hilt module for domain layer dependency injection.
 * This module provides use cases and their dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DomainModule {
    
    /**
     * Provide StartTimerUseCase
     * @param timerRepository Timer repository implementation
     * @return StartTimerUseCase instance
     */
    @Provides
    @Singleton
    public StartTimerUseCase provideStartTimerUseCase(TimerRepository timerRepository) {
        return new StartTimerUseCase(timerRepository);
    }
    
    /**
     * Provide PauseTimerUseCase
     * @param timerRepository Timer repository implementation
     * @return PauseTimerUseCase instance
     */
    @Provides
    @Singleton
    public PauseTimerUseCase providePauseTimerUseCase(TimerRepository timerRepository) {
        return new PauseTimerUseCase(timerRepository);
    }
    
    /**
     * Provide ResumeTimerUseCase
     * @param timerRepository Timer repository implementation
     * @return ResumeTimerUseCase instance
     */
    @Provides
    @Singleton
    public ResumeTimerUseCase provideResumeTimerUseCase(TimerRepository timerRepository) {
        return new ResumeTimerUseCase(timerRepository);
    }
    
    /**
     * Provide StopTimerUseCase
     * @param timerRepository Timer repository implementation
     * @return StopTimerUseCase instance
     */
    @Provides
    @Singleton
    public StopTimerUseCase provideStopTimerUseCase(TimerRepository timerRepository) {
        return new StopTimerUseCase(timerRepository);
    }
    
    /**
     * Provide GetActiveTimersUseCase
     * @param timerRepository Timer repository implementation
     * @return GetActiveTimersUseCase instance
     */
    @Provides
    @Singleton
    public GetActiveTimersUseCase provideGetActiveTimersUseCase(TimerRepository timerRepository) {
        return new GetActiveTimersUseCase(timerRepository);
    }
}
