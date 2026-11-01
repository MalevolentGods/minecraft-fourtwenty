package com.malevolentgods.fourtwenty.domain;

/** The rule every machine runs by: free with a high player nearby, otherwise on BI, otherwise paused with its progress kept. */
public final class MachinePowerRule {
    private MachinePowerRule() {}

    public static final int RADIUS = 21;

    public enum State { RUN_FREE, RUN_PAID, PAUSE }

    public static State state(boolean highNearby, long stored, long cost) {
        if (highNearby) return State.RUN_FREE;
        return stored >= cost ? State.RUN_PAID : State.PAUSE;
    }

    /** Progress this tick while running free, at {@code percent} of normal speed; {@code roll} in 0..99 settles a fraction. */
    public static int freeSteps(int percent, int roll) {
        return percent / 100 + (roll < percent % 100 ? 1 : 0);
    }
}
