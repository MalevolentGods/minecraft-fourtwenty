package com.malevolentgods.fourtwenty.domain;

import javax.annotation.Nullable;
import java.util.List;

/**
 * One track's dance routine (specs/014-midnight-machine T033): the song's sections, each with the move she dances to
 * it, and the flourishes she throws now and then. Drafted by tools/choreograph_album.py, hand-tunable after. Pure.
 */
public record Routine(List<Section> sections, List<Flourish> flourishes) {
    public static final Routine NONE = new Routine(List.of(), List.of());

    /** A stretch of the song and its move; {@code intensity} (0..1) scales how big she dances it. */
    public record Section(long startMs, long endMs, String label, String move, double intensity) {}

    /** A short showy move starting at {@code ms}. */
    public record Flourish(long ms, String move) {}

    /** The index of the section playing at {@code ms}, or -1. */
    public int sectionAt(long ms) {
        int lo = 0, hi = sections.size() - 1, found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (sections.get(mid).startMs() <= ms) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return found >= 0 && ms < sections.get(found).endMs() ? found : -1;
    }

    /** The flourish under way at {@code ms}, or null. */
    @Nullable
    public Flourish flourishAt(long ms) {
        for (Flourish f : flourishes) {
            if (f.ms() > ms) break;
            if (ms < f.ms() + DanceMoves.flourishMs(f.move())) return f;
        }
        return null;
    }
}
