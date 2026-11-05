package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.HueFamilies.Family;

/**
 * The Kush Suit and Blazer: the pieces, what they hold, and the rules every power follows. A power works only while the wearer is high
 * and the piece can pay for it, and pays only when it acts. Flight can never kill: whenever it ends in the air, the wearer floats down.
 */
public final class GearRules {
    private GearRules() {}

    public enum Piece {
        HOTBOX_HELM(600_000, Family.YELLOWS, 3), PUFF_PLATE(2_400_000, Family.GREENS, 3), COUCHLOCK_GREAVES(1_200_000, Family.PURPLES, 3),
        MOON_BOOTS(600_000, Family.BLUES, 3), BLAZER(6_000_000, Family.REDS, 4), DAB_CELL(2_400_000, null, 0);

        public final int capacity, slots;
        public final Family family;

        Piece(int capacity, Family family, int slots) {
            this.capacity = capacity;
            this.family = family;
            this.slots = slots;
        }

        public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    /** Whether a power costing {@code cost} can run. */
    public static boolean powered(boolean high, int charge, int cost) {
        return high && charge >= Math.max(0, cost) && charge > 0;
    }

    /** The charge after paying {@code cost}; the caller checks {@link #powered} first. */
    public static int spend(int charge, int cost) {
        return Math.max(0, charge - Math.max(0, cost));
    }

    /** How much moves into a piece holding {@code charge} of {@code capacity}, from a source offering {@code offer} this tick. */
    public static int chargeStep(int charge, int capacity, long offer) {
        return (int) Math.max(0, Math.min(offer, (long) capacity - charge));
    }

    /** Charge per tick at a Re-Up Station of this tier: what a Wick of the tier carries. */
    /** Ws per tick a Re-Up Station charges at: one and a half Hookahs of its tier, which leaves its Wick (rated for two) headroom. */
    public static final double STATION_HOOKAHS = 1.5;

    public static double stationRate(BIRules.Tier tier) { return tier.rate() * STATION_HOOKAHS; }

    /** A stomp's damage from how far the wearer fell: a little from a hop, up to a cap from a long drop. */
    public static float stompDamage(float fallDistance, float max) {
        return Math.min(max, Math.max(0, 2 + fallDistance));
    }

    /** Whether a fall is far enough to stomp. */
    public static boolean stomps(float fallDistance, boolean crouching) { return crouching && fallDistance >= 2; }

    /** Lent flight continues only while every condition holds; anything else ends it, safely. */
    public static boolean keepFlying(boolean high, int charge, int cost, boolean wearing) {
        return wearing && powered(high, charge, cost);
    }

    /** Ending flight in the air always lowers the wearer gently: the safety the constitution requires. */
    public static boolean needsSoftLanding(boolean onGround, boolean lent) { return lent && !onGround; }
}
