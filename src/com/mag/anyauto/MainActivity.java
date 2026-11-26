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
        section = Ui.text(this, "In the car", 14, true, Ui.accent(this));
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

        Button choose = Ui.filled(this, "Choose apps");
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
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_FULL, 0, "Full screen in the car").setCheckable(true)
                .setChecked(Prefs.fullScreen(this));
        menu.add(0, MENU_PHONE_ON, 1, "Keep the phone's screen on").setCheckable(true)
                .setChecked(Prefs.keepPhoneOn(this));
        menu.add(0, MENU_KEYBOARD, 2, "Keyboard");
        menu.add(0, MENU_NAV_SIDE, 3, "Navigation bar side");
        menu.add(0, MENU_POINTER, 3, "Pointer buttons (knob)").setCheckable(true)
                .setChecked(Prefs.pointerButtons(this));
        menu.add(0, MENU_SPLIT, 3, "Split screen (Freeform)").setCheckable(true)
                .setChecked(Prefs.splitMode(this));
        menu.add(0, MENU_AUTO_UPDATE, 4, "Check for updates automatically").setCheckable(true)
                .setChecked(Prefs.autoUpdate(this));
        menu.add(0, MENU_CHECK_UPDATE, 5, "Check for updates now");
        menu.add(0, MENU_HELP, 6, "Help");
        menu.add(0, MENU_INSTALL, 7, "Install for the car");
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
                        .setTitle("Pointer buttons")
                        .setMessage("For cars with a knob where Android Auto shows no pan button (✥). "
                                + "Android Auto's buttons on the car screen get Pointer: press it, then use the arrows "
                                + "to move the ring and Tap to click there. Done brings the usual buttons back.")
                        .setPositiveButton("OK", null)
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
            android.widget.Toast.makeText(this, "Checking for updates…", android.widget.Toast.LENGTH_SHORT).show();
            Updater.check(this, true, r -> {
                String m = r.error != null ? "Couldn't check for updates: " + r.error
                        : r.version != null ? "AnyAuto " + r.version + " is ready to install"
                        : "AnyAuto is up to date";
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
            String[] choices = {"Left", "Right"};
            new AlertDialog.Builder(this)
                    .setTitle("Navigation bar on the car screen")
                    .setSingleChoiceItems(choices, Prefs.navRight(this) ? 1 : 0, (d, which) -> {
                        Prefs.setNavRight(this, which == 1);
                        Mirror.settingsChanged();
                        d.dismiss();
                    })
                    .setNegativeButton("Close", null)
                    .show();
            return true;
        }
        if (item.getItemId() == MENU_HELP) {
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        }
        if (item.getItemId() == MENU_INSTALL) {
            new AlertDialog.Builder(this)
                    .setTitle(installedForCar(this) ? "Installed for the car" : "Not installed for the car")
                    .setMessage(installedForCar(this)
                            ? "AnyAuto was installed in a way Android Auto accepts. After an update, install it with UnknownAuto again."
                            : INSTALL_BODY)
                    .setPositiveButton(unknownAutoLabel(this), (d, w) -> openUnknownAuto(this))
                    .setNegativeButton("Close", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final String INSTALL_BODY = "Android Auto only runs apps installed from Google Play. "
            + "Reinstall AnyAuto with UnknownAuto before using it in a car. The desktop head unit works either way.";

    // ------------------------------------------------------------------ status cards

    private void refresh() {
        cards.removeAllViews();
        boolean onCar = Mirror.carShowing();
        if (onCar) {
            cards.addView(card(R.drawable.ic_car, "On the car screen",
                    Prefs.phoneGroup(this) == 0 || Prefs.keepPhoneOn(this)
                            ? "The car is using the display. Try is unavailable meanwhile."
                            : "The car is using the display. The phone stays awake with its screen off. Try is unavailable meanwhile."));
        }
        Shell.State st = Shell.state();
        // "Shizuku", or the fork the phone has instead ("Shevery")
        String shizuku = Shell.managerLabel();
        switch (st) {
            case READY:
                if (Shell.rootOnOldAndroid()) {
                    cards.addView(card(R.drawable.ic_shell, shizuku + " runs as root",
                            "On Android 13 and older, Android won't give root the car display. In " + shizuku + ", start it with Wireless debugging instead.",
                            Ui.filled(this, "Open " + shizuku), v -> openShizuku()));
                    break;
                }
                cards.addView(slim(Ui.icon(this, R.drawable.ic_check, Ui.accent(this)), null, shizuku + " is running"));
                break;
            case CONNECTING:
                ProgressBar p = new ProgressBar(this);
                p.setIndeterminate(true);
                cards.addView(slim(null, p, "Connecting to " + shizuku + "…"));
                break;
            case NO_SHIZUKU:
                cards.addView(card(R.drawable.ic_shell, "Shizuku isn't installed",
                        "AnyAuto needs Shizuku or a Shizuku fork. It gives the app the rights adb has, without root.",
                        Ui.filled(this, "Get Shizuku"), v -> openStore()));
                break;
            case NOT_RUNNING:
                cards.addView(card(R.drawable.ic_shell, shizuku + " isn't running",
                        "Start it in " + shizuku + ": Wireless debugging, then Start. It stops at every reboot.",
                        Ui.filled(this, "Open " + shizuku), v -> openShizuku()));
                break;
            case OLD_SHIZUKU:
                cards.addView(card(R.drawable.ic_shell, "Shizuku is too old",
                        "Update Shizuku to version 11 or newer.",
                        Ui.filled(this, "Update Shizuku"), v -> openStore()));
                break;
            case NO_PERMISSION:
                cards.addView(card(R.drawable.ic_shell, "Allow AnyAuto in " + shizuku,
                        shizuku + " asks once. Choose Allow all the time.",
                        Ui.filled(this, "Allow"), v -> Shell.requestPermission()));
                break;
        }
        String update = Updater.readyVersion(this);
        if (update != null) {
            boolean ua = isUnknownAutoInstalled(this);
            View c = card(R.drawable.ic_check, "AnyAuto " + update + " is ready",
                    ua ? "Tap Update, then Install in UnknownAuto. UnknownAuto keeps AnyAuto installed the way Android Auto accepts."
                            : "Updates install with UnknownAuto, so Android Auto keeps accepting AnyAuto. Get UnknownAuto first.",
                    Ui.filled(this, ua ? "Update" : "Get UnknownAuto"),
                    v -> {
                        if (ua) Updater.install(this);
                        else openUnknownAuto(this);
                    });
            Button news = Ui.textButton(this, "What's new");
            news.setOnClickListener(v -> Updater.openReleasePage(this));
            ((LinearLayout) c.findViewWithTag("buttons")).addView(news, 0);
            cards.addView(c);
        }
        long version = versionCode();
        if (!installedForCar(this) && !Prefs.installCardHidden(this, version)) {
            View c = card(R.drawable.ic_car, "Not installed for the car", INSTALL_BODY,
                    Ui.filled(this, unknownAutoLabel(this)), v -> openUnknownAuto(this));
            Button hide = Ui.textButton(this, "Hide");
            hide.setOnClickListener(v -> {
                Prefs.hideInstallCard(this, version);
                refresh();
            });
            LinearLayout buttons = c.findViewWithTag("buttons");
            buttons.addView(hide, 0);
            cards.addView(c);
        }

        // before the first drives: a black phone is on purpose, not a crash
        int group = Prefs.phoneGroup(this);
        if (!onCar && group != 0 && !Prefs.keepPhoneOn(this) && !Prefs.darkNoteHidden(this)) {
            View c = card(R.drawable.ic_phone_sleep,
                    group == 1 ? "The phone's screen goes dark in the car" : "The phone's screen may go dark in the car",
                    (group == 1 ? "On this phone, the car screen only runs while the phone is awake. So while"
                            : "On some phones, the car screen only runs while the phone is awake. On those, while")
                            + " the car shows AnyAuto, the phone stays awake with its screen off. That's on purpose, "
                            + "not a crash: the screen comes back when you leave AnyAuto in the car. "
                            + "To keep it on, turn on ⋮ → Keep the phone's screen on.",
                    Ui.textButton(this, "Got it"), v -> {
                        Prefs.hideDarkNote(this);
                        refresh();
                    });
            cards.addView(c);
        }

        boolean none = chosen.isEmpty();
        section.setVisibility(none ? View.GONE : View.VISIBLE);
        sectionCount.setVisibility(none ? View.GONE : View.VISIBLE);
        sectionCount.setText(chosen.size() == 1 ? "1 app" : chosen.size() + " apps");
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
        TextView t = Ui.text(this, "No apps in the car yet", 18, true, Ui.text(this));
        t.setPadding(0, dp(16), 0, dp(8));
        t.setGravity(Gravity.CENTER);
        e.addView(t);
        TextView b = Ui.text(this, "Choose the phone apps to show on the car screen.", 14, false, Ui.text2(this));
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
        return c.getPackageManager().getLaunchIntentForPackage(UNKNOWNAUTO) != null ? "Open UnknownAuto" : "Get UnknownAuto";
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
                r.tryIt = Ui.textButton(MainActivity.this, "Try");
                r.tryIt.setMinWidth(dp(64));
                r.tryIt.setMinimumWidth(dp(64));
                r.tryIt.setClickable(false);
                r.tryIt.setFocusable(false);
                row.addView(r.tryIt);
                r.settings = new ImageButton(MainActivity.this);
                r.settings.setFocusable(false);
                r.settings.setFocusableInTouchMode(false);
                r.settings.setContentDescription("App profile");
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
        if (dpi > 0) parts.add(dpi + " dpi");
        if (!lock) parts.add("Follow app");
        if (inset > 0) parts.add(inset + " dp");
        return TextUtils.join(" · ", parts);
    }

    private void showKeyboardDialog() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(8), dp(20), dp(4));

        boolean curOnCar = Prefs.keyboardOnCar(this);
        RadioGroup placeGroup = addRadioSection(body, "Where the keyboard opens",
                new String[]{"On the car screen", "On the phone (keeps phone screen on)"},
                new int[]{1, 0}, curOnCar ? 1 : 0, false);

        TextView voiceHeader = Ui.text(this, "Voice input", 14, true, Ui.accent(this));
        voiceHeader.setPadding(0, dp(14), 0, dp(4));
        body.addView(voiceHeader);

        CheckBox wakeCheck = new CheckBox(this);
        wakeCheck.setText("Wake phone screen briefly for voice input");
        wakeCheck.setTextColor(Ui.text(this));
        wakeCheck.setTextSize(15);
        wakeCheck.setButtonTintList(ColorStateList.valueOf(Ui.accent(this)));
        wakeCheck.setMinimumHeight(dp(40));
        wakeCheck.setChecked(Prefs.wakeForVoice(this));
        body.addView(wakeCheck);

        TextView caption = Ui.text(this,
                "Helps keyboard microphone dictation (e.g. Gboard, Yandex Keyboard) work when the phone screen is kept off in the car.",
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
                .setTitle("Keyboard")
                .setView(scroll)
                .setPositiveButton("Save", (d, w) -> {
                    Prefs.setKeyboardOnCar(this, selectedTag(placeGroup, curOnCar ? 1 : 0) == 1);
                    Prefs.setWakeForVoice(this, wakeCheck.isChecked());
                    Mirror.settingsChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAppProfileDialog(Apps.App a) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(8), dp(20), dp(4));

        int curDpi = Prefs.appDpi(this, a.component, 0);
        int[] dpiVals = {0, 120, 160, 200, 240, 320};
        String[] dpiLabels = {"Auto (car screen)", "120 dpi", "160 dpi", "200 dpi", "240 dpi", "320 dpi"};
        if (curDpi > 0 && !containsInt(dpiVals, curDpi)) {
            dpiVals = appendInt(dpiVals, curDpi);
            dpiLabels = appendString(dpiLabels, curDpi + " dpi");
        }
        RadioGroup dpiGroup = addRadioSection(body, "Display DPI", dpiLabels, dpiVals, curDpi, false);

        boolean curLock = Prefs.appOrientationLock(this, a.component);
        RadioGroup orientGroup = addRadioSection(body, "Orientation",
                new String[]{"Lock landscape", "Follow app"},
                new int[]{1, 0}, curLock ? 1 : 0, true);

        int curInset = Prefs.appInsetDp(this, a.component);
        int[] insetVals = {0, 16, 32};
        String[] insetLabels = {"0 dp (none)", "16 dp", "32 dp"};
        if (curInset > 0 && !containsInt(insetVals, curInset)) {
            insetVals = appendInt(insetVals, curInset);
            insetLabels = appendString(insetLabels, curInset + " dp");
        }
        RadioGroup insetGroup = addRadioSection(body, "Safe edge inset", insetLabels, insetVals, curInset, true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        new AlertDialog.Builder(this)
                .setTitle(a.label)
                .setView(scroll)
                .setPositiveButton("Save", (d, w) -> {
                    Prefs.setAppDpi(this, a.component, selectedTag(dpiGroup, curDpi));
                    Prefs.setAppOrientationLock(this, a.component, selectedTag(orientGroup, curLock ? 1 : 0) == 1);
                    Prefs.setAppInsetDp(this, a.component, selectedTag(insetGroup, curInset));
                    adapter.notifyDataSetChanged();
                    Mirror.settingsChanged();
                })
                .setNeutralButton("Reset", (d, w) -> {
                    Prefs.setAppDpi(this, a.component, 0);
                    Prefs.setAppOrientationLock(this, a.component, true);
                    Prefs.setAppInsetDp(this, a.component, 0);
                    adapter.notifyDataSetChanged();
                    Mirror.settingsChanged();
                })
                .setNegativeButton("Cancel", null)
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
