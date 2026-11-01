package com.malevolentgods.fourtwenty.domain;

import java.util.List;
import java.util.Optional;

/**
 * What the Rosin Rig makes: Cured Bud to Kief, two Kief to Hash, Hash to Resin, and Resin to Rosin, or to Oil with a Glass Bottle. No terpenes: those stay with the
 * Terpinator. Each step burns longer in a Hookah than the last, and past Kief each smokes stronger and longer in a pipe.
 */
public final class Extracts {
    private Extracts() {}

    private static final String BUD = "fourtwenty:weed_bud";

    /** {@code burn} is how many minutes it burns from one charcoal; {@code boost} what a pipe load of it adds to Potency and Length. */
    public record Extract(String id, double burn, int boost) {}

    public static final Extract KIEF = new Extract("fourtwenty:kief", 2, 0), HASH = new Extract("fourtwenty:hash", 5, 1), RESIN = new Extract("fourtwenty:resin", 7.5, 2),
            ROSIN = new Extract("fourtwenty:rosin", 15, 2), OIL = new Extract("fourtwenty:oil", 15, 2);
    public static final List<Extract> ALL = List.of(KIEF, HASH, RESIN, ROSIN, OIL);
    /** Biome strains burn this much longer once refined. */
    public static final double BIOME_BURN = 1.5;

    /** A step: {@code count} of {@code input} make one {@code output}, in {@code ticks}. */
    public record Step(String input, int count, String output, boolean bottle, int ticks) {}

    public static final List<Step> STEPS = List.of(
            new Step(BUD, 1, KIEF.id, false, 100),
            new Step(KIEF.id, 2, HASH.id, false, 200),
            new Step(HASH.id, 1, RESIN.id, false, 300),
            new Step(RESIN.id, 1, OIL.id, true, 400),
            new Step(RESIN.id, 1, ROSIN.id, false, 400));

    /** The step for this input, with or without a bottle to hand. */
    public static Optional<Step> step(String input, boolean bottle) {
        return STEPS.stream().filter(s -> s.input.equals(input) && (bottle || !s.bottle)).findFirst();
    }

    public static boolean refinable(String input) { return step(input, true).isPresent(); }

    public static Optional<Extract> byId(String id) {
        return ALL.stream().filter(e -> e.id.equals(id)).findFirst();
    }

    /** How long one charcoal burns this in a Hookah. A Cured Bud is the plain minute. */
    public static int burnTicks(String id, Strain strain) {
        return byId(id).map(e -> (int) (BIRules.BURN_TICKS * e.burn * (strain.family() == Strain.Family.BIOME ? BIOME_BURN : 1))).orElse(BIRules.BURN_TICKS);
    }

    /** The longest burn of all, for checking saves. */
    public static final int LONGEST_BURN = (int) (BIRules.BURN_TICKS * ROSIN.burn * BIOME_BURN);

    /** The traits a pipe load of this smokes with. */
    public static Traits boosted(String id, Traits traits) {
        int boost = byId(id).map(Extract::boost).orElse(0);
        return new Traits(Math.min(Traits.MAX, traits.potency() + boost), traits.yield(), Math.min(Traits.MAX, traits.length() + boost));
    }
}
