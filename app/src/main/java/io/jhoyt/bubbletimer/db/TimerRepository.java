package io.jhoyt.bubbletimer.db;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class TimerRepository {
    private TimerDao timerDao;
    private LiveData<List<Timer>> allTimers;

    TimerRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);

        this.timerDao = db.timerDao();
        this.allTimers = this.timerDao.getAll();
    }

    public LiveData<List<Timer>> getAllTimers() {
        return allTimers;
    }

    public LiveData<List<Timer>> getAllTimersWithTag(String tag) {
        return this.timerDao.getAllWithTag(tag);
    }

    public void insert(Timer timer) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            timerDao.insert(timer);
        });
    }

    public void deleteById(int id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            timerDao.deleteById(id);
        });
    }
}
