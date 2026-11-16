package ARKlector.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.arklector.app.R;

import ARKlector.ui.Themes.AppTheme;
import ARKlector.viewmodel.PlaybackState;

/**
 * The reader's home screen: a Material 3 top app bar (app icon, title, clipboard and
 * settings actions), the most recently retrieved clipboard text, and - only while TTS is
 * active - a "now reading" card with playback controls. Built programmatically per
 * docs/foundational/design.md; styling comes from {@link AppTheme}.
 *
 * <p>Renders state pushed in from {@code MainActivity} via {@link #setPlaybackState}
 * and {@link #setContent}; this screen never touches the {@code ViewModel} itself.
 * Intent flows back out through {@link Listener}.
 */
public final class HomeScreen extends LinearLayout {

    private static final String PLACEHOLDER_TEXT =
            "Tap the clipboard icon to load the most recently copied text.";

    /** Forwarded intent - {@code MainActivity} wires these to the ViewModel/service. */
    public interface Listener {
        /** Clipboard icon tapped: retrieve the latest clipboard entry and read it. */
        void onReadClipboardNow();

        void onOpenSettings();

        /** Play/pause button: pauses when playing, resumes when paused. */
        void onTogglePlayPause();

        /** Skip to the next queued segment. */
        void onSkipNext();

        /** Stop playback and discard the remaining queue. */
        void onStopPlayback();
    }

    private final TextView contentText;
    private final MaterialCardView playbackCard;
    private final TextView nowReadingText;
    private final FloatingActionButton playPauseButton;

    public HomeScreen(Context context, Listener listener) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        AppTheme.styleScreenBackground(this);

        addView(topBar(context, listener));

        ScrollView contentScroll = new ScrollView(context);
        contentText = new TextView(context);
        int side = AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP);
        contentText.setPadding(side, side, side, side);
        contentScroll.addView(contentText, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(contentScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // Playback card (hidden until TTS is active).
        playbackCard = new MaterialCardView(context);
        playbackCard.setCardBackgroundColor(AppTheme.secondaryContainer(this));
        playbackCard.setCardElevation(0f);
        playbackCard.setVisibility(View.GONE);

        LinearLayout cardContent = new LinearLayout(context);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        int pad = AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP);
        cardContent.setPadding(pad, pad, pad, pad);

        TextView overline = new TextView(context);
        overline.setText("Now reading");
        AppTheme.styleLabel(overline, AppTheme.onSecondaryContainer(this));
        cardContent.addView(overline);

        nowReadingText = new TextView(context);
        AppTheme.styleBody(nowReadingText);
        nowReadingText.setTextColor(AppTheme.onSecondaryContainer(this));
        nowReadingText.setMaxLines(3);
        nowReadingText.setEllipsize(TextUtils.TruncateAt.END);
        cardContent.addView(nowReadingText, topMargin(AppTheme.SPACING_SMALL_DP));

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        ImageButton stop = AppTheme.iconButton(context, R.drawable.ic_stop, "Stop",
                v -> listener.onStopPlayback());
        controls.addView(stop);

        playPauseButton = new FloatingActionButton(context);
        playPauseButton.setOnClickListener(v -> listener.onTogglePlayPause());
        LinearLayout.LayoutParams fabParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int fabMargin = AppTheme.dpToPx(this, AppTheme.SPACING_LARGE_DP);
        fabParams.leftMargin = fabMargin;
        fabParams.rightMargin = fabMargin;
        controls.addView(playPauseButton, fabParams);

        ImageButton skip = AppTheme.iconButton(context, R.drawable.ic_skip_next, "Skip to next",
                v -> listener.onSkipNext());
        controls.addView(skip);

        cardContent.addView(controls, topMargin(AppTheme.SPACING_SMALL_DP));
        playbackCard.addView(cardContent);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(side, side, side, side);
        addView(playbackCard, cardParams);

        setContent(null);
        setPlaybackState(new PlaybackState.Idle());
    }

    /** [app icon] ARKlector ........ [clipboard] [settings] */
    private LinearLayout topBar(Context context, Listener listener) {
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.app_icon);
        logo.setContentDescription("ARKlector");
        int logoSize = AppTheme.dpToPx(this, 32);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoParams.leftMargin = AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP - 4);
        logo.setLayoutParams(logoParams);

        ImageButton clipboard = AppTheme.iconButton(context, R.drawable.ic_content_paste,
                "Read clipboard now", v -> listener.onReadClipboardNow());
        ImageButton settings = AppTheme.iconButton(context, R.drawable.ic_settings,
                "Settings", v -> listener.onOpenSettings());

        return AppTheme.topBar(context, logo, "ARKlector", clipboard, settings);
    }

    /**
     * Pushed in from {@code MainActivity} - this screen stays a dumb renderer. The
     * playback card is shown only while playing or paused (i.e. TTS is active).
     */
    public void setPlaybackState(PlaybackState state) {
        // instanceof patterns, not a pattern-matching `switch`: javac compiles the latter
        // to java.lang.runtime.SwitchBootstraps, which android.jar doesn't contain, so
        // compileDebugJavaWithJavac crashes with a CompletionFailure.
        if (state instanceof PlaybackState.Playing playing) {
            showPlayback(playing.current().text(), true);
        } else if (state instanceof PlaybackState.Paused paused) {
            showPlayback(paused.current().text(), false);
        } else {
            playbackCard.setVisibility(View.GONE);
        }
    }

    private void showPlayback(String segmentText, boolean playing) {
        nowReadingText.setText(segmentText);
        playPauseButton.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        playPauseButton.setContentDescription(playing ? "Pause" : "Play");
        playbackCard.setVisibility(View.VISIBLE);
    }

    /**
     * Fills the content area with the latest retrieved clipboard text, or restores the
     * placeholder for {@code null}/blank text.
     */
    public void setContent(String text) {
        if (text == null || text.isBlank()) {
            contentText.setText(PLACEHOLDER_TEXT);
            AppTheme.styleCaption(contentText);
        } else {
            contentText.setText(text);
            AppTheme.styleBody(contentText);
        }
    }

    private LinearLayout.LayoutParams topMargin(int dp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AppTheme.dpToPx(this, dp);
        return params;
    }
}
