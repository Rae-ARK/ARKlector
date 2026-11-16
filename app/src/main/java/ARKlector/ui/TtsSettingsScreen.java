package ARKlector.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;
import java.util.Optional;

import androidx.lifecycle.Observer;

import ARKlector.service.Preferences;
import ARKlector.service.TtsEngine;
import ARKlector.service.TtsVoiceModel;
import ARKlector.service.TtsVoiceModel.TtsVoiceInfo;
import ARKlector.ui.Themes.AppTheme;

/**
 * Text-to-speech settings: speed, pitch, and the entry point into {@link TtsVoicePickerScreen}.
 * Reached from {@link SettingsScreen}'s index — the rate/pitch sliders below lived directly
 * on that screen before it became an index page; they moved here with it.
 *
 * <p>The voice row and the picker behind it are ported from ARKarium's
 * {@code ui/TtsSettingsScreen.kt} and {@code ui/TtsVoicePickerScreen.kt} (Compose there,
 * plain {@code View}s here per docs/foundational/design.md). ARKarium's chunk/paragraph
 * read-progress tracking, chapter auto-continue and notification cover art were not
 * ported — ARKlector reads a flat segment queue, not chapters, so that machinery has
 * nothing here to attach to.
 *
 * <p>Reads and writes exclusively through {@link Preferences}, same as
 * {@code SettingsScreen} — see docs/implementation/settings.md.
 */
public final class TtsSettingsScreen extends LinearLayout {

    public interface Listener {
        void onBack();

        void onOpenVoicePicker();
    }

    private static final float MIN_RATE = 0.5f;
    private static final float MAX_RATE = 2.0f;
    private static final float MIN_PITCH = 0.5f;
    private static final float MAX_PITCH = 2.0f;
    private static final int SEEK_STEPS = 100;

    private final Preferences preferences;
    private final TtsEngine voiceEngine;
    private final SeekBar rateSeekBar;
    private final SeekBar pitchSeekBar;
    private final TextView voiceName;
    private final TextView voiceDescription;
    private final Observer<Preferences.Voice> voiceObserver = this::onVoiceChanged;
    private final Observer<List<TtsVoiceInfo>> engineVoicesObserver = this::onEngineVoicesChanged;

    private Preferences.Voice currentVoice;
    private List<TtsVoiceInfo> engineVoices = List.of();
    private boolean suppressSeekCallbacks;

