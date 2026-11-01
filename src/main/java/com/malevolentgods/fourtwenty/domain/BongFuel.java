package com.malevolentgods.fourtwenty.domain;

import java.util.OptionalInt;

/**
 * What lights a Hookah: one coal burns for a minute, on every tier.
 *
 * <p>Pure registry-id rules so they can be unit tested without a game.
 */
public final class BongFuel {
    public static final String COAL = "minecraft:coal";

    /** Burn length in ticks for one lighting with {@code itemId}, or empty when it is not a Hookah fuel. */
    public static OptionalInt burnTicks(String itemId) {
        return isFuel(itemId) ? OptionalInt.of(BIRules.BURN_TICKS) : OptionalInt.empty();
    }

    public static boolean isFuel(String itemId) {
        return COAL.equals(itemId);
    }

    private BongFuel() {}
}
