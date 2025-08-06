package io.jhoyt.bubbletimer.db;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class TimerRepository {
    private TimerDao timerDao;
    private LiveData<List<Timer>> allTimers;
    private final ConcurrentHashMap<Integer, Timer> timerCache = new ConcurrentHashMap<>();
    private final MutableLiveData<List<Timer>> cachedTimers = new MutableLiveData<>();
    private final ConcurrentHashMap<String, MutableLiveData<List<Timer>>> tagCaches = new ConcurrentHashMap<>();

    TimerRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        this.timerDao = db.timerDao();
        this.allTimers = this.timerDao.getAll();
        
        // Observe database changes and update cache
        this.allTimers.observeForever(timers -> {
            if (timers != null) {
                timerCache.clear();
                for (Timer timer : timers) {
                    timerCache.put(timer.id, timer);
                }
                List<Timer> timerList = new ArrayList<>(timerCache.values());
                cachedTimers.postValue(timerList);
            }
        });
    }

    public LiveData<List<Timer>> getAllTimers() {
        return cachedTimers;
    }

    public LiveData<List<Timer>> getAllTimersWithTag(String tagsString) {
        return tagCaches.computeIfAbsent(tagsString, tag -> {
            MutableLiveData<List<Timer>> liveData = new MutableLiveData<>();
            updateTaggedTimers(tag, liveData);
            return liveData;
        });
    }

    private void updateTaggedTimers(String tag, MutableLiveData<List<Timer>> liveData) {
        List<Timer> filteredTimers = timerCache.values().stream()
            .filter(timer -> timer.tagsString != null && timer.tagsString.contains(tag))
            .collect(Collectors.toList());
        liveData.postValue(filteredTimers);
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
        List<Timer> timers = new ArrayList<>(timerCache.values());
        cachedTimers.postValue(timers);
        
        // Update tag caches in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tagCaches.forEach((tag, liveData) -> updateTaggedTimers(tag, liveData));
        });
        
        // Update database in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            timerDao.insert(timer);
        });
    }

    public void update(Timer timer) {
        // Update cache immediately
        timerCache.put(timer.id, timer);
        List<Timer> timers = new ArrayList<>(timerCache.values());
        cachedTimers.postValue(timers);
        
        // Update tag caches in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tagCaches.forEach((tag, liveData) -> updateTaggedTimers(tag, liveData));
        });
        
        // Update database in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            timerDao.update(timer);
        });
    }

    public void deleteById(int id) {
        // Update cache immediately
        Timer removedTimer = timerCache.remove(id);
        if (removedTimer != null) {
            // Update main cache immediately
            List<Timer> timers = new ArrayList<>(timerCache.values());
            cachedTimers.postValue(timers);
            
            // Update tag caches in background
            AppDatabase.databaseWriteExecutor.execute(() -> {
                tagCaches.forEach((tag, liveData) -> updateTaggedTimers(tag, liveData));
            });
        }
        
        // Update database in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                timerDao.deleteById(id);
            } catch (Exception e) {
                // If database operation fails, restore the cache
                if (removedTimer != null) {
                    timerCache.put(id, removedTimer);
                    List<Timer> timers = new ArrayList<>(timerCache.values());
                    cachedTimers.postValue(timers);
                }
            }
        });
    }


}
