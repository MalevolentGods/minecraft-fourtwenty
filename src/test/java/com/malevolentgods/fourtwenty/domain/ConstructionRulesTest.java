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
    void betterBenchesCostMore() {
        assertTrue(ConstructionRules.cost(BIRules.Tier.PUFF) > 0);
        assertTrue(ConstructionRules.cost(BIRules.Tier.BLAZE) > ConstructionRules.cost(BIRules.Tier.RIP));
    }
}
