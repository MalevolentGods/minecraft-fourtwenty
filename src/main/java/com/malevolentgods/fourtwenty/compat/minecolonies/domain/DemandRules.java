package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

/** Durations are simulation ticks. Changed settings apply only to newly scheduled deadlines. */
public record DemandRules(int intervalTicks, int graceTicks, int healthTicks) {
    public static final int MAX_TICKS = 24_000 * 365;
    public DemandRules {
        if (intervalTicks < 1 || intervalTicks > MAX_TICKS || graceTicks < 1 || graceTicks > MAX_TICKS
                || healthTicks < 1 || healthTicks > MAX_TICKS) {
            throw new IllegalArgumentException("Durations must be between 1 and " + MAX_TICKS + " ticks");
        }
    }
    public static DemandRules defaults() { return new DemandRules(24_000, 24_000, 24_000); }
    public static boolean eligible(boolean adult, boolean assignedHome, int completedLevel) {
        return adult && assignedHome && completedLevel >= 3;
    }
}
