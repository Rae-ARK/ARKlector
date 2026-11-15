# Parsing

How a raw clipboard string becomes speakable segments. No service here — this stage is deliberately stateless and lives as `static` methods, per [design.md rule 3](../foundational/design.md#code-philosophy).

## Life cycle

There isn't one, by design: nothing here holds a resource, so nothing needs creating or tearing down. Each call is independent and pure — input string in, `List<Segment>` out. This is the part of the pipeline that is heaviest on unit tests ([design.md testing strategy](../foundational/design.md#testing-strategy)).

## Pipeline

1. `TextNormalizer.clean(String raw)` — strips control characters, collapses whitespace, drops obvious markup (HTML entities, Markdown artifacts) that would otherwise be read aloud literally.
2. `TextNormalizer.detectLanguage(String clean)` — best-effort locale guess, used to pick a TTS voice in [synthesis](synthesis-playback.md); never blocks the pipeline if it fails, falls back to the device locale.
3. `Segmenter.split(String clean, int maxChars)` — breaks the cleaned text into sentence-respecting chunks under the TTS engine's per-utterance character limit, so the user hears a single long paste as a sequence rather than a single truncated utterance.

## API

```java
public record Segment(String text, Locale guessedLocale) {}

public final class TextNormalizer {
    public static String clean(String raw) { ... }
    public static Locale detectLanguage(String clean) { ... }
    private TextNormalizer() {}
}

public final class Segmenter {
    public static List<Segment> split(String clean, Locale locale, int maxChars) { ... }
    private Segmenter() {}
}
```

Output of this stage is `List<Segment>`, appended to the queue and consumed by [synthesis & playback](synthesis-playback.md).
