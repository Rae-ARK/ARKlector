package ARKlector.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Opened once in {@link ARKlector.ArklectorApplication#onCreate()}. See
 * docs/implementation/persistence.md — nothing downstream opens its own connection.
 */
@Database(entities = {QueueRow.class, HistoryRow.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract QueueDao queueDao();

    public abstract HistoryDao historyDao();

    public static AppDatabase open(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "arklector.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
