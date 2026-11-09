package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnitsTest {
    @Test
    void threeSignificantFigures() {
        assertEquals("4.20 Kb", Units.format(4.2, Units.VOLTS));
        assertEquals("100 Ig", Units.format(100, Units.AMPS));
        assertEquals("25.0 Ig", Units.format(25, Units.AMPS));
        assertEquals("105 W", Units.format(105, Units.WATTS));
    }

    @Test
    void smallValuesTakeAPrefix() {
        assertEquals("168 mΦ", Units.format(0.168, Units.OHMS));
        assertEquals("6.30 µΦ", Units.format(6.3e-6, Units.OHMS));
        assertEquals("800 mIg", Units.format(0.8, Units.AMPS));
        assertEquals("1.00 Φ", Units.format(0.99996, Units.OHMS));
    }

    @Test
    void largeValuesTakeAPrefixPast9999() {
        assertEquals("4,200 Kb", Units.format(4_200, Units.VOLTS));
        assertEquals("25.2 kWs", Units.format(25_200, Units.WS));
        assertEquals("252 MWs", Units.format(252_000_000, Units.WS));
    }

    @Test
    void zeroAndNegatives() {
        assertEquals("0 Ig", Units.format(0, Units.AMPS));
        assertEquals("-1.50 Ig", Units.format(-1.5, Units.AMPS));
    }

    /** Below the smallest prefix is rounding noise: it reads 0, not a string of zeros. */
    @org.junit.jupiter.api.Test void tooSmallForAPrefixIsZero() {
        org.junit.jupiter.api.Assertions.assertEquals("0 Ig", Units.format(1.67e-25, "Ig"));
        org.junit.jupiter.api.Assertions.assertEquals("0 W", Units.format(-7.2e-30, "W"));
        org.junit.jupiter.api.Assertions.assertEquals("1.11 nKb", Units.format(1.11e-9, "Kb"));
    }
}
