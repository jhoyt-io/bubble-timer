package io.jhoyt.bubbletimer.db;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimerViewModel extends AndroidViewModel {
    private final TimerRepository repository;
    // Removed unnecessary deletion tracking - UI updates immediately via repository cache

    public TimerViewModel(@NonNull Application application) {
        super(application);
        repository = new TimerRepository(application);
    }

    public LiveData<List<Timer>> getAllTimers() {
        return repository.getAllTimers();
    }

    public LiveData<List<Timer>> getAllTimersWithTag(String tag) {
        return repository.getAllTimersWithTag(tag);
    }

    public LiveData<Timer> getById(int id) {
        return repository.getById(id);
    }

    public void insert(Timer timer) {
        Log.d("TimerViewModel", "Inserting timer: " + timer.title + " (id: " + timer.id + ")");
        // Repository handles background execution
        repository.insert(timer);
    }

    public void update(Timer timer) {
        Log.d("TimerViewModel", "Updating timer: " + timer.title + " (id: " + timer.id + ")");
        // Repository handles background execution
        repository.update(timer);
    }

    public void deleteById(int id) {
        Log.d("TimerViewModel", "Deleting timer with id: " + id);
        // Repository handles background execution and immediate UI updates
        repository.deleteById(id);
    }
}
