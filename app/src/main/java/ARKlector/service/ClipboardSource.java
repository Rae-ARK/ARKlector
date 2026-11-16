package ARKlector.service;

import android.content.ClipData;
import android.content.ClipboardManager;

import java.util.function.Consumer;

/**
 * Owns clipboard access. See docs/implementation/acquisition.md.
 */
public final class ClipboardSource {

    private final ClipboardManager manager;
    private final Preferences preferences;
    private final ClipboardManager.OnPrimaryClipChangedListener listener = this::onPrimaryClipChanged;

    private Consumer<String> onNewText;
    private String lastEmitted;

    public ClipboardSource(ClipboardManager manager, Preferences preferences) {
        this.manager = manager;
        this.preferences = preferences;
        this.manager.addPrimaryClipChangedListener(listener);
    }

    public void setListener(Consumer<String> onNewText) {
        this.onNewText = onNewText;
    }

    /** Manual "Read clipboard now" action — bypasses the auto-read setting. */
    public void readNow() {
        readClip(true);
    }

    /** Share-intent entry point. Produces identical output to a clipboard change. */
    public void submit(String text) {
        emit(text);
    }

    public void stop() {
        manager.removePrimaryClipChangedListener(listener);
    }

    private void onPrimaryClipChanged() {
        readClip(false);
    }

    private void readClip(boolean force) {
        if (!force && !preferences.autoReadOnCopy()) {
            return;
        }
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return;
        }
        // getText(), not coerceToText(null): coerceToText needs a Context for URI/intent
        // items and throws NullPointerException without one. Non-text clips are skipped.
        CharSequence text = clip.getItemAt(0).getText();
        if (text != null) {
            emit(text.toString());
        }
    }

    private void emit(String text) {
        if (text == null || text.equals(lastEmitted)) {
            return;
        }
        lastEmitted = text;
        if (onNewText != null) {
            onNewText.accept(text);
        }
    }
}
