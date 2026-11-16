package ARKlector.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import ARKlector.service.Preferences;
import ARKlector.ui.Themes.AppTheme;

/**
 * The settings <strong>index</strong> page — a short list of settings categories, not a
 * settings form itself. Each category (so far, just text-to-speech) lives on its own
 * screen reached by tapping its row; this screen only renders the auto-read-on-copy
 * switch directly, since that's a single one-line reading preference with nowhere else
 * to put it. Speech rate, pitch and voice selection moved out to {@link TtsSettingsScreen}
 * when that screen was added — see its doc comment for where that logic came from.
 */
public final class SettingsScreen extends LinearLayout {

    /** Forwarded intent — {@code MainActivity} wires these to navigation. */
    public interface Listener {
        void onBack();

        void onOpenTtsSettings();
    }

    public SettingsScreen(Context context, Preferences preferences, Listener listener) {
        super(context);
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

        addView(sectionLabel(context, "Reading"), topMarginParams(AppTheme.SPACING_LARGE_DP));

        LinearLayout autoReadRow = new LinearLayout(context);
        autoReadRow.setOrientation(LinearLayout.HORIZONTAL);
        autoReadRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView autoReadLabel = new TextView(context);
        autoReadLabel.setText("Read clipboard automatically on copy");
        AppTheme.styleBody(autoReadLabel);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        autoReadRow.addView(autoReadLabel, labelParams);

        Switch autoReadSwitch = new Switch(context);
        autoReadSwitch.setChecked(preferences.autoReadOnCopy());
        autoReadSwitch.setOnCheckedChangeListener((CompoundButton button, boolean checked) ->
                preferences.setAutoReadOnCopy(checked));
        autoReadRow.addView(autoReadSwitch);

        addView(autoReadRow, topMarginParams(AppTheme.SPACING_SMALL_DP));
        addView(divider(context), dividerParams(AppTheme.SPACING_MEDIUM_DP));

        addView(navRow(context, "Text-to-speech", v -> listener.onOpenTtsSettings()),
                topMarginParams(AppTheme.SPACING_MEDIUM_DP));
        addView(divider(context), dividerParams(AppTheme.SPACING_MEDIUM_DP));
    }

    private LinearLayout navRow(Context context, String label, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(onClick);

        TextView label1 = new TextView(context);
        label1.setText(label);
        AppTheme.styleBody(label1);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(label1, labelParams);

        TextView chevron = new TextView(context);
        chevron.setText(">");
        AppTheme.styleCaption(chevron);
        row.addView(chevron);

        return row;
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
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AppTheme.dpToPx(this, spacingDp);
        return params;
    }

    /** A hairline divider needs an explicit height — WRAP_CONTENT alone renders as 0px. */
    private ViewGroup.LayoutParams dividerParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AppTheme.dpToPx(this, 1));
        params.topMargin = AppTheme.dpToPx(this, topMarginDp);
        return params;
    }

    private ViewGroup.LayoutParams leftMarginParams(int spacingDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = AppTheme.dpToPx(this, spacingDp);
        return params;
    }
}
