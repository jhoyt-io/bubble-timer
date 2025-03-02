package io.jhoyt.bubbletimer.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TimerDao {
    @Query("SELECT * FROM timer")
    LiveData<List<Timer>> getAll();

    @Query("SELECT * FROM timer WHERE tagsString LIKE :tagsString")
    LiveData<List<Timer>> getAllWithTag(String tagsString);

    @Query("SELECT * FROM timer WHERE id = :id")
    LiveData<Timer> getById(int id);

    @Insert
    void insert(Timer timer);

    @Update
    void update(Timer timer);

    @Delete
    void delete(Timer timer);

    @Query("DELETE FROM timer WHERE id == :timerId")
    void deleteById(int timerId);
}
