package ARKlector.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import ARKlector.data.QueueDao;
import ARKlector.data.QueueRow;
import ARKlector.model.Segment;

/**
 * Holds the in-flight queue between acquisition/parsing and synthesis/playback, write-through
 * mirrored to {@link QueueDao}. See docs/implementation/synthesis-playback.md.
 */
public final class ReadingQueueService {

    private final Deque<Segment> queue = new ArrayDeque<>();
    private final QueueDao queueDao;

    public ReadingQueueService(QueueDao queueDao) {
        this.queueDao = queueDao;
    }

    public synchronized void enqueue(List<Segment> segments) {
        queue.addAll(segments);
        persist();
    }

    public synchronized Optional<Segment> next() {
        Segment head = queue.poll();
        if (head != null) {
            persist();
        }
        return Optional.ofNullable(head);
    }

    public synchronized void reorder(int from, int to) {
        List<Segment> snapshot = new ArrayList<>(queue);
        if (from < 0 || from >= snapshot.size() || to < 0 || to >= snapshot.size()) {
            return;
        }
        Segment moved = snapshot.remove(from);
        snapshot.add(to, moved);
        queue.clear();
        queue.addAll(snapshot);
        persist();
    }

    public synchronized void remove(int index) {
        List<Segment> snapshot = new ArrayList<>(queue);
        if (index < 0 || index >= snapshot.size()) {
            return;
        }
        snapshot.remove(index);
        queue.clear();
        queue.addAll(snapshot);
        persist();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    private void persist() {
        List<QueueRow> rows = new ArrayList<>();
        int position = 0;
        for (Segment segment : queue) {
            rows.add(new QueueRow(0, segment.text(), segment.guessedLocale().toLanguageTag(), position++));
        }
        queueDao.replaceAll(rows);
    }
}
