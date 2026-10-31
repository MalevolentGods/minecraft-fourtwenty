package com.malevolentgods.fourtwenty.domain;

import java.util.List;

/** Curing tiers: how long a batch takes, how many buds it holds, and its BI per tick (the Curing Rack, tier 1, needs none). */
public final class CureRules {
    private CureRules() {}

    public record Tier(int ticks, int batch, int cost) {}

    public static final List<Tier> DEFAULTS = List.of(
            new Tier(6_000, 8, 0), new Tier(3_000, 16, 16), new Tier(1_200, 32, 64), new Tier(400, 64, 256));

    public static boolean cured(int ticks, Tier tier) {
        return ticks >= tier.ticks();
    }

    /** Whole minutes left, rounded up, for status messages. */
    public static int minutesLeft(int ticks, Tier tier) {
        return Math.max(0, (tier.ticks() - ticks + 1199) / 1200);
    }
}
