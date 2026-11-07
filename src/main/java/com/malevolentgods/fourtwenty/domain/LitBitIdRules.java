package com.malevolentgods.fourtwenty.domain;

import java.util.Set;

/** Lit Bit IDs: whole numbers from 1 to 65,535. A lamp placed by hand takes the lowest one not in use near it. */
public final class LitBitIdRules {
    private LitBitIdRules() {}

    public static final int MIN = 1, MAX = 65_535;

    public static int clamp(int id) { return Math.max(MIN, Math.min(MAX, id)); }

    /** The lowest ID at or above {@code from} that isn't in {@code used}; {@code from} itself when every ID is taken. */
    public static int nextFree(Set<Integer> used, int from) {
        for (int id = clamp(from); id <= MAX; id++) if (!used.contains(id)) return id;
        return clamp(from);
    }

    /** The glove's next ID after placing one: up by one, wrapping past the top. */
    public static int after(int id) { return id >= MAX ? MIN : clamp(id) + 1; }
}
