package com.malevolentgods.fourtwenty.domain;

/**
 * The Pave Pen: what it does to a block, how long that takes, and what it costs. Roads climb Cobble Path (tier 1), Paved Road (2) and
 * Yellow Brick Road (3). Laying a Cobble Path takes only weed; each tier above takes the material its crafting recipe does, one item for
 * eight roads: smooth stone for Paved Road, gold for Yellow Brick Road.
 */
public final class PaveRules {
    private PaveRules() {}

    public static final int TIERS = 3;
    public static final int ROADS_PER_MATERIAL = 8;
    /** BI a bud gives the pen: rough, trimmed, cured. */
    public static final int ROUGH = 400, TRIMMED = 600, CURED = 800;

    public enum Shape {
        FULL, SLAB, STAIRS;

        public static Shape of(int index) { return values()[Math.floorMod(index, values().length)]; }
    }

    public enum Action { NONE, LAY, UPGRADE, RESHAPE }

    /** The tunable numbers; the config builds these. */
    public record Numbers(int capacity, double layBase, double layPerHardness, double ticksBase, double ticksPerHardness, double maxHardness,
                          int upgradeCost, int upgradeTicks, int reshapeCost, int reshapeTicks) {
        public static final Numbers DEFAULT = new Numbers(3200, 20, 20, 4, 10, 5, 20, 8, 10, 6);
    }

    /** The shape the pen makes: a Pro makes the chosen one; a plain pen keeps a road's own shape and lays whole blocks. */
    public static Shape wanted(boolean pro, Shape chosen, int roadTier, Shape roadShape) {
        if (pro) return chosen;
        return roadTier > 0 ? roadShape : Shape.FULL;
    }

    /** What the pen does to a block; {@code roadTier} is 0 for anything that isn't one of our roads. It never lowers a road. */
    public static Action action(int roadTier, Shape roadShape, int target, Shape shape) {
        if (roadTier <= 0) return Action.LAY;
        if (roadTier < target) return Action.UPGRADE;
        return roadShape != shape ? Action.RESHAPE : Action.NONE;
    }

    /** The tier a road ends at: the target, or its own if that's better. */
    public static int resultTier(int roadTier, int target) {
        return Math.max(roadTier, Math.max(1, Math.min(TIERS, target)));
    }

    /** Whether the pen can lay road over a block this hard: not unbreakable, not too tough. */
    public static boolean layable(double hardness, Numbers n) {
        return hardness >= 0 && hardness <= n.maxHardness();
    }

    public static int ticks(Action action, double hardness, Numbers n) {
        return switch (action) {
            case LAY -> Math.max(1, (int) Math.round(n.ticksBase() + n.ticksPerHardness() * Math.max(0, hardness)));
            case UPGRADE -> n.upgradeTicks();
            case RESHAPE -> n.reshapeTicks();
            case NONE -> 0;
        };
    }

    /** BI the job takes from the pen's charge. */
    public static int cost(Action action, double hardness, Numbers n) {
        return switch (action) {
            case LAY -> (int) Math.round(n.layBase() + n.layPerHardness() * Math.max(0, hardness));
            case UPGRADE -> n.upgradeCost();
            case RESHAPE -> n.reshapeCost();
            case NONE -> 0;
        };
    }

    /** Whether going from {@code from} up to {@code to} passes Paved Road, which takes smooth stone. */
    public static boolean needsStone(int from, int to) { return from < 2 && to >= 2; }

    /** Whether going from {@code from} up to {@code to} passes Yellow Brick Road, which takes gold. */
    public static boolean needsGold(int from, int to) { return from < 3 && to >= 3; }

    /** A Cobble Path is where every road starts, so laying climbs from tier 1. */
    public static int startTier(int roadTier) { return Math.max(1, roadTier); }

    /** How many times this much BI does a job that costs {@code cost}. */
    public static int jobs(long bi, int cost) { return cost <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, bi / cost); }
}
