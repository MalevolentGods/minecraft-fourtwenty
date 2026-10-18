package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import java.util.Map;
import java.util.Optional;

/**
 * Blocks a colony builder places as-is but pays for with a different, craftable item.
 *
 * <p>Froglights can only be obtained by letting a frog eat a small magma cube, so a builder
 * asking for one stalls an upgrade. Blueprints keep the real block; the builder is charged a
 * sea lantern instead. Pure registry-id rules so they can be unit tested without a game.
 */
public final class BuilderCostSubstitutions {
    private static final Map<String, String> COSTS = Map.of(
            "minecraft:verdant_froglight", "minecraft:sea_lantern",
            "minecraft:ochre_froglight", "minecraft:sea_lantern",
            "minecraft:pearlescent_froglight", "minecraft:sea_lantern");

    /** The item id a builder pays instead of {@code blockId}, or empty when the block costs itself. */
    public static Optional<String> costFor(String blockId) {
        return Optional.ofNullable(COSTS.get(blockId));
    }

    private BuilderCostSubstitutions() {}
}
