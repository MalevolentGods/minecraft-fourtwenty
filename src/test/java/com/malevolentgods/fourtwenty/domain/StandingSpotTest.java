package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.StandingSpot.Box;
import com.malevolentgods.fourtwenty.domain.StandingSpot.Spot;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StandingSpotTest {
    private final Set<String> solid = new HashSet<>();

    private void block(int x, int y, int z) { solid.add(x + "," + y + "," + z); }
    private boolean isSolid(int x, int y, int z) { return y < 0 || solid.contains(x + "," + y + "," + z); }   // ground at y < 0
    private boolean isFree(int x, int y, int z) { return !isSolid(x, y, z); }

    @Test
    void openGroundIsStandableRightThere() {
        assertEquals(new Spot(0, 0, 0), StandingSpot.nearest(0, 0, 0, 4, this::isSolid, this::isFree, null, 4).orElseThrow());
    }

    @Test
    void sealedBoxPicksTheNearestSpotOutsideIt() {
        // a 1x2 glass box round (0,0,0): walls at x=±1, z=±1, a lid at y=2
        for (int y = 0; y <= 1; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) block(dx, y, dz);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) block(dx, 2, dz);
        var spot = StandingSpot.nearest(0, 0, 0, 4, this::isSolid, this::isFree, new Box(-1, -1, -1, 1, 3, 1), 4).orElseThrow();
        assertFalse(Math.abs(spot.x()) <= 1 && Math.abs(spot.z()) <= 1 && spot.y() <= 2, "not inside the box: " + spot);
        assertTrue(StandingSpot.standable(spot.x(), spot.y(), spot.z(), this::isSolid, this::isFree));
    }

    @Test
    void needsTwoFreeCellsAndSolidUnderfoot() {
        block(0, 1, 0);   // a low ceiling at head height
        assertFalse(StandingSpot.standable(0, 0, 0, this::isSolid, this::isFree));
        assertFalse(StandingSpot.standable(5, 3, 5, this::isSolid, this::isFree), "floating in the air isn't standing");
    }

    @Test
    void prefersOpenSkyAtEqualDistance() {
        block(1, 3, 0);   // a roof over (1,0,0)
        var spot = StandingSpot.nearest(0, 0, 0, 1, this::isSolid, (x, y, z) -> !isSolid(x, y, z) && !(x == 0 && z == 0), null, 4).orElseThrow();
        assertNotEquals(new Spot(1, 0, 0), spot, "the roofed spot loses a tie to an open one");
    }

    @Test
    void belowFindsTheFirstFloorStraightDown() {
        block(2, 4, 2);   // a ledge
        assertEquals(new Spot(2, 5, 2), StandingSpot.below(2, 10, 2, 20, this::isSolid, this::isFree).orElseThrow());
        assertEquals(new Spot(3, 0, 3), StandingSpot.below(3, 10, 3, 20, this::isSolid, this::isFree).orElseThrow());
    }

    @Test
    void nothingWithinRadiusIsEmpty() {
        assertTrue(StandingSpot.nearest(0, 50, 0, 2, this::isSolid, this::isFree, null, 4).isEmpty());
    }
}
