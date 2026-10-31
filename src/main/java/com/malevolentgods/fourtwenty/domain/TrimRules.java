package com.malevolentgods.fourtwenty.domain;

/** Trimming a rough bud gives one trimmed bud, and sometimes a seed: the better the strain's family, the rarer the seed. */
public final class TrimRules {
    private TrimRules() {}

    /** A seed's chance per trimmed bud, by family. */
    public record Chances(double landrace, double neutrals, double warm, double green, double cool) {
        public static final Chances DEFAULT = new Chances(0.50, 0.25, 0.15, 0.10, 0.05);

        public double of(Strain.Family family) {
            return switch (family) {
                case LANDRACE -> landrace;
                case NEUTRALS -> neutrals;
                case WARM -> warm;
                case GREEN -> green;
                case COOL -> cool;
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
