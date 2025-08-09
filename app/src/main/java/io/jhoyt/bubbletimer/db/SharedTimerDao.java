package io.jhoyt.bubbletimer.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SharedTimerDao {
    @Query("SELECT * FROM shared_timers ORDER BY createdAt DESC")
    LiveData<List<SharedTimer>> getAllSharedTimers();
    
    @Query("SELECT * FROM shared_timers ORDER BY createdAt DESC")
    List<SharedTimer> getAllSharedTimersSync();
    
    @Query("SELECT * FROM shared_timers WHERE status = :status ORDER BY createdAt DESC")
    LiveData<List<SharedTimer>> getSharedTimersByStatus(String status);
    
    @Query("SELECT * FROM shared_timers WHERE timerId = :timerId")
    SharedTimer getSharedTimerById(String timerId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SharedTimer sharedTimer);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SharedTimer> sharedTimers);
    
    @Update
    void update(SharedTimer sharedTimer);
    
    @Delete
    void delete(SharedTimer sharedTimer);
    
    @Query("DELETE FROM shared_timers WHERE timerId = :timerId")
    void deleteById(String timerId);
    
    @Query("UPDATE shared_timers SET status = :status WHERE timerId = :timerId")
    void updateStatus(String timerId, String status);
    
    @Query("DELETE FROM shared_timers WHERE status = :status")
    void deleteByStatus(String status);
    
    @Query("DELETE FROM shared_timers")
    void deleteAll();
} 