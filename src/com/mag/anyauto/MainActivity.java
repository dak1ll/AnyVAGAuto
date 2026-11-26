package com.mag.anyauto;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Home: status cards only when something needs doing (the car
 * has the display, Shizuku isn't ready, not installed for the car), then the apps in the car with
 * Try, and one pinned button at the bottom, Choose apps. Help and the install guidance are in
 * the overflow menu.
 */
public final class MainActivity extends Activity implements Shell.Listener {
    static final String UNKNOWNAUTO = "com.mag.unknownauto";
    static final String UNKNOWNAUTO_RELEASES = "https://github.com/dak1ll/VAGAuto/releases/latest";
    private static final int MENU_HELP = 1, MENU_INSTALL = 2, MENU_KEYBOARD = 3, MENU_FULL = 4, MENU_PHONE_ON = 5,
            MENU_AUTO_UPDATE = 6, MENU_CHECK_UPDATE = 7, MENU_NAV_SIDE = 8, MENU_POINTER = 9, MENU_SPLIT = 10;

    private LinearLayout cards;
    private TextView section, sectionCount;
    private View empty;
    private final AppAdapter adapter = new AppAdapter();
    private List<Apps.App> chosen = new ArrayList<>();
    private volatile String lastServiceError;
    private final Runnable carChanged = this::refresh;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Shell.init(this);
        setTitle(R.string.app_name);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        header.addView(cards);

        LinearLayout sec = new LinearLayout(this);
        sec.setOrientation(LinearLayout.HORIZONTAL);
        sec.setPadding(dp(16), dp(16), dp(16), dp(4));
        section = Ui.text(this, getString(R.string.section_in_car), 14, true, Ui.accent(this));
        sec.addView(section, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sectionCount = Ui.text(this, "", 14, false, Ui.text2(this));
        sec.addView(sectionCount);
        header.addView(sec);

        empty = emptyState();
        header.addView(empty);

        ListView list = new ListView(this);
        list.addHeaderView(header, null, false);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setClipToPadding(false);
        list.setOnItemClickListener((parent, view, pos, id) -> {
            Apps.App a = (Apps.App) parent.getItemAtPosition(pos);
            if (a != null) tryApp(a);
        });

        Button choose = Ui.filled(this, getString(R.string.action_choose_apps));
        choose.setOnClickListener(v -> startActivity(new Intent(this, PickerActivity.class)));

        FrameLayout root = new FrameLayout(this);
        root.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48), Gravity.BOTTOM);
        root.addView(choose, bp);
        // the button sits 12 dp above the navigation bar; the list can scroll its last row clear of it
        // edge-to-edge (Android 15+): the top inset covers the status bar and the action bar
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars());
                top = i.top;
                bottom = i.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            bp.setMargins(dp(16), 0, dp(16), bottom + dp(12));
            choose.setLayoutParams(bp);
            list.setPadding(0, top, 0, bottom + dp(12 + 48 + 12));
            return insets;
        });
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Shell.addListener(this);
        Mirror.addCarListener(carChanged);
        Shell.connect();
        new Thread(() -> {
            List<Apps.App> apps = Apps.chosen(this);
            runOnUiThread(() -> {
                chosen = apps;
                refresh();
            });
        }, "anyauto-apps").start();
        refreshServiceError();
        refresh();
        // only here, while AnyAuto is open; skipped when turned off or checked within the hour
        Updater.check(this, false, r -> refresh());
    }

    @Override
    protected void onPause() {
        Shell.removeListener(this);
        Mirror.removeCarListener(carChanged);
        super.onPause();
    }

    @Override
    public void onShellChanged() {
        refreshServiceError();
        refresh();
    }

    private void refreshServiceError() {
        IDisplayService s = Shell.service();
        if (s == null) {
            lastServiceError = null;
            return;
        }
        new Thread(() -> {
            String err = null;
            try {
                err = s.lastError();
            } catch (Throwable ignored) {
            }
            if (!TextUtils.equals(err, lastServiceError)) {
                lastServiceError = err;
                runOnUiThread(this::refresh);
            }
        }, "anyauto-health").start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_FULL, 0, R.string.menu_full_screen).setCheckable(true)
                .setChecked(Prefs.fullScreen(this));
        menu.add(0, MENU_PHONE_ON, 1, R.string.menu_keep_phone_on).setCheckable(true)
                .setChecked(Prefs.keepPhoneOn(this));
        menu.add(0, MENU_KEYBOARD, 2, R.string.menu_keyboard);
        menu.add(0, MENU_NAV_SIDE, 3, R.string.menu_nav_side);
        menu.add(0, MENU_POINTER, 3, R.string.menu_pointer_buttons).setCheckable(true)
                .setChecked(Prefs.pointerButtons(this));
        menu.add(0, MENU_SPLIT, 3, R.string.menu_split_mode).setCheckable(true)
                .setChecked(Prefs.splitMode(this));
        menu.add(0, MENU_AUTO_UPDATE, 4, R.string.menu_auto_update).setCheckable(true)
                .setChecked(Prefs.autoUpdate(this));
        menu.add(0, MENU_CHECK_UPDATE, 5, R.string.menu_check_updates);
        menu.add(0, MENU_HELP, 6, R.string.menu_help);
        menu.add(0, MENU_INSTALL, 7, R.string.menu_install_for_car);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_FULL) {
            boolean on = !item.isChecked();
            item.setChecked(on);
            Prefs.setFullScreen(this, on);
            Mirror.settingsChanged();
            return true;
        }
        if (item.getItemId() == MENU_PHONE_ON) {
            boolean on = !item.isChecked();
            item.setChecked(on);
            Prefs.setKeepPhoneOn(this, on);
            Mirror.settingsChanged();
            return true;
        }
        if (item.getItemId() == MENU_POINTER) {
            boolean on = !item.isChecked();
            item.setChecked(on);
            Prefs.setPointerButtons(this, on);
            Mirror.settingsChanged();
            if (on) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_pointer_title)
                        .setMessage(R.string.dialog_pointer_body)
                        .setPositiveButton(R.string.action_ok, null)
                        .show();
            }
            return true;
        }
        if (item.getItemId() == MENU_SPLIT) {
            boolean on = !item.isChecked();
            item.setChecked(on);
            Prefs.setSplitMode(this, on);
            Mirror.settingsChanged();
            return true;
        }
        if (item.getItemId() == MENU_AUTO_UPDATE) {
            boolean on = !item.isChecked();
            item.setChecked(on);
            Prefs.setAutoUpdate(this, on);
            return true;
        }
        if (item.getItemId() == MENU_CHECK_UPDATE) {
            android.widget.Toast.makeText(this, R.string.toast_checking_updates, android.widget.Toast.LENGTH_SHORT).show();
            Updater.check(this, true, r -> {
                String m = r.error != null ? getString(R.string.toast_update_error, r.error)
                        : r.version != null ? getString(R.string.toast_update_ready, r.version)
                        : getString(R.string.toast_update_up_to_date);
                android.widget.Toast.makeText(this, m, android.widget.Toast.LENGTH_LONG).show();
                refresh();
            });
            return true;
        }
        if (item.getItemId() == MENU_KEYBOARD) {
            showKeyboardDialog();
            return true;
        }
        if (item.getItemId() == MENU_NAV_SIDE) {
            String[] choices = {getString(R.string.nav_side_left), getString(R.string.nav_side_right)};
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_nav_side_title)
                    .setSingleChoiceItems(choices, Prefs.navRight(this) ? 1 : 0, (d, which) -> {
                        Prefs.setNavRight(this, which == 1);
                        Mirror.settingsChanged();
                        d.dismiss();
                    })
                    .setNegativeButton(R.string.action_close, null)
                    .show();
            return true;
        }
        if (item.getItemId() == MENU_HELP) {
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        }
        if (item.getItemId() == MENU_INSTALL) {
            new AlertDialog.Builder(this)
                    .setTitle(installedForCar(this)
                            ? R.string.dialog_installed_title
                            : R.string.dialog_not_installed_title)
                    .setMessage(installedForCar(this)
                            ? R.string.dialog_installed_body
                            : R.string.install_for_car_body)
                    .setPositiveButton(unknownAutoLabel(this), (d, w) -> openUnknownAuto(this))
                    .setNegativeButton(R.string.action_close, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------ status cards

    private void refresh() {
        cards.removeAllViews();
        boolean onCar = Mirror.carShowing();
        if (onCar) {
            cards.addView(card(R.drawable.ic_car, getString(R.string.card_on_car_title),
                    getString(Prefs.phoneGroup(this) == 0 || Prefs.keepPhoneOn(this)
                            ? R.string.card_on_car_body_phone_on
                            : R.string.card_on_car_body_phone_dark)));
        }
        Shell.State st = Shell.state();
        // "Shizuku", or the fork the phone has instead ("Shevery")
        String shizuku = Shell.managerLabel();
        switch (st) {
            case READY:
                if (Shell.rootOnOldAndroid()) {
                    cards.addView(card(R.drawable.ic_shell, getString(R.string.card_shizuku_root_title, shizuku),
                            getString(R.string.card_shizuku_root_body, shizuku),
                            Ui.filled(this, getString(R.string.action_open_named, shizuku)), v -> openShizuku()));
                    break;
                }
                cards.addView(slim(Ui.icon(this, R.drawable.ic_check, Ui.accent(this)), null,
                        getString(R.string.status_shizuku_running, shizuku)));
                break;
            case CONNECTING:
                ProgressBar p = new ProgressBar(this);
                p.setIndeterminate(true);
                cards.addView(slim(null, p, getString(R.string.status_shizuku_connecting, shizuku)));
                break;
            case NO_SHIZUKU:
                cards.addView(card(R.drawable.ic_shell, getString(R.string.card_no_shizuku_title),
                        getString(R.string.card_no_shizuku_body),
                        Ui.filled(this, getString(R.string.action_get_shizuku)), v -> openStore()));
                break;
            case NOT_RUNNING:
                cards.addView(card(R.drawable.ic_shell, getString(R.string.card_shizuku_stopped_title, shizuku),
                        getString(R.string.card_shizuku_stopped_body, shizuku),
                        Ui.filled(this, getString(R.string.action_open_named, shizuku)), v -> openShizuku()));
                break;
            case OLD_SHIZUKU:
                cards.addView(card(R.drawable.ic_shell, getString(R.string.card_old_shizuku_title),
                        getString(R.string.card_old_shizuku_body),
                        Ui.filled(this, getString(R.string.action_update_shizuku)), v -> openStore()));
                break;
            case NO_PERMISSION:
                cards.addView(card(R.drawable.ic_shell, getString(R.string.card_no_permission_title, shizuku),
                        getString(R.string.card_no_permission_body, shizuku),
                        Ui.filled(this, getString(R.string.action_allow)), v -> Shell.requestPermission()));
                break;
        }
        String update = Updater.readyVersion(this);
        if (update != null) {
            boolean ua = isUnknownAutoInstalled(this);
            View c = card(R.drawable.ic_check, getString(R.string.card_update_ready_title, update),
                    getString(ua ? R.string.card_update_ready_body_ua : R.string.card_update_ready_body_no_ua),
                    Ui.filled(this, getString(ua ? R.string.action_update : R.string.action_get_unknownauto)),
                    v -> {
                        if (ua) Updater.install(this);
                        else openUnknownAuto(this);
                    });
            Button news = Ui.textButton(this, getString(R.string.action_whats_new));
            news.setOnClickListener(v -> Updater.openReleasePage(this));
            ((LinearLayout) c.findViewWithTag("buttons")).addView(news, 0);
            cards.addView(c);
        }
        long version = versionCode();
        if (!installedForCar(this) && !Prefs.installCardHidden(this, version)) {
            View c = card(R.drawable.ic_car, getString(R.string.dialog_not_installed_title),
                    getString(R.string.install_for_car_body),
                    Ui.filled(this, unknownAutoLabel(this)), v -> openUnknownAuto(this));
            Button hide = Ui.textButton(this, getString(R.string.action_hide));
            hide.setOnClickListener(v -> {
                Prefs.hideInstallCard(this, version);
                refresh();
            });
            LinearLayout buttons = c.findViewWithTag("buttons");
            buttons.addView(hide, 0);
            cards.addView(c);
        }

        for (OemHealth.WarningKind w : OemHealth.check(this, lastServiceError)) {
            if (w == OemHealth.WarningKind.HYPEROS_SECURITY_DEBUGGING) {
                View c = card(R.drawable.ic_phone_alert,
                        getString(R.string.card_hyperos_sec_title),
                        getString(R.string.card_hyperos_sec_body),
                        Ui.filled(this, getString(R.string.action_developer_options)),
                        v -> OemHealth.openDeveloperSettings(this));
                if (Shell.manager() != null) {
                    Button openShizukuBtn = Ui.textButton(this, getString(R.string.action_open_named, shizuku));
                    openShizukuBtn.setOnClickListener(v -> openShizuku());
                    ((LinearLayout) c.findViewWithTag("buttons")).addView(openShizukuBtn, 0);
                }
                cards.addView(c);
            } else if (w == OemHealth.WarningKind.BATTERY_OPTIMIZATION) {
                cards.addView(card(R.drawable.ic_phone_alert,
                        getString(R.string.card_battery_opt_title),
                        getString(R.string.card_battery_opt_body),
                        Ui.filled(this, getString(R.string.action_disable_battery_limit)),
                        v -> OemHealth.openBatteryOptimization(this)));
            } else if (w == OemHealth.WarningKind.HYPEROS_BACKGROUND_GUIDE && !Prefs.hyperosGuideHidden(this)) {
                View c = card(R.drawable.ic_phone_alert,
                        getString(R.string.card_hyperos_guide_title),
                        getString(R.string.card_hyperos_guide_body),
                        Ui.filled(this, getString(R.string.action_app_settings)),
                        v -> OemHealth.openAppSettings(this));
                Button gotIt = Ui.textButton(this, getString(R.string.action_got_it));
                gotIt.setOnClickListener(v -> {
                    Prefs.hideHyperosGuide(this);
                    refresh();
                });
                ((LinearLayout) c.findViewWithTag("buttons")).addView(gotIt, 0);
                cards.addView(c);
            }
        }

        // before the first drives: a black phone is on purpose, not a crash
        int group = Prefs.phoneGroup(this);
        if (!onCar && group != 0 && !Prefs.keepPhoneOn(this) && !Prefs.darkNoteHidden(this)) {
            View c = card(R.drawable.ic_phone_sleep,
                    getString(group == 1 ? R.string.card_dark_goes_title : R.string.card_dark_may_title),
                    getString(group == 1 ? R.string.card_dark_goes_body : R.string.card_dark_may_body),
                    Ui.textButton(this, getString(R.string.action_got_it)), v -> {
                        Prefs.hideDarkNote(this);
                        refresh();
                    });
            cards.addView(c);
        }

        boolean none = chosen.isEmpty();
        section.setVisibility(none ? View.GONE : View.VISIBLE);
        sectionCount.setVisibility(none ? View.GONE : View.VISIBLE);
        sectionCount.setText(getResources().getQuantityString(R.plurals.app_count, chosen.size(), chosen.size()));
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private View card(int icon, String title, String body) {
        return card(icon, title, body, null, null);
    }

    /** A card: icon + title, body, buttons right-aligned (the fixing one filled). */
    private View card(int icon, String title, String body, Button button, View.OnClickListener action) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(button != null ? 8 : 16));
        c.setBackground(Ui.round(Ui.card(this), dp(16)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(12), dp(16), 0);
        c.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(Ui.icon(this, icon, Ui.text(this)));
        top.addView(iv, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView t = Ui.text(this, title, 16, true, Ui.text(this));
        t.setPadding(dp(12), 0, 0, 0);
        top.addView(t);
        c.addView(top);
        TextView bt = Ui.text(this, body, 14, false, Ui.text2(this));
        bt.setPadding(0, dp(8), 0, 0);
        c.addView(bt);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setTag("buttons");
        buttons.setGravity(Gravity.END);
        if (button != null) {
            button.setOnClickListener(action);
            buttons.addView(button);
            buttons.setPadding(0, dp(8), 0, 0);
            c.addView(buttons);
        }
        return c;
    }

    /** A slim status row without a card: ready, connecting. */
    private View slim(android.graphics.drawable.Drawable icon, View leading, String text) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(8), dp(16), 0);
        r.setMinimumHeight(dp(40));
        if (leading != null) {
            r.addView(leading, new LinearLayout.LayoutParams(dp(20), dp(20)));
        } else {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(icon);
            r.addView(iv, new LinearLayout.LayoutParams(dp(20), dp(20)));
        }
        TextView t = Ui.text(this, text, 14, false, Ui.text2(this));
        t.setPadding(dp(12), 0, 0, 0);
        r.addView(t);
        return r;
    }

    private View emptyState() {
        LinearLayout e = new LinearLayout(this);
        e.setOrientation(LinearLayout.VERTICAL);
        e.setGravity(Gravity.CENTER_HORIZONTAL);
        e.setPadding(dp(32), dp(72), dp(32), dp(24));
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(Ui.icon(this, R.drawable.ic_car, Ui.text2(this)));
        e.addView(iv, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView t = Ui.text(this, getString(R.string.empty_no_apps_title), 18, true, Ui.text(this));
        t.setPadding(0, dp(16), 0, dp(8));
        t.setGravity(Gravity.CENTER);
        e.addView(t);
        TextView b = Ui.text(this, getString(R.string.empty_no_apps_body), 14, false, Ui.text2(this));
        b.setGravity(Gravity.CENTER);
        e.addView(b);
        return e;
    }

    // ------------------------------------------------------------------ actions

    private void tryApp(Apps.App a) {
        if (Mirror.carShowing()) return;
        startActivity(new Intent(this, PreviewActivity.class).putExtra(PreviewActivity.EXTRA_APP, a.component));
    }

    private void openStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Shell.SHIZUKU_PACKAGE)));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")));
        }
    }

    private void openShizuku() {
        String pkg = Shell.manager();
        Intent i = pkg == null ? null : getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) startActivity(i);
        else openStore();
    }

    static boolean isUnknownAutoInstalled(Context c) {
        return c.getPackageManager().getLaunchIntentForPackage(UNKNOWNAUTO) != null;
    }

    static String unknownAutoLabel(Context c) {
        return c.getString(isUnknownAutoInstalled(c) ? R.string.action_open_unknownauto : R.string.action_get_unknownauto);
    }

    static void openUnknownAuto(Context c) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(UNKNOWNAUTO);
        if (i == null) i = new Intent(Intent.ACTION_VIEW, Uri.parse(UNKNOWNAUTO_RELEASES));
        c.startActivity(i);
    }

    /** Installed as if from Google Play (UnknownAuto): the only install Android Auto runs in a car. */
    static boolean installedForCar(Context c) {
        try {
            PackageManager pm = c.getPackageManager();
            String installer = Build.VERSION.SDK_INT >= 30
                    ? pm.getInstallSourceInfo(c.getPackageName()).getInstallingPackageName()
                    : pm.getInstallerPackageName(c.getPackageName());
            return "com.android.vending".equals(installer);
        } catch (Exception e) {
            return false;
        }
    }

    private long versionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ chosen apps

    private final class AppAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return chosen.size();
        }

        @Override
        public Apps.App getItem(int i) {
            return chosen.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean isEnabled(int i) {
            return !Mirror.carShowing();
        }

        @Override
        public View getView(int i, View v, ViewGroup parent) {
            Row r;
            if (v == null) {
                r = new Row();
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(8), dp(8), dp(8));
                r.icon = new ImageView(MainActivity.this);
                row.addView(r.icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
                LinearLayout textCol = new LinearLayout(MainActivity.this);
                textCol.setOrientation(LinearLayout.VERTICAL);
                textCol.setPadding(dp(16), 0, dp(8), 0);
                r.label = Ui.text(MainActivity.this, "", 16, false, Ui.text(MainActivity.this));
                textCol.addView(r.label);
                r.summary = Ui.text(MainActivity.this, "", 12, false, Ui.text2(MainActivity.this));
                r.summary.setPadding(0, dp(2), 0, 0);
                textCol.addView(r.summary);
                row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                // the whole row is the target; the button only names it
                r.tryIt = Ui.textButton(MainActivity.this, getString(R.string.action_try));
                r.tryIt.setMinWidth(dp(64));
                r.tryIt.setMinimumWidth(dp(64));
                r.tryIt.setClickable(false);
                r.tryIt.setFocusable(false);
                row.addView(r.tryIt);
                r.settings = new ImageButton(MainActivity.this);
                r.settings.setFocusable(false);
                r.settings.setFocusableInTouchMode(false);
                r.settings.setContentDescription(getString(R.string.cd_app_profile));
                r.settings.setScaleType(ImageView.ScaleType.CENTER);
                r.settings.setBackground(new RippleDrawable(
                        ColorStateList.valueOf(Ui.accent(MainActivity.this) & 0x33FFFFFF),
                        null, Ui.round(0xFFFFFFFF, dp(24))));
                row.addView(r.settings, new LinearLayout.LayoutParams(dp(48), dp(48)));
                row.setTag(r);
                v = row;
            } else {
                r = (Row) v.getTag();
            }
            Apps.App a = chosen.get(i);
            r.icon.setImageDrawable(a.icon(getPackageManager()));
            r.label.setText(a.label);
            String summary = profileSummary(a.component);
            if (summary == null) {
                r.summary.setVisibility(View.GONE);
                r.settings.setImageDrawable(Ui.icon(MainActivity.this, R.drawable.ic_tune, Ui.text2(MainActivity.this)));
            } else {
                r.summary.setText(summary);
                r.summary.setVisibility(View.VISIBLE);
                r.settings.setImageDrawable(Ui.icon(MainActivity.this, R.drawable.ic_tune, Ui.accent(MainActivity.this)));
            }
            r.settings.setOnClickListener(btn -> showAppProfileDialog(a));
            r.tryIt.setAlpha(Mirror.carShowing() ? 0.38f : 1f);
            return v;
        }
    }

    private String profileSummary(ComponentName cn) {
        int dpi = Prefs.appDpi(this, cn, 0);
        boolean lock = Prefs.appOrientationLock(this, cn);
        int inset = Prefs.appInsetDp(this, cn);
        if (dpi == 0 && lock && inset == 0) return null;
        List<String> parts = new ArrayList<>(3);
        if (dpi > 0) parts.add(getString(R.string.profile_summary_dpi, dpi));
        if (!lock) parts.add(getString(R.string.profile_summary_follow_app));
        if (inset > 0) parts.add(getString(R.string.profile_summary_inset, inset));
        return TextUtils.join(" · ", parts);
    }

    private void showKeyboardDialog() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(8), dp(20), dp(4));

        boolean curOnCar = Prefs.keyboardOnCar(this);
        RadioGroup placeGroup = addRadioSection(body, getString(R.string.dialog_keyboard_where),
                new String[]{getString(R.string.keyboard_on_car), getString(R.string.keyboard_on_phone)},
                new int[]{1, 0}, curOnCar ? 1 : 0, false);

        TextView voiceHeader = Ui.text(this, getString(R.string.dialog_keyboard_voice_header), 14, true, Ui.accent(this));
        voiceHeader.setPadding(0, dp(14), 0, dp(4));
        body.addView(voiceHeader);

        CheckBox wakeCheck = new CheckBox(this);
        wakeCheck.setText(R.string.keyboard_wake_for_voice);
        wakeCheck.setTextColor(Ui.text(this));
        wakeCheck.setTextSize(15);
        wakeCheck.setButtonTintList(ColorStateList.valueOf(Ui.accent(this)));
        wakeCheck.setMinimumHeight(dp(40));
        wakeCheck.setChecked(Prefs.wakeForVoice(this));
        body.addView(wakeCheck);

        TextView caption = Ui.text(this,
                getString(R.string.keyboard_wake_for_voice_hint),
                13, false, Ui.text2(this));
        caption.setPadding(dp(4), dp(2), 0, dp(4));
        body.addView(caption);

        Runnable updateWakeEnabled = () -> {
            boolean onCar = selectedTag(placeGroup, curOnCar ? 1 : 0) == 1;
            wakeCheck.setEnabled(onCar);
            wakeCheck.setAlpha(onCar ? 1f : 0.5f);
            caption.setAlpha(onCar ? 1f : 0.5f);
        };
        updateWakeEnabled.run();
        placeGroup.setOnCheckedChangeListener((g, checkedId) -> updateWakeEnabled.run());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_keyboard)
                .setView(scroll)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    Prefs.setKeyboardOnCar(this, selectedTag(placeGroup, curOnCar ? 1 : 0) == 1);
                    Prefs.setWakeForVoice(this, wakeCheck.isChecked());
                    Mirror.settingsChanged();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showAppProfileDialog(Apps.App a) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(8), dp(20), dp(4));

        int curDpi = Prefs.appDpi(this, a.component, 0);
        int[] dpiVals = {0, 120, 160, 200, 240, 320};
        String[] dpiLabels = {
                getString(R.string.profile_dpi_auto),
                getString(R.string.profile_summary_dpi, 120),
                getString(R.string.profile_summary_dpi, 160),
                getString(R.string.profile_summary_dpi, 200),
                getString(R.string.profile_summary_dpi, 240),
                getString(R.string.profile_summary_dpi, 320)
        };
        if (curDpi > 0 && !containsInt(dpiVals, curDpi)) {
            dpiVals = appendInt(dpiVals, curDpi);
            dpiLabels = appendString(dpiLabels, getString(R.string.profile_summary_dpi, curDpi));
        }
        RadioGroup dpiGroup = addRadioSection(body, getString(R.string.dialog_profile_dpi), dpiLabels, dpiVals, curDpi, false);

        boolean curLock = Prefs.appOrientationLock(this, a.component);
        RadioGroup orientGroup = addRadioSection(body, getString(R.string.dialog_profile_orientation),
                new String[]{getString(R.string.profile_orient_lock), getString(R.string.profile_orient_follow)},
                new int[]{1, 0}, curLock ? 1 : 0, true);

        int curInset = Prefs.appInsetDp(this, a.component);
        int[] insetVals = {0, 16, 32};
        String[] insetLabels = {
                getString(R.string.profile_inset_none),
                getString(R.string.profile_summary_inset, 16),
                getString(R.string.profile_summary_inset, 32)
        };
        if (curInset > 0 && !containsInt(insetVals, curInset)) {
            insetVals = appendInt(insetVals, curInset);
            insetLabels = appendString(insetLabels, getString(R.string.profile_summary_inset, curInset));
        }
        RadioGroup insetGroup = addRadioSection(body, getString(R.string.dialog_profile_inset), insetLabels, insetVals, curInset, true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        new AlertDialog.Builder(this)
                .setTitle(a.label)
                .setView(scroll)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    Prefs.setAppDpi(this, a.component, selectedTag(dpiGroup, curDpi));
                    Prefs.setAppOrientationLock(this, a.component, selectedTag(orientGroup, curLock ? 1 : 0) == 1);
                    Prefs.setAppInsetDp(this, a.component, selectedTag(insetGroup, curInset));
                    adapter.notifyDataSetChanged();
                    Mirror.settingsChanged();
                })
                .setNeutralButton(R.string.action_reset, (d, w) -> {
                    Prefs.setAppDpi(this, a.component, 0);
                    Prefs.setAppOrientationLock(this, a.component, true);
                    Prefs.setAppInsetDp(this, a.component, 0);
                    adapter.notifyDataSetChanged();
                    Mirror.settingsChanged();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private RadioGroup addRadioSection(LinearLayout parent, String title, String[] labels,
            int[] values, int current, boolean topSpace) {
        TextView header = Ui.text(this, title, 14, true, Ui.accent(this));
        header.setPadding(0, dp(topSpace ? 14 : 4), 0, dp(4));
        parent.addView(header);
        RadioGroup group = new RadioGroup(this);
        ColorStateList tint = ColorStateList.valueOf(Ui.accent(this));
        int textColor = Ui.text(this);
        int checkedId = -1;
        for (int i = 0; i < labels.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(labels[i]);
            rb.setTextColor(textColor);
            rb.setTextSize(15);
            rb.setButtonTintList(tint);
            rb.setMinimumHeight(dp(40));
            rb.setTag(values[i]);
            group.addView(rb);
            if (values[i] == current) checkedId = rb.getId();
        }
        if (checkedId != -1) group.check(checkedId);
        parent.addView(group);
        return group;
    }

    private static int selectedTag(RadioGroup group, int fallback) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return fallback;
        View v = group.findViewById(id);
        return v != null && v.getTag() instanceof Integer ? (Integer) v.getTag() : fallback;
    }

    private static boolean containsInt(int[] a, int target) {
        for (int v : a) if (v == target) return true;
        return false;
    }

    private static int[] appendInt(int[] a, int v) {
        int[] out = java.util.Arrays.copyOf(a, a.length + 1);
        out[a.length] = v;
        return out;
    }

    private static String[] appendString(String[] a, String v) {
        String[] out = java.util.Arrays.copyOf(a, a.length + 1);
        out[a.length] = v;
        return out;
    }

    private static final class Row {
        ImageView icon;
        TextView label, summary;
        Button tryIt;
        ImageButton settings;
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }
}
