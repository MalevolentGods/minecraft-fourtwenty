package com.malevolentgods.fourtwenty.domain.electronics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShownTest {
    @Test void packsAndUnpacks() {
        int s = Shown.pack(0x1A5, 11, true, false);
        assertEquals(0x1A5, Shown.bits(s));
        assertEquals(11, Shown.level(s));
        assertTrue(Shown.active(s));
        assertFalse(Shown.hot(s));
        assertTrue(Shown.bit(s, 0));
        assertFalse(Shown.bit(s, 1));
        int t = Shown.pack(0x3FF, 99, false, true);
        assertEquals(0x1FF, Shown.bits(t), "nine bits only");
        assertEquals(15, Shown.level(t), "levels clamp");
        assertTrue(Shown.hot(t));
        assertEquals(0, Shown.level(Shown.pack(0, -3, false, false)));
    }

    @Test void currentsAreOnALogScale() {
        assertEquals(0, Shown.amps(0));
        assertEquals(0, Shown.amps(5e-5));
        assertEquals(3, Shown.amps(1e-3));
        assertEquals(9, Shown.amps(-0.1), "either direction");
        assertEquals(15, Shown.amps(10));
        assertEquals(15, Shown.amps(1e6));
        assertEquals(0, Shown.amps(Double.NaN));
    }

    @Test void heatIsTwoThirdsAtTheRating() {
        assertEquals(0, Shown.heat(0, 10));
        assertEquals(10, Shown.heat(10, 10));
        assertEquals(15, Shown.heat(15, 10));
        assertEquals(15, Shown.heat(40, 10));
        assertEquals(0, Shown.heat(5, 0), "a part with no rating doesn't warm");
        assertEquals(15, Shown.share(2));
        assertEquals(8, Shown.share(0.5));
    }
}
