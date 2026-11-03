package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.PaveRules.Action;
import com.malevolentgods.fourtwenty.domain.PaveRules.Shape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaveRulesTest {
    private static final PaveRules.Numbers N = PaveRules.Numbers.DEFAULT;

    @Test
    void anythingElseIsLaidAndRoadsOnlyClimb() {
        assertEquals(Action.LAY, PaveRules.action(0, Shape.FULL, 1, Shape.FULL));
        assertEquals(Action.UPGRADE, PaveRules.action(1, Shape.FULL, 2, Shape.FULL));
        assertEquals(Action.NONE, PaveRules.action(3, Shape.FULL, 1, Shape.FULL));
        assertEquals(3, PaveRules.resultTier(3, 1));
    }

    @Test
    void onlyAProChangesShape() {
        assertEquals(Shape.SLAB, PaveRules.wanted(false, Shape.STAIRS, 2, Shape.SLAB));
        assertEquals(Shape.FULL, PaveRules.wanted(false, Shape.STAIRS, 0, Shape.FULL));
        assertEquals(Shape.STAIRS, PaveRules.wanted(true, Shape.STAIRS, 2, Shape.SLAB));
        assertEquals(Action.RESHAPE, PaveRules.action(2, Shape.FULL, 2, Shape.SLAB));
    }

    @Test
    void dirtIsQuickAndStoneTakesLonger() {
        int dirt = PaveRules.ticks(Action.LAY, 0.5, N), stone = PaveRules.ticks(Action.LAY, 1.5, N), deepslate = PaveRules.ticks(Action.LAY, 3, N);
        assertTrue(dirt < stone && stone < deepslate, dirt + " " + stone + " " + deepslate);
        assertTrue(dirt <= 10, "dirt in half a second");
        assertTrue(PaveRules.cost(Action.LAY, 0.5, N) < PaveRules.cost(Action.LAY, 3, N));
    }

    @Test
    void unbreakableAndVeryHardBlocksStay() {
        assertFalse(PaveRules.layable(-1, N));
        assertFalse(PaveRules.layable(50, N));
        assertTrue(PaveRules.layable(0.5, N));
    }

    @Test
    void eachTierTakesItsRecipesMaterial() {
        assertFalse(PaveRules.needsStone(1, 1));
        assertTrue(PaveRules.needsStone(1, 2) && !PaveRules.needsGold(1, 2));
        assertTrue(PaveRules.needsStone(1, 3) && PaveRules.needsGold(1, 3));
        assertTrue(!PaveRules.needsStone(2, 3) && PaveRules.needsGold(2, 3));
        assertEquals(1, PaveRules.startTier(0));
    }

    @Test
    void aCuredBudPavesAboutTwentyDirtBlocks() {
        int jobs = PaveRules.jobs(PaveRules.CURED, PaveRules.cost(Action.LAY, 0.5, N));
        assertTrue(jobs >= 16 && jobs <= 32, "" + jobs);
    }
}
