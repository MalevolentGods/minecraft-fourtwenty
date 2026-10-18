package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import java.util.function.DoubleSupplier;

/**
 * Pure affinity rules. Randomness comes from a supplier of values in [0, 1) so tests can script it.
 *
 * <p>Steps: 0 no interest; 1 every 4 days; 2 every 2 days; 3 daily; 4 twice a day; 5 three times a
 * day; 6 four times a day; 7 over the limit (tolerance break).
 */
public final class AffinityRules {
    public static final int MAX_STEP = 7;
    /** Bong sessions roll drift at most once per this many ticks per citizen. */
    public static final int BONG_ROLL_WINDOW = 24_000;

    /** Tunables (server config). */
    public record Tuning(double changeChance, double upChance, int breakTicks) {
        public static final Tuning DEFAULT = new Tuning(0.10, 0.75, 48_000);
    }

    private static final Preference[] PREFERENCES = {Preference.NONE, Preference.SMOKE, Preference.EDIBLE, Preference.BOTH};
    private static final int[] PREFERENCE_WEIGHTS = {20, 35, 20, 25};
    private static final int[] STEP_WEIGHTS = {0, 10, 20, 40, 20, 10}; // index = step 1..5

    /** Ticks between doses at {@code step}, scaled from the daily interval (step 3). */
    public static int intervalTicks(int step, int dailyTicks) {
        double days = switch (step) {
            case 1 -> 4;
            case 2 -> 2;
            case 3 -> 1;
            case 4 -> 1 / 2.0;
            case 5 -> 1 / 3.0;
            case 6 -> 1 / 4.0;
            default -> 1;
        };
        long ticks = Math.round(dailyTicks * days);
        return (int) Math.max(1, Math.min(DemandRules.MAX_TICKS, ticks));
    }

    /** First exposure rolls preference and starting step once; later exposures change nothing. */
    public static Affinity expose(Affinity affinity, long now, DoubleSupplier random) {
        if (affinity.exposed()) return affinity;
        var preference = PREFERENCES[weighted(PREFERENCE_WEIGHTS, random.getAsDouble())];
        int step = weighted(STEP_WEIGHTS, random.getAsDouble());
        return new Affinity(now, preference, step, step, -1, now);
    }

    /**
     * One drift roll (after a dose, or a bong session): {@code changeChance} to move one step,
     * {@code upChance} of that upward. Reaching the top step starts a tolerance break.
     */
    public static Affinity drift(Affinity affinity, long now, Tuning tuning, DoubleSupplier random) {
        if (!affinity.exposed() || affinity.onBreak(now) || random.getAsDouble() >= tuning.changeChance()) return affinity;
        int step = random.getAsDouble() < tuning.upChance() ? affinity.step() + 1 : affinity.step() - 1;
        step = Math.max(0, Math.min(MAX_STEP, step));
        long breakUntil = step == MAX_STEP ? now + tuning.breakTicks() : affinity.breakUntil();
        return new Affinity(affinity.exposedAt(), affinity.preference(), affinity.baseStep(), step, breakUntil, affinity.lastBongRoll());
    }

    /** A bong session: exposes a newcomer, otherwise rolls drift at most once per window. */
    public static Affinity bongSession(Affinity affinity, long now, Tuning tuning, DoubleSupplier random) {
        if (!affinity.exposed()) return expose(affinity, now, random);
        if (affinity.lastBongRoll() >= 0 && now - affinity.lastBongRoll() < BONG_ROLL_WINDOW) return affinity;
        var rolled = drift(affinity, now, tuning, random);
        return new Affinity(rolled.exposedAt(), rolled.preference(), rolled.baseStep(), rolled.step(), rolled.breakUntil(), now);
    }

    /** After a tolerance break, return to the original step. */
    public static Affinity tick(Affinity affinity, long now) {
        if (affinity.breakUntil() < 0 || now < affinity.breakUntil()) return affinity;
        return new Affinity(affinity.exposedAt(), affinity.preference(), affinity.baseStep(), affinity.baseStep(), -1, affinity.lastBongRoll());
    }

    private static int weighted(int[] weights, double roll) {
        int total = 0;
        for (int weight : weights) total += weight;
        double at = roll * total;
        for (int i = 0; i < weights.length; i++) {
            at -= weights[i];
            if (at < 0) return i;
        }
        for (int i = weights.length - 1; i >= 0; i--) if (weights[i] > 0) return i;
        return 0;
    }

    private AffinityRules() {}
}
