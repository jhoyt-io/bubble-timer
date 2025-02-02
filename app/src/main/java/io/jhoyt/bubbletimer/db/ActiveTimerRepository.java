package io.jhoyt.bubbletimer.db;

import android.app.Application;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jhoyt.bubbletimer.Timer;
import io.jhoyt.bubbletimer.TimerConverter;

public class ActiveTimerRepository {
    private ActiveTimerDao activeTimerDao;
    private LiveData<List<ActiveTimer>> allActiveTimersLiveData;
    private List<Timer> allActiveTimers;
    private Map<String, Timer> timersById;


    public ActiveTimerRepository(LifecycleOwner owner, Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);

        this.timersById = new HashMap<>();

        this.activeTimerDao = db.activeTimerDao();
        this.allActiveTimersLiveData = this.activeTimerDao.getAll();
        this.allActiveTimersLiveData.observe(owner, activeTimers -> {
            this.allActiveTimers = activeTimers.stream()
                    .map(TimerConverter::fromActiveTimer)
                    .collect(Collectors.toList());
            this.timersById.clear();
            this.allActiveTimers.forEach(timer -> {
                this.timersById.put(timer.getId(), timer);
            });
        });
    }

    public Timer getById(String id) {
        return timersById.get(id);
    }

    public List<Timer> getAllActiveTimers() {
        return allActiveTimers;
    }

    public void insert(Timer timer) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activeTimerDao.insert(TimerConverter.toActiveTimer(timer));
        });
    }

    public void update(Timer timer) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activeTimerDao.update(TimerConverter.toActiveTimer(timer));
        });
    }

    public void deleteById(String id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            activeTimerDao.deleteById(id);
        });
    }
}
