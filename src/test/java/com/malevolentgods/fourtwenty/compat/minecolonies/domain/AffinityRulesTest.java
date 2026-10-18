package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules.Tuning;
import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Random;
import java.util.function.DoubleSupplier;
import static org.junit.jupiter.api.Assertions.*;

class AffinityRulesTest {
    /** Scripted rolls, in order. */
    private static DoubleSupplier rolls(double... values) {
        var queue = new ArrayDeque<Double>();
        for (double value : values) queue.add(value);
        return queue::removeFirst;
    }
    private static Affinity daily(Preference preference) { return new Affinity(100, preference, 3, 3, -1, 100); }

    @Test void unexposedCitizensHaveNoCravingAndNoDrift() {
        assertFalse(Affinity.UNEXPOSED.wantsDoses(0));
        assertSame(Affinity.UNEXPOSED, AffinityRules.drift(Affinity.UNEXPOSED, 50, Tuning.DEFAULT, rolls(0, 0)));
    }
    @Test void firstExposureRollsOnceAndIsSaved() {
        // 0.25 * 100 = 25 -> past NONE (20), into SMOKE; 0.5 * 100 = 50 -> step 3.
        var exposed = AffinityRules.expose(Affinity.UNEXPOSED, 500, rolls(0.25, 0.5));
        assertEquals(new Affinity(500, Preference.SMOKE, 3, 3, -1, 500), exposed);
        assertTrue(exposed.wantsDoses(500));
        assertSame(exposed, AffinityRules.expose(exposed, 900, rolls()));
    }
    @Test void startingRollsFollowTheWeights() {
        var random = new Random(42);
        var preferences = new EnumMap<Preference, Integer>(Preference.class);
        int[] steps = new int[8];
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            var a = AffinityRules.expose(Affinity.UNEXPOSED, 0, random::nextDouble);
            preferences.merge(a.preference(), 1, Integer::sum);
            steps[a.step()]++;
        }
        assertEquals(0.20, preferences.get(Preference.NONE) / (double) n, 0.01);
        assertEquals(0.35, preferences.get(Preference.SMOKE) / (double) n, 0.01);
        assertEquals(0.20, preferences.get(Preference.EDIBLE) / (double) n, 0.01);
        assertEquals(0.25, preferences.get(Preference.BOTH) / (double) n, 0.01);
        assertEquals(0, steps[0] + steps[6] + steps[7]);
        assertEquals(0.40, steps[3] / (double) n, 0.01);
        assertEquals(0.10, steps[1] / (double) n, 0.01);
    }
    @Test void driftIsTenPercentAndThreeQuartersUp() {
        var a = daily(Preference.SMOKE);
        assertEquals(3, AffinityRules.drift(a, 200, Tuning.DEFAULT, rolls(0.10, 0)).step());   // no change at exactly 10%
        assertEquals(4, AffinityRules.drift(a, 200, Tuning.DEFAULT, rolls(0.05, 0.74)).step());
        assertEquals(2, AffinityRules.drift(a, 200, Tuning.DEFAULT, rolls(0.05, 0.75)).step());
        var random = new Random(7);
        int up = 0, down = 0;
        for (int i = 0; i < 200_000; i++) {
            int step = AffinityRules.drift(a, 200, Tuning.DEFAULT, random::nextDouble).step();
            if (step == 4) up++; else if (step == 2) down++;
        }
        assertEquals(0.075, up / 200_000.0, 0.003);
        assertEquals(0.025, down / 200_000.0, 0.002);
    }
    @Test void stepsAreClampedAtNoInterest() {
        var low = new Affinity(0, Preference.BOTH, 1, 0, -1, -1);
        assertEquals(0, AffinityRules.drift(low, 10, Tuning.DEFAULT, rolls(0, 0.9)).step());
        assertFalse(low.wantsDoses(10));
    }
    @Test void reachingTheTopStartsATwoDayBreakThenRestoresTheOriginalStep() {
        var heavy = new Affinity(0, Preference.SMOKE, 2, 6, -1, -1);
        var broke = AffinityRules.drift(heavy, 1_000, Tuning.DEFAULT, rolls(0, 0));
        assertEquals(7, broke.step());
        assertEquals(49_000, broke.breakUntil());
        assertTrue(broke.onBreak(48_999) && !broke.wantsDoses(48_999));
        assertSame(broke, AffinityRules.drift(broke, 2_000, Tuning.DEFAULT, rolls(0, 0)), "no rolls on a break");
        assertSame(broke, AffinityRules.tick(broke, 48_999));
        var back = AffinityRules.tick(broke, 49_000);
        assertEquals(new Affinity(0, Preference.SMOKE, 2, 2, -1, -1), back);
        assertTrue(back.wantsDoses(49_000));
    }
    @Test void bongSessionsExposeNewcomersAndRollAtMostOncePerDay() {
        var exposed = AffinityRules.bongSession(Affinity.UNEXPOSED, 1_000, Tuning.DEFAULT, rolls(0.9, 0.5));
        assertEquals(1_000, exposed.exposedAt());
        assertEquals(1_000, exposed.lastBongRoll(), "first exposure is not also a drift roll");
        assertSame(exposed, AffinityRules.bongSession(exposed, 24_999, Tuning.DEFAULT, rolls(0, 0)));
        var rolled = AffinityRules.bongSession(exposed, 25_000, Tuning.DEFAULT, rolls(0, 0));
        assertEquals(exposed.step() + 1, rolled.step());
        assertEquals(25_000, rolled.lastBongRoll());
        var noChange = AffinityRules.bongSession(exposed, 25_000, Tuning.DEFAULT, rolls(0.5));
        assertEquals(exposed.step(), noChange.step());
        assertEquals(25_000, noChange.lastBongRoll(), "a roll that changes nothing still uses the day's roll");
    }
    @Test void intervalsScaleFromTheDailyInterval() {
        assertArrayEquals(new int[]{96_000, 48_000, 24_000, 12_000, 8_000, 6_000},
                Arrays.stream(new int[]{1, 2, 3, 4, 5, 6}).map(step -> AffinityRules.intervalTicks(step, 24_000)).toArray());
    }
    @Test void invalidRecordsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Affinity(-1, Preference.SMOKE, 3, 3, -1, -1));
        assertThrows(IllegalArgumentException.class, () -> new Affinity(0, Preference.SMOKE, 3, 8, -1, -1));
    }
}
