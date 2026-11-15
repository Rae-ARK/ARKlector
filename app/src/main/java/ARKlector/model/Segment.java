package ARKlector.model;

import java.util.Locale;

/**
 * A single speakable chunk, produced by {@link ARKlector.util.Segmenter}.
 * See docs/implementation/parsing.md.
 */
public record Segment(String text, Locale guessedLocale) {
}
