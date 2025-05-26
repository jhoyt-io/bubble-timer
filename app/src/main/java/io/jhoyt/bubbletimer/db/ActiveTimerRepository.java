package io.jhoyt.bubbletimer.db;

import android.app.Application;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.TimerConverter;

public class ActiveTimerRepository {
    private ActiveTimerDao activeTimerDao;
    private LiveData<List<ActiveTimer>> allActiveTimersLiveData;
    private final MutableLiveData<List<Timer>> timersLiveData;
    private final ConcurrentHashMap<String, Timer> timersById;
    private final Observer<List<ActiveTimer>> observer;
    private final Map<String, Long> lastUpdateTime;

    public ActiveTimerRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        this.timersById = new ConcurrentHashMap<>();
        this.timersLiveData = new MutableLiveData<>(List.of());
        this.lastUpdateTime = new ConcurrentHashMap<>();

        this.activeTimerDao = db.activeTimerDao();
        this.allActiveTimersLiveData = this.activeTimerDao.getAll();
        
        this.observer = activeTimers -> {
            List<Timer> timers = activeTimers.stream()
                    .map(TimerConverter::fromActiveTimer)
                    .collect(Collectors.toList());
            
            // Update cache
            timersById.clear();
            timers.forEach(timer -> timersById.put(timer.getId(), timer));
            timersLiveData.postValue(timers);
        };

        this.allActiveTimersLiveData.observeForever(observer);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.allActiveTimersLiveData.removeObserver(observer);
    }

    public Timer getById(String id) {
        return timersById.get(id);
    }

    public LiveData<List<Timer>> getAllActiveTimers() {
        return this.timersLiveData;
    }

    public void insert(Timer timer) {
        // Update cache immediately
        timersById.put(timer.getId(), timer);
        updateTimersLiveData();
        
        // Update database in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activeTimerDao.insert(TimerConverter.toActiveTimer(timer));
        });
    }

    public void update(Timer timer) {
        // Throttle updates to database (max once per second)
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(timer.getId());
        if (lastUpdate != null && currentTime - lastUpdate < 1000) {
            // Skip database update if too soon
            return;
        }
        
        // Update cache immediately
        timersById.put(timer.getId(), timer);
        updateTimersLiveData();
        lastUpdateTime.put(timer.getId(), currentTime);
        
        // Update database in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activeTimerDao.update(TimerConverter.toActiveTimer(timer));
        });
    }

    public void deleteById(String id) {
        // Update cache immediately
        timersById.remove(id);
        lastUpdateTime.remove(id);
        updateTimersLiveData();
        
        // Update database in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activeTimerDao.deleteById(id);
        });
    }

    private void updateTimersLiveData() {
        List<Timer> timers = List.copyOf(timersById.values());
        timersLiveData.postValue(timers);
    }
}
