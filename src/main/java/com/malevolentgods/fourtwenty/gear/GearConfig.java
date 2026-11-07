package com.malevolentgods.fourtwenty.gear;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings for the Kush Suit, the Blazer and charging. Before a world has loaded them, the defaults apply. */
public final class GearConfig {
    private GearConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue NIGHT_VISION = cost("helmNightVisionPerTick", "Hotbox Helm: Ws a tick for night vision.", 12);
    private static final ModConfigSpec.IntValue FLIGHT = cost("plateFlightPerTick", "Puff Plate: Ws a tick while flying.", 600);
    private static final ModConfigSpec.IntValue KNOCKBACK = cost("greavesKnockbackPerHit", "Couchlock Greaves: Ws per knockback cancelled.", 1_200);
    private static final ModConfigSpec.IntValue STOMP = cost("greavesStomp", "Couchlock Greaves: Ws per stomp.", 12_000);
    private static final ModConfigSpec.IntValue JUMP = cost("bootsJump", "Moon Boots: Ws per boosted jump.", 1_200);
    private static final ModConfigSpec.IntValue FALL = cost("bootsFall", "Moon Boots: Ws per fall cancelled.", 3_000);
    private static final ModConfigSpec.IntValue BEAM = cost("blazerBeam", "Blazer: Ws per Blaze Beam shot.", 3_000);
    private static final ModConfigSpec.IntValue DAB = cost("dabCellPerTick", "Dab Cell: the most Ws a tick it moves into gear.", 600);
    private static final ModConfigSpec.DoubleValue BEAM_DAMAGE = BUILDER.comment("Blazer: Blaze Beam damage.").defineInRange("blazerBeamDamage", 8.0, 0, 100);
    private static final ModConfigSpec.DoubleValue STOMP_DAMAGE = BUILDER.comment("Couchlock Greaves: the most a stomp hurts.").defineInRange("greavesStompMaxDamage", 12.0, 0, 100);
    private static final ModConfigSpec.IntValue TIMBER = BUILDER.comment("Blazer: the most logs Timber Torch fells at once.").defineInRange("blazerTimberMaxLogs", 256, 1, 4096);
    private static final ModConfigSpec.DoubleValue STATION_CHARGE = BUILDER.comment("Re-Up Station: how fast it charges gear from its battery, in Hookahs of its tier.")
            .defineInRange("reUpChargeHookahs", com.malevolentgods.fourtwenty.domain.GearRules.STATION_CHARGE_HOOKAHS, 0.1, 100);
    private static final ModConfigSpec.DoubleValue STATION_MINUTES = BUILDER.comment("Re-Up Station: how many minutes of its input its battery holds.")
            .defineInRange("reUpStoreMinutes", com.malevolentgods.fourtwenty.domain.GearRules.STATION_STORE_MINUTES, 1, 1440);
    private static final ModConfigSpec.IntValue PAINT = BUILDER.comment("Blazer: blocks in a Paint Puff line.").defineInRange("blazerPaintLength", 8, 1, 64);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private static ModConfigSpec.IntValue cost(String name, String comment, int value) {
        return BUILDER.comment(comment).defineInRange(name, value, 0, 10_000_000);
    }

    private static int get(ModConfigSpec.IntValue v, int fallback) { return SPEC.isLoaded() ? v.get() : fallback; }

    public static int nightVision() { return get(NIGHT_VISION, 12); }
    public static int flight() { return get(FLIGHT, 600); }
    public static int knockback() { return get(KNOCKBACK, 1_200); }
    public static int stomp() { return get(STOMP, 12_000); }
    public static int jump() { return get(JUMP, 1_200); }
    public static int fall() { return get(FALL, 3_000); }
    public static int beam() { return get(BEAM, 3_000); }
    public static int dabCell() { return get(DAB, 600); }
    public static float beamDamage() { return SPEC.isLoaded() ? BEAM_DAMAGE.get().floatValue() : 8f; }
    public static float stompDamage() { return SPEC.isLoaded() ? STOMP_DAMAGE.get().floatValue() : 12f; }
    public static int timberLogs() { return get(TIMBER, 256); }
    public static int paintLength() { return get(PAINT, 8); }
    public static double stationChargeHookahs() { return SPEC.isLoaded() ? STATION_CHARGE.get() : com.malevolentgods.fourtwenty.domain.GearRules.STATION_CHARGE_HOOKAHS; }
    public static double stationStoreMinutes() { return SPEC.isLoaded() ? STATION_MINUTES.get() : com.malevolentgods.fourtwenty.domain.GearRules.STATION_STORE_MINUTES; }
}
