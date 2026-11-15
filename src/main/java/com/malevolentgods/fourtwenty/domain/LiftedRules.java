package com.malevolentgods.fourtwenty.domain;

import java.util.Locale;
import java.util.Map;

/**
 * How long a joint keeps a Builder Lifted, and how much faster she places while Lifted (specs/013-lifted-builders,
 * FR-011, FR-015 4, research R9). Pure: the numbers come in as {@link Tuning}, from the builder config.
 */
public final class LiftedRules {
    private LiftedRules() {}

    public static final int TICKS_PER_MINUTE = 1200;
    public static final int GOLDEN_GRADE = 5;

    /**
     * @param minutes      Lifted minutes per joint by family key ({@link #familyKey})
     * @param potencyBonus extra share of time at potency 5 (0 at potency 1)
     * @param speedLowest  placing speed on Landrace (grade 0)
     * @param speedHighest placing speed on Golden God (grade 5)
     */
    public record Tuning(Map<String, Double> minutes, double potencyBonus, double speedLowest, double speedHighest) {}

    /** The config key for a family: LANDRACE → "Landrace", DOUBLE_CROSS → "DoubleCross". */
    public static String familyKey(Strain.Family family) {
        StringBuilder key = new StringBuilder();
        for (String part : family.name().split("_")) {
            key.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return key.toString();
    }

    /** Ticks one joint of this strain keeps her Lifted, at its potency (1-5). */
    public static int ticks(Strain strain, int potency, Tuning t) {
        double minutes = t.minutes().getOrDefault(familyKey(strain.family()), 5.0);
        int p = Math.clamp(potency, StrainStats.MIN_POTENCY, StrainStats.MAX_POTENCY);
        double bonus = t.potencyBonus() * (p - StrainStats.MIN_POTENCY) / (double) (StrainStats.MAX_POTENCY - StrainStats.MIN_POTENCY);
        return (int) Math.round(minutes * (1 + bonus) * TICKS_PER_MINUTE);
    }

    /** Placing speed while Lifted on this strain: lowest at Landrace, highest at Golden God, linear by grade. */
    public static double speed(Strain strain, Tuning t) {
        double share = Math.clamp(strain.grade() / (double) GOLDEN_GRADE, 0, 1);
        return t.speedLowest() + (t.speedHighest() - t.speedLowest()) * share;
    }

    /** What MineColonies' placing delay is multiplied by while Lifted: 1 / speed. */
    public static double delayFactor(Strain strain, Tuning t) {
        return 1 / Math.max(1e-6, speed(strain, t));
    }
}
