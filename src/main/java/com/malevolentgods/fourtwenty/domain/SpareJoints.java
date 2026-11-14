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

    /** How many to ask for: up to her stash, less what she carries, never more than the spare. Zero means no request. */
    public static int request(int stash, int carrying, int spare) {
        return Math.max(0, Math.min(stash - carrying, spare));
    }
}
