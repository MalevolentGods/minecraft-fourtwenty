package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiftedRulesTest {
    private static final LiftedRules.Tuning T = new LiftedRules.Tuning(Map.of(
            "Landrace", 5.0, "Neutrals", 5.5, "Warm", 5.5, "Green", 6.0, "Cool", 6.0, "Biome", 6.0,
            "Cross", 7.0, "DoubleCross", 8.0, "Ultra", 9.0, "Golden", 10.0), 0.10, 1.25, 1.5);

    @Test
    void familyKeysMatchTheConfig() {
        assertEquals("Landrace", LiftedRules.familyKey(Strain.Family.LANDRACE));
        assertEquals("DoubleCross", LiftedRules.familyKey(Strain.Family.DOUBLE_CROSS));
        assertEquals("Golden", LiftedRules.familyKey(Strain.Family.GOLDEN));
    }

    @Test
    void aLandraceJointIsFiveMinutes() {
        assertEquals(6000, LiftedRules.ticks(Strain.LANDRACE, 1, T));
    }

    @Test
    void goldenGodIsTenMinutesPlusItsPotency() {
        assertEquals(13200, LiftedRules.ticks(Strain.GOLDEN_GOD, 5, T), "10 minutes x 1.10 at potency 5");
        assertEquals(12000, LiftedRules.ticks(Strain.GOLDEN_GOD, 1, T), "no bonus at potency 1");
        assertEquals(12600, LiftedRules.ticks(Strain.GOLDEN_GOD, 3, T), "half the bonus at potency 3");
    }

    @Test
    void everyFamilyGetsItsOwnMinutes() {
        for (Strain s : Strain.values()) {
            double expected = T.minutes().get(LiftedRules.familyKey(s.family())) * LiftedRules.TICKS_PER_MINUTE;
            assertEquals((int) Math.round(expected), LiftedRules.ticks(s, 1, T), s.name());
        }
    }

    @Test
    void speedRunsFromLandraceToGoldenGodByGrade() {
        assertEquals(1.25, LiftedRules.speed(Strain.LANDRACE, T), 1e-9);
        assertEquals(1.5, LiftedRules.speed(Strain.GOLDEN_GOD, T), 1e-9);
        for (Strain s : Strain.values()) {
            double expected = 1.25 + 0.25 * s.grade() / 5.0;
            assertEquals(expected, LiftedRules.speed(s, T), 1e-9, s.name());
            assertEquals(1 / expected, LiftedRules.delayFactor(s, T), 1e-9, s.name());
        }
    }
}
