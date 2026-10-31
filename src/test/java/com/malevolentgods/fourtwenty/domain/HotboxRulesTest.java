package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HotboxRulesTest {
    @Test void aPlayerInTheAreaKeepsItFull() {
        assertEquals(HotboxRules.HOLD_TICKS, HotboxRules.next(5, true, true, 20));
        assertEquals(HotboxRules.HOLD_TICKS, HotboxRules.next(0, true, true, 20), "coming back starts it again");
    }

    @Test void itRunsDownOnceEveryoneLeavesTheArea() {
        assertEquals(HotboxRules.HOLD_TICKS - 20, HotboxRules.next(HotboxRules.HOLD_TICKS, false, true, 20));
        assertEquals(0, HotboxRules.next(10, false, true, 20), "never below zero");
    }

    @Test void anHourIsAnHour() {
        int left = HotboxRules.HOLD_TICKS, ticks = 0;
        while (left > 0) {
            left = HotboxRules.next(left, false, true, 20);
            ticks += 20;
        }
        assertEquals(20 * 3600, ticks);
    }

    @Test void itLetsGoWhenNobodyIsOnline() {
        assertEquals(0, HotboxRules.next(HotboxRules.HOLD_TICKS, false, false, 20));
    }

    @Test void minutesRoundUp() {
        assertEquals(60, HotboxRules.minutesLeft(HotboxRules.HOLD_TICKS));
        assertEquals(1, HotboxRules.minutesLeft(1));
        assertEquals(0, HotboxRules.minutesLeft(0));
    }
}
