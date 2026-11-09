package com.malevolentgods.fourtwenty.domain;

import java.util.EnumMap;
import java.util.Map;

/**
 * What a strain's family gives (specs/012-progression-balance): rough buds per harvest (a range, rolled), a potency that makes its
 * high stronger and longer, and, for the Grow Op plots, each tier's chance of an extra seed at harvest. A seed's traits add on top.
 */
public record StrainStats(Map<Strain.Family, Yield> yields, Map<Strain.Family, Integer> potency, double[] plotSeedChance) {
    public static final int MIN_POTENCY = 1, MAX_POTENCY = 5, MAX_YIELD = 8, PLOT_TIERS = 5;

    /** Rough buds per harvest before traits: rolled uniformly from {@code min} to {@code max}. */
    public record Yield(int min, int max) {
        public Yield {
            min = Math.clamp(min, 1, MAX_YIELD);
            max = Math.clamp(Math.max(max, min), min, MAX_YIELD);
        }

        /** "1-2" (a config value); a bad one gives {@code fallback}. */
        public static Yield parse(String text, Yield fallback) {
            try {
                String[] parts = text.trim().split("-");
                int min = Integer.parseInt(parts[0].trim());
                return new Yield(min, parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min);
            } catch (RuntimeException e) {
                return fallback;
            }
        }

        @Override public String toString() { return min == max ? Integer.toString(min) : min + "-" + max; }
    }

    private static final int[] POTENCY_LEVELS = {0, 0, 1, 1, 2};
    private static final double[] POTENCY_LENGTH = {1.0, 1.1, 1.2, 1.3, 1.4};

    public static final StrainStats DEFAULT = new StrainStats(
            yields(1, 1, 1, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5),
            potencies(1, 2, 2, 2, 3, 3, 3, 4, 4, 5),
            new double[]{0.10, 0.20, 0.30, 0.40, 0.50});

    public StrainStats {
        Map<Strain.Family, Yield> copied = new EnumMap<>(Strain.Family.class);
        copied.putAll(yields);
        yields = copied;
        Map<Strain.Family, Integer> clamped = new EnumMap<>(Strain.Family.class);
        potency.forEach((f, p) -> clamped.put(f, Math.clamp(p, MIN_POTENCY, MAX_POTENCY)));
        potency = clamped;
        double[] chances = new double[PLOT_TIERS];
        for (int i = 0; i < PLOT_TIERS; i++) chances[i] = i < plotSeedChance.length ? Math.clamp(plotSeedChance[i], 0, 1) : 0;
        plotSeedChance = chances;
    }

    /** min, max for each family in declaration order. */
    private static Map<Strain.Family, Yield> yields(int... minMax) {
        Map<Strain.Family, Yield> out = new EnumMap<>(Strain.Family.class);
        Strain.Family[] families = Strain.Family.values();
        for (int i = 0; i < families.length; i++) out.put(families[i], new Yield(minMax[2 * i], minMax[2 * i + 1]));
        return out;
    }

    private static Map<Strain.Family, Integer> potencies(int... values) {
        Map<Strain.Family, Integer> out = new EnumMap<>(Strain.Family.class);
        Strain.Family[] families = Strain.Family.values();
        for (int i = 0; i < families.length; i++) out.put(families[i], values[i]);
        return out;
    }

    public Yield yieldOf(Strain.Family family) { return yields.getOrDefault(family, DEFAULT.yields.get(family)); }

    public int potency(Strain.Family family) { return potency.getOrDefault(family, DEFAULT.potency.get(family)); }

    /** Rough buds from a harvest: the family's range at {@code roll} (uniform in 0..1), plus the yield trait, never fewer than 1. */
    public int rollYield(Strain.Family family, Traits traits, double roll) {
        Yield y = yieldOf(family);
        int base = y.min() + Math.min(y.max() - y.min(), (int) Math.floor(roll * (y.max() - y.min() + 1)));
        return Math.max(1, base + traits.yieldBonus());
    }

    /** Effect levels potency {@code p} adds to a high: 0, 0, 1, 1, 2 for 1 to 5 (Landrace's 1 is today's high). */
    public static int potencyLevels(int p) { return POTENCY_LEVELS[Math.clamp(p, MIN_POTENCY, MAX_POTENCY) - MIN_POTENCY]; }

    /** How much longer potency {@code p} makes a high: x1.0 to x1.4. */
    public static double potencyLength(int p) { return POTENCY_LENGTH[Math.clamp(p, MIN_POTENCY, MAX_POTENCY) - MIN_POTENCY]; }

    /** Whether a harvest on a Grow Op of {@code tier} (1 to 5; 0 for none) gives an extra seed, at {@code roll} (uniform in 0..1). */
    public boolean extraSeed(int tier, double roll) {
        return tier >= 1 && tier <= PLOT_TIERS && roll < plotSeedChance[tier - 1];
    }

    public double plotSeedChance(int tier) { return tier >= 1 && tier <= PLOT_TIERS ? plotSeedChance[tier - 1] : 0; }
}
