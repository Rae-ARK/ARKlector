package ARKlector.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import ARKlector.ui.Themes.AppTheme;
import ARKlector.viewmodel.PlaybackState;

/**
 * The reader's home screen: current playback status, the manual "read clipboard now"
 * action, and the way into {@link SettingsScreen}. Built programmatically per
 * docs/foundational/design.md — no XML layout.
 *
 * <p>Renders state pushed in from {@code MainActivity} via {@link #setPlaybackState};
 * this screen never touches the {@code ViewModel} itself. Intent flows back out through
 * {@link Listener}. See docs/foundational/architecture.md's UI layer.
 */
public final class HomeScreen extends LinearLayout {

    /** Forwarded intent — {@code MainActivity} wires these to the ViewModel/navigation. */
    public interface Listener {
        void onReadClipboardNow();

        void onOpenSettings();
    }

    private final TextView statusText;

    public HomeScreen(Context context, Listener listener) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = AppTheme.dpToPx(this, AppTheme.SPACING_LARGE_DP);
        setPadding(padding, padding, padding, padding);
        AppTheme.styleScreenBackground(this);

        TextView title = new TextView(context);
        title.setText("ARKlector");
        AppTheme.styleTitle(title);
        addView(title);

        statusText = new TextView(context);
        statusText.setText("Idle");
        AppTheme.styleBody(statusText);
        addView(statusText, topMarginParams(AppTheme.SPACING_MEDIUM_DP));

        Button readNowButton = new Button(context);
        readNowButton.setText("Read clipboard now");
        AppTheme.stylePrimaryButton(readNowButton);
        readNowButton.setOnClickListener(v -> listener.onReadClipboardNow());
        addView(readNowButton, topMarginParams(AppTheme.SPACING_LARGE_DP));

        Button settingsButton = new Button(context);
        settingsButton.setText("Settings");
        AppTheme.styleSecondaryButton(settingsButton);
        settingsButton.setOnClickListener(v -> listener.onOpenSettings());
        addView(settingsButton, topMarginParams(AppTheme.SPACING_SMALL_DP));
    }

    /**
     * Pushed in from {@code MainActivity}'s {@code PlaybackState} observer — this screen
     * stays a dumb renderer and never reads the ViewModel directly.
     */
    public void setPlaybackState(PlaybackState state) {
        String label = switch (state) {
            case PlaybackState.Idle ignored -> "Idle";
            case PlaybackState.Playing playing -> "Playing: " + playing.current().text();
            case PlaybackState.Paused paused -> "Paused: " + paused.current().text();
        };
        statusText.setText(label);
    }

    private LinearLayout.LayoutParams topMarginParams(int spacingDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AppTheme.dpToPx(this, spacingDp);
        return params;
    }
}
