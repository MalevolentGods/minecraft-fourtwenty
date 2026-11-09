package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** The resistor colour code on a Resinator's bands. */
class ResistorCodeTest {
    private static final int BLACK = 0x1E1E1E, BROWN = 0x7A4A21, RED = 0xD02020, ORANGE = 0xF08020, YELLOW = 0xF0D020, GREEN = 0x30A040,
            BLUE = 0x2050D0, VIOLET = 0x8040C0, GREY = 0x808080, GOLD = 0xD4AF37;

    private static int[] bands(double ohms) { return new int[]{ResistorCode.band(ohms, 0), ResistorCode.band(ohms, 1), ResistorCode.band(ohms, 2)}; }

    @Test void theColourCodeReadsTheValue() {
        assertArrayEquals(new int[]{BROWN, BLACK, RED}, bands(1000), "1 kΦ: brown black red");
        assertArrayEquals(new int[]{YELLOW, VIOLET, RED}, bands(4700), "4.7 kΦ: yellow violet red");
        assertArrayEquals(new int[]{BROWN, BLACK, GOLD}, bands(1), "1 Φ: brown black gold");
        assertArrayEquals(new int[]{BROWN, GREEN, GOLD}, bands(1.5), "1.5 Φ: brown green gold");
        assertArrayEquals(new int[]{RED, RED, BLACK}, bands(22), "22 Φ: red red black");
        assertArrayEquals(new int[]{BLUE, GREY, ORANGE}, bands(68_000), "68 kΦ: blue grey orange");
        assertArrayEquals(new int[]{BROWN, BLACK, GREEN}, bands(1_000_000), "1 MΦ: brown black green");
        assertArrayEquals(new int[]{ORANGE, ORANGE, BROWN}, bands(330), "330 Φ: orange orange brown");
    }
}
