package com.malevolentgods.fourtwenty.domain.tiedye;

import java.util.function.BiPredicate;

/**
 * Patterns across squares of blocks (specs/018-tie-dye-shack FR-013c, research R11). In one plane, a block of a look shows one cell of
 * a big pattern when it's part of an N x N square (N from 2 to 5) of blocks of that look and nothing of that look touches the square's
 * sides; otherwise it shows the 1 x 1 pattern. Each block decides from its own neighbourhood, so the blocks of one square always agree.
 * Pure and tested.
 */
public final class SquareFit {
    private SquareFit() {}

    public static final int MAX = 5;

    /** A block's place in its square: the square's size and the block's column {@code i} and row {@code j} from its low corner. */
    public record Fit(int n, int i, int j) {
        public static final Fit ONE = new Fit(1, 0, 0);
    }

    /**
     * The fit of the block at (0, 0) of a plane, where {@code same.test(du, dv)} says whether the block at that offset has the same look
     * (it's asked of (0, 0) too, and of offsets up to {@link #MAX} away).
     */
    public static Fit fit(BiPredicate<Integer, Integer> same) {
        if (!same.test(0, 0)) return Fit.ONE;
        for (int n = MAX; n >= 2; n--) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (square(same, -i, -j, n)) return new Fit(n, i, j);
                }
            }
        }
        return Fit.ONE;
    }

    /** Whether the n x n square with its low corner at (u0, v0) is all the look, with none of it touching its sides. */
    static boolean square(BiPredicate<Integer, Integer> same, int u0, int v0, int n) {
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                if (!same.test(u0 + a, v0 + b)) return false;
            }
        }
        for (int a = 0; a < n; a++) {
            if (same.test(u0 + a, v0 - 1) || same.test(u0 + a, v0 + n) || same.test(u0 - 1, v0 + a) || same.test(u0 + n, v0 + a)) return false;
        }
        return true;
    }
}
