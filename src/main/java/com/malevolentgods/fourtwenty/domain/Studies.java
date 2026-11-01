package com.malevolentgods.fourtwenty.domain;

import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Think Tank studies. A new study is one more line in {@link #ALL}. Length copies the University: 72 units at tier 1, doubling each tier.
 * Studies always succeed: a bad batch only means working that unit again.
 */
public final class Studies {
    private Studies() {}

    public static final int BASE_UNITS = 72, MAX_TIER = 6;
    public static final double BAD_BATCH_CHANCE = 0.1;

    /** {@code items} are registry ids, with a count; three at most, as the Think Tank has three slots. */
    public record Input(String item, int count) {}

    public record Study(String id, int tier, List<Input> inputs, String colour) {
        public int units() { return Studies.units(tier); }
    }

    public static int units(int tier) {
        return BASE_UNITS << (Math.max(1, Math.min(tier, MAX_TIER)) - 1);
    }

    /** Whether a unit just finished was a bad batch; {@code roll} is uniform in 0..1. */
    public static boolean badBatch(double roll) {
        return roll < BAD_BATCH_CHANCE;
    }

    private static final List<Input> COLOUR_INPUTS = List.of(new Input("fourtwenty:weed_bud", 8), new Input("fourtwenty:weed_seeds", 4), new Input("minecraft:glass_bottle", 1));

    public static final List<Study> ALL = build();

    private static List<Study> build() {
        List<Study> out = new ArrayList<>();
        for (DyeColor colour : DyeColor.values()) out.add(new Study("colour_" + colour.getSerializedName(), 1, COLOUR_INPUTS, colour.getSerializedName()));
        return List.copyOf(out);
    }
}
