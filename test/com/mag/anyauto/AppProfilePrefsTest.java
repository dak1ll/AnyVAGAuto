package com.mag.anyauto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppProfilePrefsTest {
    @Test
    public void profileKeyFormatsFieldAndComponent() {
        assertEquals("profile_dpi_com.example/.Main", Prefs.profileKey("dpi", "com.example/.Main"));
        assertEquals("profile_inset_com.example/.Main", Prefs.profileKey("inset", "com.example/.Main"));
        assertEquals("profile_orient_lock_com.example/.Main", Prefs.profileKey("orient_lock", "com.example/.Main"));
    }

    @Test
    public void resolveAppDpiValidatesBoundsAndFallback() {
        assertEquals(240, Prefs.resolveAppDpi(0, 240));
        assertEquals(160, Prefs.resolveAppDpi(160, 240));
        assertEquals(240, Prefs.resolveAppDpi(99, 240));
        assertEquals(240, Prefs.resolveAppDpi(641, 240));
        assertEquals(100, Prefs.resolveAppDpi(100, 240));
        assertEquals(640, Prefs.resolveAppDpi(640, 240));
    }

    @Test
    public void clampInsetDpClampsToBounds() {
        assertEquals(0, Prefs.clampInsetDp(-5));
        assertEquals(0, Prefs.clampInsetDp(0));
        assertEquals(16, Prefs.clampInsetDp(16));
        assertEquals(32, Prefs.clampInsetDp(32));
        assertEquals(64, Prefs.clampInsetDp(64));
        assertEquals(64, Prefs.clampInsetDp(99));
    }
}
