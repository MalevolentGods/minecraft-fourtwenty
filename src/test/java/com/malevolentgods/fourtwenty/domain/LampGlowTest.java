package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LampGlowTest {
    @Test void haloColoursMatchProjectRed() {
        // White: pure white re-lit at 0.9 brightness, alpha 0xA0.
        assertEquals(0xE6E6E6A0, LampGlow.rgba(0));   // 0.9 * 255 = 229.5, rounded to 230
        for (int i = 0; i < 16; i++) assertEquals(0xA0, LampGlow.rgba(i) & 0xFF, "halo alpha " + i);
        // Lime keeps a green hue; black is dim.
        int lime = LampGlow.rgba(5);
        assertTrue(((lime >> 16) & 0xFF) > ((lime >>> 24) & 0xFF), "lime is greener than red");
        assertEquals(0xC8, LampGlow.itemRgba(5) & 0xFF, "item halos are a bit stronger");
        assertEquals(LampGlow.rgba(5) & 0xFFFFFF00, LampGlow.itemRgba(5) & 0xFFFFFF00, "same colour for lime");
        int black = LampGlow.rgba(15);
        assertTrue(((black >>> 24) & 0xFF) < 0x70 && ((black >> 16) & 0xFF) < 0x70, "black glow is dim");
    }
}
