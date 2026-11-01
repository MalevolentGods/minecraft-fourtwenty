package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;
import java.util.OptionalInt;
import static org.junit.jupiter.api.Assertions.*;

class BongFuelTest {
    @Test void charcoalBurnsForAMinute() {
        assertEquals(1200, BongFuel.burnTicks(BongFuel.CHARCOAL).orElseThrow());
    }

    @Test void nothingElseIsFuel() {
        for (String id : new String[] {"minecraft:blaze_powder", "minecraft:flint_and_steel", "minecraft:lava_bucket", "minecraft:coal", "fourtwenty:weed_bud"}) {
            assertFalse(BongFuel.isFuel(id), id);
            assertEquals(OptionalInt.empty(), BongFuel.burnTicks(id), id);
        }
    }
}
