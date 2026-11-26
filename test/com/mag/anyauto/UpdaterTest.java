package com.mag.anyauto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdaterTest {
    @Test
    public void forkEndpointsPointToDak1ll() {
        assertEquals("https://api.github.com/repos/dak1ll/AnyVAGAuto/releases/latest", Updater.RELEASES_API);
        assertEquals("https://github.com/dak1ll/AnyVAGAuto/releases/latest", Updater.RELEASES_PAGE);
        assertEquals("https://github.com/dak1ll/VAGAuto/releases/latest", MainActivity.UNKNOWNAUTO_RELEASES);
    }

    @Test
    public void isReleaseApkAssetFiltersExactAnyAutoApks() {
        assertTrue(Updater.isReleaseApkAsset("AnyAuto-v0.11-ru.1.apk"));
        assertTrue(Updater.isReleaseApkAsset("AnyAuto-0.12.apk"));

        assertFalse(Updater.isReleaseApkAsset(null));
        assertFalse(Updater.isReleaseApkAsset(""));
        assertFalse(Updater.isReleaseApkAsset("anyauto.apk"));
        assertFalse(Updater.isReleaseApkAsset("AnyAuto-v0.11.apk.idsig"));
        assertFalse(Updater.isReleaseApkAsset("UnknownAuto-v1.6.apk"));
    }
}
