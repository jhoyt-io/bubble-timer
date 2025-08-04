package io.jhoyt.bubbletimer.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    version = 6,
    entities = {Timer.class, ActiveTimer.class, Tag.class, SharedTimer.class},
    autoMigrations = {
        @AutoMigration(from = 1, to = 2),
        @AutoMigration(from = 2, to = 3),
        @AutoMigration(from = 3, to = 4),
        @AutoMigration(from = 4, to = 5),
        @AutoMigration(from = 5, to = 6),
    }
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract TimerDao timerDao();
    public abstract ActiveTimerDao activeTimerDao();
    public abstract TagDao tagDao();
    public abstract SharedTimerDao sharedTimerDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class, "timer_database")
                            .addCallback(roomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    static RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            databaseWriteExecutor.execute(() -> {
                TimerDao dao = INSTANCE.timerDao() ;
                //dao.deleteAll();

                Timer timer = new Timer();
                timer.title = "Hello";
                timer.duration = Duration.ofMinutes(3);
                dao.insert(timer);

                Timer timer2 = new Timer();
                timer2.title = "World!!";
                timer2.duration = Duration.ofMinutes(9);
                dao.insert(timer2);

                TagDao tagDao = INSTANCE.tagDao();
                tagDao.insert(new Tag("Hello"));
                tagDao.insert(new Tag("World"));
                tagDao.insert(new Tag("Tabs"));
            });
        }
    };
}
