package ARKlector.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "queue")
public record QueueRow(
        @PrimaryKey(autoGenerate = true) long id,
        String text,
        String localeTag,
        int position) {
}
