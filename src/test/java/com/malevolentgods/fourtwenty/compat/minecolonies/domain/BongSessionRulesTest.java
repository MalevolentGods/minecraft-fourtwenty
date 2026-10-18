package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BongSessionRulesTest {
    @Test void bonusLastsUntilItsDeadline() {
        assertTrue(BongSessionRules.active(6_000, 5_999));
        assertFalse(BongSessionRules.active(6_000, 6_000));
        assertFalse(BongSessionRules.active(0, 0));
    }
    @Test void staleOrMissingBonusIsAlwaysRefreshed() {
        assertTrue(BongSessionRules.shouldRefresh(0, 100, 6_000));
        assertTrue(BongSessionRules.shouldRefresh(6_000, 6_000, 6_000));
    }
    @Test void activeBonusIsOnlyExtendedOnceItHasRunDownAFullStep() {
        // Exposed at t=1000 for 6000 ticks: expires at 7000.
        assertFalse(BongSessionRules.shouldRefresh(7_000, 1_000, 6_000));
        assertFalse(BongSessionRules.shouldRefresh(7_000, 1_000 + BongSessionRules.EXTEND_EVERY_TICKS - 1, 6_000));
        assertTrue(BongSessionRules.shouldRefresh(7_000, 1_000 + BongSessionRules.EXTEND_EVERY_TICKS, 6_000));
    }
}
