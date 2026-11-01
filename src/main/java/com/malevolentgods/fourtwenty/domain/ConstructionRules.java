package com.malevolentgods.fourtwenty.domain;

/** The Construction Joint and its upgrades, one to a power tier: what each multiplies a batch by, and the BI a batch costs. */
public final class ConstructionRules {
    private ConstructionRules() {}

    /** A batch costs what a machine of the tier draws in this many ticks. */
    public static final int BATCH_TICKS = 100;
    /** How many batches a bench's own store holds. */
    public static final int BATCHES_HELD = 32;
    private static final int[] YIELDS = {2, 3, 4, 6};

    public static int factor(BIRules.Tier tier) { return YIELDS[tier.ordinal()]; }

    public static long cost(BIRules.Tier tier) { return Math.max(1, (long) Math.ceil(tier.machine() * BATCH_TICKS)); }

    /** {@code count} of something, multiplied by the tier, up to a full stack. */
    public static int batch(int count, int maxStack, BIRules.Tier tier) {
        return Math.min(maxStack, count * factor(tier));
    }
}
