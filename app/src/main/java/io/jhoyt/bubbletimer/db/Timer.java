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

    @ColumnInfo(name = "tagsString", defaultValue = "")
    public String tagsString;

    public Timer() {
    }

    public Timer(String title, Duration duration) {
        this(title, duration, "");
    }

    public Timer(String title, Duration duration, String tagsString) {
        this.title = title;
        this.duration = duration;
        this.tagsString = tagsString;
    }

}
