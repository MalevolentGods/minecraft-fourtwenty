package com.malevolentgods.fourtwenty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.malevolentgods.fourtwenty.domain.BIRules.Tier;
import org.junit.jupiter.api.Test;

class BIRulesTest {
    @Test
    void oneCharcoalAndOneBudGiveTheUsersTotals() {
        assertEquals(25_200, Tier.PUFF.total());
        assertEquals(50_400, Tier.TOKE.total());
        assertEquals(100_800, Tier.RIP.total());
        assertEquals(201_600, Tier.BLAZE.total());
        for (Tier t : Tier.values()) assertEquals(t.total(), t.rate() * BIRules.BURN_TICKS, 1e-9);
    }

    @Test
    void aHookahPutsOut420Willards() {
        assertEquals(420, BIRules.watts(Tier.PUFF.rate()), 1e-9);
        assertEquals(840, BIRules.watts(Tier.PUFF.wire()), 1e-9);
        assertEquals(105, BIRules.watts(Tier.PUFF.machine()), 1e-9);
        assertEquals(3_360, BIRules.watts(Tier.BLAZE.rate()), 1e-9);
    }

    @Test
    void everyTierBurnsForAMinute() {
        assertEquals(1200, BIRules.BURN_TICKS);
    }

    @Test
    void aHookahRunsFourMachinesOfItsTierForTheWholeBurn() {
        for (Tier t : Tier.values()) assertEquals(t.total(), t.machine() * 4 * BIRules.BURN_TICKS, 1e-9);
    }

    @Test
    void tierBelow() {
        assertEquals(Tier.TOKE, Tier.RIP.below());
        assertEquals(Tier.PUFF, Tier.PUFF.below());
    }

    @Test
    void onlyAHigherTierOverloads() {
        assertTrue(BIRules.overloads(Tier.PUFF, Tier.TOKE));
        assertTrue(BIRules.overloads(Tier.TOKE, Tier.RIP));
        assertFalse(BIRules.overloads(Tier.TOKE, Tier.TOKE));
        assertFalse(BIRules.overloads(Tier.RIP, Tier.PUFF));
    }
}