    public TtsSettingsScreen(Context context, Preferences preferences, TtsEngine voiceEngine, Listener listener) {
        super(context);
        this.preferences = preferences;
        this.voiceEngine = voiceEngine;

        setOrientation(LinearLayout.VERTICAL);
        int padding = AppTheme.dpToPx(this, AppTheme.SPACING_LARGE_DP);
        setPadding(padding, padding, padding, padding);
        AppTheme.styleScreenBackground(this);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(context);
        backButton.setText("Back");
        AppTheme.styleSecondaryButton(backButton);
        backButton.setOnClickListener(v -> listener.onBack());
        header.addView(backButton);

        TextView title = new TextView(context);
        title.setText("Text-to-speech");
        AppTheme.styleTitle(title);
        header.addView(title, leftMarginParams(AppTheme.SPACING_MEDIUM_DP));
        addView(header);

        addView(sectionLabel(context, "Speech rate"), topMarginParams(AppTheme.SPACING_LARGE_DP));
        rateSeekBar = new SeekBar(context);
        rateSeekBar.setMax(SEEK_STEPS);
        rateSeekBar.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            void onUserChanged(int progress) {
                float rate = valueFor(progress, MIN_RATE, MAX_RATE);
                preferences.setVoice(withRate(currentVoice, rate));
            }
        });
        addView(rateSeekBar);

        addView(sectionLabel(context, "Pitch"), topMarginParams(AppTheme.SPACING_MEDIUM_DP));
        pitchSeekBar = new SeekBar(context);
        pitchSeekBar.setMax(SEEK_STEPS);
        pitchSeekBar.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            void onUserChanged(int progress) {
                float pitch = valueFor(progress, MIN_PITCH, MAX_PITCH);
                preferences.setVoice(withPitch(currentVoice, pitch));
            }
        });
        addView(pitchSeekBar);

        addView(divider(context), dividerParams(AppTheme.SPACING_LARGE_DP));

        LinearLayout voiceRow = new LinearLayout(context);
        voiceRow.setOrientation(LinearLayout.HORIZONTAL);
        voiceRow.setGravity(Gravity.CENTER_VERTICAL);
        voiceRow.setClickable(true);
        voiceRow.setFocusable(true);
        voiceRow.setOnClickListener(v -> listener.onOpenVoicePicker());

        LinearLayout voiceTextColumn = new LinearLayout(context);
        voiceTextColumn.setOrientation(LinearLayout.VERTICAL);

        voiceName = new TextView(context);
        AppTheme.styleBody(voiceName);
        voiceTextColumn.addView(voiceName);

        voiceDescription = new TextView(context);
        AppTheme.styleCaption(voiceDescription);
        voiceTextColumn.addView(voiceDescription, topMarginParams(4));

        LinearLayout.LayoutParams voiceColumnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        voiceRow.addView(voiceTextColumn, voiceColumnParams);

        TextView chevron = new TextView(context);
        chevron.setText(">");
        AppTheme.styleCaption(chevron);
        voiceRow.addView(chevron);

        addView(voiceRow, topMarginParams(AppTheme.SPACING_MEDIUM_DP));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Both LiveDatas already hold a value (Preferences seeds voiceLiveData in its
        // constructor; voiceEngine's defaults to an empty list), so both observers fire
        // synchronously and this screen renders a real state before any user input.
        preferences.observeVoice().observeForever(voiceObserver);
        voiceEngine.voices().observeForever(engineVoicesObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        preferences.observeVoice().removeObserver(voiceObserver);
        voiceEngine.voices().removeObserver(engineVoicesObserver);
        super.onDetachedFromWindow();
    }

    private void onVoiceChanged(Preferences.Voice voice) {
        currentVoice = voice;
        suppressSeekCallbacks = true;
        rateSeekBar.setProgress(progressFor(voice.speechRate(), MIN_RATE, MAX_RATE));
        pitchSeekBar.setProgress(progressFor(voice.pitch(), MIN_PITCH, MAX_PITCH));
        suppressSeekCallbacks = false;
        refreshVoiceSummary();
    }

    private void onEngineVoicesChanged(List<TtsVoiceInfo> voices) {
        engineVoices = voices;
        refreshVoiceSummary();
    }

    /**
     * "India · online · high quality" under the voice's own name — the same two-case
     * wording as {@link TtsVoiceModel#descriptionOf}, for the same consistency reason
     * given in that method's doc comment. Falls back to a plain label when the stored
     * voice id doesn't resolve against the engine yet (still loading, or nothing chosen).
     */
    private void refreshVoiceSummary() {
        if (currentVoice == null) {
            return;
        }
        Optional<TtsVoiceInfo> resolved = engineVoices.stream()
                .filter(voice -> voice.name().equals(currentVoice.voiceId()))
                .findFirst();
        if (currentVoice.voiceId() == null) {
            voiceName.setText("Voice: device default");
            voiceDescription.setText("Follows the system TTS voice");
        } else if (resolved.isPresent()) {
            voiceName.setText("Voice: " + resolved.get().name());
            voiceDescription.setText(TtsVoiceModel.descriptionOf(resolved.get()));
        } else {
            voiceName.setText("Voice: " + currentVoice.voiceId());
            voiceDescription.setText("Not available on this device");
        }
    }

    private abstract class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public final void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && !suppressSeekCallbacks) {
                onUserChanged(progress);
            }
        }

        abstract void onUserChanged(int progress);

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private static Preferences.Voice withRate(Preferences.Voice voice, float rate) {
        return new Preferences.Voice(voice.voiceId(), rate, voice.pitch());
    }

    private static Preferences.Voice withPitch(Preferences.Voice voice, float pitch) {
        return new Preferences.Voice(voice.voiceId(), voice.speechRate(), pitch);
    }

    private static int progressFor(float value, float min, float max) {
        return Math.round(((value - min) / (max - min)) * SEEK_STEPS);
    }

    private static float valueFor(int progress, float min, float max) {
        return min + (progress / (float) SEEK_STEPS) * (max - min);
    }

    private static TextView sectionLabel(Context context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        AppTheme.styleCaption(label);
        return label;
    }

    private View divider(Context context) {
        View divider = new View(context);
        AppTheme.styleDivider(divider);
        return divider;
    }

    private ViewGroup.LayoutParams topMarginParams(int spacingDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AppTheme.dpToPx(this, spacingDp);
        return params;
    }

    private ViewGroup.LayoutParams leftMarginParams(int spacingDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = AppTheme.dpToPx(this, spacingDp);
        return params;
    }

    /** A hairline divider needs an explicit height — WRAP_CONTENT alone renders as 0px. */
    private ViewGroup.LayoutParams dividerParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AppTheme.dpToPx(this, 1));
        params.topMargin = AppTheme.dpToPx(this, topMarginDp);
        return params;
    }
}
