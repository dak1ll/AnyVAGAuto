package com.mag.anyauto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OemHealthTest {
    @Test
    public void detectsXiaomiAndBatteryRestrictionNeed() {
        assertTrue(OemHealth.isXiaomiFamily("Xiaomi", "OS2.0.1.0"));
        assertTrue(OemHealth.isXiaomiFamily("POCO", ""));
        assertTrue(OemHealth.isXiaomiFamily("Redmi", null));
        assertTrue(OemHealth.isXiaomiFamily(null, "V14.0.1.0"));
        assertFalse(OemHealth.isXiaomiFamily("Google", ""));
        assertFalse(OemHealth.isXiaomiFamily(null, null));

        assertTrue(OemHealth.isInjectSecurityError(
                "java.lang.SecurityException: Injecting input events requires INJECT_EVENTS"));
        assertTrue(OemHealth.isInjectSecurityError("SecurityException: input injection denied"));
        assertFalse(OemHealth.isInjectSecurityError("input: no display"));
        assertFalse(OemHealth.isInjectSecurityError(null));
    }

    @Test
    public void evaluatesExpectedDiagnosticWarnings() {
        assertEquals(Collections.emptyList(),
                OemHealth.evaluateWarnings(true, false, ""));

        assertEquals(Collections.singletonList(OemHealth.WarningKind.BATTERY_OPTIMIZATION),
                OemHealth.evaluateWarnings(false, false, null));

        List<OemHealth.WarningKind> xiaomiAll = OemHealth.evaluateWarnings(
                false, true, "input: SecurityException: Injecting to another application requires INJECT_EVENTS");
        assertTrue(xiaomiAll.contains(OemHealth.WarningKind.BATTERY_OPTIMIZATION));
        assertTrue(xiaomiAll.contains(OemHealth.WarningKind.HYPEROS_SECURITY_DEBUGGING));
        assertTrue(xiaomiAll.contains(OemHealth.WarningKind.HYPEROS_BACKGROUND_GUIDE));
        assertEquals(3, xiaomiAll.size());

        List<OemHealth.WarningKind> xiaomiHealthy = OemHealth.evaluateWarnings(true, true, "");
        assertFalse(xiaomiHealthy.contains(OemHealth.WarningKind.BATTERY_OPTIMIZATION));
        assertFalse(xiaomiHealthy.contains(OemHealth.WarningKind.HYPEROS_SECURITY_DEBUGGING));
        assertEquals(Collections.emptyList(), xiaomiHealthy);

        assertEquals(Arrays.asList(OemHealth.WarningKind.HYPEROS_SECURITY_DEBUGGING),
                OemHealth.evaluateWarnings(true, false, "SecurityException: inject failed"));
    }
}
