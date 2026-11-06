package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.LedRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings for Lit Bits and the Weedweeno. Before a world has loaded them, the defaults apply. */
public final class LightsConfig {
    private LightsConfig() {}

    private static final LedRules.Numbers D = LedRules.Numbers.DEFAULT;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue RANGE = BUILDER.comment("Weedweeno: how far it reaches Lit Bits, in blocks.").defineInRange("weedweenoRange", 64, 1, 512);
    private static final ModConfigSpec.IntValue BUDGET = BUILDER.comment("Weedweeno: the most time a show may take each tick, in microseconds.")
            .defineInRange("weedweenoBudgetMicros", 2_000, 100, 50_000);
    private static final ModConfigSpec.IntValue TOTAL = BUILDER.comment("Every Weedweeno together: the most time shows may take each tick, in microseconds.")
            .defineInRange("weedweenoTotalMicros", 10_000, 100, 200_000);
    private static final ModConfigSpec.IntValue PER_TICK = BUILDER.comment("Weedweeno: the most lamps one show may change each tick; more wait for the next tick.")
            .defineInRange("weedweenoLampsPerTick", 1_024, 1, 65_536);
    private static final ModConfigSpec.BooleanValue OPERATORS = BUILDER.comment("Weedweeno: only operators may edit and run programs.").define("weedweenoOperatorsOnly", false);
    private static final ModConfigSpec.DoubleValue FORWARD = BUILDER.comment("Lit Bit: its forward voltage, as a share of 4.2 Kb; below it, it's dark.")
            .defineInRange("litBitForwardShare", D.forwardShare(), 0.05, 0.99);
    private static final ModConfigSpec.DoubleValue FULL = BUILDER.comment("Lit Bit: where its driver reaches full current, as a share of 4.2 Kb.")
            .defineInRange("litBitFullShare", D.fullShare(), 0.06, 1.0);
    private static final ModConfigSpec.DoubleValue CHANNEL = BUILDER.comment("Lit Bit: the current of one channel at full, in Ignus.")
            .defineInRange("litBitChannelAmps", D.channelAmps(), 0.0001, 10);
    private static final ModConfigSpec.DoubleValue IDLE = BUILDER.comment("Lit Bit: its driver's idle current, in Ignus.").defineInRange("litBitIdleAmps", D.idleAmps(), 0, 1);
    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int range() { return SPEC.isLoaded() ? RANGE.get() : 64; }

    public static long budgetNanos() { return (SPEC.isLoaded() ? BUDGET.get() : 2_000) * 1_000L; }

    public static long totalNanos() { return (SPEC.isLoaded() ? TOTAL.get() : 10_000) * 1_000L; }

    public static int lampsPerTick() { return SPEC.isLoaded() ? PER_TICK.get() : 1_024; }

    public static boolean operatorsOnly() { return SPEC.isLoaded() && OPERATORS.get(); }

    public static LedRules.Numbers led() {
        if (!SPEC.isLoaded()) return D;
        return new LedRules.Numbers(D.ratedVolts(), FORWARD.get(), Math.max(FORWARD.get() + 0.01, FULL.get()), CHANNEL.get(), IDLE.get());
    }
}
