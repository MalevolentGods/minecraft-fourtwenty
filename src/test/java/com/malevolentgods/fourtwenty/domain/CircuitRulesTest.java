package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.BIRules.Tier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitRulesTest {
    private static final CircuitRules.Numbers R = CircuitRules.Numbers.DEFAULT;

    @Test
    void theTierBases() {
        assertEquals(100, R.baseAmps(Tier.PUFF), 1e-9);
        assertEquals(20, R.baseAmps(Tier.TOKE), 1e-9);
        assertEquals(4, R.baseAmps(Tier.RIP), 1e-9);
        assertEquals(0.8, R.baseAmps(Tier.BLAZE), 1e-9);
        assertEquals(0.042, R.baseOhms(Tier.PUFF), 1e-12);
        assertEquals(5_250, R.baseOhms(Tier.BLAZE), 1e-9);
    }

    @Test
    void wicksAreRealCables() {
        assertEquals(200, R.wickAmps(Tier.PUFF), 1e-9);
        assertEquals(40, R.wickAmps(Tier.TOKE), 1e-9);
        assertEquals(8, R.wickAmps(Tier.RIP), 1e-9);
        assertEquals(1.6, R.wickAmps(Tier.BLAZE), 1e-9);
        assertEquals(6.3, R.wickVolts(Tier.PUFF), 1e-9);
        assertEquals(6.3e-6, R.wickOhms(Tier.PUFF), 1e-15);
        assertEquals(0.7875, R.wickOhms(Tier.BLAZE), 1e-12);
    }

    @Test
    void aHigherTierOverloadsAPuffBlock() {
        assertTrue(R.volts(Tier.TOKE) > R.overloadVolts(Tier.PUFF));
        assertTrue(R.volts(Tier.PUFF) * 1.03 < R.overloadVolts(Tier.PUFF), "a healthy line never comes near it");
    }

    @Test
    void aStashsVoltageFollowsItsCharge() {
        assertEquals(0.96 * 4.2, R.stashEmf(Tier.PUFF, 0), 1e-12);
        assertEquals(0.99 * 4.2, R.stashEmf(Tier.PUFF, 1), 1e-12);
        assertTrue(R.stashEmf(Tier.PUFF, 1) < R.volts(Tier.PUFF), "a Hookah always charges a full Stash's line");
    }

    @Test
    void loadsWaitUntilTheyveUsedSomeOfTheirBuffer() {
        assertTrue(CircuitRules.asks(false, 0.5, 0.1));
        assertFalse(CircuitRules.asks(false, 0.95, 0.1), "still nearly full");
        assertTrue(CircuitRules.asks(true, 0.95, 0.1), "keeps asking until full");
        assertFalse(CircuitRules.asks(true, 1, 0.1));
    }

    @Test
    void aSnipStationOnPuff() {
        double watts = CircuitRules.ratedWatts(Tier.PUFF.machine());
        assertEquals(105, watts, 1e-9);
        assertEquals(0.168, R.volts(Tier.PUFF) * R.volts(Tier.PUFF) / watts, 1e-12);
    }
}
