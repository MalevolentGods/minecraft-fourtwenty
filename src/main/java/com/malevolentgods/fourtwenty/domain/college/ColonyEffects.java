package com.malevolentgods.fourtwenty.domain.college;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * What a colony's finished studies do to its machines and farms (specs/017-weed-college FR-022, FR-023, FR-025;
 * contracts/colony-research-api.md). Within one effect, the finished study of the highest tier wins: a later study replaces an earlier one,
 * they never stack. Never touches the Terpinator's or the Infuser's speed (constitution Principle II): the Terpinator gets power effects only.
 */
public final class ColonyEffects {
    private ColonyEffects() {}

    public enum Effect {
        /** Multiplies the BI a colony Terpinator draws (1 = unchanged, 0 = none). Power only. */
        TERPINATOR_BI(1),
        /** Multiplies the BI a colony Grow Op draws. */
        GROW_OP_BI(1),
        /** Multiplies how long a colony Hookah's burn lasts. */
        HOOKAH_BURN(1),
        /** Multiplies a colony Stash's capacity. */
        STASH_CAPACITY(1),
        /** Divides a colony trimmer's or curer's work time. */
        TRIM_CURE_SPEED(1),
        /** Divides a colony Rosin Rig's or Pheno Chamber's work time. */
        PRESS_SPEED(1),
        /** The chance a colony citizen's weed harvest gives one extra bud (0 = never). */
        FARM_BONUS_BUD(0);

        public final double neutral;

        Effect(double neutral) { this.neutral = neutral; }

        public static Effect of(String key) { return valueOf(key.toUpperCase(Locale.ROOT)); }
    }

    /** The value of every effect for a colony that has finished {@code done}. */
    public static Map<Effect, Double> values(Collection<Study> done) {
        Map<Effect, Double> out = new EnumMap<>(Effect.class);
        Map<Effect, Integer> tierOf = new EnumMap<>(Effect.class);
        for (Effect e : Effect.values()) out.put(e, e.neutral);
        for (Study s : done) {
            if (s.kind() != Study.Kind.EFFECT || s.gives().effect() == null) continue;
            Effect e = Effect.of(s.gives().effect());
            if (s.tier() >= tierOf.getOrDefault(e, 0)) {
                tierOf.put(e, s.tier());
                out.put(e, s.gives().strength());
            }
        }
        return out;
    }

    /** One effect's value for a colony that has finished {@code done}. */
    public static double factor(Effect effect, Collection<Study> done) {
        return values(done).get(effect);
    }

    // ---- how the shared machine rules use a factor (research R7)

    /** A work time at {@code speed} times the normal pace: shorter for a faster colony, never under one tick. */
    public static int scaledTicks(int base, double speed) {
        if (speed <= 0 || speed == 1) return base;
        return Math.max(1, (int) Math.round(base / speed));
    }

    /** A BI cost multiplied by the colony's factor: 0.5 halves it, 0 makes it free. Never negative. */
    public static double scaledCost(double base, double factor) {
        return Math.max(0, base * factor);
    }

    /** A length (a burn, a capacity) multiplied by the colony's factor; never less than the base, since no study shrinks one. */
    public static long scaledLength(long base, double factor) {
        return factor <= 1 ? base : Math.round(base * factor);
    }
}
