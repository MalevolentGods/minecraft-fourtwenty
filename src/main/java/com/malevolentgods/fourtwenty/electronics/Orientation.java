package com.malevolentgods.fourtwenty.electronics;

import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/**
 * Which way a component points: any of the 24 orientations of a cube, as a facing (where its front points, any of the six directions) and
 * a spin (0 to 3 quarter turns about its front). Its model is drawn facing north with its top up; this turns it.
 * <p>
 * Spin 0 has the top up for a part facing sideways, and north for one facing up or down. Left is the front crossed with the top, so a
 * part facing north has its left to the east, as everywhere in the mod.
 */
public final class Orientation {
    private Orientation() {}

    /** Where a part's top points. */
    public static Direction top(Direction facing, int spin) {
        Direction t = facing.getAxis().isHorizontal() ? Direction.UP : Direction.NORTH;
        for (int i = 0; i < (spin & 3); i++) t = t.getClockWise(facing.getAxis());
        return t;
    }

    /** Where a part's left points: its front crossed with its top. */
    public static Direction left(Direction front, Direction top) {
        var f = front.getNormal();
        var t = top.getNormal();
        return Direction.getNearest(f.getY() * t.getZ() - f.getZ() * t.getY(), f.getZ() * t.getX() - f.getX() * t.getZ(), f.getX() * t.getY() - f.getY() * t.getX());
    }

    /** The world direction of a pin face. */
    public static Direction toWorld(Direction facing, int spin, ComponentType.Face face) {
        Direction top = top(facing, spin);
        return switch (face) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case TOP -> top;
            case BOTTOM -> top.getOpposite();
            case LEFT -> left(facing, top);
            case RIGHT -> left(facing, top).getOpposite();
        };
    }

    /** The spin that puts a part facing {@code facing} with its top toward {@code top}; 0 if none can (top along the facing). */
    public static int spinFor(Direction facing, Direction top) {
        for (int k = 0; k < 4; k++) if (top(facing, k) == top) return k;
        return 0;
    }

    /** A quarter turn of a direction about {@code axis}, one way or the other; along the axis it stays. */
    public static Direction turn(Direction d, Direction.Axis axis, boolean clockwise) {
        if (d.getAxis() == axis) return d;
        return clockwise ? d.getClockWise(axis) : d.getCounterClockWise(axis);
    }

    /** The rotation from the model (facing north, top up) to this orientation, about the block's centre. */
    public static Quaternionf rotation(Direction facing, int spin) {
        Direction top = top(facing, spin), left = left(facing, top), back = facing.getOpposite();
        // columns: where the model's east (its left), up (its top) and south (its back) go
        Matrix3f m = new Matrix3f(left.getStepX(), left.getStepY(), left.getStepZ(), top.getStepX(), top.getStepY(), top.getStepZ(),
                back.getStepX(), back.getStepY(), back.getStepZ());
        return new Quaternionf().setFromNormalized(m);
    }
}
