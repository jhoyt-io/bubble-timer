package io.jhoyt.bubbletimer.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TimerDao {
    @Query("SELECT * FROM timer")
    LiveData<List<Timer>> getAll();

    @Insert
    void insert(Timer timer);

    @Delete
    void delete(Timer timer);

    @Query("DELETE FROM timer WHERE id == :timerId")
    void deleteById(int timerId);
}
