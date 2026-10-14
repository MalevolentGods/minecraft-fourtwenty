package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.DemandRules;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class DispensaryConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue INTERVAL = duration("demandIntervalTicks");
    private static final ModConfigSpec.IntValue GRACE = duration("demandGraceTicks");
    private static final ModConfigSpec.IntValue HEALTH = duration("healthDurationTicks");
    public static final ModConfigSpec SPEC = BUILDER.build();
    private static ModConfigSpec.IntValue duration(String name) {
        return BUILDER.comment("Elapsed simulation ticks. Changes affect future deadlines; existing deadlines are preserved.")
                .defineInRange(name, 24_000, 1, DemandRules.MAX_TICKS);
    }
    public static DemandRules rules() { return new DemandRules(INTERVAL.get(), GRACE.get(), HEALTH.get()); }
    private DispensaryConfig() {}
}
