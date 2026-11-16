package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VapeRulesTest {
    @Test
    void anEmptyPenTakesTwoOils() {
        assertEquals(2, VapeRules.oilsThatFit(0, 5));
        assertEquals(1, VapeRules.oilsThatFit(0, 1));
    }

    @Test
    void onlyWholeOilsGoIn() {
        assertEquals(1, VapeRules.oilsThatFit(8, 3));
        assertEquals(1, VapeRules.oilsThatFit(3, 3));   // 3 + 8 = 11 fits, 19 wouldn't
        assertEquals(0, VapeRules.oilsThatFit(9, 3));
        assertEquals(0, VapeRules.oilsThatFit(16, 3));
    }

    @Test
    void nothingOfferedNothingTaken() {
        assertEquals(0, VapeRules.oilsThatFit(0, 0));
    }

    @Test
    void anOilLastsEightPipeHits() {
        assertEquals(8, VapeRules.HITS_PER_OIL);
        assertEquals(16, VapeRules.CAPACITY);
    }
}
