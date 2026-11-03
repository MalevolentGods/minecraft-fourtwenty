package com.malevolentgods.fourtwenty.domain;

/**
 * The Construction Joint and its upgrades, one to a power tier: what each multiplies a batch by, what a craft costs, and how fast a bench's
 * reserve charges. A craft costs the same BI for every item it makes on any tier; better benches make more items and charge faster.
 */
public final class ConstructionRules {
    private ConstructionRules() {}

    public static final int BI_PER_ITEM = 8;
    /** A bench's reserve: four full stacks of items. */
    public static final long CAPACITY = 64L * BI_PER_ITEM * 4;
    /** How many ticks of charging a bench's intake holds, so the Wicks can keep up. */
    public static final int INTAKE_TICKS = 40;
    private static final int[] YIELDS = {2, 3, 4, 6};

    public static int factor(BIRules.Tier tier) { return YIELDS[tier.ordinal()]; }

    /** The BI a craft that makes {@code made} items costs. */
    public static long cost(int made) { return (long) Math.max(0, made) * BI_PER_ITEM; }

    /** BI per tick a bench's reserve charges at: one Hookah of its tier. */
    public static double chargeRate(BIRules.Tier tier) { return tier.rate(); }

    /** How much the bench's intake holds from the Wicks. */
    public static long intake(BIRules.Tier tier) { return (long) Math.ceil(chargeRate(tier) * INTAKE_TICKS); }

    /** {@code count} of something, multiplied by the tier, up to a full stack. */
    public static int batch(int count, int maxStack, BIRules.Tier tier) {
        return Math.min(maxStack, count * factor(tier));
    }
}
