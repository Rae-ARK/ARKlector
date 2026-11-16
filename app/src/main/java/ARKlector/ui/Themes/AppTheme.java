package ARKlector.ui.Themes;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.divider.MaterialDivider;

/**
 * Material 3 styling helpers. Colors are never hard-coded: they are resolved from the
 * Activity's theme ({@code Theme.Material3.DayNight.NoActionBar}, plus dynamic color on
 * Android 12+ via {@link com.google.android.material.color.DynamicColors}), so light/dark
 * and the user's wallpaper palette are followed automatically. Type uses the M3 type
 * scale ({@code TextAppearance.Material3.*}). Everything is applied from Java, so there
 * is still no layout XML and no {@code styles.xml} of our own - see
 * docs/foundational/design.md.
 */
public final class AppTheme {

    private AppTheme() {
    }

    // Spacing, in dp (M3 uses a 4dp grid; these are 8/16/24).
    public static final int SPACING_SMALL_DP = 8;
    public static final int SPACING_MEDIUM_DP = 16;
    public static final int SPACING_LARGE_DP = 24;

    /** M3 minimum touch target. */
    public static final int MIN_TOUCH_TARGET_DP = 48;
    /** M3 list item (one-line) height. */
    public static final int LIST_ITEM_HEIGHT_DP = 56;
    /** M3 small top app bar height. */
    public static final int TOP_BAR_HEIGHT_DP = 64;

    public static int dpToPx(View view, int dp) {
        return dpToPx(view.getContext(), dp);
    }

    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ---- Color roles ------------------------------------------------------------

    public static int surface(View view) {
        return color(view, com.google.android.material.R.attr.colorSurface);
    }

    public static int onSurface(View view) {
        return color(view, com.google.android.material.R.attr.colorOnSurface);
    }

    public static int onSurfaceVariant(View view) {
        return color(view, com.google.android.material.R.attr.colorOnSurfaceVariant);
    }

    public static int primary(View view) {
        return color(view, com.google.android.material.R.attr.colorPrimary);
    }

    public static int secondaryContainer(View view) {
        return color(view, com.google.android.material.R.attr.colorSecondaryContainer);
    }

    public static int onSecondaryContainer(View view) {
        return color(view, com.google.android.material.R.attr.colorOnSecondaryContainer);
    }

    private static int color(View view, @AttrRes int attr) {
        return MaterialColors.getColor(view, attr);
    }

    // ---- Styling ----------------------------------------------------------------

    public static void styleScreenBackground(View view) {
        view.setBackgroundColor(surface(view));
    }

    /** Title Large - top app bar and screen titles. */
    public static void styleTitle(TextView textView) {
        apply(textView, com.google.android.material.R.style.TextAppearance_Material3_TitleLarge,
                onSurface(textView));
    }

    /** Body Large - primary reading/list text. */
    public static void styleBody(TextView textView) {
        apply(textView, com.google.android.material.R.style.TextAppearance_Material3_BodyLarge,
                onSurface(textView));
    }

    /** Body Medium in on-surface-variant - supporting text. */
    public static void styleCaption(TextView textView) {
        apply(textView, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
                onSurfaceVariant(textView));
    }

    /** Title Small in primary - section headers inside a screen. */
    public static void styleSectionLabel(TextView textView) {
        apply(textView, com.google.android.material.R.style.TextAppearance_Material3_TitleSmall,
                primary(textView));
    }

    /** Label Medium - overline-style captions on cards. */
    public static void styleLabel(TextView textView, int color) {
        apply(textView, com.google.android.material.R.style.TextAppearance_Material3_LabelMedium, color);
    }

    private static void apply(TextView textView, @StyleRes int appearance, int color) {
        textView.setTextAppearance(appearance);
        textView.setTextColor(color);
    }

    /** A 48dp borderless-ripple icon button, tinted on-surface-variant. */
    public static ImageButton iconButton(
            Context context, @DrawableRes int icon, String contentDescription, View.OnClickListener onClick) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(icon);
        button.setContentDescription(contentDescription);
        button.setImageTintList(ColorStateList.valueOf(onSurfaceVariant(button)));
        button.setBackgroundResource(resolveResource(context, android.R.attr.selectableItemBackgroundBorderless));
        int size = dpToPx(context, MIN_TOUCH_TARGET_DP);
        button.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        button.setOnClickListener(onClick);
        return button;
    }

    /** Ripple background for a tappable list row. */
    public static void styleRowRipple(View row) {
        row.setBackgroundResource(resolveResource(row.getContext(), android.R.attr.selectableItemBackground));
        row.setClickable(true);
        row.setFocusable(true);
    }

    /** A full-width M3 divider (outline-variant, 1dp). */
    public static MaterialDivider divider(Context context) {
        return new MaterialDivider(context);
    }

    /**
     * M3 small top app bar: 64dp, {@code leading} (nav icon / logo) at the start, title
     * filling the middle, {@code trailing} action icons at the end.
     */
    public static LinearLayout topBar(Context context, View leading, String title, View... trailing) {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setMinimumHeight(dpToPx(context, TOP_BAR_HEIGHT_DP));
        int edge = dpToPx(context, 4);
        bar.setPadding(edge, 0, edge, 0);
        styleScreenBackground(bar);

        bar.addView(leading);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        styleTitle(titleView);
        titleView.setSingleLine(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dpToPx(context, SPACING_SMALL_DP);
        bar.addView(titleView, titleParams);

        for (View action : trailing) {
            bar.addView(action);
        }
        return bar;
    }

    private static int resolveResource(Context context, @AttrRes int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.resourceId;
    }
}
