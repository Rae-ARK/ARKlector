package ARKlector.viewmodel;

import java.util.List;
import java.util.Locale;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import ARKlector.model.Segment;
import ARKlector.service.ClipboardSource;
import ARKlector.service.ReadingQueueService;
import ARKlector.util.Segmenter;
import ARKlector.util.TextNormalizer;

/**
 * Holds UI state and forwards intent. See docs/foundational/architecture.md layers,
 * and docs/implementation/acquisition.md + parsing.md for what happens before this point.
 */
public final class ReaderViewModel extends ViewModel {

    private static final int MAX_UTTERANCE_CHARS = 300;

    private final ClipboardSource clipboardSource;
    private final ReadingQueueService queueService;
    private final MutableLiveData<PlaybackState> playbackState = new MutableLiveData<>(new PlaybackState.Idle());

    /**
     * The cleaned text of the most recently retrieved clipboard entry — what
     * {@code HomeScreen}'s content area fills in with after the clipboard icon is
     * tapped. Holds the normalized text regardless of whether it produced any segments
     * (e.g. a clipboard with only whitespace), so the screen always reflects what was
     * actually retrieved. {@code null} until the first clipboard read of this
     * ViewModel's lifetime.
     */
    private final MutableLiveData<String> lastClipboardText = new MutableLiveData<>(null);

    private Runnable onSegmentsQueued = () -> { };

    public ReaderViewModel(ClipboardSource clipboardSource, ReadingQueueService queueService) {
        this.clipboardSource = clipboardSource;
        this.queueService = queueService;
        this.clipboardSource.setListener(this::onClipboardText);
    }

    public LiveData<PlaybackState> playbackState() {
        return playbackState;
    }

    public LiveData<String> lastClipboardText() {
        return lastClipboardText;
    }

    public void readClipboardNow() {
        clipboardSource.readNow();
    }

    /** Share-intent entry point; same path as a clipboard change. */
    public void submitSharedText(String text) {
        clipboardSource.submit(text);
    }

    public ReadingQueueService queueService() {
        return queueService;
    }

    /** Mirrors {@code PlaybackService}'s state into what the UI observes. Main thread. */
    public void updatePlaybackState(PlaybackState state) {
        playbackState.setValue(state);
    }

    /**
     * Invoked after segments are queued so the Activity (which owns the Context this
     * class deliberately lacks) can start {@code PlaybackService}. Pass {@code null}
     * to clear it, e.g. from {@code onDestroy}, so a retained ViewModel doesn't hold
     * a destroyed Activity.
     */
    public void setOnSegmentsQueued(Runnable callback) {
        this.onSegmentsQueued = callback != null ? callback : () -> { };
    }

    private void onClipboardText(String raw) {
        String clean = TextNormalizer.clean(raw);
        lastClipboardText.setValue(clean);
        Locale locale = TextNormalizer.detectLanguage(clean);
        List<Segment> segments = Segmenter.split(clean, locale, MAX_UTTERANCE_CHARS);
        if (!segments.isEmpty()) {
            queueService.enqueue(segments);
            // Starting PlaybackService is the Activity/Service boundary described in
            // docs/implementation/synthesis-playback.md; wired from MainActivity via
            // setOnSegmentsQueued, to keep this class free of Context.
            onSegmentsQueued.run();
        }
    }

    @Override
    protected void onCleared() {
        clipboardSource.stop();
    }
}
