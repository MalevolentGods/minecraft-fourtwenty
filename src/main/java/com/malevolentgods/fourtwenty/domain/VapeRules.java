package com.malevolentgods.fourtwenty.domain;

/**
 * The Vape Pen (user, 2026-11-15): loaded with Oil, one Oil fills it for {@link #HITS_PER_OIL} hits, and it holds
 * {@link #CAPACITY}. Each hit is one high, as a pipe hit is, so an Oil lasts eight times as long as a bud in a pipe.
 */
public final class VapeRules {
    private VapeRules() {}

    public static final String OIL = "fourtwenty:oil";
    public static final int HITS_PER_OIL = 8;
    public static final int CAPACITY = 16;

    /** How many Oils go in, of {@code offered}, with {@code hits} already in it: only whole Oils that fit. */
    public static int oilsThatFit(int hits, int offered) {
        return Math.max(0, Math.min(offered, (CAPACITY - hits) / HITS_PER_OIL));
    }
}
