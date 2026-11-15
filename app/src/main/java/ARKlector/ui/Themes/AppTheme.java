package ARKlector.ui.Themes;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Central source of colors, spacing and typography, expressed as Java constants and
 * static styling helpers. Per docs/foundational/design.md: no {@code res/values/styles.xml},
 * no XML layouts — a screen built with the classic {@code View} toolkit reaches for this
 * class instead of a style resource.
 */
public final class AppTheme {

    private AppTheme() {
    }

    // Colors
    public static final int COLOR_BACKGROUND = Color.parseColor("#121212");
    public static final int COLOR_SURFACE = Color.parseColor("#1E1E1E");
    public static final int COLOR_PRIMARY = Color.parseColor("#7C4DFF");
    public static final int COLOR_ON_PRIMARY = Color.parseColor("#FFFFFF");
    public static final int COLOR_TEXT_PRIMARY = Color.parseColor("#F5F5F5");
    public static final int COLOR_TEXT_SECONDARY = Color.parseColor("#B0B0B0");
    public static final int COLOR_DIVIDER = Color.parseColor("#2C2C2C");

    // Spacing, in dp — convert with dpToPx before handing to a LayoutParams/padding call.
    public static final int SPACING_SMALL_DP = 8;
    public static final int SPACING_MEDIUM_DP = 16;
    public static final int SPACING_LARGE_DP = 24;

    // Typography, in sp.
    public static final float TEXT_SIZE_TITLE_SP = 22f;
    public static final float TEXT_SIZE_BODY_SP = 16f;
    public static final float TEXT_SIZE_CAPTION_SP = 13f;

    public static int dpToPx(View view, int dp) {
        float density = view.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static void styleScreenBackground(View view) {
        view.setBackgroundColor(COLOR_BACKGROUND);
    }

    public static void styleTitle(TextView textView) {
        textView.setTextColor(COLOR_TEXT_PRIMARY);
        textView.setTextSize(TEXT_SIZE_TITLE_SP);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
    }

    public static void styleBody(TextView textView) {
        textView.setTextColor(COLOR_TEXT_PRIMARY);
        textView.setTextSize(TEXT_SIZE_BODY_SP);
    }

    public static void styleCaption(TextView textView) {
        textView.setTextColor(COLOR_TEXT_SECONDARY);
        textView.setTextSize(TEXT_SIZE_CAPTION_SP);
    }

    public static void stylePrimaryButton(Button button) {
        button.setTextColor(COLOR_ON_PRIMARY);
        button.setBackgroundColor(COLOR_PRIMARY);
    }

    public static void styleSecondaryButton(Button button) {
        button.setTextColor(COLOR_PRIMARY);
        button.setBackgroundColor(COLOR_SURFACE);
    }
}
