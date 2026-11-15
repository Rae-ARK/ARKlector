package ARKlector.data;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface HistoryDao {

    @Insert
    void insert(HistoryRow row);

    @Query("SELECT * FROM history ORDER BY finishedAtEpochMs DESC")
    LiveData<List<HistoryRow>> observeAll();
}
