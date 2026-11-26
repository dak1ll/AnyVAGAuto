package com.mag.anyauto;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OEM and battery-restriction health diagnostics, especially for Xiaomi / HyperOS / MIUI devices
 * where background battery limits and the separate "USB debugging (Security settings)" toggle can
 * block Shizuku display keep-alive or {@code INJECT_EVENTS} input injection.
 */
final class OemHealth {
    enum WarningKind {
        BATTERY_OPTIMIZATION,
        HYPEROS_SECURITY_DEBUGGING,
        HYPEROS_BACKGROUND_GUIDE
    }

    private OemHealth() {
    }

    /** Pure helper: returns true for Xiaomi / Redmi / POCO devices or MIUI / HyperOS builds. */
    static boolean isXiaomiFamily(String manufacturer, String miuiVersionName) {
        if (miuiVersionName != null && !miuiVersionName.trim().isEmpty()) {
            return true;
        }
        if (manufacturer == null) {
            return false;
        }
        String m = manufacturer.toLowerCase(Locale.ROOT);
        return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco");
    }

    /** Pure helper: returns true when a service error indicates blocked input injection permissions. */
    static boolean isInjectSecurityError(String errorText) {
        if (errorText == null) {
            return false;
        }
        String e = errorText.toLowerCase(Locale.ROOT);
        if (e.contains("inject_events")) {
            return true;
        }
        return e.contains("securityexception") && (e.contains("inject") || e.contains("input"));
    }

    /** Pure helper: evaluates which diagnostic warnings apply to the current device and error state. */
    static List<WarningKind> evaluateWarnings(boolean ignoringBattery, boolean xiaomiFamily, String lastServiceError) {
        List<WarningKind> out = new ArrayList<>();
        if (isInjectSecurityError(lastServiceError)) {
            out.add(WarningKind.HYPEROS_SECURITY_DEBUGGING);
        }
        if (!ignoringBattery) {
            out.add(WarningKind.BATTERY_OPTIMIZATION);
            if (xiaomiFamily) {
                out.add(WarningKind.HYPEROS_BACKGROUND_GUIDE);
            }
        }
        return out;
    }

    /** Evaluates current runtime health warnings on the device. */
    static List<WarningKind> check(Context c, String lastServiceError) {
        PowerManager pm = c.getSystemService(PowerManager.class);
        boolean ignoring = pm == null || pm.isIgnoringBatteryOptimizations(c.getPackageName());
        boolean xiaomi = isXiaomiDevice();
        return evaluateWarnings(ignoring, xiaomi, lastServiceError);
    }

    static boolean isXiaomiDevice() {
        String miui = readSystemProperty("ro.miui.ui.version.name");
        return isXiaomiFamily(Build.MANUFACTURER, miui) || isXiaomiFamily(Build.BRAND, miui);
    }

    private static String readSystemProperty(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Object val = sp.getMethod("get", String.class, String.class).invoke(null, key, "");
            return val != null ? String.valueOf(val) : "";
        } catch (Throwable t) {
            return "";
        }
    }

    /** Opens the system battery-optimization exemption prompt, falling back to battery or app settings. */
    static void openBatteryOptimization(Activity a) {
        Uri pkg = Uri.parse("package:" + a.getPackageName());
        try {
            a.startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg));
            return;
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // fall through to list/settings
        }
        try {
            a.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            return;
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // fall through to app details
        }
        openAppDetails(a);
    }

    /** Opens MIUI/HyperOS Autostart settings when available, otherwise standard App info. */
    static void openAppSettings(Activity a) {
        if (isXiaomiDevice()) {
            Intent autostart = new Intent().setComponent(new ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            try {
                if (a.getPackageManager().resolveActivity(autostart, 0) != null) {
                    a.startActivity(autostart);
                    return;
                }
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // fall back to standard app details
            }
        }
        openAppDetails(a);
    }

    /** Opens Developer options (where HyperOS "USB debugging (Security settings)" lives), else Settings. */
    static void openDeveloperSettings(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            return;
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // fall back to general settings
        }
        try {
            a.startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    private static void openAppDetails(Activity a) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", a.getPackageName(), null));
        try {
            a.startActivity(i);
        } catch (ActivityNotFoundException ignored) {
        }
    }
}
