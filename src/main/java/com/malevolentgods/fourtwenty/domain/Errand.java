package com.malevolentgods.fourtwenty.domain;

/**
 * Where a Lifted Builder is being sent and whether she's getting there (specs/015-lifted-body R4): every walk
 * MineColonies asks of her while she's Lifted becomes an errand she flies. Reached within {@link #REACHED} blocks;
 * "progress" is getting at least {@link #STEP} blocks closer than her best within {@link #PATIENCE_TICKS}; without it
 * she's recalled. Pure.
 */
public final class Errand {
    /** Close enough to count as there, in blocks (about MineColonies' own arrival distance). */
    public static final double REACHED = 1.5;
    /** How much closer counts as progress. */
    public static final double STEP = 2;
    /** How long without progress before she's recalled (30 s). */
    public static final long PATIENCE_TICKS = 30 * 20;

    public enum Purpose { JOB, BREAK }

    public enum Verdict { ON_HER_WAY, REACHED, RECALL }

    private final int x, y, z;
    private final Purpose purpose;
    private double best = Double.MAX_VALUE;
    private long bestAt = Long.MIN_VALUE;

    public Errand(int x, int y, int z, Purpose purpose) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.purpose = purpose;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public Purpose purpose() { return purpose; }

    public boolean sameDestination(int ox, int oy, int oz) {
        return ox == x && oy == y && oz == z;
    }

    /** Where she stands against this errand at {@code now}, {@code distance} blocks from its destination. */
    public Verdict check(double distance, long now) {
        if (distance <= REACHED) return Verdict.REACHED;
        if (bestAt == Long.MIN_VALUE || distance <= best - STEP) {
            best = distance;
            bestAt = now;
            return Verdict.ON_HER_WAY;
        }
        return now - bestAt >= PATIENCE_TICKS ? Verdict.RECALL : Verdict.ON_HER_WAY;
    }
}
