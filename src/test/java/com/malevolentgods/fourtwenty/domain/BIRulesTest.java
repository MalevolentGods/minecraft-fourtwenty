package com.malevolentgods.fourtwenty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.malevolentgods.fourtwenty.domain.BIRules.Tier;
import org.junit.jupiter.api.Test;

class BIRulesTest {
    @Test
    void oneCharcoalAndOneBudGiveTheUsersTotals() {
        assertEquals(4_200, Tier.PUFF.total());
        assertEquals(8_400, Tier.TOKE.total());
        assertEquals(16_800, Tier.RIP.total());
        for (Tier t : Tier.values()) assertEquals(t.total(), t.rate() * BIRules.BURN_TICKS, 1e-9);
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
    void aWireCarriesTwoHookahs() {
        for (Tier t : Tier.values()) {
            assertFalse(BIRules.wireBurns(t, (long) (t.rate() * 2 * 20)), t.name());
            assertTrue(BIRules.wireBurns(t, (long) (t.rate() * 3 * 20)), t.name());
        }
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

    @Test
    void sharesAddUpToEverything() {
        assertEquals(11, BIRules.share(32, 3));
        assertEquals(32, BIRules.share(32, 1));
        assertEquals(0, BIRules.share(32, 0));
    }

    @Test
    void stashesSettleDownhill() {
        assertTrue(BIRules.flows(0.8, 0.5));
        assertFalse(BIRules.flows(0.5, 0.5));
        assertFalse(BIRules.flows(0.5, 0.8));
    }
}
