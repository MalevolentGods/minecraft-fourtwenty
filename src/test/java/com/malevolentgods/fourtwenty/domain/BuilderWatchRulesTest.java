package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.BuilderWatchRules.Config;
import com.malevolentgods.fourtwenty.domain.BuilderWatchRules.Sample;
import com.malevolentgods.fourtwenty.domain.BuilderWatchRules.State;
import com.malevolentgods.fourtwenty.domain.BuilderWatchRules.Step;
import com.malevolentgods.fourtwenty.domain.BuilderWatchRules.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuilderWatchRulesTest {
    // slow window 400 ticks (20 s), slow under a third, stuck after 900 ticks (45 s); she should place a block every 10 ticks
    private static final Config CFG = new Config(400, 0.33, 900);
    private static final double EXPECTED = 10;

    private static BuilderWatchRules.Decision at(State st, long t, boolean building, long progress, long placements) {
        return BuilderWatchRules.next(st, new Sample(t, building, progress, placements), EXPECTED, CFG);
    }

    @Test
    void okWhileTheBuildMovesOnAtPace() {
        var st = new State();
        long placed = 0;
        for (long t = 0; t <= 2000; t += 20) {
            placed += 2;   // one block every 10 ticks
            assertEquals(Verdict.OK, at(st, t, true, placed, placed).verdict(), "at " + t);
        }
    }

    @Test
    void slowWhenFarBelowHerPaceGivesOneFreshSpot() {
        var st = new State();
        long placed = 0;
        int freshSpots = 0;
        for (long t = 0; t <= 1200; t += 20) {
            if (t % 100 == 0) placed++;   // one block every 100 ticks: a tenth of her pace
            var d = at(st, t, true, placed, placed);
            if (t >= 400) assertEquals(Verdict.SLOW, d.verdict(), "at " + t);
            if (d.rescue() == Step.FRESH_SPOT) freshSpots++;
        }
        assertEquals(1, freshSpots, "one fresh spot per slow spell, not one every check");
    }

    @Test
    void stuckEscalatesOneStepPerStuckWindow() {
        var st = new State();
        at(st, 0, true, 7, 5);
        assertEquals(Step.FRESH_SPOT, at(st, 400, true, 7, 5).rescue(), "first the gentle rescue: a fresh spot once she's been slow a window");
        assertEquals(Step.NONE, at(st, 880, true, 7, 5).rescue());
        assertEquals(Step.NEAR_BLOCK, at(st, 900, true, 7, 5).rescue());
        assertEquals(Step.NONE, at(st, 1000, true, 7, 5).rescue(), "nothing more until another stuck window passes");
        assertEquals(Step.OUTSIDE, at(st, 1800, true, 7, 5).rescue());
        assertEquals(Step.RECALL, at(st, 2700, true, 7, 5).rescue());
        assertEquals(Step.RECALL, at(st, 3600, true, 7, 5).rescue(), "recall again if she's still stuck");
    }

    @Test
    void progressResetsTheStall() {
        var st = new State();
        at(st, 0, true, 7, 5);
        at(st, 400, true, 7, 5);
        assertEquals(Step.NEAR_BLOCK, at(st, 900, true, 7, 5).rescue());
        assertEquals(Verdict.OK, at(st, 920, true, 8, 6).verdict(), "going again after a rescue isn't judged across the stall");
        assertEquals(Step.FRESH_SPOT, at(st, 1320, true, 8, 6).rescue(), "a new stall gets the gentle rescue first");
        assertEquals(Step.NONE, at(st, 1700, true, 8, 6).rescue(), "a new stall starts its own clock");
        assertEquals(Step.NEAR_BLOCK, at(st, 1820, true, 8, 6).rescue(), "and starts again from the first stuck step");
    }

    @Test
    void notBuildingNeverCountsAndResetsTheWatch() {
        var st = new State();
        at(st, 0, true, 7, 5);
        for (long t = 20; t <= 5000; t += 20) {
            assertEquals(Step.NONE, at(st, t, false, 7, 5).rescue(), "eating, sleeping or fetching is never rescued");
        }
        assertEquals(Verdict.OK, at(st, 5020, true, 7, 5).verdict(), "back to building starts a fresh watch");
        assertEquals(Step.FRESH_SPOT, at(st, 5420, true, 7, 5).rescue());
        assertEquals(Step.NONE, at(st, 5900, true, 7, 5).rescue());
        assertEquals(Step.NEAR_BLOCK, at(st, 5920, true, 7, 5).rescue());
    }

    @Test
    void movingTheProgressWithoutPlacingIsNeitherSlowNorStuck() {
        var st = new State();
        at(st, 0, true, 1, 0);
        for (long t = 20; t <= 3000; t += 20) {
            var d = at(st, t, true, 1 + t, 0);   // skipping blocks that already match: progress moves, nothing placed
            assertEquals(Verdict.OK, d.verdict(), "at " + t);
            assertEquals(Step.NONE, d.rescue(), "at " + t);
        }
    }
}
