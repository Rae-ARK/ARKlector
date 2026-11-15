package ARKlector.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.lifecycle.Observer;

import ARKlector.service.Preferences;
import ARKlector.ui.Themes.AppTheme;

/**
 * Settings screen: auto-read-on-copy, speech rate and pitch. Reads and writes
 * exclusively through {@link Preferences} — see docs/implementation/settings.md —
 * never through {@code SharedPreferences} directly, and never through a {@code ViewModel};
 * the settings doc settles this screen talking straight to the service.
 */
public final class SettingsScreen extends LinearLayout {

    /** Forwarded intent — {@code MainActivity} wires this back to {@code HomeScreen}. */
    public interface Listener {
        void onBack();
    }

    private static final float MIN_RATE = 0.5f;
    private static final float MAX_RATE = 2.0f;
    private static final float MIN_PITCH = 0.5f;
    private static final float MAX_PITCH = 2.0f;
    private static final int SEEK_STEPS = 100;

    private final Preferences preferences;
    private final SeekBar rateSeekBar;
    private final SeekBar pitchSeekBar;
    private final Observer<Preferences.Voice> voiceObserver = this::onVoiceChanged;

    private Preferences.Voice currentVoice;
    private boolean suppressSeekCallbacks;

    public SettingsScreen(Context context, Preferences preferences, Listener listener) {
        super(context);
        this.preferences = preferences;

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
        title.setText("Settings");
        AppTheme.styleTitle(title);
        header.addView(title, leftMarginParams(AppTheme.SPACING_MEDIUM_DP));
        addView(header);

        addView(sectionLabel(context, "Read clipboard automatically on copy"),
                topMarginParams(AppTheme.SPACING_LARGE_DP));
        Switch autoReadSwitch = new Switch(context);
        autoReadSwitch.setChecked(preferences.autoReadOnCopy());
        autoReadSwitch.setOnCheckedChangeListener((CompoundButton button, boolean checked) ->
                preferences.setAutoReadOnCopy(checked));
        addView(autoReadSwitch, topMarginParams(AppTheme.SPACING_SMALL_DP));

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
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // MutableLiveData already holds a value (Preferences seeds it in its constructor),
        // so this delivers synchronously and currentVoice is non-null before any user input.
        preferences.observeVoice().observeForever(voiceObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        preferences.observeVoice().removeObserver(voiceObserver);
        super.onDetachedFromWindow();
    }

    private void onVoiceChanged(Preferences.Voice voice) {
        currentVoice = voice;
        suppressSeekCallbacks = true;
        rateSeekBar.setProgress(progressFor(voice.speechRate(), MIN_RATE, MAX_RATE));
        pitchSeekBar.setProgress(progressFor(voice.pitch(), MIN_PITCH, MAX_PITCH));
        suppressSeekCallbacks = false;
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
}
