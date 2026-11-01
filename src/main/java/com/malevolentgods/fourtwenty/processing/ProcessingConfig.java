package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.CureRules;
import com.malevolentgods.fourtwenty.domain.TrimRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings for trimming and curing. Before a world has loaded them (a tooltip on the title screen), the defaults apply. */
public final class ProcessingConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final double TERPINATOR_DEFAULT = com.malevolentgods.fourtwenty.domain.BIRules.Tier.PUFF.machine();
    private static final int INFUSER_DEFAULT = 500, FIRST_POWERED = 1, TIERS = 4;
    private static final TrimRules.Chances D = TrimRules.Chances.DEFAULT;
    private static final ModConfigSpec.DoubleValue LANDRACE = chance("seedChanceLandrace", D.landrace());
    private static final ModConfigSpec.DoubleValue NEUTRALS = chance("seedChanceNeutrals", D.neutrals());
    private static final ModConfigSpec.DoubleValue WARM = chance("seedChanceWarm", D.warm());
    private static final ModConfigSpec.DoubleValue GREEN = chance("seedChanceGreen", D.green());
    private static final ModConfigSpec.DoubleValue COOL = chance("seedChanceCool", D.cool());
    private static final ModConfigSpec.IntValue RACK_TICKS = BUILDER
            .comment("Ticks a bud hangs on a Curing Rack.")
            .defineInRange("curingRackTicks", CureRules.DEFAULTS.get(0).ticks(), 1, 1_728_000);
    private static final ModConfigSpec.IntValue[] TRIM_TICKS = tiers("trimTicks", "Ticks per bud.", i -> TrimRules.DEFAULTS.get(i).ticks());
    private static final ModConfigSpec.DoubleValue[] TRIM_COST = costs("trimCost", i -> TrimRules.DEFAULTS.get(i).cost());
    private static final ModConfigSpec.IntValue[] CURE_TICKS = tiers("cureTicks", "Ticks per batch.", i -> CureRules.DEFAULTS.get(i).ticks());
    private static final ModConfigSpec.IntValue[] CURE_BATCH = tiers("cureBatch", "Buds per batch.", i -> CureRules.DEFAULTS.get(i).batch());
    private static final ModConfigSpec.DoubleValue[] CURE_COST = costs("cureCost", i -> CureRules.DEFAULTS.get(i).cost());
    private static final ModConfigSpec.IntValue HIGH_SPEED = BUILDER
            .comment("How fast a machine runs free for a high player, in percent of its speed on BI.")
            .defineInRange("highSpeedPercent", 100, 1, 1000);
    private static final ModConfigSpec.DoubleValue TERPINATOR_COST = BUILDER
            .comment("Terpinator: BI per tick while sober.")
            .defineInRange("terpinatorCost", TERPINATOR_DEFAULT, 0, 512);
    private static final ModConfigSpec.IntValue INFUSER_COST = BUILDER
            .comment("Inspiration Infuser: BI per caught colour or infused seed while sober.")
            .defineInRange("infuserCost", INFUSER_DEFAULT, 0, 1_000_000);
    private static final ModConfigSpec.DoubleValue[] PLOT_SPEED = plotSpeeds();
    private static final ModConfigSpec.IntValue UNIT_TICKS = BUILDER
            .comment("Think Tank: ticks one study unit takes at speed 1. The University's pace is 1200, one unit a minute.")
            .defineInRange("studyUnitTicks", 1200, 1, 1_728_000);
    private static final ModConfigSpec.DoubleValue MAX_PLOT_SPEED = BUILDER
            .comment("Think Tank: the most that plots in range add up to.")
            .defineInRange("studyMaxPlotSpeed", 16.0, 1, 1000);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile TrimRules.Chances chancesOverride;
    private static volatile CureRules.Tier rackOverride;
    private static volatile Integer unitTicksOverride;

    private ProcessingConfig() {}

    private static ModConfigSpec.IntValue[] tiers(String name, String comment, java.util.function.IntUnaryOperator value) {
        var out = new ModConfigSpec.IntValue[TIERS];
        for (int i = FIRST_POWERED; i < TIERS; i++) out[i] = BUILDER.comment("Tier " + (i + 1) + ": " + comment).defineInRange(name + (i + 1), value.applyAsInt(i), 1, 1_728_000);
        return out;
    }

    private static ModConfigSpec.DoubleValue[] plotSpeeds() {
        var out = new ModConfigSpec.DoubleValue[com.malevolentgods.fourtwenty.domain.GrowRules.SPEEDS.size()];
        for (int i = 0; i < out.length; i++) out[i] = BUILDER.comment("Grow Op tier " + (i + 1) + ": how many times as fast a crop grows.").defineInRange("plotSpeed" + (i + 1), com.malevolentgods.fourtwenty.domain.GrowRules.SPEEDS.get(i), 1, 100);
        return out;
    }

    private static ModConfigSpec.DoubleValue[] costs(String name, java.util.function.IntToDoubleFunction value) {
        var out = new ModConfigSpec.DoubleValue[TIERS];
        for (int i = FIRST_POWERED; i < TIERS; i++) out[i] = BUILDER.comment("Tier " + (i + 1) + ": BI per tick.").defineInRange(name + (i + 1), value.applyAsDouble(i), 0, 512);
        return out;
    }

    private static ModConfigSpec.DoubleValue chance(String name, double value) {
        return BUILDER.comment("Chance per trimmed bud of a seed.").defineInRange(name, value, 0, 1);
    }

    /** Game tests set exact numbers here; null goes back to the config. */
    public static void overrideChances(TrimRules.Chances chances) { chancesOverride = chances; }
    public static void overrideRack(CureRules.Tier tier) { rackOverride = tier; }
    public static void overrideUnitTicks(Integer ticks) { unitTicksOverride = ticks; }

    public static TrimRules.Chances chances() {
        var override = chancesOverride;
        if (override != null) return override;
        return SPEC.isLoaded() ? new TrimRules.Chances(LANDRACE.get(), NEUTRALS.get(), WARM.get(), GREEN.get(), COOL.get()) : D;
    }

    /** A powered curing tier, by index: 1 to 3 for tiers 2 to 4. */
    public static CureRules.Tier cure(int index) {
        return SPEC.isLoaded() ? new CureRules.Tier(CURE_TICKS[index].get(), CURE_BATCH[index].get(), CURE_COST[index].get()) : CureRules.DEFAULTS.get(index);
    }

    /** A powered trimming tier, by index: 1 to 3 for tiers 2 to 4. */
    public static TrimRules.Tier trim(int index) {
        return SPEC.isLoaded() ? new TrimRules.Tier(TRIM_TICKS[index].get(), TRIM_COST[index].get()) : TrimRules.DEFAULTS.get(index);
    }

    /** A plot's speed, by tier from 1. */
    public static double plotSpeed(int level) {
        int i = Math.max(1, Math.min(level, PLOT_SPEED.length)) - 1;
        return SPEC.isLoaded() ? PLOT_SPEED[i].get() : com.malevolentgods.fourtwenty.domain.GrowRules.SPEEDS.get(i);
    }

    public static int unitTicks() {
        var override = unitTicksOverride;
        if (override != null) return override;
        return SPEC.isLoaded() ? UNIT_TICKS.get() : 1200;
    }

    public static double maxPlotSpeed() { return SPEC.isLoaded() ? MAX_PLOT_SPEED.get() : 16; }

    public static int highSpeedPercent() { return SPEC.isLoaded() ? HIGH_SPEED.get() : 100; }

    public static double terpinatorCost() { return SPEC.isLoaded() ? TERPINATOR_COST.get() : TERPINATOR_DEFAULT; }

    public static int infuserCost() { return SPEC.isLoaded() ? INFUSER_COST.get() : INFUSER_DEFAULT; }

    public static CureRules.Tier rack() {
        var override = rackOverride;
        if (override != null) return override;
        CureRules.Tier base = CureRules.DEFAULTS.get(0);
        return SPEC.isLoaded() ? new CureRules.Tier(RACK_TICKS.get(), base.batch(), base.cost()) : base;
    }
}
