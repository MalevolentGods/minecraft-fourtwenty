package com.malevolentgods.fourtwenty.electronics;

import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The 24 orientations: every face somewhere different, the model turned the same way the pins are, and twisting reaching them all. */
class OrientationTest {
    private static final ComponentType.Face[] FACES = ComponentType.Face.values();

    @Test void eachOrientationPutsTheSixFacesOnTheSixSides() {
        Set<List<Direction>> seen = new HashSet<>();
        for (Direction facing : Direction.values()) {
            for (int spin = 0; spin < 4; spin++) {
                EnumSet<Direction> sides = EnumSet.noneOf(Direction.class);
                List<Direction> where = new java.util.ArrayList<>();
                for (ComponentType.Face f : FACES) {
                    Direction d = Orientation.toWorld(facing, spin, f);
                    sides.add(d);
                    where.add(d);
                }
                assertEquals(6, sides.size(), facing + " spin " + spin);
                assertEquals(facing, Orientation.toWorld(facing, spin, ComponentType.Face.FRONT));
                seen.add(where);
            }
        }
        assertEquals(24, seen.size(), "24 different orientations");
    }

    @Test void sidewaysWithNoSpinIsAsBefore() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            assertEquals(Direction.UP, Orientation.toWorld(facing, 0, ComponentType.Face.TOP));
            assertEquals(facing.getClockWise(), Orientation.toWorld(facing, 0, ComponentType.Face.LEFT), "left is the clockwise side");
            assertEquals(facing.getCounterClockWise(), Orientation.toWorld(facing, 0, ComponentType.Face.RIGHT));
        }
    }

    /** The model faces north with its top up and its left east: the rotation must carry each of those where the pins are. */
    @Test void theModelTurnsWhereThePinsAre() {
        Direction[] model = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
        ComponentType.Face[] faces = {ComponentType.Face.FRONT, ComponentType.Face.BACK, ComponentType.Face.LEFT, ComponentType.Face.RIGHT,
                ComponentType.Face.TOP, ComponentType.Face.BOTTOM};
        for (Direction facing : Direction.values()) {
            for (int spin = 0; spin < 4; spin++) {
                var q = Orientation.rotation(facing, spin);
                for (int i = 0; i < 6; i++) {
                    Vector3f v = new Vector3f(model[i].getStepX(), model[i].getStepY(), model[i].getStepZ()).rotate(q);
                    Direction got = Direction.getNearest(v.x, v.y, v.z);
                    assertEquals(Orientation.toWorld(facing, spin, faces[i]), got, facing + " spin " + spin + " " + faces[i]);
                }
            }
        }
    }

    @Test void twistingReachesEveryOrientationAndFourTwistsComeBack() {
        record O(Direction facing, int spin) {}
        Set<O> reached = new HashSet<>();
        java.util.ArrayDeque<O> queue = new java.util.ArrayDeque<>(List.of(new O(Direction.NORTH, 0)));
        while (!queue.isEmpty()) {
            O o = queue.poll();
            if (!reached.add(o)) continue;
            for (Direction.Axis axis : Direction.Axis.values()) {
                for (boolean cw : new boolean[]{true, false}) {
                    Direction f = Orientation.turn(o.facing(), axis, cw), t = Orientation.turn(Orientation.top(o.facing(), o.spin()), axis, cw);
                    O next = new O(f, Orientation.spinFor(f, t));
                    assertEquals(t, Orientation.top(next.facing(), next.spin()), "the top lands where it was turned");
                    queue.add(next);
                }
            }
        }
        assertEquals(24, reached.size());
        for (Direction.Axis axis : Direction.Axis.values()) {
            Direction f = Direction.NORTH, t = Direction.UP;
            for (int i = 0; i < 4; i++) {
                f = Orientation.turn(f, axis, true);
                t = Orientation.turn(t, axis, true);
            }
            assertTrue(f == Direction.NORTH && t == Direction.UP, "four quarter turns about " + axis);
        }
    }
}
