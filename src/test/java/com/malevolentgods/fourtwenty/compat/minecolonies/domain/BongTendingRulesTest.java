package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import com.malevolentgods.fourtwenty.domain.BongFuel;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class BongTendingRulesTest {
    @Test void fullBongNeedsNothing() {
        assertFalse(BongTendingRules.need(2, true, BongFuel.BLAZE_POWDER, true).any());
        assertFalse(BongTendingRules.need(1, true, BongFuel.FLINT_AND_STEEL, false).any());
    }
    @Test void budsAreOnlyAddedWhileServing() {
        // Set-up (no customers): water and fuel only, so the bong does not burn to an empty room.
        var setup = BongTendingRules.need(0, false, "minecraft:air", false);
        assertTrue(setup.water() && setup.fuel() && !setup.buds());
        var serving = BongTendingRules.need(0, true, BongFuel.BLAZE_POWDER, true);
        assertTrue(serving.buds() && !serving.water() && !serving.fuel());
    }
    @Test void emptyLavaBucketMustBeClearedAndReplaced() {
        var need = BongTendingRules.need(3, true, BongTendingRules.EMPTY_BUCKET, true);
        assertTrue(need.fuel() && need.clearBucket() && !need.buds() && !need.water());
    }
    @Test void bestAvailableFuelWins() {
        assertEquals(Optional.of(BongFuel.BLAZE_POWDER), BongTendingRules.bestFuel(Map.of(
                BongFuel.FLINT_AND_STEEL, 1, BongFuel.LAVA_BUCKET, 2, BongFuel.BLAZE_POWDER, 5)));
        assertEquals(Optional.of(BongFuel.LAVA_BUCKET), BongTendingRules.bestFuel(Map.of(
                BongFuel.FLINT_AND_STEEL, 1, BongFuel.LAVA_BUCKET, 1, BongFuel.BLAZE_POWDER, 0)));
        assertEquals(Optional.empty(), BongTendingRules.bestFuel(Map.of("minecraft:coal", 64)));
        assertEquals(4, BongTendingRules.fuelAmount(BongFuel.BLAZE_POWDER));
        assertEquals(1, BongTendingRules.fuelAmount(BongFuel.LAVA_BUCKET));
    }
    @Test void waterIsFreeSoAMissingWaterAlwaysJustifiesAVisit() {
        assertTrue(BongTendingRules.canFix(BongTendingRules.need(0, false, "minecraft:air", false), 0, Optional.empty()));
    }
    @Test void missingFuelOrBudsNeedStockOtherwiseTheyAreRequested() {
        var noFuel = BongTendingRules.need(2, true, "minecraft:air", true);
        assertFalse(BongTendingRules.canFix(noFuel, 10, Optional.empty()));
        assertTrue(BongTendingRules.canFix(noFuel, 0, Optional.of(BongFuel.FLINT_AND_STEEL)));
        var noBuds = BongTendingRules.need(0, true, BongFuel.BLAZE_POWDER, true);
        assertFalse(BongTendingRules.canFix(noBuds, 0, Optional.empty()));
        assertTrue(BongTendingRules.canFix(noBuds, 4, Optional.empty()));
        assertFalse(BongTendingRules.canFix(BongTendingRules.need(1, true, BongFuel.BLAZE_POWDER, true), 4, Optional.empty()));
    }
}
