package com.malevolentgods.fourtwenty.domain;

/** The Hotbox's countdown: full while a player is in its area, running down once they leave, gone when the server empties. */
public final class HotboxRules {
    public static final int HOLD_TICKS = 20 * 60 * 60;

    private HotboxRules() {}

    public static int next(int left, boolean playerInArea, boolean anyoneOnline, int elapsed) {
        if (playerInArea) return HOLD_TICKS;
        if (!anyoneOnline) return 0;
        return Math.max(0, left - elapsed);
    }

    /** Whole minutes left, rounded up: the last seconds still read as one minute. */
    public static int minutesLeft(int left) {
        return (left + 1199) / 1200;
    }
}
