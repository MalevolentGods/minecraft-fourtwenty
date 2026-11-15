package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 015 T013: doors and gates are a way through, straight on and at a cost; anything else is a wall. */
class FlightPlannerDoorsTest {
    // a wall along x = 5, floor at y = 0 (y below 0 solid), with whatever openings each test gives it
    private static FlightPlanner.Free world(int... gapsZ) {
        return (x, y, z) -> {
            if (y < 0) return false;
            if (x != 5) return true;
            for (int g : gapsZ) if (z == g && y <= 1) return true;
            return false;
        };
    }

    private static FlightPlanner.Doorway doorAt(int z) {
        return (x, y, zz) -> x == 5 && zz == z && (y == 0 || y == 1);
    }

    private static boolean through(List<FlightPlanner.Cell> route, int z) {
        return route.stream().anyMatch(c -> c.x() == 5 && c.z() == z);
    }

    @Test
    void aClosedDoorIsAWayThrough() {
        var route = FlightPlanner.plan(new FlightPlanner.Cell(0, 0, 0), new FlightPlanner.Cell(10, 0, 0), world(), doorAt(0), 12, 4000);
        assertFalse(route.isEmpty(), "a route through the door");
        assertTrue(through(route, 0), "it goes through the doorway");
    }

    @Test
    void withoutADoorTheWallStopsHer() {
        var route = FlightPlanner.plan(new FlightPlanner.Cell(0, 0, 0), new FlightPlanner.Cell(10, 0, 0), world(), FlightPlanner.Doorway.NONE, 12, 4000);
        assertTrue(route.isEmpty(), "an iron door or trapdoor isn't a doorway: no way through");
    }

    @Test
    void anOpenWayIsPreferredWhenItsAsShort() {
        // a door at z = 1 and an open gap at z = -1: the same detour either way, but the door costs extra
        var route = FlightPlanner.plan(new FlightPlanner.Cell(0, 0, 0), new FlightPlanner.Cell(10, 0, 0), world(-1), doorAt(1), 12, 4000);
        assertTrue(through(route, -1), "she takes the open gap");
        assertFalse(through(route, 1), "not the door");
    }

    @Test
    void sheCrossesADoorwayStraightOn() {
        var route = FlightPlanner.plan(new FlightPlanner.Cell(0, 0, 3), new FlightPlanner.Cell(10, 0, -3), world(), doorAt(0), 12, 4000);
        assertFalse(route.isEmpty());
        for (int i = 1; i < route.size(); i++) {
            var a = route.get(i - 1);
            var b = route.get(i);
            boolean intoDoor = b.x() == 5 && b.z() == 0, outOfDoor = a.x() == 5 && a.z() == 0;
            if (intoDoor || outOfDoor) {
                assertTrue(a.y() == b.y() && (a.x() == b.x() || a.z() == b.z()), "straight and level through the doorway: " + a + " -> " + b);
            }
        }
    }
}
