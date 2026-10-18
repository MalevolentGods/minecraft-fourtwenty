package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;
import java.util.OptionalInt;
import static org.junit.jupiter.api.Assertions.*;

class BongFuelTest {
    @Test void fuelsBurnLongerFromFlintAndSteelToLavaToBlazePowder() {
        int flint = BongFuel.burnTicks(BongFuel.FLINT_AND_STEEL).orElseThrow();
        int lava = BongFuel.burnTicks(BongFuel.LAVA_BUCKET).orElseThrow();
        int blaze = BongFuel.burnTicks(BongFuel.BLAZE_POWDER).orElseThrow();
        assertTrue(flint < lava && lava < blaze);
        // Blaze powder keeps the original 60 second session.
        assertEquals(1200, blaze);
    }
    @Test void otherItemsAreNotFuel() {
        for (String id : new String[] {"minecraft:bucket", "minecraft:water_bucket", "minecraft:coal", "fourtwenty:weed_bud"}) {
            assertFalse(BongFuel.isFuel(id), id);
            assertEquals(OptionalInt.empty(), BongFuel.burnTicks(id), id);
        }
    }
}
