# Synthesis & Playback

Where segments become sound. Owned by `TtsEngine` (synthesis) and `PlaybackService` (the foreground `Service` that keeps it alive off-screen), with `ReadingQueueService` holding the in-flight list between the two.

## Life cycle — `ReadingQueueService`

**Created** — once, held by `ReaderViewModel`, backed by an in-memory `Deque<Segment>` mirrored to `QueueDao` ([persistence](persistence.md)) on every mutation so a process death doesn't lose the queue.

**Active** — [acquisition](acquisition.md) and [parsing](parsing.md) append; `PlaybackService` polls the head via `ReadingQueueService.next()`; user reordering/removal in the UI calls through the same service so the persisted copy never drifts from memory.

**Torn down** — cleared with the owning `ViewModel`; the persisted copy in `QueueDao` is what a later app launch resumes from.

## Life cycle — `TtsEngine`

**Created** — wraps `android.speech.tts.TextToSpeech`, constructed with an `OnInitListener`. Not usable until that callback fires — callers await `TtsEngine.ready()` (a `LiveData<Boolean>`) before the first `speak()` call.

**Active** — `speak(Segment)` sets voice/locale/pitch/rate from [settings](settings.md) for *that* call (so a mid-queue voice change takes effect on the next segment, not retroactively), then delegates to `TextToSpeech.speak()`. Utterance-progress callbacks (`onStart`/`onDone`/`onError`) are forwarded to `PlaybackService` so it knows when to advance the queue.

**Torn down** — `shutdown()` calls `TextToSpeech.shutdown()`. Called from `PlaybackService.onDestroy()` — `TtsEngine`'s lifecycle is tied to the service, not to any `Activity`.

## Life cycle — `PlaybackService` (foreground `Service`)

**Created** — started (`startForegroundService`) by `ReaderViewModel` the moment the queue goes from empty to non-empty. Immediately calls `startForeground()` with a playback notification (play/pause/skip actions), so playback survives the app leaving the foreground.

**Active** — drains `ReadingQueueService` one segment at a time through `TtsEngine`; updates the notification and publishes playback state back to `ReaderViewModel` via a bound-service callback (no Compose state, no Kotlin `Flow` — a plain `LiveData<PlaybackState>` exposed through the binder).

**Torn down** — stops itself (`stopSelf()`) when the queue empties and no new segment arrives within a short grace window, or immediately on an explicit "stop" action from the notification/UI. `onDestroy()` shuts down `TtsEngine` and releases the wake lock taken out for the duration of active playback.

## API

```java
public final class TtsEngine {
    public TtsEngine(Context context) { ... }
    public LiveData<Boolean> ready() { ... }
    public void speak(Segment segment, Preferences.Voice voice) { ... }
    public void stop() { ... }
    public void shutdown() { ... }
}

public final class ReadingQueueService {
    public void enqueue(List<Segment> segments) { ... }
    public Optional<Segment> next() { ... }
    public void reorder(int from, int to) { ... }
    public void remove(int index) { ... }
}

public final class PlaybackService extends Service {
    // binder exposes LiveData<PlaybackState>; no XML-declared notification layout —
    // the notification's custom views, if any, are built the same programmatic way as everything else.
}
```
