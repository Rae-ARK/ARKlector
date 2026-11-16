package ARKlector.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.lifecycle.Observer;

import ARKlector.service.Preferences;
import ARKlector.service.TtsEngine;
import ARKlector.service.TtsVoiceModel;
import ARKlector.service.TtsVoiceModel.TtsVoiceInfo;
import ARKlector.ui.Themes.AppTheme;

/**
 * Full voice picker: every installed engine voice, grouped by language (device language
 * first), each row showing the name plus region/online/quality the way
 * {@link TtsVoiceModel#descriptionOf} states it, with a star to favorite a voice within
 * its language group. Reached from {@link TtsSettingsScreen}.
 *
 * <p>Ported from ARKarium's {@code ui/TtsVoicePickerScreen.kt} (Compose there, plain
 * {@code View}s here per docs/foundational/design.md); the grouping/sorting/favorites
 * logic itself lives in {@link TtsVoiceModel}, ported alongside it so it stays plain-JVM
 * testable rather than living inline in this screen. The "not-installed voices aren't
 * specially marked yet" gap {@code TtsVoiceModel.installedOnly}'s doc comment flags in
 * ARKarium is carried over here too, for the same reason — nothing ARKlector does today
 * prevents picking one; it just fails with a clear error once spoken, same as before this
 * screen existed.
 *
 * <p>Tapping a row applies that voice immediately via {@link Preferences#setVoice} and
 * returns to {@code TtsSettingsScreen}; tapping the star only toggles the favorite and
 * stays on this screen.
 */
public final class TtsVoicePickerScreen extends LinearLayout {

    public interface Listener {
        void onBack();
    }

    private final Preferences preferences;
    private final Listener listener;
    private final LinearLayout content;

    private final Observer<List<TtsVoiceInfo>> engineVoicesObserver = this::onEngineVoicesChanged;
    private final Observer<Preferences.Voice> voiceObserver = this::onVoiceChanged;
    private final Observer<Set<String>> favoritesObserver = this::onFavoritesChanged;

    private final TtsEngine voiceEngine;
    private List<TtsVoiceInfo> engineVoices = List.of();
    private Set<String> favorites = Set.of();
    private String currentVoiceId;

    public TtsVoicePickerScreen(Context context, Preferences preferences, TtsEngine voiceEngine, Listener listener) {
        super(context);
        this.preferences = preferences;
        this.voiceEngine = voiceEngine;
        this.listener = listener;

        setOrientation(LinearLayout.VERTICAL);
        AppTheme.styleScreenBackground(this);

        int padding = AppTheme.dpToPx(this, AppTheme.SPACING_LARGE_DP);
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(padding, padding, padding, AppTheme.dpToPx(this, AppTheme.SPACING_SMALL_DP));

        Button backButton = new Button(context);
        backButton.setText("Back");
        AppTheme.styleSecondaryButton(backButton);
        backButton.setOnClickListener(v -> listener.onBack());
        header.addView(backButton);

        TextView title = new TextView(context);
        title.setText("Choose a voice");
        AppTheme.styleTitle(title);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.leftMargin = AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP);
        header.addView(title, titleParams);
        addView(header);

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, 0, padding, padding);

        ScrollView scroll = new ScrollView(context);
        scroll.addView(content);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        addView(scroll, scrollParams);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // All three LiveDatas already hold a value, so each observer fires synchronously
        // and the first render() reflects real state rather than a placeholder frame.
        voiceEngine.voices().observeForever(engineVoicesObserver);
        preferences.observeVoice().observeForever(voiceObserver);
        preferences.observeFavoriteVoiceNames().observeForever(favoritesObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        voiceEngine.voices().removeObserver(engineVoicesObserver);
        preferences.observeVoice().removeObserver(voiceObserver);
        preferences.observeFavoriteVoiceNames().removeObserver(favoritesObserver);
        super.onDetachedFromWindow();
    }

    private void onEngineVoicesChanged(List<TtsVoiceInfo> voices) {
        engineVoices = voices;
        render();
    }

    private void onVoiceChanged(Preferences.Voice voice) {
        currentVoiceId = voice.voiceId();
        render();
    }

    private void onFavoritesChanged(Set<String> updated) {
        favorites = updated;
        render();
    }

    private void render() {
        Context context = getContext();
        content.removeAllViews();

        if (engineVoices.isEmpty()) {
            TextView loading = new TextView(context);
            loading.setText("Loading voices…");
            AppTheme.styleCaption(loading);
            content.addView(loading, topMarginParams(AppTheme.SPACING_MEDIUM_DP));
            return;
        }

        String deviceLanguage = Locale.getDefault().getLanguage();
        List<String> languages = TtsVoiceModel.languagesOf(engineVoices, deviceLanguage);
        for (String language : languages) {
            TextView languageHeader = new TextView(context);
            languageHeader.setText(displayLanguage(language));
            AppTheme.styleTitle(languageHeader);
            content.addView(languageHeader, topMarginParams(AppTheme.SPACING_LARGE_DP));

            List<TtsVoiceInfo> rows = TtsVoiceModel.withFavoritesFirst(
                    TtsVoiceModel.rowsForLanguage(engineVoices, language), favorites);
            for (TtsVoiceInfo voice : rows) {
                content.addView(voiceRow(context, voice), topMarginParams(AppTheme.SPACING_SMALL_DP));
            }
        }
    }

    private View voiceRow(Context context, TtsVoiceInfo voice) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            Preferences.Voice latest = preferences.observeVoice().getValue();
            preferences.setVoice(withVoiceId(latest, voice.name()));
            listener.onBack();
        });

        boolean favorite = favorites.contains(voice.name());
        TextView star = new TextView(context);
        star.setText(favorite ? "★" : "☆");
        AppTheme.styleBody(star);
        if (favorite) {
            star.setTextColor(AppTheme.COLOR_PRIMARY);
        }
        star.setClickable(true);
        star.setFocusable(true);
        star.setOnClickListener(v -> preferences.setVoiceFavorite(voice.name(), !favorite));
        row.addView(star);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);

        TextView name = new TextView(context);
        boolean selected = voice.name().equals(currentVoiceId);
        name.setText(selected ? voice.name() + "  ✓" : voice.name());
        AppTheme.styleBody(name);
        textColumn.addView(name);

        TextView description = new TextView(context);
        description.setText(TtsVoiceModel.descriptionOf(voice));
        AppTheme.styleCaption(description);
        textColumn.addView(description);

        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        columnParams.leftMargin = AppTheme.dpToPx(this, AppTheme.SPACING_SMALL_DP);
        row.addView(textColumn, columnParams);

        return row;
    }

    private static Preferences.Voice withVoiceId(Preferences.Voice voice, String voiceId) {
        return new Preferences.Voice(voiceId, voice.speechRate(), voice.pitch());
    }

    private static String displayLanguage(String languageTag) {
        String display = new Locale(languageTag).getDisplayLanguage();
        return display.isBlank() ? languageTag : display;
    }

    private ViewGroup.LayoutParams topMarginParams(int spacingDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AppTheme.dpToPx(this, spacingDp);
        return params;
    }
}
