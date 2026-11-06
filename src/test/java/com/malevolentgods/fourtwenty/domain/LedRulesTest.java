package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedRulesTest {
    private static final LedRules.Numbers N = LedRules.Numbers.DEFAULT;

    @Test
    void whiteDrawsThreeTimesRed() {
        double red = N.amps(255, 0, 0, 4.2) - N.idleAmps(), white = N.amps(255, 255, 255, 4.2) - N.idleAmps();
        assertEquals(3 * red, white, 1e-12);
        assertEquals(0.238 * 3 * 4.2, white * 4.2, 1e-9, "about 3 W at full white");
    }

    @Test
    void blackDrawsOnlyItsIdle() {
        assertEquals(N.idleAmps(), N.amps(0, 0, 0, 4.2), 1e-15);
    }

    @Test
    void itsKnee() {
        assertEquals(2.52, N.forwardVolts(), 1e-12);
        assertEquals(0, N.amps(255, 255, 255, 2.5), 0, "dark below its forward voltage");
        assertEquals(0.5, N.brightness((2.52 + 3.99) / 2), 1e-12, "halfway up its knee");
        assertEquals(1, N.brightness(4.18), 0, "a healthy line lights it fully");
        assertEquals(1, N.brightness(5.0), 0, "the driver holds its current above rated");
        assertEquals(0, N.amps(255, 0, 0, -4.2), 0, "wired backwards: nothing");
    }

    @Test
    void lightLevels() {
        assertEquals(15, LedRules.lightLevel(LedRules.pack(255, 10, 10), 1));
        assertEquals(0, LedRules.lightLevel(0, 1));
        assertEquals(8, LedRules.lightLevel(LedRules.pack(0, 0, 255), 0.5));
    }

    @Test
    void hsvToRgb() {
        assertEquals(LedRules.pack(255, 0, 0), LedRules.hsv(0, 1, 1));
        assertEquals(LedRules.pack(0, 255, 0), LedRules.hsv(120, 1, 1));
        assertEquals(LedRules.pack(0, 0, 255), LedRules.hsv(240, 1, 1));
        assertEquals(LedRules.pack(255, 255, 255), LedRules.hsv(42, 0, 1));
        assertEquals(LedRules.hsv(30, 1, 1), LedRules.hsv(390, 1, 1), "hue wraps");
    }

    @Test
    void ids() {
        assertEquals(1, LitBitIdRules.nextFree(Set.of(), 1));
        assertEquals(3, LitBitIdRules.nextFree(Set.of(1, 2, 4), 1));
        assertEquals(101, LitBitIdRules.after(100));
        assertEquals(1, LitBitIdRules.after(65_535), "wraps past the top");
        assertEquals(65_535, LitBitIdRules.clamp(99_999));
    }

    @Test
    void programs() {
        assertEquals("Untitled", Programs.name("  "));
        assertEquals("Chase 2", Programs.unique("Chase", List.of("Chase")));
        assertTrue(Programs.fits("x".repeat(30_000)));
        assertTrue(!Programs.fits("x".repeat(30_001)));
        assertEquals(32, Programs.name("y".repeat(50)).length());
    }
}
