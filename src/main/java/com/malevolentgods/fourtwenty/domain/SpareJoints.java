package com.malevolentgods.fourtwenty.domain;

/**
 * Citizens first (specs/013-lifted-builders FR-010, clarification Q2): a Builder may ask the colony only for joints
 * beyond what its citizens are still owed today. Joints a player drops in her hut aren't counted here at all.
 */
public final class SpareJoints {
    private SpareJoints() {}

    /** Joints the colony can spare: stock in its Dispensaries and Warehouses, minus one per citizen still owed one today. */
    public static int spare(int stock, int owed) {
        return Math.max(0, stock - Math.max(0, owed));
    }

    /**
     * How many to ask for: one, only when she needs it (none left to smoke, and about to want one) and the colony has
     * a spare. Zero means no request, and an open one is cancelled. She used to keep a stash of three on order whenever
     * there were spares, and the requests ran all the time (the user, 2026-11-14: "shouldn't request until he needs one").
     */
    public static int request(boolean needSoon, int carrying, int spare) {
        return needSoon && carrying <= 0 && spare > 0 ? 1 : 0;
    }
}
