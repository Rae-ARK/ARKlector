# Acquisition

How content enters the app. Owned by `ClipboardSource`.

## Life cycle

**Created** — `ClipboardSource` is constructed once, in `ReaderViewModel`'s constructor, with the app `ClipboardManager` injected in. It registers a single `OnPrimaryClipChangedListener`.

**Active** — while `MainActivity` is started (`onStart`/`onStop`-bounded, via the `ViewModel`'s lifecycle observation):
* Every primary-clip change fires the listener.
* `ClipboardSource` reads the clip, checks it against "auto-read on copy" in [settings](settings.md), and — if the text differs from the last segment already queued — emits it to `ReaderViewModel` as a new raw string.
* A manual "Read clipboard now" action in the UI calls `ClipboardSource.readNow()` directly, bypassing the auto-read setting.
* Share-intent (`ACTION_SEND`, `text/plain`) received by `MainActivity` is routed to the same `ClipboardSource.submit(String)` entry point, so both paths produce identical output.

**Torn down** — listener is unregistered when the owning `ViewModel` is cleared (`onCleared`). No resource outlives the Activity/ViewModel pair; `ClipboardSource` holds no persistent state of its own.

## API

```java
public final class ClipboardSource {
    public ClipboardSource(ClipboardManager manager, Preferences preferences) { ... }
    public void setListener(Consumer<String> onNewText) { ... }
    public void readNow() { ... }
    public void submit(String text) { ... } // share-intent entry point
    public void stop() { ... } // unregisters the clipboard listener
}
```

Output of this stage is a raw `String`, handed to [parsing](parsing.md).
