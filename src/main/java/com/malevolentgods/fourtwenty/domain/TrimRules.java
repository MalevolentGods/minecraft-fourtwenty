package com.malevolentgods.fourtwenty.domain;

/** Trimming a rough bud gives one trimmed bud, and sometimes a seed: the better the strain's family, the rarer the seed. */
public final class TrimRules {
    private TrimRules() {}

    /** A powered trimmer: ticks per bud and BI per tick. Tier 1, the Trim Tray, is by hand. */
    public record Tier(int ticks, double cost) {}

    public static final java.util.List<Tier> DEFAULTS = java.util.List.of(new Tier(0, 0), new Tier(40, BIRules.Tier.PUFF.machine()),
            new Tier(20, BIRules.Tier.TOKE.machine()), new Tier(10, BIRules.Tier.RIP.machine()));

    /** A seed's chance per trimmed bud, by family. */
    public record Chances(double landrace, double neutrals, double warm, double green, double cool,
                          double biome, double cross, double doubleCross, double ultra, double golden) {
        public static final Chances DEFAULT = new Chances(0.50, 0.25, 0.15, 0.10, 0.05, 0.20, 0.04, 0.02, 0.01, 0.005);

        public Chances(double landrace, double neutrals, double warm, double green, double cool) {
            this(landrace, neutrals, warm, green, cool, DEFAULT.biome, DEFAULT.cross, DEFAULT.doubleCross, DEFAULT.ultra, DEFAULT.golden);
        }

        public double of(Strain.Family family) {
            return switch (family) {
                case LANDRACE -> landrace;
                case NEUTRALS -> neutrals;
                case WARM -> warm;
                case GREEN -> green;
                case COOL -> cool;
                case BIOME -> biome;
                case CROSS -> cross;
                case DOUBLE_CROSS -> doubleCross;
                case ULTRA -> ultra;
                case GOLDEN -> golden;
            };
        }
    }

    /** {@code roll} is uniform in 0..1. */
    public static boolean seeded(Chances chances, Strain strain, double roll) {
        return roll < chances.of(strain.family());
    }

    /** "50%", for tooltips. */
    public static int percent(Chances chances, Strain strain) {
        return (int) Math.round(chances.of(strain.family()) * 100);
    }
}
