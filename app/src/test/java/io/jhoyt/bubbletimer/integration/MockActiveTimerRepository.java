package io.jhoyt.bubbletimer.integration;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.jhoyt.bubbletimer.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of ActiveTimerRepository for testing.
 * This provides a simple in-memory implementation for integration tests.
 */
public class MockActiveTimerRepository {
    
    private final MutableLiveData<List<Timer>> timersLiveData;
    private final ConcurrentHashMap<String, Timer> timersById;
    
    public MockActiveTimerRepository() {
        this.timersLiveData = new MutableLiveData<>(new ArrayList<>());
        this.timersById = new ConcurrentHashMap<>();
    }
    
    public Timer getById(String id) {
        return timersById.get(id);
    }
    
    public LiveData<List<Timer>> getAllActiveTimers() {
        return timersLiveData;
    }
    
    public void insert(Timer timer) {
        timersById.put(timer.getId(), timer);
        updateTimersLiveData();
    }
    
    public void update(Timer timer) {
        timersById.put(timer.getId(), timer);
        updateTimersLiveData();
    }
    
    public void deleteById(String id) {
        timersById.remove(id);
        updateTimersLiveData();
    }
    
    private void updateTimersLiveData() {
        List<Timer> timers = new ArrayList<>(timersById.values());
        timersLiveData.postValue(timers);
    }
}
