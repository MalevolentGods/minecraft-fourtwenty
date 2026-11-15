package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.FlightPlanner.Cell;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlannerTest {
    private final Set<Cell> solid = new HashSet<>();

    private boolean free(int x, int y, int z) { return y >= 0 && !solid.contains(new Cell(x, y, z)); }   // ground below y 0

    private List<Cell> plan(Cell a, Cell b) { return FlightPlanner.plan(a, b, this::free, 24, 4000); }

    private void assertEveryStepFits(List<Cell> path) {
        for (Cell c : path) assertTrue(FlightPlanner.fits(this::free, c.x(), c.y(), c.z()), "body fits at " + c);
        for (int i = 1; i < path.size(); i++) {
            Cell a = path.get(i - 1), b = path.get(i);
            assertTrue(Math.abs(a.x() - b.x()) <= 1 && Math.abs(a.y() - b.y()) <= 1 && Math.abs(a.z() - b.z()) <= 1, "one step at a time");
        }
    }

    @Test
    void openAirIsAStraightLine() {
        var path = plan(new Cell(0, 0, 0), new Cell(6, 3, 0));
        assertEquals(new Cell(0, 0, 0), path.getFirst());
        assertEquals(new Cell(6, 3, 0), path.getLast());
        assertEquals(7, path.size(), "diagonal steps take the short way");
        assertEveryStepFits(path);
    }

    @Test
    void goesAroundAWall() {
        for (int y = 0; y < 3; y++) for (int z = -3; z <= 3; z++) solid.add(new Cell(3, y, z));   // a wall 3 high, 7 wide
        var path = plan(new Cell(0, 0, 0), new Cell(6, 0, 0));
        assertFalse(path.isEmpty());
        assertEveryStepFits(path);
        assertTrue(path.stream().noneMatch(c -> solid.contains(c)), "never through the wall");
    }

    @Test
    void goesOverARoof() {
        // a closed box with the goal on its roof
        for (int x = 2; x <= 6; x++) for (int z = -2; z <= 2; z++) for (int y = 0; y <= 3; y++) solid.add(new Cell(x, y, z));
        var path = plan(new Cell(0, 0, 0), new Cell(4, 4, 0));
        assertFalse(path.isEmpty(), "up the side and onto the roof");
        assertEquals(new Cell(4, 4, 0), path.getLast());
        assertEveryStepFits(path);
    }

    @Test
    void noRouteIntoASealedRoom() {
        for (int x = -1; x <= 1; x++) for (int y = 9; y <= 12; y++) for (int z = -1; z <= 1; z++)
            if (Math.abs(x) == 1 || Math.abs(z) == 1 || y == 9 || y == 12) solid.add(new Cell(x, y, z));
        assertTrue(plan(new Cell(5, 0, 5), new Cell(0, 10, 0)).isEmpty());
    }

    @Test
    void diagonalsNeverCutACorner() {
        // two blocks touching at a corner: squeezing between them diagonally would clip both
        solid.add(new Cell(1, 0, 0)); solid.add(new Cell(1, 1, 0));
        solid.add(new Cell(0, 0, 1)); solid.add(new Cell(0, 1, 1));
        var path = plan(new Cell(0, 0, 0), new Cell(1, 0, 1));
        assertFalse(path.isEmpty());
        assertTrue(path.size() > 2, "goes round, not through the corner");
        assertEveryStepFits(path);
    }

    @Test
    void stopsWithinItsBudget() {
        assertTrue(FlightPlanner.plan(new Cell(0, 0, 0), new Cell(20, 20, 20), this::free, 24, 10).isEmpty(), "10 cells isn't enough");
        assertTrue(FlightPlanner.plan(new Cell(0, 0, 0), new Cell(30, 0, 0), this::free, 24, 4000).isEmpty(), "beyond the radius");
    }

    @Test
    void aGoalSheDoesntFitAtIsNoRoute() {
        solid.add(new Cell(3, 1, 0));   // a ceiling at her head
        assertTrue(plan(new Cell(0, 0, 0), new Cell(3, 0, 0)).isEmpty());
    }
}
