package com.thuc.pushlog;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(14, 15, 17);
    static final int CARD = Color.rgb(28, 29, 32);
    static final int CARD_ALT = Color.rgb(35, 36, 39);
    static final int GOLD = Color.rgb(245, 184, 46);
    static final int GOLD_DARK = Color.rgb(185, 126, 14);
    static final int TEXT = Color.rgb(247, 247, 242);
    static final int MUTED = Color.rgb(166, 168, 174);
    static final int LINE = Color.rgb(61, 62, 67);
    static final int GOLD_WASH = Color.rgb(55, 46, 25);

    private Ui() {}

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context context, String value, float sizeSp, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setFontFeatureSettings("kern");
        return view;
    }

    static TextView title(Context context, String value, float sizeSp) {
        TextView view = text(context, value, sizeSp, TEXT);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return view;
    }

    static GradientDrawable rounded(int color, float radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static GradientDrawable outlined(int color, int strokeColor, float radiusDp, Context context) {
        GradientDrawable drawable = rounded(color, radiusDp, context);
        drawable.setStroke(dp(context, 1), strokeColor);
        return drawable;
    }

    static RippleDrawable ripple(Context context, int color, float radiusDp, int rippleColor) {
        return new RippleDrawable(
                ColorStateList.valueOf(rippleColor),
                rounded(color, radiusDp, context),
                rounded(Color.WHITE, radiusDp, context));
    }

    static RippleDrawable outlinedRipple(Context context, int fill, int stroke, float radiusDp) {
        return new RippleDrawable(
                ColorStateList.valueOf(Color.argb(50, 255, 255, 255)),
                outlined(fill, stroke, radiusDp, context),
                rounded(Color.WHITE, radiusDp, context));
    }

    static TextView action(Context context, String label, boolean primary) {
        TextView button = title(context, label, 16);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(context, 56));
        button.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
        button.setClickable(true);
        button.setFocusable(true);
        if (primary) {
            button.setTextColor(Color.rgb(20, 21, 23));
            button.setBackground(ripple(context, GOLD, 18, Color.argb(70, 255, 255, 255)));
        } else {
            button.setTextColor(TEXT);
            button.setBackground(outlinedRipple(context, CARD, LINE, 18));
        }
        return button;
    }

    static TextView segment(Context context, String label, boolean selected) {
        TextView button = title(context, label, 14);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(context, 48));
        button.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        button.setClickable(true);
        button.setFocusable(true);
        button.setTextColor(selected ? GOLD : MUTED);
        button.setBackground(selected
                ? outlinedRipple(context, GOLD_WASH, GOLD_DARK, 14)
                : ripple(context, CARD, 14, Color.argb(45, 255, 255, 255)));
        return button;
    }

    static ImageButton toolbarButton(Context context, int drawable, String description) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(drawable);
        button.setImageTintList(ColorStateList.valueOf(TEXT));
        button.setContentDescription(description);
        button.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        button.setBackground(ripple(context, CARD, 24, Color.argb(55, 255, 255, 255)));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    static TextView iconButton(Context context, String glyph, float sizeSp) {
        TextView button = title(context, glyph, sizeSp);
        button.setGravity(Gravity.CENTER);
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setIncludeFontPadding(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(0, 0, 0, 0);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(outlinedRipple(context, CARD, LINE, 18));
        return button;
    }

    static View spacer(Context context, int heightDp) {
        View view = new View(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(context, heightDp)));
        return view;
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
