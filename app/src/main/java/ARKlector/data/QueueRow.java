package ARKlector.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "queue")
public class QueueRow {

    @PrimaryKey(autoGenerate = true)
    public final long id;
    public final String text;
    public final String localeTag;
    public final int position;

    public QueueRow(long id, String text, String localeTag, int position) {
        this.id = id;
        this.text = text;
        this.localeTag = localeTag;
        this.position = position;
    }
}
