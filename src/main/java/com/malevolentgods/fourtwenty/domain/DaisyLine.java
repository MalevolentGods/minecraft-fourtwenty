package com.malevolentgods.fourtwenty.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The order of a Daisy line (specs/009-twin-daisy-wicks, research R3): its cable blocks and the blocks they touch, as one series string.
 *
 * <p>Cables are numbered by the caller in a fixed order (by position); the line is read from the end with the lower number. Cables that
 * join each other form runs, which must be paths. A block touching one run sits beside it, spliced after the first of its cables in the
 * line; a block touching the ends of two runs is inline and joins them. The runs and inline blocks must make one path. Anything else is
 * a branch, or a ring, and the line carries nothing.
 */
public final class DaisyLine {
    private DaisyLine() {}

    public enum Reason { BRANCH, RING, EMPTY }

    /** One step of the line: a cable, or a block spliced into it. */
    public record Item(boolean device, int id) {
        public static Item cable(int id) { return new Item(false, id); }

        public static Item device(int id) { return new Item(true, id); }
    }

    /** Where a block touches the line: which cable, and on which face of the cable (for ties: north, east, south, west, up, down = 0-5). */
    public record Contact(int cable, int face) {}

    /** The line in order, or why there is none. */
    public record Result(List<Item> items, Reason failure) {
        public boolean ok() { return failure == null; }

        static Result fail(Reason why) { return new Result(List.of(), why); }

        /** The same line read from its other end. */
        public Result reversed() {
            if (!ok()) return this;
            List<Item> copy = new ArrayList<>(items);
            Collections.reverse(copy);
            return new Result(List.copyOf(copy), null);
        }

        /** The first and last cable of the line. */
        public int firstCable() { return items.stream().filter(i -> !i.device()).findFirst().orElseThrow().id(); }

        public int lastCable() {
            for (int i = items.size() - 1; i >= 0; i--) if (!items.get(i).device()) return items.get(i).id();
            throw new IllegalStateException();
        }
    }

