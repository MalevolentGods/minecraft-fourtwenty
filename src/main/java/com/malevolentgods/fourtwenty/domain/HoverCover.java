package com.malevolentgods.fourtwenty.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Higher Ground's hover point (specs/013-lifted-builders FR-015 2): of the free cells within reach of the block she's
 * placing, the one that also reaches the most of the blocks coming up, so she places a stretch from one spot before
 * moving on. Ties go to the higher. Never a cell where one of the next blocks goes, or right under one (she'd be
 * built in), and never in the column of the block she's placing (MineColonies steps her aside from it). Pure.
 */
public final class HoverCover {
    private HoverCover() {}

    public record Pos(int x, int y, int z) {
        double dist(Pos o) {
            double dx = x - o.x, dy = y - o.y, dz = z - o.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        Pos above() { return new Pos(x, y + 1, z); }
    }

    /**
     * @param candidates free cells she fits in
     * @param current    the block she places now: the point must reach it
     * @param nextBlocks blocks coming up (the current one may be among them)
     * @param reach      how far she places from
     */
    public static Optional<Pos> choose(List<Pos> candidates, Pos current, List<Pos> nextBlocks, int reach) {
        Set<Pos> upcoming = Set.copyOf(nextBlocks);
        Pos best = null;
        int bestCover = -1;
        for (Pos c : candidates) {
            if (upcoming.contains(c) || upcoming.contains(c.above()) || c.equals(current) || c.above().equals(current)
                    || c.x() == current.x() && c.z() == current.z() && c.y() > current.y()) continue;   // never in the column she's building
            if (c.dist(current) > reach) continue;
            int cover = 0;
            for (Pos b : nextBlocks) if (c.dist(b) <= reach) cover++;
            if (cover > bestCover || cover == bestCover && best != null && c.y() > best.y()) {
                best = c;
                bestCover = cover;
            }
        }
        return Optional.ofNullable(best);
    }
}
