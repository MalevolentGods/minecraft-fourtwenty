package com.malevolentgods.fourtwenty.gear;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings for the Kush Suit, the Blazer and charging. Before a world has loaded them, the defaults apply. */
public final class GearConfig {
    private GearConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue NIGHT_VISION = cost("helmNightVisionPerTick", "Hotbox Helm: BI a tick for night vision.", 2);
    private static final ModConfigSpec.IntValue FLIGHT = cost("plateFlightPerTick", "Puff Plate: BI a tick while flying.", 100);
    private static final ModConfigSpec.IntValue KNOCKBACK = cost("greavesKnockbackPerHit", "Couchlock Greaves: BI per knockback cancelled.", 200);
    private static final ModConfigSpec.IntValue STOMP = cost("greavesStomp", "Couchlock Greaves: BI per stomp.", 2_000);
    private static final ModConfigSpec.IntValue JUMP = cost("bootsJump", "Moon Boots: BI per boosted jump.", 200);
    private static final ModConfigSpec.IntValue FALL = cost("bootsFall", "Moon Boots: BI per fall cancelled.", 500);
    private static final ModConfigSpec.IntValue BEAM = cost("blazerBeam", "Blazer: BI per Blaze Beam shot.", 500);
    private static final ModConfigSpec.IntValue DAB = cost("dabCellPerTick", "Dab Cell: the most BI a tick it moves into gear.", 100);
    private static final ModConfigSpec.DoubleValue BEAM_DAMAGE = BUILDER.comment("Blazer: Blaze Beam damage.").defineInRange("blazerBeamDamage", 8.0, 0, 100);
    private static final ModConfigSpec.DoubleValue STOMP_DAMAGE = BUILDER.comment("Couchlock Greaves: the most a stomp hurts.").defineInRange("greavesStompMaxDamage", 12.0, 0, 100);
    private static final ModConfigSpec.IntValue TIMBER = BUILDER.comment("Blazer: the most logs Timber Torch fells at once.").defineInRange("blazerTimberMaxLogs", 256, 1, 4096);
    private static final ModConfigSpec.IntValue PAINT = BUILDER.comment("Blazer: blocks in a Paint Puff line.").defineInRange("blazerPaintLength", 8, 1, 64);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private static ModConfigSpec.IntValue cost(String name, String comment, int value) {
        return BUILDER.comment(comment).defineInRange(name, value, 0, 1_000_000);
    }

    private static int get(ModConfigSpec.IntValue v, int fallback) { return SPEC.isLoaded() ? v.get() : fallback; }

    public static int nightVision() { return get(NIGHT_VISION, 2); }
    public static int flight() { return get(FLIGHT, 100); }
    public static int knockback() { return get(KNOCKBACK, 200); }
    public static int stomp() { return get(STOMP, 2_000); }
    public static int jump() { return get(JUMP, 200); }
    public static int fall() { return get(FALL, 500); }
    public static int beam() { return get(BEAM, 500); }
    public static int dabCell() { return get(DAB, 100); }
    public static float beamDamage() { return SPEC.isLoaded() ? BEAM_DAMAGE.get().floatValue() : 8f; }
    public static float stompDamage() { return SPEC.isLoaded() ? STOMP_DAMAGE.get().floatValue() : 12f; }
    public static int timberLogs() { return get(TIMBER, 256); }
    public static int paintLength() { return get(PAINT, 8); }
}
