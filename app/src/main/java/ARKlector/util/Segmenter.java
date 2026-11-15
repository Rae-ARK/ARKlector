package ARKlector.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ARKlector.model.Segment;

/**
 * Splits cleaned text into sentence-respecting chunks under a TTS engine's per-utterance
 * character limit. See docs/implementation/parsing.md.
 */
public final class Segmenter {

    private Segmenter() {
    }

    public static List<Segment> split(String clean, Locale locale, int maxChars) {
        List<Segment> segments = new ArrayList<>();
        if (clean == null || clean.isBlank()) {
            return segments;
        }

        String[] sentences = clean.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            if (current.length() + sentence.length() + 1 > maxChars && current.length() > 0) {
                segments.add(new Segment(current.toString().trim(), locale));
                current.setLength(0);
            }
            current.append(sentence).append(' ');

            // A single sentence longer than maxChars on its own: hard-wrap it.
            while (current.length() > maxChars) {
                segments.add(new Segment(current.substring(0, maxChars).trim(), locale));
                current.delete(0, maxChars);
            }
        }
        if (current.length() > 0) {
            segments.add(new Segment(current.toString().trim(), locale));
        }
        return segments;
    }
}
