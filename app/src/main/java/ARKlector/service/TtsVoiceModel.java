package ARKlector.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure, {@code android.speech.tts.Voice}-free model for the voice picker, ported from
 * ARKarium's {@code playback/TtsVoiceModel.kt}. {@code Voice} has no public constructor,
 * so code that takes one directly can't be exercised from a plain JVM test; everything
 * here only ever sees the plain {@link TtsVoiceInfo} below, so it stays in the "plain
 * logic" tier of docs/foundational/design.md's testing strategy (no Robolectric, no
 * device) and is covered by {@code TtsVoiceModelTest}. Converting a real {@code Voice}
 * into {@link TtsVoiceInfo} lives in {@link TtsEngine}, which already imports
 * {@code android.speech.tts.*} for other reasons.
 */
public final class TtsVoiceModel {

    private TtsVoiceModel() {
    }

    /**
     * @param name     the engine's voice name/id — what {@code TextToSpeech.setVoice} takes
     *                 and what {@link Preferences.Voice#voiceId()} stores.
     * @param language BCP-47-ish language subtag only (e.g. "en"), not the full locale —
     *                 keeping language and region apart is what lets {@link #languagesOf}
     *                 group "English (India)" and "English (United States)" under one
     *                 language, rather than one group per full locale.
     * @param region   display region, e.g. "India"; "" if the voice's locale has none.
     * @param quality  one of {@code Voice.QUALITY_VERY_LOW}(100)..{@code QUALITY_VERY_HIGH}(500),
     *                 passed through as a plain {@code int} so this file doesn't need the
     *                 {@code Voice} class just to compare it.
     */
    public record TtsVoiceInfo(
            String name,
            String language,
            String region,
            int quality,
            boolean online,
            boolean installed) {
    }

    /**
     * The languages present in {@code voices}: {@code deviceLanguage} first if it's among
     * them, then the rest alphabetically — "device's languages first" ordering.
     */
    public static List<String> languagesOf(List<TtsVoiceInfo> voices, String deviceLanguage) {
        LinkedHashSet<String> languages = new LinkedHashSet<>();
        for (TtsVoiceInfo voice : voices) {
            languages.add(voice.language());
        }
        List<String> rest = languages.stream()
                .filter(language -> !language.equals(deviceLanguage))
                .sorted()
                .collect(Collectors.toList());
        List<String> result = new ArrayList<>();
        if (languages.contains(deviceLanguage)) {
            result.add(deviceLanguage);
        }
        result.addAll(rest);
        return result;
    }

    /**
     * Every voice for one {@code language}: local voices before online ones, then by
     * region, then name — stable and scannable rather than whatever order the engine's
     * {@code getVoices()} happened to return.
     */
    public static List<TtsVoiceInfo> rowsForLanguage(List<TtsVoiceInfo> voices, String language) {
        return voices.stream()
                .filter(voice -> voice.language().equals(language))
                .sorted(Comparator.comparing(TtsVoiceInfo::online)
                        .thenComparing(TtsVoiceInfo::region)
                        .thenComparing(TtsVoiceInfo::name))
                .collect(Collectors.toList());
    }

    /**
     * "India · online · high quality" — one line that states region, online/offline and
     * quality tier the same way for both cases.
     */
    public static String descriptionOf(TtsVoiceInfo voice) {
        List<String> parts = new ArrayList<>();
        if (!voice.region().isBlank()) {
            parts.add(voice.region());
        }
        parts.add(voice.online() ? "online" : "offline");
        parts.add(qualityLabel(voice.quality()));
        return String.join(" · ", parts);
    }

    /**
     * Same five-tier >= ladder as the screen's own summary text — {@code Voice.quality}
     * is an {@code int} constant, not a bounded enum.
     */
    public static String qualityLabel(int quality) {
        if (quality >= 500) {
            return "very high quality";
        } else if (quality >= 400) {
            return "high quality";
        } else if (quality >= 300) {
            return "standard quality";
        } else if (quality >= 200) {
            return "low quality";
        } else {
            return "very low quality";
        }
    }

    /** Voices whose data is actually downloaded. */
    public static List<TtsVoiceInfo> installedOnly(List<TtsVoiceInfo> voices) {
        return voices.stream().filter(TtsVoiceInfo::installed).collect(Collectors.toList());
    }

    /**
     * {@code voices} (already filtered/sorted by {@link #rowsForLanguage}) with any name
     * present in {@code favorites} moved to the front; favorites and non-favorites each
     * keep their existing relative order — {@code Stream.sorted} is stable, so this only
     * ever reorders across the favorite/non-favorite boundary, never within either group.
     */
    public static List<TtsVoiceInfo> withFavoritesFirst(List<TtsVoiceInfo> voices, Set<String> favorites) {
        return voices.stream()
                .sorted(Comparator.comparing(voice -> !favorites.contains(voice.name())))
                .collect(Collectors.toList());
    }

    /**
     * The name {@link TtsEngine} should hand to {@code setVoice()} — the stored
     * voice-name preference, if it's still among the names actually available right now,
     * or {@code null} if there's nothing to apply (no stored name, or a stored name that
     * doesn't resolve against this engine/device: an uninstalled language pack, or a
     * changed engine).
     */
    public static String voiceNameToApply(String storedName, List<String> availableNames) {
        if (storedName != null && availableNames.contains(storedName)) {
            return storedName;
        }
        return null;
    }
}
