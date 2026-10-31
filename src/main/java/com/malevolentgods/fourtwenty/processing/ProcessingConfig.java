package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.CureRules;
import com.malevolentgods.fourtwenty.domain.TrimRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings for trimming and curing. Before a world has loaded them (a tooltip on the title screen), the defaults apply. */
public final class ProcessingConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final TrimRules.Chances D = TrimRules.Chances.DEFAULT;
    private static final ModConfigSpec.DoubleValue LANDRACE = chance("seedChanceLandrace", D.landrace());
    private static final ModConfigSpec.DoubleValue NEUTRALS = chance("seedChanceNeutrals", D.neutrals());
    private static final ModConfigSpec.DoubleValue WARM = chance("seedChanceWarm", D.warm());
    private static final ModConfigSpec.DoubleValue GREEN = chance("seedChanceGreen", D.green());
    private static final ModConfigSpec.DoubleValue COOL = chance("seedChanceCool", D.cool());
    private static final ModConfigSpec.IntValue RACK_TICKS = BUILDER
            .comment("Ticks a bud hangs on a Curing Rack.")
            .defineInRange("curingRackTicks", CureRules.DEFAULTS.get(0).ticks(), 1, 1_728_000);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile TrimRules.Chances chancesOverride;
    private static volatile CureRules.Tier rackOverride;

    private ProcessingConfig() {}

    private static ModConfigSpec.DoubleValue chance(String name, double value) {
        return BUILDER.comment("Chance per trimmed bud of a seed.").defineInRange(name, value, 0, 1);
    }

    /** Game tests set exact numbers here; null goes back to the config. */
    public static void overrideChances(TrimRules.Chances chances) { chancesOverride = chances; }
    public static void overrideRack(CureRules.Tier tier) { rackOverride = tier; }

    public static TrimRules.Chances chances() {
        var override = chancesOverride;
        if (override != null) return override;
        return SPEC.isLoaded() ? new TrimRules.Chances(LANDRACE.get(), NEUTRALS.get(), WARM.get(), GREEN.get(), COOL.get()) : D;
    }

    public static CureRules.Tier rack() {
        var override = rackOverride;
        if (override != null) return override;
        CureRules.Tier base = CureRules.DEFAULTS.get(0);
        return SPEC.isLoaded() ? new CureRules.Tier(RACK_TICKS.get(), base.batch(), base.cost()) : base;
    }
}
