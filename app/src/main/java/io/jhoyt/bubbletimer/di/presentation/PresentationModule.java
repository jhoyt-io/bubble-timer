package io.jhoyt.bubbletimer.di.presentation;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.hilt.android.scopes.ViewModelScoped;

import io.jhoyt.bubbletimer.ui.viewmodels.TimerViewModel;
import io.jhoyt.bubbletimer.ui.viewmodels.SharedTimerViewModel;
import io.jhoyt.bubbletimer.ui.viewmodels.UserViewModel;

/**
 * Hilt module for presentation layer dependency injection.
 * This module provides ViewModels and their dependencies.
 */
@Module
@InstallIn(ViewModelComponent.class)
public class PresentationModule {
    
    /**
     * Provide TimerViewModel
     * @return TimerViewModel instance
     */
    @Provides
    @ViewModelScoped
    public TimerViewModel provideTimerViewModel() {
        // This will be injected by Hilt automatically
        return null; // Hilt will handle the actual instantiation
    }
    
    /**
     * Provide SharedTimerViewModel
     * @return SharedTimerViewModel instance
     */
    @Provides
    @ViewModelScoped
    public SharedTimerViewModel provideSharedTimerViewModel() {
        // This will be injected by Hilt automatically
        return null; // Hilt will handle the actual instantiation
    }
    
    /**
     * Provide UserViewModel
     * @return UserViewModel instance
     */
    @Provides
    @ViewModelScoped
    public UserViewModel provideUserViewModel() {
        // This will be injected by Hilt automatically
        return null; // Hilt will handle the actual instantiation
    }
}
