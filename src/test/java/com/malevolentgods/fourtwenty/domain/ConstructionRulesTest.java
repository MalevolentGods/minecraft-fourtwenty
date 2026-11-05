package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructionRulesTest {
    @Test
    void betterBenchesMakeMore() {
        assertEquals(2, ConstructionRules.factor(BIRules.Tier.PUFF));
        assertEquals(3, ConstructionRules.factor(BIRules.Tier.TOKE));
        assertEquals(4, ConstructionRules.factor(BIRules.Tier.RIP));
        assertEquals(6, ConstructionRules.factor(BIRules.Tier.BLAZE));
    }

    @Test
    void aBatchStopsAtAFullStack() {
        assertEquals(8, ConstructionRules.batch(4, 64, BIRules.Tier.PUFF));
        assertEquals(64, ConstructionRules.batch(16, 64, BIRules.Tier.BLAZE));
    }

    @Test
    void aCraftCostsTheSameForEveryItemItMakes() {
        assertEquals(0, ConstructionRules.cost(0));
        assertEquals(ConstructionRules.BI_PER_ITEM, ConstructionRules.cost(1));
        assertEquals(64L * ConstructionRules.BI_PER_ITEM, ConstructionRules.cost(64));
    }

    @Test
    void theReserveHoldsFourStacks() {
        assertEquals(4 * ConstructionRules.cost(64), ConstructionRules.CAPACITY);
    }

    @Test
    void lowerBenchesChargeSlower() {
        BIRules.Tier[] tiers = BIRules.Tier.values();
        for (int i = 1; i < tiers.length; i++) assertTrue(ConstructionRules.chargeRate(tiers[i - 1]) < ConstructionRules.chargeRate(tiers[i]), tiers[i].name());
        assertEquals(420, ConstructionRules.chargeRate(BIRules.Tier.PUFF) * BIRules.TICKS_PER_SECOND, 1e-9);
        for (BIRules.Tier t : tiers) assertTrue(ConstructionRules.intake(t) >= ConstructionRules.chargeRate(t), t.name());
    }
}
