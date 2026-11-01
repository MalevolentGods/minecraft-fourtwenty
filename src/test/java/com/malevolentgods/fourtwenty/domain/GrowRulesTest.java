package com.malevolentgods.fourtwenty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrowRulesTest {
    @Test
    void theFivePlotsAreThePlansSpeeds() {
        assertEquals(java.util.List.of(1.5, 2.0, 3.0, 5.0, 8.0), GrowRules.SPEEDS);
    }

    @Test
    void extraTicksMakeUpTheDifference() {
        double perTick = 3.0 / GrowRules.RANDOM_TICK_ODDS;
        assertEquals(perTick * 7, GrowRules.extraTickChance(8, 3), 1e-12);
        assertEquals(0, GrowRules.extraTickChance(1, 3), 1e-12);
        assertTrue(GrowRules.extraTickChance(8, 3) < GrowRules.extraTickChance(8, 30));
    }

    @Test
    void bigHarvestIsHalfAsManyAgainRoundedUp() {
        assertEquals(2, GrowRules.bigHarvest(1));
        assertEquals(3, GrowRules.bigHarvest(2));
        assertEquals(5, GrowRules.bigHarvest(3));
    }
}
