package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 015 T004: an errand is reached close by, progress resets the clock, 30 s without it recalls her. */
class ErrandTest {
    @Test
    void reachedWithinTheArrivalDistance() {
        var e = new Errand(0, 64, 0, Errand.Purpose.JOB);
        assertEquals(Errand.Verdict.REACHED, e.check(1.4, 0));
        assertEquals(Errand.Verdict.ON_HER_WAY, new Errand(0, 64, 0, Errand.Purpose.JOB).check(1.6, 0));
    }

    @Test
    void gettingCloserKeepsHerGoing() {
        var e = new Errand(0, 64, 0, Errand.Purpose.JOB);
        long t = 0;
        for (double d = 60; d > 2; d -= 3) {
            assertEquals(Errand.Verdict.ON_HER_WAY, e.check(d, t));
            t += Errand.PATIENCE_TICKS - 20;   // a long trip in steps, each well inside the patience
        }
    }

    @Test
    void thirtySecondsWithoutProgressRecalls() {
        var e = new Errand(0, 64, 0, Errand.Purpose.BREAK);
        assertEquals(Errand.Verdict.ON_HER_WAY, e.check(40, 0));
        assertEquals(Errand.Verdict.ON_HER_WAY, e.check(39, 300), "a block closer isn't progress, but it's early");
        assertEquals(Errand.Verdict.ON_HER_WAY, e.check(41, Errand.PATIENCE_TICKS - 1));
        assertEquals(Errand.Verdict.RECALL, e.check(39, Errand.PATIENCE_TICKS));
    }

    @Test
    void progressResetsTheClock() {
        var e = new Errand(0, 64, 0, Errand.Purpose.JOB);
        e.check(40, 0);
        e.check(37, Errand.PATIENCE_TICKS - 10);   // 3 closer: progress
        assertEquals(Errand.Verdict.ON_HER_WAY, e.check(37, 2 * Errand.PATIENCE_TICKS - 20));
        assertEquals(Errand.Verdict.RECALL, e.check(37, 2 * Errand.PATIENCE_TICKS));
    }

    @Test
    void sameDestinationIsByBlock() {
        var e = new Errand(5, 64, -3, Errand.Purpose.JOB);
        assertTrue(e.sameDestination(5, 64, -3));
        assertFalse(e.sameDestination(5, 65, -3));
    }
}
