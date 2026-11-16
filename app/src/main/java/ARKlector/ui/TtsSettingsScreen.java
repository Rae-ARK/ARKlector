package ARKlector.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import androidx.lifecycle.Observer;

import com.google.android.material.slider.Slider;

import org.arklector.app.R;

import ARKlector.service.Preferences;
import ARKlector.service.TtsEngine;
import ARKlector.service.TtsVoiceModel;
import ARKlector.service.TtsVoiceModel.TtsVoiceInfo;
import ARKlector.ui.Themes.AppTheme;

/**
 * Text-to-speech settings: speech rate and pitch (Material 3 sliders) and the entry to
 * the voice picker. Reads and writes exclusively through {@link Preferences}, same as
 * {@code SettingsScreen} - see docs/implementation/settings.md.
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

    private final Preferences preferences;
    private final TtsEngine voiceEngine;
    private final Slider rateSlider;
    private final Slider pitchSlider;
    private final TextView rateLabel;
    private final TextView pitchLabel;
    private final TextView voiceName;
    private final TextView voiceDescription;
    private final Observer<Preferences.Voice> voiceObserver = this::onVoiceChanged;
    private final Observer<List<TtsVoiceInfo>> engineVoicesObserver = this::onEngineVoicesChanged;

    private Preferences.Voice currentVoice;
    private List<TtsVoiceInfo> engineVoices = List.of();

    public TtsSettingsScreen(Context context, Preferences preferences, TtsEngine voiceEngine, Listener listener) {
        super(context);
        this.preferences = preferences;
        this.voiceEngine = voiceEngine;

        setOrientation(LinearLayout.VERTICAL);
        AppTheme.styleScreenBackground(this);

        addView(AppTheme.topBar(context,
                AppTheme.iconButton(context, R.drawable.ic_arrow_back, "Back", v -> listener.onBack()),
                "Text-to-speech"));

        int side = AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP);

        rateLabel = new TextView(context);
        AppTheme.styleSectionLabel(rateLabel);
        rateLabel.setPadding(side, side, side, 0);
        addView(rateLabel);
        rateSlider = slider(context, MIN_RATE, MAX_RATE, (value) -> preferences.setVoice(withRate(currentVoice, value)));
        addView(rateSlider);

        pitchLabel = new TextView(context);
        AppTheme.styleSectionLabel(pitchLabel);
        pitchLabel.setPadding(side, side, side, 0);
        addView(pitchLabel);
        pitchSlider = slider(context, MIN_PITCH, MAX_PITCH, (value) -> preferences.setVoice(withPitch(currentVoice, value)));
        addView(pitchSlider);

        addView(AppTheme.divider(context));

        LinearLayout voiceRow = new LinearLayout(context);
        voiceRow.setOrientation(LinearLayout.HORIZONTAL);
        voiceRow.setGravity(Gravity.CENTER_VERTICAL);
        voiceRow.setPadding(side, AppTheme.dpToPx(this, AppTheme.SPACING_SMALL_DP),
                side, AppTheme.dpToPx(this, AppTheme.SPACING_SMALL_DP));
        voiceRow.setMinimumHeight(AppTheme.dpToPx(this, AppTheme.LIST_ITEM_HEIGHT_DP));
        AppTheme.styleRowRipple(voiceRow);
        voiceRow.setOnClickListener(v -> listener.onOpenVoicePicker());

        LinearLayout voiceTextColumn = new LinearLayout(context);
        voiceTextColumn.setOrientation(LinearLayout.VERTICAL);

        voiceName = new TextView(context);
        AppTheme.styleBody(voiceName);
        voiceTextColumn.addView(voiceName);

        voiceDescription = new TextView(context);
        AppTheme.styleCaption(voiceDescription);
        voiceTextColumn.addView(voiceDescription);

        voiceRow.addView(voiceTextColumn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        voiceRow.addView(SettingsScreen.chevron(context));
        addView(voiceRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        addView(AppTheme.divider(context));
    }

    private interface ValueConsumer {
        void accept(float value);
    }

    private Slider slider(Context context, float min, float max, ValueConsumer onUserChanged) {
        Slider slider = new Slider(context);
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setValue(1.0f);
        // Only react to the user dragging: programmatic setValue() in onVoiceChanged
        // reports fromUser == false, so it can't loop back into Preferences.
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) {
                onUserChanged.accept(value);
            }
        });
        return slider;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Both LiveDatas already hold a value, so both observers fire synchronously and
        // this screen renders a real state before any user input.
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
        // Values saved by the old SeekBar UI can be arbitrary floats, and Slider.setValue
        // throws outside [from, to], so clamp.
        rateSlider.setValue(clamp(voice.speechRate(), MIN_RATE, MAX_RATE));
        pitchSlider.setValue(clamp(voice.pitch(), MIN_PITCH, MAX_PITCH));
        rateLabel.setText(String.format(Locale.getDefault(), "Speech rate · %.2f×", voice.speechRate()));
        pitchLabel.setText(String.format(Locale.getDefault(), "Pitch · %.2f", voice.pitch()));
        refreshVoiceSummary();
    }

    private void onEngineVoicesChanged(List<TtsVoiceInfo> voices) {
        engineVoices = voices;
        refreshVoiceSummary();
    }

    /**
     * "India · online · high quality" under the voice's own name - the same wording as
     * {@link TtsVoiceModel#descriptionOf}. Falls back to a plain label when the stored
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Preferences.Voice withRate(Preferences.Voice voice, float rate) {
        return new Preferences.Voice(voice.voiceId(), rate, voice.pitch());
    }

    private static Preferences.Voice withPitch(Preferences.Voice voice, float pitch) {
        return new Preferences.Voice(voice.voiceId(), voice.speechRate(), pitch);
    }
}
