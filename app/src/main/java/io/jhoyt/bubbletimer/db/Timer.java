package io.jhoyt.bubbletimer.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Duration;

@Entity
public class Timer {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "duration")
    public Duration duration;

    public Timer() {
    }

    public Timer(String title, Duration duration) {
        this.title = title;
        this.duration = duration;
    }

}
