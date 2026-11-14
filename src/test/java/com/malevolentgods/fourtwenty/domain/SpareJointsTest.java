package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpareJointsTest {
    @Test
    void spareIsStockBeyondWhatCitizensAreOwed() {
        assertEquals(7, SpareJoints.spare(12, 5));
        assertEquals(0, SpareJoints.spare(5, 5), "exactly enough for the citizens leaves nothing spare");
        assertEquals(0, SpareJoints.spare(3, 5), "short of what citizens are owed: never negative");
        assertEquals(4, SpareJoints.spare(4, -2), "a bad owed count can't create joints");
    }

    @Test
    void sheAsksForHerStashLessWhatSheCarriesWithinTheSpare() {
        assertEquals(3, SpareJoints.request(3, 0, 10));
        assertEquals(1, SpareJoints.request(3, 2, 10));
        assertEquals(2, SpareJoints.request(3, 0, 2), "never more than the spare");
        assertEquals(0, SpareJoints.request(3, 0, 0), "no spare: no request (an open one cancels)");
        assertEquals(0, SpareJoints.request(3, 5, 10), "carrying more than her stash: no request");
    }
}
