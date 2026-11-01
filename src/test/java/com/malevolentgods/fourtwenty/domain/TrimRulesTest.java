package com.malevolentgods.fourtwenty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrimRulesTest {
    private static final TrimRules.Chances C = TrimRules.Chances.DEFAULT;

    @Test
    void familiesGetRarer() {
        assertEquals(50, TrimRules.percent(C, Strain.LANDRACE));
        assertEquals(25, TrimRules.percent(C, Strain.COUCHLOCK_HUM));
        assertEquals(15, TrimRules.percent(C, Strain.CHERRY_PIE));
        assertEquals(10, TrimRules.percent(C, Strain.STICKY_ICKY));
        assertEquals(5, TrimRules.percent(C, Strain.BLUE_DREAM));
    }

    @Test
    void everyStrainHasAChance() {
        for (Strain s : Strain.values()) assertTrue(C.of(s.family()) > 0, s.name());
    }

    @Test
    void ratesHoldWithinThreePointsOverAThousandTrims() {
        var rng = new java.util.Random(420);
        for (Strain s : Strain.values()) {
            int seeds = 0;
            for (int i = 0; i < 1000; i++) if (TrimRules.seeded(C, s, rng.nextDouble())) seeds++;
            assertTrue(Math.abs(seeds / 1000.0 - C.of(s.family())) <= 0.03, s + ": " + seeds);
        }
    }

    @Test
    void rollIsBelowTheChance() {
        assertTrue(TrimRules.seeded(C, Strain.LANDRACE, 0.49));
        assertFalse(TrimRules.seeded(C, Strain.LANDRACE, 0.50));
        assertFalse(TrimRules.seeded(C, Strain.BLUE_DREAM, 0.05));
    }
}
