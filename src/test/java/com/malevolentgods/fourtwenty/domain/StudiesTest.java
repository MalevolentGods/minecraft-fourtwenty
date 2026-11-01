package com.malevolentgods.fourtwenty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StudiesTest {
    @Test
    void lengthsDoubleFromSeventyTwo() {
        assertEquals(72, Studies.units(1));
        assertEquals(144, Studies.units(2));
        assertEquals(2304, Studies.units(6));
    }

    @Test
    void thereIsAStudyForEachMinecraftColour() {
        assertEquals(16, Studies.ALL.size());
        for (var study : Studies.ALL) {
            assertEquals(1, study.tier());
            assertTrue(study.inputs().size() <= 3, study.id());
        }
    }

    @Test
    void aBadBatchComesUpAboutOneInTen() {
        assertTrue(Studies.badBatch(0.09));
        assertFalse(Studies.badBatch(0.11));
    }
}
