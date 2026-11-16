package ARKlector.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import ARKlector.ui.Themes.AppTheme;
import ARKlector.viewmodel.PlaybackState;

/**
 * The reader's home screen: a top bar (settings icon, app name, clipboard icon) over a
 * content area that fills with the most recently retrieved clipboard text. Built
 * programmatically per docs/foundational/design.md — no XML layout, and no drawable
 * icon resources, so the top bar's icons are Unicode glyphs via
 * {@link AppTheme#styleIconButton} (same approach the voice picker uses for its
 * favorite star).
 *
 * <p>Renders state pushed in from {@code MainActivity} via {@link #setPlaybackState}
 * and {@link #setContent}; this screen never touches the {@code ViewModel} itself.
 * Intent flows back out through {@link Listener}. See docs/foundational/architecture.md's
 * UI layer.
 */
public final class HomeScreen extends LinearLayout {

    private static final String PLACEHOLDER_TEXT =
            "Tap the clipboard icon to load the most recently copied text.";

    /** Forwarded intent — {@code MainActivity} wires these to the ViewModel/navigation. */
    public interface Listener {
        /** Clipboard icon tapped: retrieve the latest clipboard entry and read it. */
        void onReadClipboardNow();

        void onOpenSettings();
    }

    private final TextView statusText;
    private final TextView contentText;

    public HomeScreen(Context context, Listener listener) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        AppTheme.styleScreenBackground(this);

        addView(topBar(context, listener));
        addView(divider(context), dividerParams(0));

        statusText = new TextView(context);
        statusText.setText("Idle");
        AppTheme.styleCaption(statusText);
        int sidePadding = AppTheme.dpToPx(this, AppTheme.SPACING_LARGE_DP);
        statusText.setPadding(sidePadding, AppTheme.dpToPx(this, AppTheme.SPACING_SMALL_DP), sidePadding, 0);
        addView(statusText);

        ScrollView contentScroll = new ScrollView(context);
        contentText = new TextView(context);
        contentText.setPadding(
                sidePadding, AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP), sidePadding, sidePadding);
        contentScroll.addView(contentText, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContent(null);

        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        addView(contentScroll, contentParams);
    }

    private LinearLayout topBar(Context context, Listener listener) {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int sidePadding = AppTheme.dpToPx(this, AppTheme.SPACING_SMALL_DP);
        bar.setPadding(sidePadding, sidePadding, sidePadding, sidePadding);

        TextView settingsIcon = new TextView(context);
        settingsIcon.setText("⚙"); // gear
        AppTheme.styleIconButton(settingsIcon);
        settingsIcon.setClickable(true);
        settingsIcon.setFocusable(true);
        settingsIcon.setContentDescription("Settings");
        settingsIcon.setOnClickListener(v -> listener.onOpenSettings());
        bar.addView(settingsIcon);

        TextView title = new TextView(context);
        title.setText("ARKlector");
        AppTheme.styleTitle(title);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(title, titleParams);

        TextView clipboardIcon = new TextView(context);
        clipboardIcon.setText("📋"); // clipboard
        AppTheme.styleIconButton(clipboardIcon);
        clipboardIcon.setClickable(true);
        clipboardIcon.setFocusable(true);
        clipboardIcon.setContentDescription("Read clipboard now");
        clipboardIcon.setOnClickListener(v -> listener.onReadClipboardNow());
        bar.addView(clipboardIcon);

        return bar;
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

    /**
     * Fills the content area with the latest retrieved clipboard text (pushed in from
     * {@code MainActivity}'s {@code ReaderViewModel.lastClipboardText()} observer), or
     * restores the placeholder for {@code null}/blank text — nothing retrieved yet, or
     * an empty clipboard.
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

    private View divider(Context context) {
        View divider = new View(context);
        AppTheme.styleDivider(divider);
        return divider;
    }

    private ViewGroup.LayoutParams dividerParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AppTheme.dpToPx(this, 1));
        params.topMargin = AppTheme.dpToPx(this, topMarginDp);
        return params;
    }
}
