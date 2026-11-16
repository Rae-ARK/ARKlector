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
                addIfNotBlank(segments, current.toString(), locale);
                current.setLength(0);
            }
            current.append(sentence).append(' ');

            // A single sentence longer than maxChars on its own: hard-wrap it, preferring
            // the last space within the limit so words aren't cut in half.
            while (current.length() > maxChars) {
                int cut = current.lastIndexOf(" ", maxChars);
                if (cut <= 0) {
                    cut = maxChars;
                }
                addIfNotBlank(segments, current.substring(0, cut), locale);
                current.delete(0, cut);
                while (current.length() > 0 && current.charAt(0) == ' ') {
                    current.deleteCharAt(0);
                }
            }
        }
        addIfNotBlank(segments, current.toString(), locale);
        return segments;
    }

    private static void addIfNotBlank(List<Segment> segments, String text, Locale locale) {
        String trimmed = text.trim();
        if (!trimmed.isEmpty()) {
            segments.add(new Segment(trimmed, locale));
        }
    }
}
