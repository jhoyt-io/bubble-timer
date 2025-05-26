package io.jhoyt.bubbletimer.db;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;

public class TimerRepository {
    private TimerDao timerDao;
    private LiveData<List<Timer>> allTimers;
    private final ConcurrentHashMap<Integer, Timer> timerCache = new ConcurrentHashMap<>();
    private final MutableLiveData<List<Timer>> cachedTimers = new MutableLiveData<>();
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    TimerRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        this.timerDao = db.timerDao();
        this.allTimers = this.timerDao.getAll();
        
        // Observe database changes and update cache
        this.allTimers.observeForever(timers -> {
            if (timers != null && !isUpdating.get()) {
                timerCache.clear();
                for (Timer timer : timers) {
                    timerCache.put(timer.id, timer);
                }
                cachedTimers.postValue(timers);
            }
        });
    }

    public LiveData<List<Timer>> getAllTimers() {
        return cachedTimers;
    }

    public LiveData<List<Timer>> getAllTimersWithTag(String tagsString) {
        return this.timerDao.getAllWithTag(tagsString);
    }

    public LiveData<Timer> getById(int id) {
        Timer cachedTimer = timerCache.get(id);
        if (cachedTimer != null) {
            MutableLiveData<Timer> result = new MutableLiveData<>();
            result.setValue(cachedTimer);
            return result;
        }
        return this.timerDao.getById(id);
    }

    public void insert(Timer timer) {
        // Update cache immediately
        timerCache.put(timer.id, timer);
        updateCachedTimers();
        
        // Update database in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            timerDao.insert(timer);
        });
    }

    public void update(Timer timer) {
        // Update cache immediately
        timerCache.put(timer.id, timer);
        updateCachedTimers();
        
        // Update database in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            timerDao.update(timer);
        });
    }

    public void deleteById(int id) {
        // Update cache immediately
        Timer removedTimer = timerCache.remove(id);
        if (removedTimer != null) {
            updateCachedTimers();
        }
        
        // Update database in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                timerDao.deleteById(id);
            } catch (Exception e) {
                // If database operation fails, restore the cache
                if (removedTimer != null) {
                    timerCache.put(id, removedTimer);
                    updateCachedTimers();
                }
            }
        });
    }

    private void updateCachedTimers() {
        isUpdating.set(true);
        try {
            // Create a new ArrayList instead of using List.copyOf()
            List<Timer> timers = new ArrayList<>(timerCache.values());
            cachedTimers.postValue(timers);
        } finally {
            isUpdating.set(false);
        }
    }
}