    /**
     * @param neighbours each cable's cable neighbours (every cable of the line appears as a key)
     * @param devices    each block touching the line, with where it touches
     */
    public static Result order(Map<Integer, Set<Integer>> neighbours, Map<Integer, List<Contact>> devices) {
        if (neighbours.isEmpty()) return Result.fail(Reason.EMPTY);
        // 1. runs of joined cables, each a path, as a list from one end to the other
        Map<Integer, Integer> runOf = new HashMap<>();
        List<List<Integer>> runs = new ArrayList<>();
        for (int start : new TreeSet<>(neighbours.keySet())) {
            if (runOf.containsKey(start)) continue;
            Set<Integer> members = new HashSet<>();
            ArrayDeque<Integer> queue = new ArrayDeque<>(List.of(start));
            while (!queue.isEmpty()) {
                int c = queue.poll();
                if (!members.add(c)) continue;
                for (int n : neighbours.getOrDefault(c, Set.of())) if (neighbours.containsKey(n)) queue.add(n);
            }
            int end = -1;
            for (int c : new TreeSet<>(members)) {
                int degree = degree(neighbours, c);
                if (degree > 2) return Result.fail(Reason.BRANCH);
                if (degree < 2 && end < 0) end = c;
            }
            if (end < 0) return Result.fail(Reason.RING);
            List<Integer> path = walkRun(neighbours, end);
            if (path.size() != members.size()) return Result.fail(Reason.BRANCH);
            for (int c : path) runOf.put(c, runs.size());
            runs.add(path);
        }
        // 2. each block is beside one run, or inline between the ends of two
        Map<Integer, List<Integer>> beside = new TreeMap<>();
        List<int[]> inline = new ArrayList<>(); // device, run a, cable a, run b, cable b
        for (var e : new TreeMap<>(devices).entrySet()) {
            Map<Integer, Set<Integer>> byRun = new TreeMap<>();
            for (Contact c : e.getValue()) if (runOf.containsKey(c.cable())) byRun.computeIfAbsent(runOf.get(c.cable()), k -> new TreeSet<>()).add(c.cable());
            if (byRun.isEmpty()) continue;
            if (byRun.size() == 1) {
                beside.put(e.getKey(), List.copyOf(byRun.values().iterator().next()));
                continue;
            }
            if (byRun.size() > 2) return Result.fail(Reason.BRANCH);
            int[] link = new int[5];
            link[0] = e.getKey();
            int k = 1;
            for (var r : byRun.entrySet()) {
                List<Integer> run = runs.get(r.getKey());
                if (r.getValue().size() != 1) return Result.fail(Reason.BRANCH);
                int cable = r.getValue().iterator().next();
                if (cable != run.get(0) && cable != run.get(run.size() - 1)) return Result.fail(Reason.BRANCH);
                link[k++] = r.getKey();
                link[k++] = cable;
            }
            inline.add(link);
        }
        // 3. runs joined by inline blocks: each end of a run takes at most one, and they make one path
        Map<Integer, List<int[]>> atRun = new HashMap<>();
        for (int[] link : inline) {
            atRun.computeIfAbsent(link[1], x -> new ArrayList<>()).add(link);
            atRun.computeIfAbsent(link[3], x -> new ArrayList<>()).add(link);
        }
        for (int r = 0; r < runs.size(); r++) {
            List<int[]> links = atRun.getOrDefault(r, List.of());
            if (links.size() > 2) return Result.fail(Reason.BRANCH);
            List<Integer> run = runs.get(r);
            if (links.size() == 2 && run.size() > 1 && cableAt(links.get(0), r) == cableAt(links.get(1), r)) return Result.fail(Reason.BRANCH);
        }
        if (inline.size() >= runs.size()) return Result.fail(inline.size() == runs.size() && runs.size() > 1 ? Reason.RING : Reason.BRANCH);
        // the line's ends: runs with fewer than two inline links; read from the free end with the lower number
        int startRun = -1, startCable = Integer.MAX_VALUE;
        for (int r = 0; r < runs.size(); r++) {
            List<int[]> links = atRun.getOrDefault(r, List.of());
            if (links.size() >= 2) continue;
            List<Integer> run = runs.get(r);
            for (int end : new int[]{run.get(0), run.get(run.size() - 1)}) {
                boolean taken = !links.isEmpty() && run.size() > 1 && cableAt(links.get(0), r) == end;
                if (!taken && end < startCable) {
                    startCable = end;
                    startRun = r;
                }
            }
        }
        if (startRun < 0) return Result.fail(Reason.RING);
        // 4. the cables in line order, with the inline blocks between runs
        List<Item> items = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Set<int[]> used = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        int run = startRun, enter = startCable;
        while (true) {
            visited.add(run);
            List<Integer> path = new ArrayList<>(runs.get(run));
            if (path.get(0) != enter) Collections.reverse(path);
            for (int c : path) items.add(Item.cable(c));
            int exit = path.get(path.size() - 1);
            int[] next = null;
            for (int[] link : atRun.getOrDefault(run, List.of())) {
                if (used.contains(link)) continue;
                if (path.size() == 1 || cableAt(link, run) == exit) next = link;
            }
            if (next == null) break;
            used.add(next);
            items.add(Item.device(next[0]));
            int other = next[1] == run ? next[3] : next[1];
            if (visited.contains(other)) return Result.fail(Reason.RING);
            enter = cableAt(next, other);
            run = other;
        }
        if (visited.size() != runs.size()) return Result.fail(Reason.BRANCH);
        // 5. blocks beside the line, each after the first of its cables, ties by the face they're on
        Map<Integer, Integer> index = new HashMap<>();
        for (int i = 0; i < items.size(); i++) if (!items.get(i).device()) index.put(items.get(i).id(), i);
        Map<Integer, List<int[]>> after = new LinkedHashMap<>();
        for (var e : beside.entrySet()) {
            int anchor = e.getValue().stream().min(Comparator.comparingInt(index::get)).orElseThrow();
            int face = devices.get(e.getKey()).stream().filter(c -> c.cable() == anchor).mapToInt(Contact::face).min().orElse(0);
            after.computeIfAbsent(anchor, x -> new ArrayList<>()).add(new int[]{e.getKey(), face});
        }
        List<Item> out = new ArrayList<>();
        for (Item item : items) {
            out.add(item);
            if (item.device()) continue;
            List<int[]> here = new ArrayList<>(after.getOrDefault(item.id(), List.of()));
            here.sort(Comparator.<int[]>comparingInt(d -> d[1]).thenComparingInt(d -> d[0]));
            for (int[] d : here) out.add(Item.device(d[0]));
        }
        return new Result(List.copyOf(out), null);
    }

    private static int degree(Map<Integer, Set<Integer>> neighbours, int cable) {
        int n = 0;
        for (int other : neighbours.getOrDefault(cable, Set.of())) if (neighbours.containsKey(other)) n++;
        return n;
    }

    private static List<Integer> walkRun(Map<Integer, Set<Integer>> neighbours, int end) {
        List<Integer> path = new ArrayList<>();
        int prev = -1, at = end;
        while (true) {
            path.add(at);
            int next = -1;
            for (int n : new TreeSet<>(neighbours.getOrDefault(at, Set.of()))) if (n != prev && neighbours.containsKey(n) && !path.contains(n)) next = n;
            if (next < 0) return path;
            prev = at;
            at = next;
        }
    }

    private static int cableAt(int[] link, int run) { return link[1] == run ? link[2] : link[4]; }
}
