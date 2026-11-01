package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import com.malevolentgods.fourtwenty.domain.BongFuel;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class BongTendingRulesTest {
    @Test void fullBongNeedsNothing() {
        assertFalse(BongTendingRules.need(2, true, BongFuel.CHARCOAL, true).any());
        assertFalse(BongTendingRules.need(1, true, BongFuel.CHARCOAL, false).any());
    }
    @Test void budsAreOnlyAddedWhileServing() {
        // Set-up (no customers): water and fuel only, so the bong does not burn to an empty room.
        var setup = BongTendingRules.need(0, false, "minecraft:air", false);
        assertTrue(setup.water() && setup.fuel() && !setup.buds());
        var serving = BongTendingRules.need(0, true, BongFuel.CHARCOAL, true);
        assertTrue(serving.buds() && !serving.water() && !serving.fuel());
    }
    @Test void oldFuelsMustBeClearedAndReplaced() {
        var need = BongTendingRules.need(3, true, "minecraft:blaze_powder", true);
        assertTrue(need.fuel() && need.clearBucket() && !need.buds() && !need.water());
        assertFalse(BongTendingRules.need(3, true, "minecraft:air", true).clearBucket());
    }
    @Test void charcoalIsTheFuel() {
        assertEquals(Optional.of(BongFuel.CHARCOAL), BongTendingRules.bestFuel(Map.of(BongFuel.CHARCOAL, 5)));
        assertEquals(Optional.empty(), BongTendingRules.bestFuel(Map.of("minecraft:blaze_powder", 64)));
        assertEquals(4, BongTendingRules.fuelAmount(BongFuel.CHARCOAL));
    }
    @Test void oneSessionPerTenMinutes() {
        assertTrue(BongTendingRules.sessionDue(5, -1));
        assertFalse(BongTendingRules.sessionDue(11_999, 0));
        assertTrue(BongTendingRules.sessionDue(12_000, 0));
    }
    @Test void waterIsFreeSoAMissingWaterAlwaysJustifiesAVisit() {
        assertTrue(BongTendingRules.canFix(BongTendingRules.need(0, false, "minecraft:air", false), 0, Optional.empty()));
    }
    @Test void missingFuelOrBudsNeedStockOtherwiseTheyAreRequested() {
        var noFuel = BongTendingRules.need(2, true, "minecraft:air", true);
        assertFalse(BongTendingRules.canFix(noFuel, 10, Optional.empty()));
        assertTrue(BongTendingRules.canFix(noFuel, 0, Optional.of(BongFuel.CHARCOAL)));
        var noBuds = BongTendingRules.need(0, true, BongFuel.CHARCOAL, true);
        assertFalse(BongTendingRules.canFix(noBuds, 0, Optional.empty()));
        assertTrue(BongTendingRules.canFix(noBuds, 4, Optional.empty()));
        assertFalse(BongTendingRules.canFix(BongTendingRules.need(1, true, BongFuel.CHARCOAL, true), 4, Optional.empty()));
    }
}
