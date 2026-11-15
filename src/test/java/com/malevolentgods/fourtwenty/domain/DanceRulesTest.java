package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 014 T022: the dance follows the beat map, and is only ever a pose. */
class DanceRulesTest {
    private static final DanceRules.Tuning T = DanceRules.Tuning.of(0.12, 2.0, 100);
    private static final long[] BEATS_120 = beats(500, 0, 200);   // 120 BPM

    private static long[] beats(long period, long start, int n) {
        long[] out = new long[n];
        for (int i = 0; i < n; i++) out[i] = start + i * period;
        return out;
    }

    private static DanceRules.Pose at(long ms) {
        return DanceRules.pose(120, BEATS_120, new long[0], ms, false, T);
    }

    @Test
    void theBobPeaksOnEachBeat() {
        for (long beat : new long[] {1000, 1500, 7000}) {
            double peak = at(beat).bob();
            assertEquals(0.12, peak, 1e-9, "full height on the beat at " + beat);
            assertTrue(at(beat + 50).bob() < peak && at(beat - 50).bob() < peak, "lower a frame either side of " + beat);
            assertTrue(at(beat + 250).bob() < 0.01, "down between beats");
        }
    }

    @Test
    void doubleTimeTracksBobEveryOtherBeat() {
        long[] beats = beats(350, 0, 200);   // ~171 BPM
        var onBeat = DanceRules.pose(171, beats, new long[0], 700, false, T);
        var offBeat = DanceRules.pose(171, beats, new long[0], 1050, false, T);
        assertEquals(0.12, onBeat.bob(), 1e-9);
        assertTrue(offBeat.bob() < 0.01, "the beat in between is skipped");
    }

    @Test
    void aSpinStartsOnAnAccentLastsFourTenthsAndRespectsTheCooldown() {
        long[] accents = {2000, 2600, 4100};
        assertEquals(0, DanceRules.spin(accents, 1999, 2000));
        assertTrue(DanceRules.spin(accents, 2200, 2000) > 0, "spinning");
        assertEquals(0, DanceRules.spin(accents, 2400, 2000), "done after 0.4 s");
        assertEquals(0, DanceRules.spin(accents, 2700, 2000), "no second spin within the cooldown");
        assertTrue(DanceRules.spin(accents, 4200, 2000) > 0, "the next one after it");
        assertEquals(180, DanceRules.spin(accents, 2200, 2000), 1e-9, "halfway round halfway through");
    }

    @Test
    void theSwayTakesTwoBeats() {
        assertEquals(0, at(1000).swayDeg(), 1e-9);
        assertEquals(9, Math.abs(at(1250).swayDeg()), 1e-9);
        assertEquals(0, at(1500).swayDeg(), 1e-9);
        assertEquals(-at(1250).swayDeg(), at(1750).swayDeg(), 1e-9, "the other way on the next beat");
        assertEquals(at(1250).swayDeg(), at(2250).swayDeg(), 1e-9, "back again two beats later");
    }

    @Test
    void waitingGivesBiggerArmsThanMoving() {
        var still = DanceRules.pose(120, BEATS_120, new long[0], 1000, false, T);
        var moving = DanceRules.pose(120, BEATS_120, new long[0], 1000, true, T);
        assertTrue(still.armPitch() > moving.armPitch());
    }

    @Test
    void anEmptyBeatMapFollowsTheFallbackTempo() {
        // 100 BPM: a beat every 600 ms, no spins
        var onBeat = DanceRules.pose(0, new long[0], new long[] {600}, 1200, false, T);
        var between = DanceRules.pose(0, new long[0], new long[] {600}, 1500, false, T);
        assertEquals(0.12, onBeat.bob(), 1e-9);
        assertTrue(between.bob() < 0.01);
        assertEquals(0, onBeat.spinDeg());
    }
}
