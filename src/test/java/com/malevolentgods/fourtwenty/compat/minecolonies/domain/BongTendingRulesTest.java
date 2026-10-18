package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import com.malevolentgods.fourtwenty.domain.BongFuel;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class BongTendingRulesTest {
    @Test void fullBongNeedsNothing() {
        assertFalse(BongTendingRules.need(2, true, BongFuel.BLAZE_POWDER).any());
        assertFalse(BongTendingRules.need(1, true, BongFuel.FLINT_AND_STEEL).any());
    }
    @Test void missingInputsAreReported() {
        var need = BongTendingRules.need(0, false, "minecraft:air");
        assertTrue(need.buds() && need.water() && need.fuel() && !need.clearBucket());
    }
    @Test void emptyLavaBucketMustBeClearedAndReplaced() {
        var need = BongTendingRules.need(3, true, BongTendingRules.EMPTY_BUCKET);
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
    @Test void onlyWalkOverWhenTheBongCouldLightAfterwards() {
        var noFuel = BongTendingRules.need(2, true, "minecraft:air");
        assertFalse(BongTendingRules.canFix(noFuel, 10, true, Optional.empty()));
        assertTrue(BongTendingRules.canFix(noFuel, 0, false, Optional.of(BongFuel.FLINT_AND_STEEL)));
        var empty = BongTendingRules.need(0, false, BongFuel.BLAZE_POWDER);
        assertFalse(BongTendingRules.canFix(empty, 4, false, Optional.empty()));
        assertTrue(BongTendingRules.canFix(empty, 4, true, Optional.empty()));
        assertFalse(BongTendingRules.canFix(BongTendingRules.need(1, true, BongFuel.BLAZE_POWDER), 4, true, Optional.empty()));
    }
}
