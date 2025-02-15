package io.jhoyt.bubbletimer.db;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class TagViewModel extends AndroidViewModel {
    private final TagRepository tagRepository;

    public TagViewModel(@NonNull Application application) {
        super(application);

        tagRepository = new TagRepository(application);
    }

    public LiveData<List<Tag>> getAllTags() {
        return tagRepository.getAllTags();
    }

    public void insert(Tag tag) {
        tagRepository.insert(tag);
    }

    public void delete(Tag tag) {
        tagRepository.delete(tag);
    }
}
