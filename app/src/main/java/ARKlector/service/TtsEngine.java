package ARKlector.service;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import ARKlector.model.Segment;
import ARKlector.service.TtsVoiceModel.TtsVoiceInfo;

/**
 * Wraps {@link TextToSpeech}. See docs/implementation/synthesis-playback.md.
 * Voice enumeration and selection ported from ARKarium's {@code playback/TtsPlaybackService.kt}
 * (the part of it that converts a real {@code Voice} into the framework-free
 * {@link TtsVoiceInfo} from {@link TtsVoiceModel}); ARKarium's chunk/paragraph progress
 * tracking and chapter-aware session state were not — ARKlector has no chapters, and that
 * part of ARKarium is Compose/service machinery well past what a clipboard reader needs.
 * A second implementation of this surface (cloud/neural voices) would promote this
 * class to an interface per docs/foundational/design.md rule 1 — not done until that
 * proposal (docs/proposal/open-questions.md item 1) is settled.
 */
public final class TtsEngine {

    private final TextToSpeech tts;
    private final MutableLiveData<Boolean> ready = new MutableLiveData<>(false);
    private final MutableLiveData<List<TtsVoiceInfo>> voices = new MutableLiveData<>(List.of());

    private Consumer<Segment> onSegmentDone = s -> { };
    private Consumer<Segment> onSegmentError = s -> { };
    private Segment pendingSegment;

    public TtsEngine(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            boolean success = status == TextToSpeech.SUCCESS;
            ready.postValue(success);
            if (success) {
                voices.postValue(loadVoices());
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                if (pendingSegment != null) {
                    onSegmentDone.accept(pendingSegment);
                }
            }

            @Override
            public void onError(String utteranceId) {
                if (pendingSegment != null) {
                    onSegmentError.accept(pendingSegment);
                }
            }
        });
    }

    public LiveData<Boolean> ready() {
        return ready;
    }

    /**
     * The device/engine's installed voices, as the framework-free {@link TtsVoiceInfo}
     * the voice picker (and {@link TtsVoiceModel}'s grouping/sorting) works with. Empty
     * until {@link #ready()} goes true.
     */
    public LiveData<List<TtsVoiceInfo>> voices() {
        return voices;
    }

    /** Called by {@link PlaybackService} so it knows when to advance the queue. */
    public void setProgressListeners(Consumer<Segment> onDone, Consumer<Segment> onError) {
        this.onSegmentDone = onDone != null ? onDone : s -> { };
        this.onSegmentError = onError != null ? onError : s -> { };
    }

    /**
     * Sets voice/locale/pitch/rate from {@code voice} for this call only (see
     * docs/implementation/synthesis-playback.md), then speaks {@code segment}. A
     * {@code voice.voiceId()} that still resolves against this engine takes precedence
     * and stays pinned regardless of the segment's guessed locale; falls back to the
     * per-segment locale otherwise. One answer to docs/proposal/open-questions.md item
     * 5 ("stay pinned to the user's chosen voice") — revisit there if that's ever
     * settled the other way.
     */
    public void speak(Segment segment, Preferences.Voice voice) {
        boolean voicePinned = false;
        if (voice != null) {
            tts.setSpeechRate(voice.speechRate());
            tts.setPitch(voice.pitch());
            voicePinned = applyVoice(voice.voiceId());
        }
        if (!voicePinned) {
            Locale locale = segment.guessedLocale() != null ? segment.guessedLocale() : Locale.getDefault();
            tts.setLanguage(locale);
        }
        pendingSegment = segment;
        String utteranceId = UUID.randomUUID().toString();
        tts.speak(segment.text(), TextToSpeech.QUEUE_ADD, null, utteranceId);
    }

    public void stop() {
        tts.stop();
    }

    public void shutdown() {
        tts.shutdown();
    }

    /** @return true if {@code voiceId} resolved to an installed engine voice and was applied. */
    private boolean applyVoice(String voiceId) {
        if (voiceId == null) {
            return false;
        }
        Set<Voice> engineVoices = tts.getVoices();
        if (engineVoices == null) {
            return false;
        }
        for (Voice candidate : engineVoices) {
            if (candidate.getName().equals(voiceId)) {
                tts.setVoice(candidate);
                return true;
            }
        }
        return false;
    }

    private List<TtsVoiceInfo> loadVoices() {
        Set<Voice> engineVoices = tts.getVoices();
        if (engineVoices == null) {
            return List.of();
        }
        List<TtsVoiceInfo> result = new ArrayList<>();
        for (Voice voice : engineVoices) {
            result.add(toVoiceInfo(voice));
        }
        return result;
    }

    private static TtsVoiceInfo toVoiceInfo(Voice voice) {
        Locale locale = voice.getLocale();
        Set<String> features = voice.getFeatures();
        boolean notInstalled = features != null
                && features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
        return new TtsVoiceInfo(
                voice.getName(),
                locale.getLanguage(),
                locale.getDisplayCountry(),
                voice.getQuality(),
                voice.isNetworkConnectionRequired(),
                !notInstalled);
    }
}
