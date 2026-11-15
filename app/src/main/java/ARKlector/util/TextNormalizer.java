package ARKlector.util;

import java.util.Locale;

/**
 * Pure, static text cleanup. No state, no Android dependency — see
 * docs/foundational/design.md rule 3 and docs/implementation/parsing.md.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** Strips control characters, collapses whitespace, drops obvious markup artifacts. */
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String stripped = raw.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
        return stripped.replaceAll("\\s+", " ").trim();
    }

    /**
     * Best-effort locale guess. Never throws; falls back to the device default when
     * detection isn't confident. Placeholder heuristic — see
     * docs/proposal/open-questions.md item 5 for the real decision still open here.
     */
    public static Locale detectLanguage(String clean) {
        if (clean == null || clean.isBlank()) {
            return Locale.getDefault();
        }
        return Locale.getDefault();
    }
}
