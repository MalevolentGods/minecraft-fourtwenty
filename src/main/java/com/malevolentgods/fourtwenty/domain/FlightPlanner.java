package com.malevolentgods.fourtwenty.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A Lifted Builder's route through the air (specs/013-lifted-builders FR-012, research R7): a bounded 3D A* over
 * cells where her body fits (the cell and the one above it free). Diagonal steps never cut a corner: every cell the
 * step sweeps must fit too. Pure: the world is one lookup.
 */
public final class FlightPlanner {
    private FlightPlanner() {}

    @FunctionalInterface
    public interface Free {
        boolean test(int x, int y, int z);
    }

    public record Cell(int x, int y, int z) {}

    /**
     * A doorway cell (spec 015 US2): a wooden door or fence gate she can open on her way. Passable, at
     * {@link #DOOR_COST} extra, and only crossed straight on (never diagonally through the frame).
     */
    @FunctionalInterface
    public interface Doorway {
        Doorway NONE = (x, y, z) -> false;

        boolean test(int x, int y, int z);
    }

    public static final double DOOR_COST = 2;

    /** Whether her body (two cells tall) fits at a cell. */
    public static boolean fits(Free free, int x, int y, int z) {
        return free.test(x, y, z) && free.test(x, y + 1, z);
    }

    /**
     * The route from start to goal (both included), or an empty list if there's none within {@code maxCells} searched
     * or {@code radius} of the start.
     */
    /** Her body fits a cell when both of its cells are open or doorways. */
    public static boolean fits(Free free, Doorway door, int x, int y, int z) {
        return pass(free, door, x, y, z) && pass(free, door, x, y + 1, z);
    }

    private static boolean pass(Free free, Doorway door, int x, int y, int z) {
        return free.test(x, y, z) || door.test(x, y, z);
    }

    public static List<Cell> plan(Cell start, Cell goal, Free free, int radius, int maxCells) {
        return plan(start, goal, free, Doorway.NONE, radius, maxCells);
    }

    /** As {@link #plan(Cell, Cell, Free, int, int)}, through doorways too. */
    public static List<Cell> plan(Cell start, Cell goal, Free free, Doorway door, int radius, int maxCells) {
        if (!fits(free, door, goal.x(), goal.y(), goal.z())) return List.of();
        if (start.equals(goal)) return List.of(start);
        record Node(Cell cell, double g, double f) {}
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.f(), b.f()));
        Map<Cell, Double> best = new HashMap<>();
        Map<Cell, Cell> from = new HashMap<>();
        open.add(new Node(start, 0, dist(start, goal)));
        best.put(start, 0.0);
        int searched = 0;
        while (!open.isEmpty() && searched < maxCells) {
            Node n = open.poll();
            if (n.g() > best.getOrDefault(n.cell(), Double.MAX_VALUE)) continue;
            if (n.cell().equals(goal)) return path(from, goal);
            searched++;
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                Cell c = new Cell(n.cell().x() + dx, n.cell().y() + dy, n.cell().z() + dz);
                if (Math.abs(c.x() - start.x()) > radius || Math.abs(c.y() - start.y()) > radius || Math.abs(c.z() - start.z()) > radius) continue;
                if (!sweepFits(free, door, n.cell(), dx, dy, dz)) continue;
                double g = n.g() + Math.sqrt(dx * dx + dy * dy + dz * dz)
                        + (door.test(c.x(), c.y(), c.z()) || door.test(c.x(), c.y() + 1, c.z()) ? DOOR_COST : 0);
                if (g >= best.getOrDefault(c, Double.MAX_VALUE)) continue;
                best.put(c, g);
                from.put(c, n.cell());
                open.add(new Node(c, g, g + dist(c, goal)));
            }
        }
        return List.of();
    }

    /** Every cell a step from {@code a} by (dx, dy, dz) passes through fits her body, so diagonals can't clip corners. */
    private static boolean sweepFits(Free free, Doorway door, Cell a, int dx, int dy, int dz) {
        boolean straight = dy == 0 && (dx == 0 || dz == 0);
        for (int ix = Math.min(0, dx); ix <= Math.max(0, dx); ix++)
            for (int iy = Math.min(0, dy); iy <= Math.max(0, dy); iy++)
                for (int iz = Math.min(0, dz); iz <= Math.max(0, dz); iz++) {
                    int x = a.x() + ix, y = a.y() + iy, z = a.z() + iz;
                    if (!fits(free, door, x, y, z)) return false;
                    // through a doorway only straight on, level: never diagonally through the frame or up through it
                    if (!straight && (door.test(x, y, z) || door.test(x, y + 1, z))) return false;
                }
        return true;
    }

    private static double dist(Cell a, Cell b) {
        int dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static List<Cell> path(Map<Cell, Cell> from, Cell goal) {
        List<Cell> out = new ArrayList<>();
        for (Cell c = goal; c != null; c = from.get(c)) out.add(c);
        Collections.reverse(out);
        return out;
    }
}
