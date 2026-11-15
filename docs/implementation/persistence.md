# Persistence

What outlives a process. Room (Java annotation-processor API), two tables, no Kotlin coroutines/Flow on the DAO surface — `LiveData<T>` or a plain blocking call off the main thread.

## Life cycle

**Created** — the Room database is opened once, in `Application.onCreate()`, as a singleton held by a small `AppDatabase` holder class, and handed to `QueueDao`/`HistoryDao` constructors from there. Nothing downstream opens its own connection.

**Active**
* `QueueDao` mirrors `ReadingQueueService`'s in-memory deque on every mutation (see [synthesis & playback](synthesis-playback.md)) — write-through, not write-behind, so a kill mid-playback loses at most the segment that was actually mid-utterance.
* `HistoryDao` receives a row once a segment finishes (`onDone`) or is explicitly discarded — history is append-only; there is no "undo" at this layer, only at the UI layer (toast with an undo action, before the write happens).

**Torn down** — never, within a process's life; the database outlives every `Activity`/`ViewModel`/`Service` above it. Closed only if the process is killed by the OS.

## Schema (settled shape, not final column list)

```java
@Entity(tableName = "queue")
public record QueueRow(@PrimaryKey(autoGenerate = true) long id, String text, String localeTag, int position) {}

@Entity(tableName = "history")
public record HistoryRow(@PrimaryKey(autoGenerate = true) long id, String text, long finishedAtEpochMs) {}
```

## API

```java
@Dao
public interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY position")
    LiveData<List<QueueRow>> observeAll();

    @Insert
    void insertAll(List<QueueRow> rows);

    @Delete
    void delete(QueueRow row);
}

@Dao
public interface HistoryDao {
    @Insert
    void insert(HistoryRow row);

    @Query("SELECT * FROM history ORDER BY finishedAtEpochMs DESC")
    LiveData<List<HistoryRow>> observeAll();
}
```

Preferences (voice, speed, pitch, auto-read) are deliberately **not** in Room — see [settings](settings.md); they're small, flat, and `SharedPreferences` fits without the overhead of a table.
