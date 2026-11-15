package ARKlector.data;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

@Dao
public interface QueueDao {

    @Query("SELECT * FROM queue ORDER BY position")
    LiveData<List<QueueRow>> observeAll();

    @Insert
    void insertAll(List<QueueRow> rows);

    @Query("DELETE FROM queue")
    void clear();

    @Delete
    void delete(QueueRow row);

    /** Write-through replace: ReadingQueueService calls this on every mutation. */
    @Transaction
    default void replaceAll(List<QueueRow> rows) {
        clear();
        if (!rows.isEmpty()) {
            insertAll(rows);
        }
    }
}
