package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The Weed College's tunable numbers (specs/017-weed-college; constitution Principle VI). Server config, fourtwenty-college-server.toml. */
public final class CollegeConfig {
    private CollegeConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue UNIT_TICKS = BUILDER
            .comment("Working ticks a Weedologist spends on one unit of study (one cycle: plot, Think Tank, desk). 1200 is the University's pace.")
            .defineInRange("unitTicks", 1200, 20, 72_000);
    private static final ModConfigSpec.DoubleValue BAD_BASE = BUILDER
            .comment("Chance a unit is a bad batch (worked again), before the Weedologist's Intelligence lowers it.")
            .defineInRange("badBatchChance", 0.1, 0, 0.9);
    private static final ModConfigSpec.DoubleValue BAD_FLOOR = BUILDER
            .comment("The lowest the bad-batch chance can go, whatever the Intelligence.")
            .defineInRange("badBatchFloor", 0.03, 0, 0.9);
    private static final ModConfigSpec.DoubleValue BAD_STEP = BUILDER
            .comment("How much each point of Intelligence lowers the bad-batch chance.")
            .defineInRange("badBatchPerIntelligence", 0.001, 0, 0.1);
    private static final ModConfigSpec.DoubleValue KNOWN_FACTOR = BUILDER
            .comment("Share of a colour's supplies a colour study still takes when a colony owner or officer already knows that colour.")
            .defineInRange("knownColourFactor", 0.5, 0, 1);
    private static final ModConfigSpec.IntValue SEEDS_PER_PLOT = BUILDER
            .comment("Weed seeds the College keeps (and asks the colony for) for each of its plots, to keep them planted.")
            .defineInRange("seedsPerPlot", 1, 0, 16);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean loaded() { return SPEC.isLoaded(); }

    public static int unitTicks() { return loaded() ? UNIT_TICKS.get() : 1200; }
    public static double badBase() { return loaded() ? BAD_BASE.get() : 0.1; }
    public static double badFloor() { return loaded() ? BAD_FLOOR.get() : 0.03; }
    public static double badStep() { return loaded() ? BAD_STEP.get() : 0.001; }
    public static double knownFactor() { return loaded() ? KNOWN_FACTOR.get() : 0.5; }
    public static int seedsPerPlot() { return loaded() ? SEEDS_PER_PLOT.get() : 1; }
}
