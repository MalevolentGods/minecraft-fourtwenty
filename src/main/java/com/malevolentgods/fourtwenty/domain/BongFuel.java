package com.malevolentgods.fourtwenty.domain;

import java.util.Map;
import java.util.OptionalInt;

/**
 * What can light a bong and how long one lighting burns, weakest to strongest:
 * flint and steel (costs durability) &lt; lava bucket (leaves an empty bucket) &lt; blaze powder.
 *
 * <p>Pure registry-id rules so they can be unit tested without a game.
 */
public final class BongFuel {
    public static final String FLINT_AND_STEEL = "minecraft:flint_and_steel";
    public static final String LAVA_BUCKET = "minecraft:lava_bucket";
    public static final String BLAZE_POWDER = "minecraft:blaze_powder";

    private static final Map<String, Integer> BURN_TICKS = Map.of(
            FLINT_AND_STEEL, 600,   // 30 s
            LAVA_BUCKET, 900,       // 45 s
            BLAZE_POWDER, 1200);    // 60 s, the original bong session

    /** Burn length in ticks for one lighting with {@code itemId}, or empty when it is not a bong fuel. */
    public static OptionalInt burnTicks(String itemId) {
        Integer ticks = BURN_TICKS.get(itemId);
        return ticks == null ? OptionalInt.empty() : OptionalInt.of(ticks);
    }

    public static boolean isFuel(String itemId) {
        return BURN_TICKS.containsKey(itemId);
    }

    private BongFuel() {}
}
