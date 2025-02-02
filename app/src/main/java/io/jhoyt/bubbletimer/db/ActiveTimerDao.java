package io.jhoyt.bubbletimer.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ActiveTimerDao {
    @Query("SELECT * FROM activeTimer WHERE id == :activeTimerId")
    ActiveTimer get(String activeTimerId);

    @Query("SELECT * FROM activeTimer")
    LiveData<List<ActiveTimer>> getAll();

    @Insert
    void insert(ActiveTimer activeTimer);

    @Update
    void update(ActiveTimer activeTimer);

    @Query("DELETE FROM activeTimer WHERE id == :activeTimerId")
    void deleteById(String activeTimerId);

}
