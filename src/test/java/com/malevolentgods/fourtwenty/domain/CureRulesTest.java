package com.malevolentgods.fourtwenty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CureRulesTest {
    private static final CureRules.Tier RACK = CureRules.DEFAULTS.get(0);

    @Test
    void theRackTakesFiveMinutesAndNoPower() {
        assertEquals(6_000, RACK.ticks());
        assertEquals(8, RACK.batch());
        assertEquals(0, RACK.cost());
    }

    @Test
    void eachTierIsFasterAndHoldsMore() {
        for (int i = 1; i < CureRules.DEFAULTS.size(); i++) {
            assertTrue(CureRules.DEFAULTS.get(i).ticks() < CureRules.DEFAULTS.get(i - 1).ticks());
            assertTrue(CureRules.DEFAULTS.get(i).batch() > CureRules.DEFAULTS.get(i - 1).batch());
        }
    }

    @Test
    void curedOnceTheTimeIsUp() {
        assertFalse(CureRules.cured(5_999, RACK));
        assertTrue(CureRules.cured(6_000, RACK));
    }

    @Test
    void minutesLeftRoundUp() {
        assertEquals(5, CureRules.minutesLeft(0, RACK));
        assertEquals(1, CureRules.minutesLeft(5_999, RACK));
        assertEquals(0, CureRules.minutesLeft(6_000, RACK));
    }
}
