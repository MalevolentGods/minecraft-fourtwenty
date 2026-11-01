package com.malevolentgods.fourtwenty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.malevolentgods.fourtwenty.domain.BIRules.Tier;
import org.junit.jupiter.api.Test;

class BIRulesTest {
    @Test
    void tierRates() {
        assertEquals(32, Tier.PUFF.rate());
        assertEquals(128, Tier.TOKE.rate());
        assertEquals(512, Tier.RIP.rate());
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
