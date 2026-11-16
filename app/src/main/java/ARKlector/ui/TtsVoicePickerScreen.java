package ARKlector.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.lifecycle.Observer;

import org.arklector.app.R;

import ARKlector.service.Preferences;
import ARKlector.service.TtsEngine;
import ARKlector.service.TtsVoiceModel;
import ARKlector.service.TtsVoiceModel.TtsVoiceInfo;
import ARKlector.ui.Themes.AppTheme;

/**
 * Full voice picker: every engine voice, grouped by language (device language first),
 * each row showing the name plus region/online/quality as
 * {@link TtsVoiceModel#descriptionOf} states it, a star to favorite a voice within its
 * language group, and a trailing check on the selected voice. Reached from
 * {@link TtsSettingsScreen}; styled as Material 3 list items.
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

        addView(AppTheme.topBar(context,
                AppTheme.iconButton(context, R.drawable.ic_arrow_back, "Back", v -> listener.onBack()),
                "Choose a voice"));

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, AppTheme.dpToPx(this, AppTheme.SPACING_LARGE_DP));

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
            int pad = AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP);
            loading.setPadding(pad, pad, pad, pad);
            content.addView(loading);
            return;
        }

        String deviceLanguage = Locale.getDefault().getLanguage();
        List<String> languages = TtsVoiceModel.languagesOf(engineVoices, deviceLanguage);
        for (String language : languages) {
            TextView languageHeader = new TextView(context);
            languageHeader.setText(displayLanguage(language));
            AppTheme.styleSectionLabel(languageHeader);
            int pad = AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP);
            languageHeader.setPadding(pad, pad, pad, AppTheme.dpToPx(this, AppTheme.SPACING_SMALL_DP));
            content.addView(languageHeader);

            List<TtsVoiceInfo> rows = TtsVoiceModel.withFavoritesFirst(
                    TtsVoiceModel.rowsForLanguage(engineVoices, language), favorites);
            for (TtsVoiceInfo voice : rows) {
                content.addView(voiceRow(context, voice));
            }
        }
    }

    private View voiceRow(Context context, TtsVoiceInfo voice) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(AppTheme.dpToPx(this, AppTheme.LIST_ITEM_HEIGHT_DP));
        int pad = AppTheme.dpToPx(this, AppTheme.SPACING_SMALL_DP);
        row.setPadding(pad, pad / 2, AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP), pad / 2);
        AppTheme.styleRowRipple(row);
        row.setOnClickListener(v -> {
            Preferences.Voice latest = preferences.observeVoice().getValue();
            preferences.setVoice(withVoiceId(latest, voice.name()));
            listener.onBack();
        });

        boolean favorite = favorites.contains(voice.name());
        ImageButton star = AppTheme.iconButton(context,
                favorite ? R.drawable.ic_star : R.drawable.ic_star_border,
                favorite ? "Remove from favorites" : "Add to favorites",
                v -> preferences.setVoiceFavorite(voice.name(), !favorite));
        if (favorite) {
            star.setImageTintList(ColorStateList.valueOf(AppTheme.primary(star)));
        }
        row.addView(star);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);

        TextView name = new TextView(context);
        name.setText(voice.name());
        AppTheme.styleBody(name);
        textColumn.addView(name);

        TextView description = new TextView(context);
        description.setText(TtsVoiceModel.descriptionOf(voice));
        AppTheme.styleCaption(description);
        textColumn.addView(description);

        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        columnParams.leftMargin = pad;
        row.addView(textColumn, columnParams);

        if (voice.name().equals(currentVoiceId)) {
            ImageView check = new ImageView(context);
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(ColorStateList.valueOf(AppTheme.primary(check)));
            check.setContentDescription("Selected");
            row.addView(check);
        }

        return row;
    }

    private static Preferences.Voice withVoiceId(Preferences.Voice voice, String voiceId) {
        return new Preferences.Voice(voiceId, voice.speechRate(), voice.pitch());
    }

    private static String displayLanguage(String languageTag) {
        String display = Locale.forLanguageTag(languageTag).getDisplayLanguage();
        return display.isBlank() ? languageTag : display;
    }
}
