package ARKlector.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryRow {

    @PrimaryKey(autoGenerate = true)
    public final long id;
    public final String text;
    public final long finishedAtEpochMs;

    public HistoryRow(long id, String text, long finishedAtEpochMs) {
        this.id = id;
        this.text = text;
        this.finishedAtEpochMs = finishedAtEpochMs;
    }
}
