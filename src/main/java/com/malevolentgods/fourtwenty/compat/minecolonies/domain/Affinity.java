package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

/**
 * A citizen's cannabis affinity: what they consume and how often (the step). Unset until the
 * citizen's first exposure, which rolls it once; it then drifts with use and bong sessions, and
 * a citizen who reaches the top step takes a tolerance break, then returns to their original step.
 *
 * @param exposedAt    simulation time of first exposure, or -1 (N/A) if never exposed
 * @param baseStep     the step rolled at first exposure, restored after a tolerance break
 * @param breakUntil   end of the current tolerance break, or -1
 * @param lastBongRoll time of the last bong-session drift roll, or -1
 */
public record Affinity(long exposedAt, Preference preference, int baseStep, int step, long breakUntil, long lastBongRoll) {
    public static final Affinity UNEXPOSED = new Affinity(-1, Preference.NONE, 0, 0, -1, -1);

    public Affinity {
        if (preference == null || exposedAt < -1 || breakUntil < -1 || lastBongRoll < -1
                || step < 0 || step > AffinityRules.MAX_STEP || baseStep < 0 || baseStep > AffinityRules.MAX_STEP
                || (exposedAt < 0 && !(preference == Preference.NONE && step == 0 && baseStep == 0 && breakUntil == -1 && lastBongRoll == -1))) {
            throw new IllegalArgumentException("Invalid affinity record");
        }
    }

    public boolean exposed() { return exposedAt >= 0; }
    public boolean onBreak(long now) { return breakUntil > now; }
    /** Has a craving at all: exposed, interested, not on a break, and between "every 4 days" and "4 times a day". */
    public boolean wantsDoses(long now) {
        return exposed() && !onBreak(now) && preference != Preference.NONE && step >= 1 && step < AffinityRules.MAX_STEP;
    }
}
