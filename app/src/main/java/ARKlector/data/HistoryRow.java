package ARKlector.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public record HistoryRow(
        @PrimaryKey(autoGenerate = true) long id,
        String text,
        long finishedAtEpochMs) {
}
