package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.GrowRules;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.CureRules;
import com.malevolentgods.fourtwenty.domain.TrimRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings for trimming and curing. Before a world has loaded them (a tooltip on the title screen), the defaults apply. */
public final class ProcessingConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final double TERPINATOR_DEFAULT = com.malevolentgods.fourtwenty.domain.BIRules.Tier.PUFF.machine();
    private static final int INFUSER_DEFAULT = 3_000, FIRST_POWERED = 1, TIERS = 4;
    private static final TrimRules.Chances D = TrimRules.Chances.DEFAULT;
    private static final ModConfigSpec.DoubleValue LANDRACE = chance("seedChanceLandrace", D.landrace());
    private static final ModConfigSpec.DoubleValue NEUTRALS = chance("seedChanceNeutrals", D.neutrals());
    private static final ModConfigSpec.DoubleValue WARM = chance("seedChanceWarm", D.warm());
    private static final ModConfigSpec.DoubleValue GREEN = chance("seedChanceGreen", D.green());
    private static final ModConfigSpec.DoubleValue COOL = chance("seedChanceCool", D.cool());
    private static final ModConfigSpec.DoubleValue BIOME = chance("seedChanceBiome", D.biome());
    private static final ModConfigSpec.DoubleValue CROSS = chance("seedChanceCross", D.cross());
    private static final ModConfigSpec.DoubleValue DOUBLE_CROSS = chance("seedChanceDoubleCross", D.doubleCross());
    private static final ModConfigSpec.DoubleValue ULTRA = chance("seedChanceUltra", D.ultra());
    private static final ModConfigSpec.DoubleValue GOLDEN = chance("seedChanceGolden", D.golden());
    private static final ModConfigSpec.DoubleValue POLLEN_CHANCE = BUILDER.comment("Chance per harvest that Double Cross parents side by side give their hybrid's seed.").defineInRange("pollenChance", 0.05, 0, 1);
    private static final ModConfigSpec.IntValue BOT_BUD_MINUTES = BUILDER.comment("How many minutes one Cured Bud runs a Buzz Bot.").defineInRange("botBudMinutes", 60, 1, 1440);
    private static final ModConfigSpec.IntValue BOT_CROSS_MINUTES = BUILDER.comment("How many minutes a Buzz Bot waits after one cross before the next.").defineInRange("botCrossMinutes", 5, 0, 1440);
    private static final ModConfigSpec.IntValue BOT_CHARGE_BI = BUILDER.comment("The Ws a Buzz Bot takes from a Stash for one bud's worth of power.").defineInRange("botChargeBI", 25_200, 1, 10_000_000);
    private static final ModConfigSpec.DoubleValue CLIMATE_CHANCE = BUILDER.comment("Chance per Landrace harvest in a climate biome of a climate-touched seed. A Cosmic Kush Pod's copy gives half.").defineInRange("climateChance", com.malevolentgods.fourtwenty.domain.Climate.CHANCE, 0, 1);
    private static final ModConfigSpec.DoubleValue CROSS_TIME = BUILDER.comment("Multiplies how long a Pheno Chamber's cross takes.").defineInRange("crossTime", 1.0, 0.01, 100);
    private static final ModConfigSpec.DoubleValue CROP_SPEED = BUILDER.comment("How fast weed grows: the share of random ticks that count.").defineInRange("cropSpeed", GrowRules.CROP_SPEED, 0.01, 1);
    private static final ModConfigSpec.DoubleValue[] GRADE_SPEEDS = {null, null, gradeSpeed(2), gradeSpeed(3), gradeSpeed(4), gradeSpeed(5)};
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
            .comment("Terpinator: Ws per tick while sober.")
            .defineInRange("terpinatorCost", TERPINATOR_DEFAULT, 0, 3_072);
    private static final ModConfigSpec.IntValue INFUSER_COST = BUILDER
            .comment("Inspiration Infuser: Ws per caught colour or infused seed while sober.")
            .defineInRange("infuserCost", INFUSER_DEFAULT, 0, 10_000_000);
    private static final ModConfigSpec.DoubleValue[] PLOT_SPEED = plotSpeeds();
    private static final ModConfigSpec.IntValue UNIT_TICKS = BUILDER
            .comment("Think Tank: ticks one study unit takes at speed 1. The University's pace is 1200, one unit a minute.")
            .defineInRange("studyUnitTicks", 1200, 1, 1_728_000);
    private static final ModConfigSpec.DoubleValue MAX_PLOT_SPEED = BUILDER
            .comment("Think Tank: the most that plots in range add up to.")
            .defineInRange("studyMaxPlotSpeed", 16.0, 1, 1000);
    private static final com.malevolentgods.fourtwenty.domain.PaveRules.Numbers PAVE = com.malevolentgods.fourtwenty.domain.PaveRules.Numbers.DEFAULT;
    private static final ModConfigSpec.IntValue PAVE_CAPACITY = BUILDER.comment("Pave Pen: the Ws it holds.").defineInRange("paveCapacity", PAVE.capacity(), 600, 10_000_000);
    private static final ModConfigSpec.DoubleValue PAVE_LAY_BASE = BUILDER.comment("Pave Pen: Ws to lay a road, before hardness.").defineInRange("paveLayCost", PAVE.layBase(), 0, 1_000_000);
    private static final ModConfigSpec.DoubleValue PAVE_LAY_HARDNESS = BUILDER.comment("Pave Pen: more Ws for each point of the block's hardness (dirt 0.5, stone 1.5, deepslate 3).")
            .defineInRange("paveLayCostPerHardness", PAVE.layPerHardness(), 0, 1_000_000);
    private static final ModConfigSpec.DoubleValue PAVE_TICKS_BASE = BUILDER.comment("Pave Pen: ticks to lay a road, before hardness.").defineInRange("paveTicks", PAVE.ticksBase(), 1, 1200);
    private static final ModConfigSpec.DoubleValue PAVE_TICKS_HARDNESS = BUILDER.comment("Pave Pen: more ticks for each point of hardness.").defineInRange("paveTicksPerHardness", PAVE.ticksPerHardness(), 0, 1200);
    private static final ModConfigSpec.DoubleValue PAVE_MAX_HARDNESS = BUILDER.comment("Pave Pen: the hardest block it paves over (obsidian is 50).").defineInRange("paveMaxHardness", PAVE.maxHardness(), 0, 100);
    private static final com.malevolentgods.fourtwenty.domain.CircuitRules.Numbers C = com.malevolentgods.fourtwenty.domain.CircuitRules.Numbers.DEFAULT;
    private static final ModConfigSpec.DoubleValue[] CIRCUIT = circuitValues();
    private static final ModConfigSpec.IntValue BURN_TICKS = BUILDER.comment("Circuit: ticks a Wick stays over a rating before it burns out.").defineInRange("circuit.burnTicks", C.burnTicks(), 1, 1200);
    // specs/012-progression-balance: each family's rough buds per harvest ("min-max") and potency, and the Grow Op plots' extra seeds
    private static final com.malevolentgods.fourtwenty.domain.StrainStats STATS = com.malevolentgods.fourtwenty.domain.StrainStats.DEFAULT;
    private static final java.util.Map<Strain.Family, ModConfigSpec.ConfigValue<String>> YIELDS = familyValues(f -> BUILDER
            .comment("Rough buds a harvest of a " + f.name() + " strain gives before seed traits, as min-max (rolled each harvest).")
            .define("yield" + key(f), STATS.yieldOf(f).toString()));
    private static final java.util.Map<Strain.Family, ModConfigSpec.IntValue> POTENCY = familyValues(f -> BUILDER
            .comment("Potency of " + f.name() + " strains, 1 to 5: each step makes a high a little stronger and longer (1 is plain).")
            .defineInRange("potency" + key(f), STATS.potency(f), 1, 5));
    private static final ModConfigSpec.DoubleValue[] PLOT_SEED = plotSeeds();
    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile TrimRules.Chances chancesOverride;
    private static volatile CureRules.Tier rackOverride;
    private static volatile Integer unitTicksOverride;
    private static volatile Integer crossTicksOverride;
    private static volatile Double pollenChanceOverride;
    private static volatile Double climateChanceOverride;
    private static volatile Integer botChargeOverride;

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
        for (int i = FIRST_POWERED; i < TIERS; i++) out[i] = BUILDER.comment("Tier " + (i + 1) + ": Ws per tick.").defineInRange(name + (i + 1), value.applyAsDouble(i), 0, 3_072);
        return out;
    }

    private static ModConfigSpec.DoubleValue gradeSpeed(int grade) {
        return BUILDER.comment("How fast grade " + grade + " strains grow.").defineInRange("gradeSpeed" + grade, GrowRules.gradeSpeed(grade), 0, 1);
    }

    private static ModConfigSpec.DoubleValue chance(String name, double value) {
        return BUILDER.comment("Chance per trimmed bud of a seed.").defineInRange(name, value, 0, 1);
    }

    /** Game tests set exact numbers here; null goes back to the config. */
    public static void overrideChances(TrimRules.Chances chances) { chancesOverride = chances; }
    public static void overrideRack(CureRules.Tier tier) { rackOverride = tier; }
    public static void overrideUnitTicks(Integer ticks) { unitTicksOverride = ticks; }
    public static void overrideCrossTicks(Integer ticks) { crossTicksOverride = ticks; }
    public static void overridePollenChance(Double chance) { pollenChanceOverride = chance; }

    /** How long one Cured Bud runs a Buzz Bot. */
    public static int botBudTicks() { return (SPEC.isLoaded() ? BOT_BUD_MINUTES.get() : 60) * 1200; }

    public static int botCrossTicks() { return (SPEC.isLoaded() ? BOT_CROSS_MINUTES.get() : 5) * 1200; }

    public static void overrideBotCharge(Integer bi) { botChargeOverride = bi; }

    public static int botChargeBI() {
        var override = botChargeOverride;
        if (override != null) return override;
        return SPEC.isLoaded() ? BOT_CHARGE_BI.get() : 25_200;
    }

    public static void overrideClimateChance(Double chance) { climateChanceOverride = chance; }

    public static double climateChance() {
        var override = climateChanceOverride;
        if (override != null) return override;
        return SPEC.isLoaded() ? CLIMATE_CHANCE.get() : com.malevolentgods.fourtwenty.domain.Climate.CHANCE;
    }

    public static double pollenChance() {
        var override = pollenChanceOverride;
        if (override != null) return override;
        return SPEC.isLoaded() ? POLLEN_CHANCE.get() : 0.05;
    }

    /** How long a cross of this grade takes. */
    public static int crossTicks(int grade) {
        var override = crossTicksOverride;
        if (override != null) return override;
        return (int) Math.ceil(com.malevolentgods.fourtwenty.domain.Crosses.ticks(grade) * (SPEC.isLoaded() ? CROSS_TIME.get() : 1));
    }

    private static String key(Strain.Family f) {
        StringBuilder out = new StringBuilder();
        for (String part : f.name().toLowerCase(java.util.Locale.ROOT).split("_")) out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        return out.toString();
    }

    private static <V> java.util.Map<Strain.Family, V> familyValues(java.util.function.Function<Strain.Family, V> make) {
        java.util.Map<Strain.Family, V> out = new java.util.EnumMap<>(Strain.Family.class);
        for (Strain.Family f : Strain.Family.values()) out.put(f, make.apply(f));
        return out;
    }

    private static ModConfigSpec.DoubleValue[] plotSeeds() {
        ModConfigSpec.DoubleValue[] out = new ModConfigSpec.DoubleValue[com.malevolentgods.fourtwenty.domain.StrainStats.PLOT_TIERS];
        for (int t = 1; t <= out.length; t++) {
            out[t - 1] = BUILDER.comment("Grow Op tier " + t + ": chance of an extra seed at each harvest on it.")
                    .defineInRange("plotSeedChance" + t, STATS.plotSeedChance(t), 0, 1);
        }
        return out;
    }

    private static volatile com.malevolentgods.fourtwenty.domain.StrainStats statsOverride;

    public static void overrideStrainStats(com.malevolentgods.fourtwenty.domain.StrainStats stats) { statsOverride = stats; }

    /** Each family's yield and potency and the plots' extra seeds, from the config (the defaults before it has loaded). */
    public static com.malevolentgods.fourtwenty.domain.StrainStats strainStats() {
        var override = statsOverride;
        if (override != null) return override;
        if (!SPEC.isLoaded()) return STATS;
        java.util.Map<Strain.Family, com.malevolentgods.fourtwenty.domain.StrainStats.Yield> yields = new java.util.EnumMap<>(Strain.Family.class);
        java.util.Map<Strain.Family, Integer> potency = new java.util.EnumMap<>(Strain.Family.class);
        for (Strain.Family f : Strain.Family.values()) {
            yields.put(f, com.malevolentgods.fourtwenty.domain.StrainStats.Yield.parse(YIELDS.get(f).get(), STATS.yieldOf(f)));
            potency.put(f, POTENCY.get(f).get());
        }
        double[] plots = new double[PLOT_SEED.length];
        for (int i = 0; i < plots.length; i++) plots[i] = PLOT_SEED[i].get();
        return new com.malevolentgods.fourtwenty.domain.StrainStats(yields, potency, plots);
    }

    public static TrimRules.Chances chances() {
        var override = chancesOverride;
        if (override != null) return override;
        return SPEC.isLoaded() ? new TrimRules.Chances(LANDRACE.get(), NEUTRALS.get(), WARM.get(), GREEN.get(), COOL.get(),
                BIOME.get(), CROSS.get(), DOUBLE_CROSS.get(), ULTRA.get(), GOLDEN.get()) : D;
    }

    /** The share of random ticks weed grows on, for every strain, in a pot or on a plot too. */
    /** The Pave Pen's numbers; upgrading and reshaping keep their defaults. */
    public static com.malevolentgods.fourtwenty.domain.PaveRules.Numbers pave() {
        return SPEC.isLoaded() ? new com.malevolentgods.fourtwenty.domain.PaveRules.Numbers(PAVE_CAPACITY.get(), PAVE_LAY_BASE.get(), PAVE_LAY_HARDNESS.get(),
                PAVE_TICKS_BASE.get(), PAVE_TICKS_HARDNESS.get(), PAVE_MAX_HARDNESS.get(), PAVE.upgradeCost(), PAVE.upgradeTicks(), PAVE.reshapeCost(), PAVE.reshapeTicks()) : PAVE;
    }

    public static double cropSpeed() { return SPEC.isLoaded() ? CROP_SPEED.get() : GrowRules.CROP_SPEED; }

    /** How fast a strain of this grade grows; grades 0 and 1 aren't slowed. */
    public static double gradeSpeed(Strain strain) {
        int grade = strain.grade();
        return GRADE_SPEEDS[grade] != null && SPEC.isLoaded() ? GRADE_SPEEDS[grade].get() : GrowRules.gradeSpeed(grade);
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

    private static ModConfigSpec.DoubleValue[] circuitValues() {
        var c = C;
        Object[][] values = {
                {"voltsPuff", "Nominal Kerberos of Puff.", c.voltsPuff(), 0.001, 1e9}, {"voltsToke", "Nominal Kerberos of Toke.", c.voltsToke(), 0.001, 1e9},
                {"voltsRip", "Nominal Kerberos of Rip.", c.voltsRip(), 0.001, 1e9}, {"voltsBlaze", "Nominal Kerberos of Blaze.", c.voltsBlaze(), 0.001, 1e9},
                {"sourceDroop", "A Hookah's internal resistance, per unit of its tier's base resistance.", c.sourceDroop(), 1e-6, 1},
                {"sourceLimit", "A Hookah's current limit, per unit of its rated current.", c.sourceLimit(), 0.01, 100},
                {"stashEmfLow", "A Stash's voltage empty, per unit of nominal.", c.stashEmfLow(), 0.1, 1.5},
                {"stashEmfHigh", "A Stash's voltage full, per unit of nominal.", c.stashEmfHigh(), 0.1, 1.5},
                {"stashResistance", "A Stash's internal resistance, per unit.", c.stashResistance(), 1e-6, 1},
                {"stashLimit", "A Stash's current limit each way, per unit.", c.stashLimit(), 0.01, 100},
                {"loadRegulation", "A machine holds its power from this share of its tier's voltage up; below it, it is a plain resistance, R = V² / P.", c.loadMinVolts(), 0.01, 1.5},
                {"loadHysteresis", "The share of its buffer a machine uses before it asks for power again.", c.loadHysteresis(), 0, 1},
                {"wickOhmsPerBlock", "A Wick block's resistance, per unit of its tier's base resistance.", c.wickOhmsPerBlock(), 0, 1},
                {"wickAmpRating", "A Wick's current rating, per unit of its tier's Hookah current.", c.wickAmpRating(), 0.01, 1000},
                {"wickVoltRating", "A Wick's insulation rating, times its tier's voltage.", c.wickVoltRating(), 1, 1000},
                {"overloadMargin", "A block overloads above this times its tier's voltage.", c.overloadMargin(), 1, 1000},
                {"hitBoxLoss", "The share of power lost through a Hit Box.", c.hitBoxLoss(), 0, 0.99}};
        var out = new ModConfigSpec.DoubleValue[values.length];
        for (int i = 0; i < values.length; i++) {
            Object[] v = values[i];
            out[i] = BUILDER.comment("Circuit: " + v[1]).defineInRange("circuit." + v[0], ((Number) v[2]).doubleValue(), ((Number) v[3]).doubleValue(), ((Number) v[4]).doubleValue());
        }
        return out;
    }

    private static volatile com.malevolentgods.fourtwenty.domain.CircuitRules.Numbers circuitOverride;

    /** Game tests set exact circuit numbers here; null goes back to the config. */
    public static void overrideCircuit(com.malevolentgods.fourtwenty.domain.CircuitRules.Numbers numbers) { circuitOverride = numbers; }

    /** The circuit's numbers: from the config once a world has loaded it, else the defaults. */
    public static com.malevolentgods.fourtwenty.domain.CircuitRules.Numbers circuit() {
        var override = circuitOverride;
        if (override != null) return override;
        if (!SPEC.isLoaded()) return C;
        double[] d = new double[CIRCUIT.length];
        for (int i = 0; i < d.length; i++) d[i] = CIRCUIT[i].get();
        return new com.malevolentgods.fourtwenty.domain.CircuitRules.Numbers(d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9], d[10], d[11],
                d[12], d[13], d[14], BURN_TICKS.get(), d[15], d[16]);
    }
}
