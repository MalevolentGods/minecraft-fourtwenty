package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.HoverCover.Pos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HoverCoverTest {
    private static List<Pos> row(int fromX, int toX, int y, int z) {
        List<Pos> out = new ArrayList<>();
        for (int x = fromX; x <= toX; x++) out.add(new Pos(x, y, z));
        return out;
    }

    @Test
    void picksThePointThatReachesMostOfTheRow() {
        var next = row(0, 20, 0, 0);   // a long wall row
        var candidates = List.of(new Pos(0, 3, 2), new Pos(5, 3, 2), new Pos(10, 3, 2));
        var best = HoverCover.choose(candidates, new Pos(0, 0, 0), next, 9).orElseThrow();
        assertEquals(new Pos(5, 3, 2), best, "reaches the current block and the most of what's next ((10,3,2) can't reach (0,0,0))");
    }

    @Test
    void mustReachTheCurrentBlock() {
        var next = row(0, 30, 0, 0);
        var candidates = List.of(new Pos(15, 3, 2), new Pos(3, 3, 2));
        assertEquals(new Pos(3, 3, 2), HoverCover.choose(candidates, new Pos(0, 0, 0), next, 9).orElseThrow());
    }

    @Test
    void tiesGoToTheHigherPoint() {
        var next = List.of(new Pos(0, 0, 0));
        var candidates = List.of(new Pos(2, 1, 0), new Pos(2, 4, 0));
        assertEquals(new Pos(2, 4, 0), HoverCover.choose(candidates, new Pos(0, 0, 0), next, 9).orElseThrow());
    }

    @Test
    void neverWhereSheWouldBeBuiltIn() {
        var next = List.of(new Pos(0, 0, 0), new Pos(1, 0, 0), new Pos(1, 2, 0));
        var candidates = List.of(new Pos(1, 0, 0), new Pos(1, 1, 0), new Pos(0, 1, 0), new Pos(4, 1, 0));
        var best = HoverCover.choose(candidates, new Pos(0, 0, 0), next, 9).orElseThrow();
        assertEquals(new Pos(4, 1, 0), best, "not on a next block, not under one, not over the block she's placing");
    }

    @Test
    void noCandidatesIsEmpty() {
        assertTrue(HoverCover.choose(List.of(), new Pos(0, 0, 0), List.of(new Pos(0, 0, 0)), 9).isEmpty());
        assertTrue(HoverCover.choose(List.of(new Pos(50, 0, 0)), new Pos(0, 0, 0), List.of(), 9).isEmpty(), "all out of reach");
    }
}
