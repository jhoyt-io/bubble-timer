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
    private final MutableLiveData<Boolean> isDeleting = new MutableLiveData<>(false);
    private final AtomicBoolean deletionInProgress = new AtomicBoolean(false);

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
        repository.insert(timer);
    }

    public void update(Timer timer) {
        Log.d("TimerViewModel", "Updating timer: " + timer.title + " (id: " + timer.id + ")");
        repository.update(timer);
    }

    public void deleteById(int id) {
        Log.d("TimerViewModel", "Deleting timer with id: " + id);
        if (deletionInProgress.compareAndSet(false, true)) {
            try {
                isDeleting.postValue(true);
                repository.deleteById(id);
            } finally {
                isDeleting.postValue(false);
                deletionInProgress.set(false);
            }
        }
    }

    public LiveData<Boolean> isDeleting() {
        return isDeleting;
    }
}
