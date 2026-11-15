package au.ludger.germantv;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building a TV-friendly, remote-control-friendly UI in code. */
final class Ui {
    static final int BG = 0xFF0F1318;
    static final int PANEL = 0xFF1A2129;
    static final int TILE = 0xFF1E262F;
    static final int FOCUS = 0xFF2C6BD6;
    static final int TEXT = 0xFFF2F4F6;
    static final int MUTED = 0xFFA3ADB8;
    static final int OK = 0xFF35C27A;
    static final int WARN = 0xFFF0B429;
    static final int ERROR = 0xFFEF5350;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Background that shows a white outline and a blue fill when the item has remote-control focus. */
    static Drawable focusBackground(Context c, int normal) {
        StateListDrawable s = new StateListDrawable();
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(FOCUS);
        focused.setCornerRadius(dp(c, 12));
        focused.setStroke(dp(c, 3), 0xFFFFFFFF);
        GradientDrawable plain = new GradientDrawable();
        plain.setColor(normal);
        plain.setCornerRadius(dp(c, 12));
        s.addState(new int[]{android.R.attr.state_focused}, focused);
        s.addState(new int[]{android.R.attr.state_pressed}, focused);
        s.addState(new int[]{}, plain);
        return s;
    }

    static Drawable panel(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(PANEL);
        g.setCornerRadius(dp(c, 12));
        return g;
    }

    static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    static TextView heading(Context c, String s, float sp) {
        TextView t = text(c, s, sp, TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static Button button(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(18);
        b.setTextColor(TEXT);
        b.setBackground(focusBackground(c, TILE));
        b.setPadding(dp(c, 24), dp(c, 12), dp(c, 24), dp(c, 12));
        b.setFocusable(true);
        b.setGravity(Gravity.CENTER);
        return b;
    }

    static LinearLayout.LayoutParams wrap(Context c, int marginEndDp, int marginBottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(c, marginEndDp), dp(c, marginBottomDp));
        return lp;
    }

    static LinearLayout.LayoutParams matchWidth(Context c, int marginBottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, marginBottomDp));
        return lp;
    }

    /** Slightly enlarges a view while it has focus, like the Android TV home screen. */
    static void zoomOnFocus(View v) {
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                float s = hasFocus ? 1.06f : 1f;
                view.animate().scaleX(s).scaleY(s).setDuration(120).start();
            }
        });
    }
}
