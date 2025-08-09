package io.jhoyt.bubbletimer.integration;

import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.repositories.TimerRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple in-memory TimerRepository implementation for testing.
 * This provides a clean, testable implementation without complex dependencies.
 */
public class TestTimerRepository implements TimerRepository {
    
    private final ConcurrentHashMap<String, Timer> timersById;
    private final List<TimerRepository.Observer<List<Timer>>> allTimersObservers;
    private final List<TimerRepository.Observer<List<Timer>>> activeTimersObservers;
    
    public TestTimerRepository() {
        this.timersById = new ConcurrentHashMap<>();
        this.allTimersObservers = new ArrayList<>();
        this.activeTimersObservers = new ArrayList<>();
    }
    
    @Override
    public List<Timer> getAllTimers() {
        return new ArrayList<>(timersById.values());
    }
    
    @Override
    public List<Timer> getTimersByTag(String tag) {
        return timersById.values().stream()
                .filter(timer -> timer.getTags().contains(tag))
                .collect(Collectors.toList());
    }
    
    @Override
    public Timer getTimerById(String id) {
        return timersById.get(id);
    }
    
    @Override
    public List<Timer> getActiveTimers() {
        return timersById.values().stream()
                .filter(Timer::isActive)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Timer> getTimersByUserId(String userId) {
        return timersById.values().stream()
                .filter(timer -> timer.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Timer> getTimersSharedWithUser(String userId) {
        return timersById.values().stream()
                .filter(timer -> timer.getSharedWith().contains(userId))
                .collect(Collectors.toList());
    }
    
    @Override
    public void saveTimer(Timer timer) {
        timersById.put(timer.getId(), timer);
        notifyAllTimersObservers();
        if (timer.isActive()) {
            notifyActiveTimersObservers();
        }
    }
    
    @Override
    public void updateTimer(Timer timer) {
        timersById.put(timer.getId(), timer);
        notifyAllTimersObservers();
        if (timer.isActive()) {
            notifyActiveTimersObservers();
        }
    }
    
    @Override
    public void deleteTimer(String id) {
        Timer removed = timersById.remove(id);
        if (removed != null) {
            notifyAllTimersObservers();
            if (removed.isActive()) {
                notifyActiveTimersObservers();
            }
        }
    }
    
    @Override
    public void deleteAllTimers() {
        timersById.clear();
        notifyAllTimersObservers();
        notifyActiveTimersObservers();
    }
    
    @Override
    public void deleteTimersByUserId(String userId) {
        List<String> toRemove = timersById.values().stream()
                .filter(timer -> timer.getUserId().equals(userId))
                .map(Timer::getId)
                .collect(Collectors.toList());
        
        toRemove.forEach(timersById::remove);
        if (!toRemove.isEmpty()) {
            notifyAllTimersObservers();
            notifyActiveTimersObservers();
        }
    }
    
    @Override
    public void startTimer(String id) {
        Timer timer = timersById.get(id);
        if (timer != null) {
            // For testing, we'll create a new timer with running state
            Timer updatedTimer = Timer.fromData(
                timer.getId(), timer.getName(), timer.getUserId(), timer.getTotalDuration(),
                timer.getRemainingDurationWhenPaused(), LocalDateTime.now().plus(timer.getTotalDuration()),
                timer.getSharedWith(), timer.getTags(), io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING,
                timer.getCreatedAt(), LocalDateTime.now()
            );
            timersById.put(id, updatedTimer);
            notifyAllTimersObservers();
            notifyActiveTimersObservers();
        }
    }
    
    @Override
    public void stopTimer(String id) {
        Timer timer = timersById.get(id);
        if (timer != null) {
            Timer updatedTimer = timer.stop();
            timersById.put(id, updatedTimer);
            notifyAllTimersObservers();
            notifyActiveTimersObservers();
        }
    }
    
    @Override
    public void pauseTimer(String id) {
        Timer timer = timersById.get(id);
        if (timer != null && timer.getState() == io.jhoyt.bubbletimer.domain.entities.TimerState.RUNNING) {
            Timer updatedTimer = timer.pause();
            timersById.put(id, updatedTimer);
            notifyAllTimersObservers();
            notifyActiveTimersObservers();
        }
    }
    
    @Override
    public void resumeTimer(String id) {
        Timer timer = timersById.get(id);
        if (timer != null && timer.getState() == io.jhoyt.bubbletimer.domain.entities.TimerState.PAUSED) {
            Timer updatedTimer = timer.resume();
            timersById.put(id, updatedTimer);
            notifyAllTimersObservers();
            notifyActiveTimersObservers();
        }
    }
    
    @Override
    public void addTimeToTimer(String id, Duration additionalDuration) {
        Timer timer = timersById.get(id);
        if (timer != null) {
            Timer updatedTimer = timer.addTime(additionalDuration);
            timersById.put(id, updatedTimer);
            notifyAllTimersObservers();
            if (updatedTimer.isActive()) {
                notifyActiveTimersObservers();
            }
        }
    }
    
    @Override
    public void shareTimer(String timerId, String userId) {
        Timer timer = timersById.get(timerId);
        if (timer != null) {
            Timer updatedTimer = timer.shareWith(userId);
            timersById.put(timerId, updatedTimer);
            notifyAllTimersObservers();
        }
    }
    
    @Override
    public void removeTimerSharing(String timerId, String userId) {
        Timer timer = timersById.get(timerId);
        if (timer != null) {
            Timer updatedTimer = timer.removeSharing(userId);
            timersById.put(timerId, updatedTimer);
            notifyAllTimersObservers();
        }
    }
    
    @Override
    public void addTagToTimer(String timerId, String tag) {
        Timer timer = timersById.get(timerId);
        if (timer != null) {
            Timer updatedTimer = timer.addTag(tag);
            timersById.put(timerId, updatedTimer);
            notifyAllTimersObservers();
        }
    }
    
    @Override
    public void removeTagFromTimer(String timerId, String tag) {
        Timer timer = timersById.get(timerId);
        if (timer != null) {
            Timer updatedTimer = timer.removeTag(tag);
            timersById.put(timerId, updatedTimer);
            notifyAllTimersObservers();
        }
    }
    
    @Override
    public TimerRepository.Observable<List<Timer>> observeAllTimers() {
        return new SimpleObservable<>(getAllTimers());
    }
    
    @Override
    public TimerRepository.Observable<List<Timer>> observeActiveTimers() {
        return new SimpleObservable<>(getActiveTimers());
    }
    
    @Override
    public TimerRepository.Observable<Timer> observeTimerById(String id) {
        return new SimpleObservable<>(getTimerById(id));
    }
    
    @Override
    public TimerRepository.Observable<List<Timer>> observeTimersByUserId(String userId) {
        return new SimpleObservable<>(getTimersByUserId(userId));
    }
    
    @Override
    public TimerRepository.Observable<List<Timer>> observeTimersSharedWithUser(String userId) {
        return new SimpleObservable<>(getTimersSharedWithUser(userId));
    }
    
    private void notifyAllTimersObservers() {
        List<Timer> allTimers = getAllTimers();
        for (TimerRepository.Observer<List<Timer>> observer : allTimersObservers) {
            observer.onChanged(allTimers);
        }
    }
    
    private void notifyActiveTimersObservers() {
        List<Timer> activeTimers = getActiveTimers();
        for (TimerRepository.Observer<List<Timer>> observer : activeTimersObservers) {
            observer.onChanged(activeTimers);
        }
    }
    
    private static class SimpleObservable<T> implements TimerRepository.Observable<T> {
        private final T value;
        
        public SimpleObservable(T value) {
            this.value = value;
        }
        
        @Override
        public void subscribe(TimerRepository.Observer<T> observer) {
            observer.onChanged(value);
        }
        
        @Override
        public void unsubscribe(TimerRepository.Observer<T> observer) {
            // No-op for simple implementation
        }
        
        @Override
        public T getValue() {
            return value;
        }
    }
}
