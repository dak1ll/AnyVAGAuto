package com.mag.anyauto;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Help: set-up, what happens while in the car, the limits. */
public final class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle(R.string.menu_help);
        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(8), dp(16), dp(16));

        heading(col, R.string.help_setup_heading);
        para(col, R.string.help_setup_1);
        para(col, R.string.help_setup_2);
        para(col, R.string.help_setup_3);
        para(col, R.string.help_setup_4);
        para(col, R.string.help_setup_5);

        heading(col, R.string.help_running_heading);
        para(col, R.string.help_running_1);
        para(col, R.string.help_running_2);
        para(col, R.string.help_running_3);
        para(col, R.string.help_running_4);
        para(col, R.string.help_running_5);
        para(col, R.string.help_running_6);
        para(col, R.string.help_running_7);

        heading(col, R.string.help_knob_heading);
        para(col, R.string.help_knob_1);
        para(col, R.string.help_knob_2);
        para(col, R.string.help_knob_3);
        para(col, R.string.help_knob_4);

        heading(col, R.string.help_updates_heading);
        para(col, R.string.help_updates_1);

        heading(col, R.string.help_limits_heading);
        para(col, R.string.help_limits_1);
        para(col, R.string.help_limits_2);
        para(col, R.string.help_limits_3);
        para(col, R.string.help_limits_4);

        Button ua = Ui.textButton(this, MainActivity.unknownAutoLabel(this));
        ua.setOnClickListener(v -> MainActivity.openUnknownAuto(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        col.addView(ua, lp);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = Build.VERSION.SDK_INT >= 30
                    ? insets.getInsets(WindowInsets.Type.systemBars()).top
                    : insets.getSystemWindowInsetTop();
            int bottom = Build.VERSION.SDK_INT >= 30
                    ? insets.getInsets(WindowInsets.Type.systemBars()).bottom
                    : insets.getSystemWindowInsetBottom();
            v.setPadding(0, top, 0, bottom);
            return insets;
        });
        setContentView(scroll);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void heading(LinearLayout col, int resId) {
        TextView t = Ui.text(this, getString(resId), 16, true, Ui.text(this));
        t.setPadding(0, dp(20), 0, dp(4));
        col.addView(t);
    }

    private void para(LinearLayout col, int resId) {
        TextView t = Ui.text(this, getString(resId), 14, false, Ui.text2(this));
        t.setPadding(0, dp(4), 0, dp(4));
        t.setLineSpacing(0, 1.15f);
        col.addView(t);
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }
}
