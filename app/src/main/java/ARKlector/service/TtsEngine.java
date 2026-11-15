package ARKlector.service;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import ARKlector.model.Segment;

/**
 * Wraps {@link TextToSpeech}. See docs/implementation/synthesis-playback.md.
 * A second implementation of this surface (cloud/neural voices) would promote this
 * class to an interface per docs/foundational/design.md rule 1 — not done until that
 * proposal (docs/proposal/open-questions.md item 1) is settled.
 */
public final class TtsEngine {

    private final TextToSpeech tts;
    private final MutableLiveData<Boolean> ready = new MutableLiveData<>(false);

    private Consumer<Segment> onSegmentDone = s -> { };
    private Consumer<Segment> onSegmentError = s -> { };
    private Segment pendingSegment;

    public TtsEngine(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status ->
                ready.postValue(status == TextToSpeech.SUCCESS));
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

    /** Called by {@link PlaybackService} so it knows when to advance the queue. */
    public void setProgressListeners(Consumer<Segment> onDone, Consumer<Segment> onError) {
        this.onSegmentDone = onDone != null ? onDone : s -> { };
        this.onSegmentError = onError != null ? onError : s -> { };
    }

    public void speak(Segment segment, Preferences.Voice voice) {
        if (voice != null) {
            tts.setSpeechRate(voice.speechRate());
            tts.setPitch(voice.pitch());
        }
        Locale locale = segment.guessedLocale() != null ? segment.guessedLocale() : Locale.getDefault();
        tts.setLanguage(locale);
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
}
