package io.jhoyt.bubbletimer.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Tag {
    @PrimaryKey
    @NonNull
    public String name;

    public Tag(@NonNull String name) {
        this.name = name;
    }
}
