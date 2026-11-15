# Architecture

The *what* behind [`design.md`](design.md)'s *why*. Described as a life cycle: a piece of content's journey from clipboard to spoken audio, and back down to history.

## Life cycle stages

```
 Acquisition -> Parsing -> Queueing -> Synthesis & Playback -> Persistence
      ^                                                            |
      '--------------------- Settings (cross-cutting) -------------'
```

1. **Acquisition** — content enters the app: clipboard read (primary), share-intent (secondary). Owned by a `ClipboardSource` service. See [`implementation/acquisition.md`](../implementation/acquisition.md).
2. **Parsing** — raw clipboard text is normalized and split into speakable segments. Pure, static, unit-testable. See [`implementation/parsing.md`](../implementation/parsing.md).
3. **Queueing** — segments are appended to an in-memory + persisted reading queue, so a session survives a process restart. Covered alongside synthesis below.
4. **Synthesis & Playback** — segments are handed to `TtsEngine` (wraps Android `TextToSpeech`), run from a foreground `Service` so playback survives the app going to background. See [`implementation/synthesis-playback.md`](../implementation/synthesis-playback.md).
5. **Persistence** — finished/abandoned items move from queue to history; preferences persist across launches. See [`implementation/persistence.md`](../implementation/persistence.md).
6. **Settings** — cross-cutting: voice, speed, pitch, auto-play-on-copy. Read by every other stage, owned by none of them. See [`implementation/settings.md`](../implementation/settings.md).

## Layers

| Layer | Holds | Examples |
|---|---|---|
| UI | Renders state, forwards intent. Built with `View`/`ViewGroup` in Java, no XML. | `MainActivity`, `PlaybackControlsView` |
| ViewModel | UI state, survives configuration changes. | `ReaderViewModel` |
| Service | Logic and I/O, owns one resource or capability. | `ClipboardSource`, `TtsEngine`, `ReadingQueueService` |
| DAO | Persistence only. | `HistoryDao`, `QueueDao` (Room, Java) |

Dependencies point one direction only: UI → ViewModel → Service → DAO. Nothing below reaches back up.

## Tech stack (settled)

* Language: Java 21. Min SDK 34.
* UI toolkit: classic Views, built programmatically — no XML layouts, no Compose.
* State: `androidx.lifecycle.ViewModel` + `LiveData`, observed from Activities/Views.
* Persistence: Room (Java annotation-processor API, not KSP/Kotlin).
* TTS: Android's built-in `android.speech.tts.TextToSpeech`, wrapped by `TtsEngine`.
* Background playback: a foreground `Service` with a persistent notification, started from `ReaderViewModel` when playback begins and stopped when the queue empties.
* Preferences: `SharedPreferences`, wrapped by a `Preferences` service (never read directly outside it).

## Open items

Anything not settled above lives in [`../proposal/`](../proposal/readme.md), not here.
