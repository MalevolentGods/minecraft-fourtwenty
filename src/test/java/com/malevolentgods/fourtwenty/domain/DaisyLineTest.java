package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.DaisyLine.Contact;
import com.malevolentgods.fourtwenty.domain.DaisyLine.Item;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaisyLineTest {
    /** A little builder: cables numbered as given, devices numbered from 100. */
    private static final class Line {
        final Map<Integer, Set<Integer>> cables = new HashMap<>();
        final Map<Integer, List<Contact>> devices = new HashMap<>();

        Line run(int... ids) {
            for (int id : ids) cables.computeIfAbsent(id, k -> new HashSet<>());
            for (int i = 0; i + 1 < ids.length; i++) join(ids[i], ids[i + 1]);
            return this;
        }

        Line join(int a, int b) {
            cables.computeIfAbsent(a, k -> new HashSet<>()).add(b);
            cables.computeIfAbsent(b, k -> new HashSet<>()).add(a);
            return this;
        }

        Line device(int id, int... cablesTouched) {
            for (int c : cablesTouched) devices.computeIfAbsent(id, k -> new ArrayList<>()).add(new Contact(c, 0));
            return this;
        }

        Line deviceAt(int id, int cable, int face) {
            devices.computeIfAbsent(id, k -> new ArrayList<>()).add(new Contact(cable, face));
            return this;
        }

        DaisyLine.Result order() { return DaisyLine.order(cables, devices); }

        String text() {
            var r = order();
            if (!r.ok()) return r.failure().name();
            StringBuilder s = new StringBuilder();
            for (Item i : r.items()) s.append(i.device() ? "D" : "c").append(i.id()).append(' ');
            return s.toString().trim();
        }
    }

    @Test void aStraightLineReadsFromItsLowerEnd() {
        assertEquals("c1 c2 c3", new Line().run(3, 2, 1).text());
    }

    @Test void blocksBesideTheLineFollowTheirCable() {
        assertEquals("c1 D100 c2 D101 c3", new Line().run(1, 2, 3).device(100, 1).device(101, 2).text());
    }

    @Test void blocksInlineJoinRuns() {
        // Hookah, cable, machine, cable, machine: runs {1}, {2}, {3} joined by 100 and 101
        assertEquals("c1 D100 c2 D101 c3", new Line().run(1).run(2).run(3).device(100, 1, 2).device(101, 2, 3).text());
    }

    @Test void inlineAndBesideMix() {
        assertEquals("c1 c2 D102 D100 c3 c4", new Line().run(1, 2).run(3, 4).device(100, 2, 3).device(102, 2).text());
    }

    @Test void twoBlocksBesideOneCableGoByFace() {
        assertEquals("c1 D101 D100 c2", new Line().run(1, 2).deviceAt(100, 1, 3).deviceAt(101, 1, 1).text());
    }

    @Test void aBlockAtACornerIsBesideNotInline() {
        // the line turns at 2-3 and the block touches both: one run, so beside, after cable 2
        assertEquals("c1 c2 D100 c3 c4", new Line().run(1, 2, 3, 4).device(100, 2, 3).text());
    }

    @Test void aBranchCarriesNothing() {
        assertEquals("BRANCH", new Line().run(1, 2, 3).join(2, 4).text());
    }

    @Test void aRingCarriesNothing() {
        assertEquals("RING", new Line().run(1, 2, 3, 4).join(4, 1).text());
        assertEquals("RING", new Line().run(1, 2).run(3, 4).device(100, 2, 3).device(101, 4, 1).text());
    }

    @Test void aBlockTouchingThreeRunsIsABranch() {
        assertEquals("BRANCH", new Line().run(1).run(2).run(3).device(100, 1, 2, 3).text());
    }

    @Test void anInlineBlockMustMeetRunsAtTheirEnds() {
        assertEquals("BRANCH", new Line().run(1, 2, 3).run(5).device(100, 2, 5).text());
    }

    @Test void twoInlineBlocksOnOneEndAreABranch() {
        assertEquals("BRANCH", new Line().run(1, 2).run(3).run(4).device(100, 2, 3).device(101, 2, 4).text());
    }

    @Test void runsThatDontMeetAreNotOneLine() {
        assertEquals("BRANCH", new Line().run(1, 2).run(5, 6).text());
    }

    @Test void nothingIsEmpty() {
        assertEquals("EMPTY", new Line().text());
    }

    @Test void reversedReadsTheOtherWay() {
        var r = new Line().run(1, 2).device(100, 1).order().reversed();
        assertEquals(2, r.firstCable());
        assertEquals(1, r.lastCable());
        assertTrue(r.items().get(1).device());
    }

    @Test void aSingleCableWithBlocksOnBothSides() {
        assertEquals("c1 D100 D101", new Line().run(1).deviceAt(100, 1, 0).deviceAt(101, 1, 2).text());
    }
}
