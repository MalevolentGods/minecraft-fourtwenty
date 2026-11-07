package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GearRulesTest {
    @Test
    void theApprovedCapacities() {
        assertEquals(600_000, Piece.HOTBOX_HELM.capacity);
        assertEquals(2_400_000, Piece.PUFF_PLATE.capacity);
        assertEquals(1_200_000, Piece.COUCHLOCK_GREAVES.capacity);
        assertEquals(600_000, Piece.MOON_BOOTS.capacity);
        assertEquals(6_000_000, Piece.BLAZER.capacity);
    }

    @Test
    void powersNeedTheHighAndTheCharge() {
        assertTrue(GearRules.powered(true, 500, 200));
        assertFalse(GearRules.powered(false, 500, 200), "sober");
        assertFalse(GearRules.powered(true, 100, 200), "short");
        assertFalse(GearRules.powered(true, 0, 0), "empty, even for a free power");
    }

    @Test
    void spendingAndChargingStayInBounds() {
        assertEquals(300, GearRules.spend(500, 200));
        assertEquals(0, GearRules.spend(100, 200));
        assertEquals(50, GearRules.chargeStep(99_950, 100_000, 1000), "never past full");
        assertEquals(0, GearRules.chargeStep(100_000, 100_000, 1000));
        assertEquals(672.0, GearRules.stationRate(BIRules.Tier.BLAZE), 1e-9);
        for (int i = 1; i < BIRules.Tier.values().length; i++)
            assertEquals(2 * GearRules.stationRate(BIRules.Tier.values()[i - 1]), GearRules.stationRate(BIRules.Tier.values()[i]), 1e-9);
    }

    @Test
    void aStompHitsHarderFromHigher() {
        assertFalse(GearRules.stomps(5, false), "only crouching");
        assertFalse(GearRules.stomps(1, true), "not from a hop");
        assertTrue(GearRules.stompDamage(10, 12) > GearRules.stompDamage(3, 12));
        assertEquals(12, GearRules.stompDamage(40, 12), 1e-6);
    }

    @Test
    void flightEndsSafely() {
        assertTrue(GearRules.keepFlying(true, 1000, 5, true));
        assertFalse(GearRules.keepFlying(false, 1000, 5, true), "sober");
        assertFalse(GearRules.keepFlying(true, 0, 5, true), "empty");
        assertFalse(GearRules.keepFlying(true, 1000, 5, false), "Plate off");
        assertTrue(GearRules.needsSoftLanding(false, true));
        assertFalse(GearRules.needsSoftLanding(true, true), "already down");
        assertFalse(GearRules.needsSoftLanding(false, false), "flight we never lent isn't ours to land");
    }

    @Test
    void everyGearPieceHasAFamilyAndSlots() {
        for (Piece p : Piece.values()) {
            if (p == Piece.DAB_CELL) continue;
            assertTrue(p.family != null && p.slots >= 3, p.name());
        }
    }

    @Test
    void aStationFillsGentlyAndChargesFast() {
        for (BIRules.Tier t : BIRules.Tier.values()) {
            assertTrue(GearRules.stationInput(t) < t.wire(), "fills inside its Wick's rating: " + t);
            assertTrue(GearRules.stationRate(t) > GearRules.stationInput(t), "charges faster than it fills: " + t);
        }
        assertEquals(1_008_000, GearRules.stationCapacity(BIRules.Tier.PUFF, 40));
        assertEquals(84.0, GearRules.stationRate(BIRules.Tier.PUFF), 1e-9, "1,680 W on Puff");
    }
}
