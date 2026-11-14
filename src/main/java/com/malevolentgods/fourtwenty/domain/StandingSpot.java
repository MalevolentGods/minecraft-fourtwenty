package com.malevolentgods.fourtwenty.domain;

import java.util.Optional;

/**
 * Finds a spot a citizen can stand: solid underfoot and two free cells for her body (specs/013-lifted-builders,
 * watchdog rescues and landing). Pure: the world is two lookups by coordinates.
 */
public final class StandingSpot {
    private StandingSpot() {}

    @FunctionalInterface
    public interface Cells {
        boolean test(int x, int y, int z);
    }

    public record Spot(int x, int y, int z) {}

    /** A box to stay out of (a building's footprint), inclusive. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    public static boolean standable(int x, int y, int z, Cells solid, Cells free) {
        return solid.test(x, y - 1, z) && free.test(x, y, z) && free.test(x, y + 1, z);
    }

    /**
     * The standable spot nearest the centre within the radius (horizontally and vertically), outside the box if one is
     * given. Ties go to the one with open sky above (no solid cell for {@code skyCheck} blocks up), then the higher.
     */
    public static Optional<Spot> nearest(int cx, int cy, int cz, int radius, Cells solid, Cells free, Box outside, int skyCheck) {
        Spot best = null;
        double bestScore = Double.MAX_VALUE;
        for (int dy = radius; dy >= -radius; dy--) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = cx + dx, y = cy + dy, z = cz + dz;
                    if (outside != null && outside.contains(x, y, z)) continue;
                    if (!standable(x, y, z, solid, free)) continue;
                    double score = dx * dx + dy * dy + dz * dz;
                    if (!openSky(x, y, z, solid, skyCheck)) score += 0.5;
                    if (score < bestScore) {
                        bestScore = score;
                        best = new Spot(x, y, z);
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** The first standable spot straight down from (x, y, z), at most {@code depth} below. */
    public static Optional<Spot> below(int x, int y, int z, int depth, Cells solid, Cells free) {
        for (int yy = y; yy >= y - depth; yy--) {
            if (standable(x, yy, z, solid, free)) return Optional.of(new Spot(x, yy, z));
        }
        return Optional.empty();
    }

    private static boolean openSky(int x, int y, int z, Cells solid, int up) {
        for (int yy = y + 2; yy < y + 2 + up; yy++) if (solid.test(x, yy, z)) return false;
        return true;
    }
}
