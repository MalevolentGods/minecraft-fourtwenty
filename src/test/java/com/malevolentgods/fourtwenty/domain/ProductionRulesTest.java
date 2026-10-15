package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.ProductionRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionRulesTest {
    @Test void completedLevelsHaveIndividualItemTargets() {
        int[] expected = {0, 16, 32, 64, 96, 128};
        for (int level = 0; level <= 5; level++) assertEquals(expected[level], ProductionRules.stockTarget(level));
        assertEquals(0, ProductionRules.stockTarget(-1));
        assertEquals(0, ProductionRules.stockTarget(6));
    }
    @Test void replenishmentIsBoundedAndDeduplicated() {
        assertEquals(16, ProductionRules.batch(5, 0, false));
        assertEquals(1, ProductionRules.batch(1, 15, false));
        assertEquals(0, ProductionRules.batch(1, 16, false));
        assertEquals(0, ProductionRules.batch(1, 64, false));
        assertEquals(0, ProductionRules.batch(5, 0, true));
        assertEquals(0, ProductionRules.batch(0, 0, false));
    }
}
