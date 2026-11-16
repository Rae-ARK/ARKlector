package ARKlector.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.arklector.app.R;

import ARKlector.service.Preferences;
import ARKlector.ui.Themes.AppTheme;

/**
 * The settings <strong>index</strong> page - a Material 3 top app bar over a short list
 * of settings. Each category (so far, just text-to-speech) lives on its own screen
 * reached by tapping its row; this screen only renders the auto-read-on-copy switch
 * directly, since that's a single one-line reading preference with nowhere else to go.
 */
public final class SettingsScreen extends LinearLayout {

    /** Forwarded intent - {@code MainActivity} wires these to navigation. */
    public interface Listener {
        void onBack();

        void onOpenTtsSettings();
    }

    public SettingsScreen(Context context, Preferences preferences, Listener listener) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        AppTheme.styleScreenBackground(this);

        addView(AppTheme.topBar(context,
                AppTheme.iconButton(context, R.drawable.ic_arrow_back, "Back", v -> listener.onBack()),
                "Settings"));

        TextView section = new TextView(context);
        section.setText("Reading");
        AppTheme.styleSectionLabel(section);
        int side = AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP);
        section.setPadding(side, side, side, AppTheme.dpToPx(this, AppTheme.SPACING_SMALL_DP));
        addView(section);

        // One-line M3 list item with a trailing switch; the whole row toggles it.
        LinearLayout autoReadRow = row(context);
        TextView autoReadLabel = new TextView(context);
        autoReadLabel.setText("Read clipboard automatically on copy");
        AppTheme.styleBody(autoReadLabel);
        autoReadRow.addView(autoReadLabel, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        MaterialSwitch autoReadSwitch = new MaterialSwitch(context);
        autoReadSwitch.setChecked(preferences.autoReadOnCopy());
        autoReadSwitch.setOnCheckedChangeListener((button, checked) -> preferences.setAutoReadOnCopy(checked));
        autoReadRow.addView(autoReadSwitch);
        AppTheme.styleRowRipple(autoReadRow);
        autoReadRow.setOnClickListener(v -> autoReadSwitch.toggle());
        addView(autoReadRow);

        addView(AppTheme.divider(context));

        LinearLayout ttsRow = row(context);
        TextView ttsLabel = new TextView(context);
        ttsLabel.setText("Text-to-speech");
        AppTheme.styleBody(ttsLabel);
        ttsRow.addView(ttsLabel, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ttsRow.addView(chevron(context));
        AppTheme.styleRowRipple(ttsRow);
        ttsRow.setOnClickListener(v -> listener.onOpenTtsSettings());
        addView(ttsRow);

        addView(AppTheme.divider(context));
    }

    private LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int side = AppTheme.dpToPx(this, AppTheme.SPACING_MEDIUM_DP);
        row.setPadding(side, 0, side, 0);
        row.setMinimumHeight(AppTheme.dpToPx(this, AppTheme.LIST_ITEM_HEIGHT_DP));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    static View chevron(Context context) {
        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setImageTintList(android.content.res.ColorStateList.valueOf(AppTheme.onSurfaceVariant(chevron)));
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return chevron;
    }
}
