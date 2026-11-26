package com.mag.anyauto;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MirrorHistoryTest {
    @Test
    public void tracksPreviousDistinctComponent() {
        Mirror.AppHistory<String> history = new Mirror.AppHistory<>();
        assertFalse(history.canSwap());
        assertNull(history.swap());

        history.recordOpen("pkg.a/.Main");
        assertEquals("pkg.a/.Main", history.current());
        assertNull(history.previous());
        assertFalse(history.canSwap());

        // Duplicate open must keep previous unchanged
        history.recordOpen("pkg.a/.Main");
        assertEquals("pkg.a/.Main", history.current());
        assertNull(history.previous());

        history.recordOpen("pkg.b/.Main");
        assertEquals("pkg.b/.Main", history.current());
        assertEquals("pkg.a/.Main", history.previous());
        assertTrue(history.canSwap());

        // Duplicate open of current must keep previous unchanged
        history.recordOpen("pkg.b/.Main");
        assertEquals("pkg.b/.Main", history.current());
        assertEquals("pkg.a/.Main", history.previous());

        assertEquals("pkg.a/.Main", history.swap());
        assertEquals("pkg.a/.Main", history.current());
        assertEquals("pkg.b/.Main", history.previous());

        assertEquals("pkg.b/.Main", history.swap());
        assertEquals("pkg.b/.Main", history.current());
        assertEquals("pkg.a/.Main", history.previous());

        history.clearCurrent();
        assertNull(history.current());
        assertEquals("pkg.a/.Main", history.previous());
        assertFalse(history.canSwap());
        assertNull(history.swap());

        history.recordOpen("pkg.c/.Main");
        assertEquals("pkg.c/.Main", history.current());
        assertEquals("pkg.a/.Main", history.previous());
        assertTrue(history.canSwap());

        history.reset();
        assertNull(history.current());
        assertNull(history.previous());
        assertFalse(history.canSwap());
    }

    @Test
    public void splitZoneBoundsSplits60Primary40Secondary() {
        assertArrayEquals(new int[]{0, 0, 720, 720}, Mirror.splitZoneBounds(1200, 720, false));
        assertArrayEquals(new int[]{720, 0, 1200, 720}, Mirror.splitZoneBounds(1200, 720, true));
        assertArrayEquals(new int[]{0, 0, 0, 0}, Mirror.splitZoneBounds(0, 0, false));
    }

    @Test
    public void navBarFitsSixButtonsOn480pScreen() {
        assertEquals(6, Compositor.navCount());
        assertTrue(Compositor.navSlotHeight(480, 0, 160) >= 60f);
    }
}
