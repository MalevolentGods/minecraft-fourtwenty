package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import java.util.Map;
import java.util.Optional;

/**
 * Blocks a colony builder places as-is but pays for with a different, craftable item.
 *
 * <p>Froglights can only be obtained by letting a frog eat a small magma cube, so a builder
 * asking for one stalls an upgrade; the builder is charged a sea lantern instead. The Domum
 * Ornamentum architect's cutter is a workstation the Sensi Heights Builder's Hut hands out early,
 * so it costs a single cobblestone. A Potted Pot Pot only comes from planting a seed in a Pot Pot,
 * so it costs the Pot Pot of its colour (the seed is free). Blueprints keep the real block. Pure registry-id rules so they
 * can be unit tested without a game.
 */
public final class BuilderCostSubstitutions {
    private static final Map<String, String> COSTS = Map.of(
            "minecraft:verdant_froglight", "minecraft:sea_lantern",
            "minecraft:ochre_froglight", "minecraft:sea_lantern",
            "minecraft:pearlescent_froglight", "minecraft:sea_lantern",
            "domum_ornamentum:architectscutter", "minecraft:cobblestone");

    private static final String POTTED = "fourtwenty:potted_pot_pot_";

    /** The item id a builder pays instead of {@code blockId}, or empty when the block costs itself. */
    public static Optional<String> costFor(String blockId) {
        if (blockId.startsWith(POTTED)) return Optional.of("fourtwenty:pot_pot_" + blockId.substring(POTTED.length()));
        return Optional.ofNullable(COSTS.get(blockId));
    }

    private BuilderCostSubstitutions() {}
}
