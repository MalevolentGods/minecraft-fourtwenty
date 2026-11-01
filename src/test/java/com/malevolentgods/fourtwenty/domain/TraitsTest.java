package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitsTest {
    private static DoubleSupplier always(double roll) { return () -> roll; }

    @Test
    void aTwoChangesNothing() {
        assertEquals(0, Traits.DEFAULT.potencyBonus());
        assertEquals(0, Traits.DEFAULT.yieldBonus());
        assertEquals(1.0, Traits.DEFAULT.lengthFactor());
    }

    @Test
    void aChildIsTheAverageWithNoDrift() {
        Traits child = Traits.cross(new Traits(1, 3, 5), new Traits(3, 5, 5), always(0.5));
        assertEquals(new Traits(2, 4, 5), child);
    }

    @Test
    void driftMovesEachTraitByOne() {
        Traits parent = new Traits(3, 3, 3);
        assertEquals(new Traits(2, 2, 2), Traits.cross(parent, parent, always(0.1)));
        assertEquals(new Traits(4, 4, 4), Traits.cross(parent, parent, always(0.9)));
    }

    @Test
    void ratingsStayInRange() {
        assertEquals(new Traits(1, 1, 1), Traits.cross(new Traits(1, 1, 1), new Traits(1, 1, 1), always(0.1)));
        assertEquals(new Traits(5, 5, 5), Traits.cross(new Traits(5, 5, 5), new Traits(5, 5, 5), always(0.9)));
    }

    @Test
    void anOddSumRoundsEitherWay() {
        double[] rolls = {0.4, 0.5, 0.6, 0.5, 0.4, 0.5};
        int[] next = {0};
        Traits child = Traits.cross(new Traits(2, 2, 2), new Traits(3, 3, 3), () -> rolls[next[0]++ % rolls.length]);
        assertEquals(3, child.potency());
        assertEquals(2, child.yield());
        assertEquals(3, child.length());
    }

    @Test
    void theLadderClimbs() {
        for (Strain s : Strain.values()) assertTrue(GrowRules.gradeSpeed(s.grade()) <= 1, s.name());
        assertEquals(0, Strain.LANDRACE.grade());
        assertEquals(1, Strain.BLUE_DREAM.grade());
        assertEquals(1, Strain.DUST_DEVIL.grade());
        assertEquals(2, Strain.MOSS_BOSS.grade());
        assertEquals(3, Strain.OLD_GROWTH.grade());
        assertEquals(4, Strain.THE_ELDER_BUD.grade());
        assertEquals(5, Strain.GOLDEN_GOD.grade());
        assertEquals(39, Strain.values().length);
        assertEquals(0, GrowRules.gradePlot(1));
        assertEquals(1, GrowRules.gradePlot(2));
        assertEquals(3, GrowRules.gradePlot(3));
        assertEquals(4, GrowRules.gradePlot(4));
        assertEquals(5, GrowRules.gradePlot(5));
    }
}
