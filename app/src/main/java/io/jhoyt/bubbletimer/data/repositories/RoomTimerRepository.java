package io.jhoyt.bubbletimer.data.repositories;

import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;
import io.jhoyt.bubbletimer.db.ActiveTimerRepository;
import io.jhoyt.bubbletimer.db.TimerDao;
import io.jhoyt.bubbletimer.data.converters.DomainTimerConverter;
import io.jhoyt.bubbletimer.db.ActiveTimer;


import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Room-based implementation of TimerRepository that bridges domain entities with existing data models.
 * This adapter converts between domain Timer entities and existing Timer/ActiveTimer entities.
 * 
 * "RoomTimerRepository is a TimerRepository that uses Room database for persistence."
 */
@Singleton
public class RoomTimerRepository implements TimerRepository {
    
    private final ActiveTimerRepository activeTimerRepository;
    private final TimerDao timerDao;
    @Inject
    public RoomTimerRepository(ActiveTimerRepository activeTimerRepository,
                             TimerDao timerDao) {
        this.activeTimerRepository = activeTimerRepository;
        this.timerDao = timerDao;
    }
    
    @Override
    public List<Timer> getAllTimers() {
        // Get all active timers from the existing repository
        List<io.jhoyt.bubbletimer.Timer> existingTimers = activeTimerRepository.getAllActiveTimers().getValue();
        if (existingTimers == null) {
            return List.of();
        }
        
        // Convert existing timers to domain timers
        return existingTimers.stream()
            .map(existingTimer -> {
                // Convert existing Timer to ActiveTimer format first
                ActiveTimer activeTimer = io.jhoyt.bubbletimer.TimerConverter.toActiveTimer(existingTimer);
                return DomainTimerConverter.activeTimerToDomainTimer(activeTimer);
            })
            .filter(timer -> timer != null)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Timer> getTimersByTag(String tag) {
        // Get all timers and filter by tag
        List<Timer> allTimers = getAllTimers();
        return allTimers.stream()
            .filter(timer -> timer.getTags().contains(tag))
            .collect(Collectors.toList());
    }
    
    @Override
    public Timer getTimerById(String id) {
        // Get all timers and find by ID
        List<Timer> allTimers = getAllTimers();
        return allTimers.stream()
            .filter(timer -> timer.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public List<Timer> getActiveTimers() {
        // Get all timers and filter for active ones
        List<Timer> allTimers = getAllTimers();
        return allTimers.stream()
            .filter(Timer::isActive)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Timer> getTimersByUserId(String userId) {
        // Get all timers and filter by user ID
        List<Timer> allTimers = getAllTimers();
        return allTimers.stream()
            .filter(timer -> timer.getUserId().equals(userId))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Timer> getTimersSharedWithUser(String userId) {
        // Get all timers and filter by shared users
        List<Timer> allTimers = getAllTimers();
        return allTimers.stream()
            .filter(timer -> timer.getSharedWith().contains(userId))
            .collect(Collectors.toList());
    }
    
    @Override
    public void saveTimer(Timer timer) {
        // Convert domain timer to ActiveTimer and save
        ActiveTimer activeTimer = DomainTimerConverter.domainTimerToActiveTimer(timer);
        activeTimerRepository.insert(io.jhoyt.bubbletimer.TimerConverter.fromActiveTimer(activeTimer));
    }
    
    @Override
    public void updateTimer(Timer timer) {
        // Convert domain timer to ActiveTimer and update
        ActiveTimer activeTimer = DomainTimerConverter.domainTimerToActiveTimer(timer);
        activeTimerRepository.update(io.jhoyt.bubbletimer.TimerConverter.fromActiveTimer(activeTimer));
    }
    
    @Override
    public void deleteTimer(String id) {
        activeTimerRepository.deleteById(id);
    }
    
    @Override
    public void deleteAllTimers() {
        // TODO: Implement with proper repository method
        // For now, do nothing as we need to bridge between different Timer types
    }
    
    @Override
    public void deleteTimersByUserId(String userId) {
        // TODO: Implement user-specific deletion
        // For now, just delete all timers
        deleteAllTimers();
    }
    
    @Override
    public void startTimer(String id) {
        Timer timer = getTimerById(id);
        if (timer != null && timer.canResume()) {
            Timer startedTimer = timer.resume();
            updateTimer(startedTimer);
        }
    }
    
    @Override
    public void stopTimer(String id) {
        Timer timer = getTimerById(id);
        if (timer != null) {
            Timer stoppedTimer = timer.stop();
            updateTimer(stoppedTimer);
        }
    }
    
    @Override
    public void pauseTimer(String id) {
        Timer timer = getTimerById(id);
        if (timer != null && timer.canPause()) {
            Timer pausedTimer = timer.pause();
            updateTimer(pausedTimer);
        }
    }
    
    @Override
    public void resumeTimer(String id) {
        Timer timer = getTimerById(id);
        if (timer != null && timer.canResume()) {
            Timer resumedTimer = timer.resume();
            updateTimer(resumedTimer);
        }
    }
    
    @Override
    public void addTimeToTimer(String id, Duration additionalDuration) {
        Timer timer = getTimerById(id);
        if (timer != null) {
            Timer updatedTimer = timer.addTime(additionalDuration);
            updateTimer(updatedTimer);
        }
    }
    
    @Override
    public void shareTimer(String timerId, String userId) {
        Timer timer = getTimerById(timerId);
        if (timer != null) {
            Timer sharedTimer = timer.shareWith(userId);
            updateTimer(sharedTimer);
        }
    }
    
    @Override
    public void removeTimerSharing(String timerId, String userId) {
        Timer timer = getTimerById(timerId);
        if (timer != null) {
            Timer unsharedTimer = timer.removeSharing(userId);
            updateTimer(unsharedTimer);
        }
    }
    
    @Override
    public void addTagToTimer(String timerId, String tag) {
        Timer timer = getTimerById(timerId);
        if (timer != null) {
            Timer updatedTimer = timer.addTag(tag);
            updateTimer(updatedTimer);
        }
    }
    
    @Override
    public void removeTagFromTimer(String timerId, String tag) {
        Timer timer = getTimerById(timerId);
        if (timer != null) {
            Timer updatedTimer = timer.removeTag(tag);
            updateTimer(updatedTimer);
        }
    }
    
    // Observable implementations - simplified for now
    @Override
    public Observable<List<Timer>> observeAllTimers() {
        // TODO: Implement with LiveData or RxJava
        return new SimpleObservable<>(getAllTimers());
    }
    
    @Override
    public Observable<List<Timer>> observeActiveTimers() {
        // TODO: Implement with LiveData or RxJava
        return new SimpleObservable<>(getActiveTimers());
    }
    
    @Override
    public Observable<Timer> observeTimerById(String id) {
        // TODO: Implement with LiveData or RxJava
        return new SimpleObservable<>(getTimerById(id));
    }
    
    @Override
    public Observable<List<Timer>> observeTimersByUserId(String userId) {
        // TODO: Implement with LiveData or RxJava
        return new SimpleObservable<>(getTimersByUserId(userId));
    }
    
    @Override
    public Observable<List<Timer>> observeTimersSharedWithUser(String userId) {
        // TODO: Implement with LiveData or RxJava
        return new SimpleObservable<>(getTimersSharedWithUser(userId));
    }
    
    /**
     * Simple Observable implementation for now.
     * TODO: Replace with proper LiveData or RxJava implementation
     */
    private static class SimpleObservable<T> implements Observable<T> {
        private final T value;
        
        public SimpleObservable(T value) {
            this.value = value;
        }
        
        @Override
        public void subscribe(Observer<T> observer) {
            observer.onChanged(value);
        }
        
        @Override
        public void unsubscribe(Observer<T> observer) {
            // No-op for simple implementation
        }
        
        @Override
        public T getValue() {
            return value;
        }
    }
}
