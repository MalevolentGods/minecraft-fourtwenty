package com.malevolentgods.fourtwenty.domain;

import java.awt.Color;

/**
 * Halo colours for lit lamps, matching ProjectRed's illumar lamps (MIT License, Copyright (c)
 * MrTJP; see licenses/ProjectRed-LICENSE.txt): a base RGBA per dye colour, re-lit at a per-colour
 * brightness, at alpha 0xA0. Indexed in dye colour order.
 */
public final class LampGlow {
    private static final int[] BASE_RGBA = {
            -96, -1067253600, -1256540768, 1870983584, -1078001504, 1810956448, -247040608, 1397969824,
            -1819044960, 8882080, 1577107616, 320061600, 1327956128, 143065248, -1576073568, 522133408};
    private static final float[] BRIGHTNESS = {
            0.9F, 0.8F, 0.8F, 0.9F, 0.8F, 0.9F, 0.9F, 0.5F, 0.6F, 0.8F, 0.8F, 0.8F, 0.5F, 0.7F, 0.8F, 0.4F};
    /** How far the halo extends past the lamp on every side, in blocks. */
    public static final double EXPAND = 0.05;

    /** The halo colour (0xRRGGBBAA) for dye colour index {@code index} (0 white .. 15 black). */
    public static int rgba(int index) {
        int base = BASE_RGBA[index];
        int r = base >> 24 & 0xFF, g = base >> 16 & 0xFF, b = base >> 8 & 0xFF, a = base & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        return Color.HSBtoRGB(hsb[0], hsb[1], BRIGHTNESS[index]) << 8 | a;
    }

    private LampGlow() {}
}
