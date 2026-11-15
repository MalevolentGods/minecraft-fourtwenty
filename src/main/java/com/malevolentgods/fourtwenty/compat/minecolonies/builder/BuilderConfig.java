package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/** fourtwenty-builders-server.toml (specs/013-lifted-builders/contracts/config.md). */
public final class BuilderConfig {
    private BuilderConfig() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // [diagnostics]
    private static final ModConfigSpec.BooleanValue LOG_PATHING;
    // [watchdog]
    private static final ModConfigSpec.BooleanValue WATCHDOG;
    private static final ModConfigSpec.IntValue SLOW_SECONDS;
    private static final ModConfigSpec.DoubleValue SLOW_FRACTION;
    private static final ModConfigSpec.IntValue STUCK_SECONDS;
    private static final ModConfigSpec.IntValue RESCUE_RADIUS;
    private static final ModConfigSpec.BooleanValue FIX_STEP_ASIDE;
    // [lifted]
    private static final ModConfigSpec.BooleanValue LIFTED;
    private static final Map<String, ModConfigSpec.DoubleValue> MINUTES = new LinkedHashMap<>();
    private static final ModConfigSpec.DoubleValue POTENCY_BONUS;
    private static final ModConfigSpec.DoubleValue SPEED_LOWEST;
    private static final ModConfigSpec.DoubleValue SPEED_HIGHEST;
    private static final ModConfigSpec.IntValue REACH;
    private static final ModConfigSpec.IntValue FLIGHT_RADIUS;
    private static final ModConfigSpec.DoubleValue LANDING_SPEED;
    private static final ModConfigSpec.IntValue REQUEST_INTERVAL;
    private static final ModConfigSpec.IntValue HOVER_LOOKAHEAD;
    private static final ModConfigSpec.IntValue HOVER_GRID;
    // [music]
    private static final ModConfigSpec.BooleanValue MUSIC;
    private static final ModConfigSpec.DoubleValue FALLBACK_BPM;
    private static final ModConfigSpec.DoubleValue DANCE_BOB;
    private static final ModConfigSpec.IntValue SMOKE_SECONDS;
    private static final ModConfigSpec.BooleanValue BODY_FLIGHT;
    private static final ModConfigSpec.BooleanValue LIFTED_LOOK;
    private static final ModConfigSpec.DoubleValue SPIN_COOLDOWN;

    /** Lifted minutes per joint by strain family (the keys match {@code StrainStats} family names). */
    public static final Map<String, Double> DEFAULT_MINUTES = Map.of(
            "Landrace", 5.0, "Neutrals", 5.5, "Warm", 5.5, "Green", 6.0, "Cool", 6.0, "Biome", 6.0,
            "Cross", 7.0, "DoubleCross", 8.0, "Ultra", 9.0, "Golden", 10.0);
    private static final String[] FAMILIES = {"Landrace", "Neutrals", "Warm", "Green", "Cool", "Biome", "Cross", "DoubleCross", "Ultra", "Golden"};

