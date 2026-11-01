package com.malevolentgods.fourtwenty.domain;

import java.util.List;

/** Grow Ops: how much faster each plot grows a crop, and what the top tiers add to a harvest. */
public final class GrowRules {
    private GrowRules() {}

    /** Tiers 1 to 5: Closet Grow, Grow Tent, Hydro Bed, Aero Pod, Cosmic Kush Pod. */
    public static final List<Double> SPEEDS = List.of(1.5, 2.0, 3.0, 5.0, 8.0);
    /** The first tier, counting from 1, with bigger harvests that replant themselves. */
    public static final int BIG_HARVEST_TIER = 4;
    public static final int RANDOM_TICK_ODDS = 4096;
    /** By strain grade: how fast a crop grows, and the lowest plot tier it grows on (0 for anywhere). */
    public static final List<Double> GRADE_SPEEDS = List.of(1.0, 1.0, 0.7, 0.5, 0.35, 0.25);
    private static final List<Integer> GRADE_PLOTS = List.of(0, 0, 1, 3, 4, 5);

    public static double gradeSpeed(int grade) { return GRADE_SPEEDS.get(grade); }

    public static int gradePlot(int grade) { return GRADE_PLOTS.get(grade); }

    /** The chance each tick of an extra random tick, so a crop gets {@code speed} times as many as it would alone. */
    public static double extraTickChance(double speed, int randomTickSpeed) {
        return Math.max(0, speed - 1) * randomTickSpeed / RANDOM_TICK_ODDS;
    }

    /** {@code count} non-seed drops at a big-harvest tier: half as many again, rounded up. */
    public static int bigHarvest(int count) {
        return (count * 3 + 1) / 2;
    }
}
