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

    private void onClipboardText(String raw) {
        String clean = TextNormalizer.clean(raw);
        lastClipboardText.setValue(clean);
        Locale locale = TextNormalizer.detectLanguage(clean);
        List<Segment> segments = Segmenter.split(clean, locale, MAX_UTTERANCE_CHARS);
        if (!segments.isEmpty()) {
            queueService.enqueue(segments);
            // Starting PlaybackService on a queue empty -> non-empty transition is the
            // Activity/Service boundary described in docs/implementation/synthesis-playback.md;
            // wired from MainActivity, not here, to keep this class free of Context.
        }
    }

    @Override
    protected void onCleared() {
        clipboardSource.stop();
    }
}
