package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

/**
 * The Bud Barn's Trim Daddy: what to do next, in a fixed order. Take cured buds down first, so racks free up; then hang trimmed buds;
 * then trim, but only as many as the racks have room for plus a small buffer; then fetch rough buds from the hut; then ask the colony.
 */
public final class BudBarnRules {
    private BudBarnRules() {}

    /** Ticks per trim by level 1-5 (defaults; the config can change them). */
    public static final int[] TRIM_TICKS = {40, 30, 20, 15, 10};
    public static final int BUFFER = 8, ROUGH_PER_TRAY = 16;

    public enum Step { COLLECT, HANG, TRIM, FETCH, REQUEST, IDLE }

    public enum Status { NO_WORKER, NO_TRAYS, NO_RACKS, WAITING_FOR_ROUGH, TRIMMING, CURING, RACKS_FULL }

    /**
     * What the hut and its worker hold right now. {@code hanging} is every bud on the racks, {@code cured} the finished ones among them,
     * {@code free} the empty rack spots.
     */
    public record Snapshot(int trays, int racks, int hanging, int cured, int free,
                           int trimmedHeld, int trimmedInHut, int roughHeld, int roughInHut) {
        public int trimmed() { return trimmedHeld + trimmedInHut; }
    }

    /** Trimming stops once this many trimmed buds wait for a spot. */
    public static int trimmedCap(int free, int buffer) { return Math.max(0, free) + Math.max(0, buffer); }

    public static Step next(Snapshot s, int buffer) {
        if (s.racks() > 0 && s.cured() > 0) return Step.COLLECT;
        if (s.racks() > 0 && s.free() > 0 && s.trimmed() > 0) return Step.HANG;
        if (s.trays() == 0 || s.trimmed() >= trimmedCap(s.free(), buffer)) return Step.IDLE;
        if (s.roughHeld() > 0) return Step.TRIM;
        if (s.roughInHut() > 0) return Step.FETCH;
        return Step.REQUEST;
    }

    /** What the hut's status shows, given its snapshot and whether it has a worker. */
    public static Status status(Snapshot s, boolean worker, int buffer) {
        if (!worker) return Status.NO_WORKER;
        if (s.trays() == 0) return Status.NO_TRAYS;
        if (s.racks() == 0) return Status.NO_RACKS;
        Step step = next(s, buffer);
        if (step == Step.REQUEST) return s.hanging() > 0 ? Status.CURING : Status.WAITING_FOR_ROUGH;   // still busy while buds hang
        if (step == Step.TRIM || step == Step.FETCH) return Status.TRIMMING;
        if (s.free() == 0 && s.hanging() > 0) return Status.RACKS_FULL;
        return s.hanging() > 0 ? Status.CURING : Status.WAITING_FOR_ROUGH;
    }

    /** Ticks per trim at {@code level}, from a table of five. */
    public static int trimTicks(int level, int[] table) {
        return table[Math.max(1, Math.min(table.length, level)) - 1];
    }
}
