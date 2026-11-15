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
    void sheAsksForOneJointOnlyWhenSheNeedsIt() {
        assertEquals(1, SpareJoints.request(true, 0, 10), "none left and about to want one: one joint");
        assertEquals(0, SpareJoints.request(true, 1, 10), "still has one to smoke: no request");
        assertEquals(0, SpareJoints.request(false, 0, 10), "high for a while yet: no request");
        assertEquals(0, SpareJoints.request(true, 0, 0), "no spare: no request (an open one cancels)");
    }
}
