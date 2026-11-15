package com.malevolentgods.fourtwenty.domain;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 014 T032: the move library, the flourishes, routines, and every routine shipped with the album. */
class DanceMovesTest {
    private static final DanceRules.Tuning T = DanceRules.Tuning.of(0.12, 2.0, 100);
    private static final long[] BEATS_120 = new long[400];

    static {
        for (int i = 0; i < BEATS_120.length; i++) BEATS_120[i] = i * 500L;
    }

    private static void withinLimits(DanceMoves.Body b, String what) {
        for (var limb : List.of(b.leftArm(), b.rightArm())) {
            assertTrue(Math.abs(limb.pitch()) <= 180 && Math.abs(limb.roll()) <= 100 && Math.abs(limb.yaw()) <= 45, what + " arm " + limb);
        }
        var h = b.head();
        assertTrue(Math.abs(h.pitch()) <= 30 && Math.abs(h.yaw()) <= 45 && Math.abs(h.roll()) <= 30, what + " head " + h);
        assertTrue(Math.abs(b.leftLeg()) <= 70 && Math.abs(b.rightLeg()) <= 70, what + " legs");
        assertTrue(Math.abs(b.lean()) <= 20 && Math.abs(b.tilt()) <= 15, what + " lean/tilt");
        assertTrue(Math.abs(b.bob()) <= 0.4, what + " bob");
    }

    @Test
    void everyMoveStaysWithinJointLimitsOverTwoBars() {
        for (String move : DanceMoves.MOVES) {
            for (double b = 0; b < 8; b += 0.05) {
                withinLimits(DanceMoves.move(move, b, b / 8), move + " at beat " + b);
            }
        }
    }

    @Test
    void flourishesStartAndEndAtRestAndStayWithinLimits() {
        for (String f : DanceMoves.FLOURISHES) {
            for (double s : new double[] {0, 1}) {
                var b = DanceMoves.flourish(f, s);
                double turn = s == 1 ? b.turn() % 360 : b.turn();
                assertEquals(0, b.leftArm().pitch() + b.rightArm().pitch() + b.leftArm().roll() + b.rightArm().roll(), 1e-6, f + " arms at " + s);
                assertEquals(0, b.bob(), 1e-6, f + " bob at " + s);
                assertEquals(0, turn, 1e-6, f + " turn at " + s);
                assertEquals(0, b.leftLeg() + b.rightLeg() + b.lean(), 1e-6, f + " legs and lean at " + s);
            }
            for (double s = 0; s <= 1; s += 0.02) {
                var b = DanceMoves.flourish(f, s);
                withinLimits(new DanceMoves.Body(b.leftArm(), b.rightArm(), b.head(), b.leftLeg(), b.rightLeg(), b.lean(), b.tilt(), b.bob(), 0), f + " at " + s);
            }
            assertTrue(DanceMoves.flourishMs(f) >= 500 && DanceMoves.flourishMs(f) <= 2000, f + " length");
        }
    }

    @Test
    void aRoutineFindsItsSectionAndFlourish() {
        var r = new Routine(List.of(new Routine.Section(0, 10_000, "intro", "groove", 1), new Routine.Section(10_000, 20_000, "drop", "lean_pump", 1)),
                List.of(new Routine.Flourish(12_000, "jump_kick")));
        assertEquals(0, r.sectionAt(0));
        assertEquals(0, r.sectionAt(9_999));
        assertEquals(1, r.sectionAt(10_000));
        assertEquals(-1, r.sectionAt(20_000), "after the last section");
        assertNull(r.flourishAt(11_999));
        assertNotNull(r.flourishAt(12_000));
        assertNotNull(r.flourishAt(12_799));
        assertNull(r.flourishAt(12_800), "a jump kick lasts 0.8 s");
    }

    @Test
    void aSectionChangeBlendsOverHalfASecond() {
        var r = new Routine(List.of(new Routine.Section(0, 10_000, "intro", "float", 1), new Routine.Section(10_000, 20_000, "drop", "lean_pump", 1)),
                List.of());
        double before = DanceRules.pose(120, BEATS_120, new long[0], 9_999, false, T, r).body().lean();
        double start = DanceRules.pose(120, BEATS_120, new long[0], 10_000, false, T, r).body().lean();
        double mid = DanceRules.pose(120, BEATS_120, new long[0], 10_250, false, T, r).body().lean();
        double after = DanceRules.pose(120, BEATS_120, new long[0], 10_500, false, T, r).body().lean();
        assertEquals(before, start, 0.5, "no jump at the change");
        assertTrue(mid > before && mid < after, "on its way: " + before + " " + mid + " " + after);
    }

    @Test
    void aFlourishTakesThePlaceOfTheAccentSpin() {
        var r = new Routine(List.of(new Routine.Section(0, 60_000, "verse", "groove", 1)), List.of(new Routine.Flourish(5_000, "big_spin_point")));
        var during = DanceRules.pose(120, BEATS_120, new long[] {5_000}, 5_200, false, T, r);
        assertEquals(0, during.spinDeg(), "no accent spin on top of the flourish");
        assertTrue(during.body().turn() > 0, "the flourish turns her instead");
    }

    @Test
    void everyShippedRoutineUsesKnownMovesInOrder() throws Exception {
        for (int t = 1; t <= 7; t++) {
            String path = "/assets/fourtwenty/midnight_machine/track" + t + ".routine.json";
            try (var in = DanceMovesTest.class.getResourceAsStream(path)) {
                assertNotNull(in, path + " ships");
                var root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                long last = -1;
                for (var e : root.getAsJsonArray("sections")) {
                    var o = e.getAsJsonObject();
                    String move = o.get("move").getAsString();
                    assertTrue(DanceMoves.MOVES.contains(move), path + ": unknown move " + move);
                    long start = o.get("startMs").getAsLong();
                    assertTrue(start > last && o.get("endMs").getAsLong() > start, path + ": sections in order");
                    last = start;
                }
                for (var e : root.getAsJsonArray("flourishes")) {
                    String move = e.getAsJsonObject().get("move").getAsString();
                    assertTrue(DanceMoves.FLOURISHES.contains(move), path + ": unknown flourish " + move);
                }
            }
        }
    }
}