    static {
        B.push("diagnostics");
        LOG_PATHING = B.comment("Log each Builder's route searches, step-asides, standing-spot changes and time between placements.")
                .define("logBuilderPathing", false);
        B.pop();

        B.push("watchdog");
        WATCHDOG = B.comment("Rescue Builders stuck or crawling while they build (never while eating, sleeping, fetching or in a raid).")
                .define("enabled", true);
        SLOW_SECONDS = B.comment("Window over which a slow pace is judged.").defineInRange("slowSeconds", 20, 5, 300);
        SLOW_FRACTION = B.comment("Below this share of her expected placing rate counts as slow.").defineInRange("slowFraction", 0.33, 0.05, 0.9);
        STUCK_SECONDS = B.comment("No progress for this long while building counts as stuck; also the time between escalation steps.")
                .defineInRange("stuckSeconds", 45, 10, 600);
        RESCUE_RADIUS = B.comment("How far from her next block a rescue looks for a standing spot.").defineInRange("rescueRadius", 4, 1, 16);
        FIX_STEP_ASIDE = B.comment("Forget a Builder's standing spot after she steps aside from her own column (the crawl fix).")
                .define("fixStepAside", true);
        B.pop();

        B.push("lifted");
        LIFTED = B.comment("Lifted as a whole: off means no Builder asks for or smokes joints to fly.").define("enabled", true);
        for (String family : FAMILIES) {
            MINUTES.put(family, B.comment("Minutes a Builder stays Lifted on one " + family + " joint.")
                    .defineInRange("minutes" + family, DEFAULT_MINUTES.get(family), 0.5, 60));
        }
        POTENCY_BONUS = B.comment("Extra Lifted time at potency 5 (scales from 0 at potency 1).").defineInRange("potencyBonus", 0.10, 0, 1);
        SPEED_LOWEST = B.comment("Placing speed multiplier on Landrace.").defineInRange("speedLowest", 1.25, 1, 4);
        SPEED_HIGHEST = B.comment("Placing speed multiplier on Golden God (other families in between by grade).").defineInRange("speedHighest", 1.5, 1, 4);
        REACH = B.comment("Higher Ground reach in blocks.").defineInRange("reach", 9, 4, 16);
        FLIGHT_RADIUS = B.comment("How far the flight planner searches.").defineInRange("flightRadius", 24, 8, 48);
        BODY_FLIGHT = B.comment("Every trip a Lifted Builder makes is flown (job, breaks, into buildings through doors). Off: only build-site trips fly, as in 0.26.0.").define("bodyFlight", true);
        LIFTED_LOOK = B.comment("Lifted Builders are drawn in the Lifted look. Off: everyone sees them as in 0.26.0.").define("liftedLook", true);
        SMOKE_SECONDS = B.comment("Seconds a Builder smokes at her hut before she is Lifted.").defineInRange("smokeSeconds", 20, 2, 60);
        LANDING_SPEED = B.comment("Blocks a tick a Builder descends when landing (no fall damage at any value).").defineInRange("landingSpeed", 0.15, 0.05, 0.5);
        REQUEST_INTERVAL = B.comment("How often a Builder checks the colony's spare joints and opens or cancels her request.")
                .defineInRange("requestIntervalTicks", 100, 20, 1200);
        HOVER_LOOKAHEAD = B.comment("How many upcoming blocks Higher Ground considers when choosing a hover point.").defineInRange("hoverLookahead", 64, 8, 256);
        HOVER_GRID = B.comment("Spacing of candidate hover points around the work.").defineInRange("hoverGrid", 3, 1, 6);
        B.pop();

        B.push("music");
        MUSIC = B.comment("The Midnight Machine: a Lifted Builder plays the album from her boombox. Off keeps the boombox and dancing, without music.")
                .define("enabled", true);
        FALLBACK_BPM = B.comment("Dance tempo when the music is off.").defineInRange("fallbackBpm", 100.0, 40, 200);
        DANCE_BOB = B.comment("How high she bobs on the beat, in blocks.").defineInRange("danceBob", 0.12, 0, 0.5);
        SPIN_COOLDOWN = B.comment("Least seconds between spins on the big hits.").defineInRange("spinCooldownSeconds", 2.0, 0.5, 30);
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private static <T> T get(ModConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return fallback;   // game tests and early ticks before the server config loads
        }
    }

    public static boolean logBuilderPathing() { return get(LOG_PATHING, false); }
    public static boolean watchdogEnabled() { return get(WATCHDOG, true); }
    public static int slowTicks() { return get(SLOW_SECONDS, 20) * 20; }
    public static double slowFraction() { return get(SLOW_FRACTION, 0.33); }
    public static int stuckTicks() { return get(STUCK_SECONDS, 45) * 20; }
    public static int rescueRadius() { return get(RESCUE_RADIUS, 4); }
    public static boolean fixStepAside() { return get(FIX_STEP_ASIDE, true); }
    public static boolean liftedEnabled() { return get(LIFTED, true); }
    public static double liftedMinutes(String family) {
        var value = MINUTES.get(family);
        return value == null ? DEFAULT_MINUTES.getOrDefault(family, 5.0) : get(value, DEFAULT_MINUTES.getOrDefault(family, 5.0));
    }
    public static double potencyBonus() { return get(POTENCY_BONUS, 0.10); }
    public static double speedLowest() { return get(SPEED_LOWEST, 1.25); }
    public static double speedHighest() { return get(SPEED_HIGHEST, 1.5); }
    public static int reach() { return get(REACH, 9); }
    public static int flightRadius() { return get(FLIGHT_RADIUS, 24); }
    public static double landingSpeed() { return get(LANDING_SPEED, 0.15); }
    public static int requestIntervalTicks() { return get(REQUEST_INTERVAL, 100); }
    public static int hoverLookahead() { return get(HOVER_LOOKAHEAD, 64); }
    public static int hoverGrid() { return get(HOVER_GRID, 3); }
    public static boolean musicEnabled() { return get(MUSIC, true); }
    public static double fallbackBpm() { return get(FALLBACK_BPM, 100.0); }
    public static boolean bodyFlight() { return get(BODY_FLIGHT, true); }
    public static boolean liftedLook() { return get(LIFTED_LOOK, true); }
    public static int smokeSeconds() { return get(SMOKE_SECONDS, 20); }
    public static double danceBob() { return get(DANCE_BOB, 0.12); }
    public static double spinCooldownSeconds() { return get(SPIN_COOLDOWN, 2.0); }

    /** The Lifted numbers as the pure rules take them. */
    public static com.malevolentgods.fourtwenty.domain.LiftedRules.Tuning liftedTuning() {
        var minutes = new java.util.HashMap<String, Double>();
        for (String family : FAMILIES) minutes.put(family, liftedMinutes(family));
        return new com.malevolentgods.fourtwenty.domain.LiftedRules.Tuning(minutes, potencyBonus(), speedLowest(), speedHighest());
    }
}
