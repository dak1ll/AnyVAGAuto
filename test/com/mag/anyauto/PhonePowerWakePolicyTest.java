package com.mag.anyauto;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PhonePowerWakePolicyTest {
    @Test
    public void tempWakeSuspendsBlankWithoutMarkingFights() {
        PhonePower.DarkPolicy policy = new PhonePower.DarkPolicy();
        policy.setUserDark(true);
        assertTrue(policy.shouldBlank());
        assertFalse(policy.isTempWake());
        assertFalse(policy.fightsDetected());

        policy.setTempWake(true);
        assertTrue(policy.isTempWake());
        assertFalse(policy.shouldBlank());
        assertFalse(policy.fightsDetected());

        policy.setTempWake(false);
        assertFalse(policy.isTempWake());
        assertTrue(policy.shouldBlank());
        assertFalse(policy.fightsDetected());
    }

    @Test
    public void threeNonSettleBlanksWithinWindowTriggerFights() {
        PhonePower.DarkPolicy policy = new PhonePower.DarkPolicy();
        policy.setUserDark(true);

        assertTrue(policy.recordBlank(1000L, false));
        assertFalse(policy.fightsDetected());
        assertTrue(policy.shouldBlank());

        assertTrue(policy.recordBlank(4000L, false));
        assertFalse(policy.fightsDetected());
        assertTrue(policy.shouldBlank());

        assertFalse(policy.recordBlank(7000L, false));
        assertTrue(policy.fightsDetected());
        assertFalse(policy.shouldBlank());
    }

    @Test
    public void preAndPostTempWakeBlanksDoNotAccumulateIntoFalseFights() {
        PhonePower.DarkPolicy policy = new PhonePower.DarkPolicy();
        policy.setUserDark(true);

        assertTrue(policy.recordBlank(1000L, false));
        assertTrue(policy.recordBlank(3000L, false));

        policy.setTempWake(true);
        assertFalse(policy.recordBlank(3500L, false));
        policy.setTempWake(false);

        assertTrue(policy.recordBlank(5000L, false));
        assertFalse(policy.fightsDetected());
        assertTrue(policy.shouldBlank());
    }

    @Test
    public void settleWindowBlanksDoNotCountTowardFights() {
        PhonePower.DarkPolicy policy = new PhonePower.DarkPolicy();
        policy.setUserDark(true);

        assertTrue(policy.recordBlank(1000L, true));
        assertTrue(policy.recordBlank(1500L, true));
        assertTrue(policy.recordBlank(2000L, true));
        assertFalse(policy.fightsDetected());

        assertTrue(policy.recordBlank(3000L, false));
        assertFalse(policy.fightsDetected());
    }
}
