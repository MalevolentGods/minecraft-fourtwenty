package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.DemandRules;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class DispensaryConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue INTERVAL = duration("demandIntervalTicks");
    private static final ModConfigSpec.IntValue GRACE = duration("demandGraceTicks");
    private static final ModConfigSpec.IntValue HEALTH = duration("healthDurationTicks");
    private static final ModConfigSpec.DoubleValue BONUS_WEIGHT = BUILDER
            .comment("Native weighted happiness contribution at factor 1.1, not flat happiness points. Zero disables the bonus.")
            .defineInRange("satisfactionWeight", 0.1, 0, 1);
    private static final ModConfigSpec.DoubleValue PENALTY_WEIGHT = BUILDER
            .comment("Native weighted happiness contribution at factor 0, not flat happiness points. Zero disables the penalty.")
            .defineInRange("unmetDemandWeight", 0.1, 0, 1);
    private static final ModConfigSpec.DoubleValue BONG_WEIGHT = BUILDER
            .comment("Bong session: native weighted happiness contribution at factor 1.1 for colonists near a burning bong. Zero disables it.")
            .defineInRange("bongSessionWeight", 0.1, 0, 1);
    private static final ModConfigSpec.IntValue BONG_RADIUS = BUILDER
            .comment("Bong session: blocks from a burning bong within which colonists get the bonus.")
            .defineInRange("bongSessionRadius", 6, 1, 32);
    private static final ModConfigSpec.IntValue BONG_TICKS = BUILDER
            .comment("Bong session: elapsed simulation ticks the bonus lasts after a colonist was last near a burning bong.")
            .defineInRange("bongSessionTicks", 6_000, 20, DemandRules.MAX_TICKS);
    private static final ModConfigSpec.DoubleValue CHANGE_CHANCE = BUILDER
            .comment("Affinity: chance that a dose (or the day's first bong session) shifts a colonist's frequency one step.")
            .defineInRange("affinityChangeChance", 0.10, 0, 1);
    private static final ModConfigSpec.DoubleValue UP_CHANCE = BUILDER
            .comment("Affinity: when frequency shifts, the chance it goes up rather than down.")
            .defineInRange("affinityUpChance", 0.75, 0, 1);
    private static final ModConfigSpec.IntValue BREAK_TICKS = BUILDER
            .comment("Affinity: simulation ticks of the tolerance break taken on reaching the top step (default two days).")
            .defineInRange("toleranceBreakTicks", 48_000, 20, DemandRules.MAX_TICKS);
    private static final ModConfigSpec.DoubleValue BREAK_WEIGHT = BUILDER
            .comment("Affinity: native weighted happiness contribution at factor 0.5 during a tolerance break. Zero disables the debuff.")
            .defineInRange("toleranceBreakWeight", 0.1, 0, 1);
    private static final ModConfigSpec.IntValue SECONDHAND_RADIUS = BUILDER
            .comment("Blocks from someone smoking a joint or pipe within which colonists count as exposed for the first time.")
            .defineInRange("secondhandSmokeRadius", 4, 0, 32);
    private static final java.util.Map<com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem, ModConfigSpec.IntValue> PRICES = prices();
    public static final ModConfigSpec SPEC = BUILDER.build();
    private static java.util.Map<com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem, ModConfigSpec.IntValue> prices() {
        var prices = new java.util.EnumMap<com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem, ModConfigSpec.IntValue>(
                com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem.class);
        for (var item : com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem.values()) {
            String name = item.name().toLowerCase(java.util.Locale.ROOT);
            prices.put(item, BUILDER.comment("Emeralds a player without colony permission pays the Budtender for one " + name + ". Colony members are served free.")
                    .defineInRange(name + "Price", item.defaultPrice, 0, 64));
        }
        return prices;
    }
    /** Native colony tests pin drift (null = use the config). Never set in normal play. */
    private static volatile com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.Tuning tuningOverride;
    public static void overrideTuning(com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.Tuning tuning) { tuningOverride = tuning; }
    public static com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.Tuning tuning() {
        var override = tuningOverride;
        if (override != null) return override;
        return new com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.Tuning(CHANGE_CHANCE.get(), UP_CHANCE.get(), BREAK_TICKS.get());
    }
    public static double toleranceBreakWeight() { return BREAK_WEIGHT.get(); }
    public static int secondhandSmokeRadius() { return SECONDHAND_RADIUS.get(); }
    public static int price(com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem item) { return PRICES.get(item).get(); }
    private static ModConfigSpec.IntValue duration(String name) {
        return BUILDER.comment("Elapsed simulation ticks. Changes affect future deadlines; existing deadlines are preserved.")
                .defineInRange(name, 24_000, 1, DemandRules.MAX_TICKS);
    }
    public static DemandRules rules() { return new DemandRules(INTERVAL.get(), GRACE.get(), HEALTH.get()); }
    public static double satisfactionWeight() { return BONUS_WEIGHT.get(); }
    public static double unmetDemandWeight() { return PENALTY_WEIGHT.get(); }
    public static double bongSessionWeight() { return BONG_WEIGHT.get(); }
    public static int bongSessionRadius() { return BONG_RADIUS.get(); }
    public static int bongSessionTicks() { return BONG_TICKS.get(); }
    private DispensaryConfig() {}
}
