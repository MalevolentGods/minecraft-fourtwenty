package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispensaryPackRulesTest {
    @Test
    void anOldHutKeepsItsLevel() {
        assertEquals("craftsmanship/luxury/dispensary5.blueprint", DispensaryPackRules.newPath("dispensary/dispensary5.blueprint"));
        assertEquals("/craftsmanship/luxury/dispensary1.blueprint", DispensaryPackRules.newPath("/dispensary/dispensary1.blueprint"));
    }

    @Test
    void aSensiHeightsHutIsLeftAlone() {
        assertFalse(DispensaryPackRules.isOldPack(DispensaryPackRules.PACK));
        assertTrue(DispensaryPackRules.isOldPack("FourTwenty"));
        assertEquals("craftsmanship/luxury/dispensary3.blueprint", DispensaryPackRules.newPath("craftsmanship/luxury/dispensary3.blueprint"));
    }
}
