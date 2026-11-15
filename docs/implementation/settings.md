# Settings

Cross-cutting configuration, read by every other stage, owned by none of them. Backed by `SharedPreferences`, wrapped by a single `Preferences` service — nothing outside it touches `SharedPreferences` directly.

## Life cycle

**Created** — constructed once in `Application.onCreate()`, holding the default `SharedPreferences` instance. Handed by constructor injection to anything that needs it (`ClipboardSource`, `TtsEngine` callers, `PlaybackService`).

**Active** — reads are synchronous and cheap (cached `SharedPreferences` values, no disk I/O per read). Writes go through typed setters on `Preferences`, each of which also updates an in-memory `LiveData` so open screens reflect a change (e.g. the voice picker) without needing to re-read on resume.

**Torn down** — never explicitly; it's process-scoped like the database, and `SharedPreferences` itself persists to disk independently of any in-memory state here.

## Settings (current set)

| Key | Type | Default | Read by |
|---|---|---|---|
| `autoReadOnCopy` | boolean | `true` | [Acquisition](acquisition.md) |
| `voiceId` | String | device default | [Synthesis & Playback](synthesis-playback.md) |
| `speechRate` | float | `1.0` | [Synthesis & Playback](synthesis-playback.md) |
| `pitch` | float | `1.0` | [Synthesis & Playback](synthesis-playback.md) |

## API

```java
public final class Preferences {
    public Preferences(SharedPreferences prefs) { ... }

    public boolean autoReadOnCopy() { ... }
    public void setAutoReadOnCopy(boolean value) { ... }

    public record Voice(String voiceId, float speechRate, float pitch) {}
    public LiveData<Voice> observeVoice() { ... }
    public void setVoice(Voice voice) { ... }
}
```

Settings UI (a plain `View`-based screen, no XML) reads/writes exclusively through this class — never through `getSharedPreferences()` directly from an `Activity`.
