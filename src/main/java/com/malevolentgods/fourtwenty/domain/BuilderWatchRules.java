package com.malevolentgods.fourtwenty.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * The Builder watchdog's judgement (specs/013-lifted-builders, FR-003 to FR-005, data-model "BuilderWatch"). Pure:
 * fed one sample a check, it says whether she's fine, slow or stuck, and which rescue to apply now.
 *
 * Only samples taken while she is building count; any other state (eating, sleeping, fetching, a raid) resets the
 * watch. Progress is the build moving on, never her moving about.
 */
public final class BuilderWatchRules {
    private BuilderWatchRules() {}

    public enum Verdict { OK, SLOW, STUCK }

    /** Rescues in the order they're tried while a stall lasts. */
    public enum Step { NONE, FRESH_SPOT, NEAR_BLOCK, OUTSIDE, RECALL }

    /**
     * @param slowTicks     window the pace is judged over
     * @param slowFraction  below this share of the expected placements in the window is slow
     * @param stuckTicks    no progress this long is stuck, and the gap between escalation steps
     */
    public record Config(int slowTicks, double slowFraction, int stuckTicks) {}

    /** One check: the game time, whether she's building, the build's progress key (null if none), her placement count. */
    public record Sample(long time, boolean building, Long progressKey, long placements) {}

    /** What to do after a check: the verdict, and a rescue to apply now (NONE for nothing). */
    public record Decision(Verdict verdict, Step rescue) {
        static final Decision OK = new Decision(Verdict.OK, Step.NONE);
    }

    /** One Builder's watch. Mutable, kept in memory only. */
    public static final class State {
        Long lastProgress;
        long lastProgressTime = Long.MIN_VALUE;
        long lastStepTime = Long.MIN_VALUE;
        long lastPlacements = -1;
        long watchStart = Long.MIN_VALUE;
        final Deque<Long> placedAt = new ArrayDeque<>();
        Step step = Step.NONE;

        public Step step() { return step; }

        void reset(long now, Sample s) {
            lastProgress = s.progressKey();
            lastProgressTime = now;
            lastStepTime = Long.MIN_VALUE;
            lastPlacements = s.placements();
            watchStart = now;
            placedAt.clear();
            step = Step.NONE;
        }
    }

    public static Decision next(State st, Sample s, double expectedTicksPerBlock, Config cfg) {
        long now = s.time();
        if (!s.building()) {
            st.reset(now, s);
            st.watchStart = Long.MIN_VALUE;
            return Decision.OK;
        }
        if (st.watchStart == Long.MIN_VALUE) {
            st.reset(now, s);
            return Decision.OK;
        }
        // Placements since the last check (a counter going backwards, e.g. a restart, just restarts the count).
        long placed = st.lastPlacements < 0 || s.placements() < st.lastPlacements ? 0 : s.placements() - st.lastPlacements;
        st.lastPlacements = s.placements();
        for (long i = 0; i < Math.min(placed, 256); i++) st.placedAt.addLast(now);
        while (!st.placedAt.isEmpty() && st.placedAt.peekFirst() <= now - cfg.slowTicks()) st.placedAt.removeFirst();

        boolean moved = !Objects.equals(s.progressKey(), st.lastProgress);
        if (moved && placed == 0) st.placedAt.addLast(now);   // skipping blocks that already match is progress too
        boolean progressed = placed > 0 || moved;
        st.lastProgress = s.progressKey();
        if (progressed) {
            if (st.step == Step.NEAR_BLOCK || st.step == Step.OUTSIDE || st.step == Step.RECALL) {
                // Going again after a stuck rescue: judge her pace from here, not across the stall.
                st.watchStart = now;
                st.placedAt.clear();
                st.placedAt.addLast(now);
            }
            st.lastProgressTime = now;
            st.lastStepTime = Long.MIN_VALUE;
            if (st.step != Step.FRESH_SPOT) st.step = Step.NONE;
        }

        // Stuck: nothing for stuckTicks, then one escalation step per further stuckTicks.
        long still = now - st.lastProgressTime;
        if (still >= cfg.stuckTicks()) {
            boolean due = st.lastStepTime == Long.MIN_VALUE || now - st.lastStepTime >= cfg.stuckTicks();
            if (!due) return new Decision(Verdict.STUCK, Step.NONE);
            Step nextStep = switch (st.step) {
                case NONE, FRESH_SPOT -> Step.NEAR_BLOCK;
                case NEAR_BLOCK -> Step.OUTSIDE;
                case OUTSIDE, RECALL -> Step.RECALL;
            };
            st.step = nextStep;
            st.lastStepTime = now;
            return new Decision(Verdict.STUCK, nextStep);
        }

        // Slow: a full window watched, and fewer than slowFraction of the placements her skills allow.
        if (Double.isFinite(expectedTicksPerBlock) && expectedTicksPerBlock > 0 && now - st.watchStart >= cfg.slowTicks()) {
            double expected = cfg.slowTicks() / expectedTicksPerBlock;
            if (st.placedAt.size() < cfg.slowFraction() * expected) {
                if (st.step == Step.FRESH_SPOT) return new Decision(Verdict.SLOW, Step.NONE);   // one fresh spot per slow spell
                st.step = Step.FRESH_SPOT;
                return new Decision(Verdict.SLOW, Step.FRESH_SPOT);
            }
            if (st.step == Step.FRESH_SPOT) st.step = Step.NONE;   // back up to pace
        }
        return Decision.OK;
    }
}
