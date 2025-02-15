package io.jhoyt.bubbletimer.db;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import io.jhoyt.bubbletimer.TimerConverter;

public class TagRepository {
    private TagDao tagDao;
    private LiveData<List<Tag>> allTagsLiveData;

    public TagRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);

        this.tagDao = db.tagDao();
        this.allTagsLiveData = this.tagDao.getAll();
    }

    public LiveData<List<Tag>> getAllTags() {
        return this.allTagsLiveData;
    }

    public void insert(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tagDao.insert(tag);
        });
    }

    public void delete(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tagDao.delete(tag);
        });
    }
}
