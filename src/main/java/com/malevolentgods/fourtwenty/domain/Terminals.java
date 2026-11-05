package com.malevolentgods.fourtwenty.domain;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * Which faces of a powered block are its terminals. Seen from a block's front, + is on its left and − on its right, like a battery on its
 * side (user, 2026-11-05). A Hit Box also has a second, isolated winding: + on its front and − on its back. See
 * specs/007-polarity-circuits/contracts/terminals.md.
 */
public final class Terminals {
    private Terminals() {}

    public enum Sign { PLUS, MINUS }

    /** The + face of a block whose front faces {@code front}: its left as you look at it, so clockwise from the front seen from above. */
    public static Direction plus(Direction front) { return front.getClockWise(); }

    public static Direction minus(Direction front) { return front.getCounterClockWise(); }

    /** The terminal on {@code side} of an ordinary powered block, or null when that face doesn't connect. */
    public static @Nullable Sign at(Direction front, @Nullable Direction side) {
        if (side == null) return null;
        if (side == plus(front)) return Sign.PLUS;
        if (side == minus(front)) return Sign.MINUS;
        return null;
    }

    /** The Hit Box's second winding: + on its front, − on its back. */
    public static @Nullable Sign highWinding(Direction front, @Nullable Direction side) {
        if (side == null) return null;
        if (side == front) return Sign.PLUS;
        if (side == front.getOpposite()) return Sign.MINUS;
        return null;
    }
}
