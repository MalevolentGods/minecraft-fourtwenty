package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrainStatsTest {
    private static final StrainStats S = StrainStats.DEFAULT;
    private static final double TOP = Math.nextDown(1.0);

    @Test void defaultsFollowTheSpecTable() {
        Map<Strain.Family, int[]> table = Map.of(
                Strain.Family.LANDRACE, new int[]{1, 1, 1}, Strain.Family.NEUTRALS, new int[]{1, 2, 2}, Strain.Family.WARM, new int[]{1, 2, 2},
                Strain.Family.GREEN, new int[]{2, 2, 2}, Strain.Family.COOL, new int[]{2, 2, 3}, Strain.Family.BIOME, new int[]{2, 2, 3},
                Strain.Family.CROSS, new int[]{2, 3, 3}, Strain.Family.DOUBLE_CROSS, new int[]{3, 3, 4}, Strain.Family.ULTRA, new int[]{3, 4, 4},
                Strain.Family.GOLDEN, new int[]{4, 5, 5});
        table.forEach((family, row) -> {
            assertEquals(row[0], S.yieldOf(family).min(), family + " min");
            assertEquals(row[1], S.yieldOf(family).max(), family + " max");
            assertEquals(row[2], S.potency(family), family + " potency");
        });
        for (int t = 1; t <= 5; t++) assertEquals(t / 10.0, S.plotSeedChance(t), 1e-12);
    }

    @Test void aYieldRollCoversItsRange() {
        for (Strain.Family f : Strain.Family.values()) {
            assertEquals(S.yieldOf(f).min(), S.rollYield(f, Traits.DEFAULT, 0), f + " at 0");
            assertEquals(S.yieldOf(f).max(), S.rollYield(f, Traits.DEFAULT, TOP), f + " just under 1");
        }
        assertEquals(4, S.rollYield(Strain.Family.GOLDEN, Traits.DEFAULT, 0.49));
        assertEquals(5, S.rollYield(Strain.Family.GOLDEN, Traits.DEFAULT, 0.5));
    }

    @Test void theYieldTraitAddsAndTakesButNeverBelowOne() {
        assertEquals(1, S.rollYield(Strain.Family.LANDRACE, new Traits(2, 1, 2), 0), "a poor yield trait on Landrace still gives a bud");
        assertEquals(4, S.rollYield(Strain.Family.LANDRACE, new Traits(2, 5, 2), 0), "the best yield trait adds 3");
        assertEquals(3, S.rollYield(Strain.Family.CROSS, new Traits(2, 3, 2), 0));
    }

    @Test void potencyStepsUpModestly() {
        assertEquals(0, StrainStats.potencyLevels(1));
        assertEquals(1.0, StrainStats.potencyLength(1));
        assertEquals(2, StrainStats.potencyLevels(5), "Golden God: at most two levels stronger");
        assertTrue(StrainStats.potencyLength(5) <= 2.0, "and at most twice as long");
        for (int p = 2; p <= 5; p++) {
            assertTrue(StrainStats.potencyLevels(p) >= StrainStats.potencyLevels(p - 1));
            assertTrue(StrainStats.potencyLength(p) > StrainStats.potencyLength(p - 1));
        }
    }

    @Test void plotsGiveExtraSeedsByTier() {
        assertFalse(S.extraSeed(0, 0), "no plot, no extra seed");
        assertTrue(S.extraSeed(1, 0.09));
        assertFalse(S.extraSeed(1, 0.10));
        assertTrue(S.extraSeed(5, 0.49));
        assertFalse(S.extraSeed(5, 0.5));
        assertFalse(S.extraSeed(6, 0));
    }

    @Test void aBadConfigIsClamped() {
        var y = new StrainStats.Yield(3, 1);
        assertEquals(3, y.max(), "max raised to min");
        assertEquals(StrainStats.MAX_YIELD, new StrainStats.Yield(99, 99).min());
        assertEquals(new StrainStats.Yield(2, 3), StrainStats.Yield.parse("2-3", null));
        assertEquals(new StrainStats.Yield(4, 4), StrainStats.Yield.parse("4", null));
        assertEquals(new StrainStats.Yield(1, 1), StrainStats.Yield.parse("junk", new StrainStats.Yield(1, 1)));
        var bad = new StrainStats(Map.of(), Map.of(Strain.Family.LANDRACE, 9), new double[]{-1, 2});
        assertEquals(5, bad.potency(Strain.Family.LANDRACE));
        assertEquals(0, bad.plotSeedChance(1));
        assertEquals(1, bad.plotSeedChance(2));
        assertEquals(StrainStats.DEFAULT.yieldOf(Strain.Family.CROSS), bad.yieldOf(Strain.Family.CROSS), "a missing family falls back to the default");
    }
}
