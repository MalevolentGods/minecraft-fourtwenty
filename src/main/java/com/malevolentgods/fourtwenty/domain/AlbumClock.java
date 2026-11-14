package com.malevolentgods.fourtwenty.domain;

/**
 * Where a Builder is in the Midnight Machine album (specs/014-midnight-machine FR-004): a track and how far into it.
 * Plays in order and loops after the last track. Pure.
 */
public final class AlbumClock {
    private AlbumClock() {}

    public record Position(int track, long ms) {}

    public static Position start() {
        return new Position(0, 0);
    }

    /** Moves on by {@code ms}, rolling into the next track (and past several short ones), looping after the last. */
    public static Position advance(Position pos, long ms, long[] lengths) {
        if (lengths.length == 0) return start();
        Position p = clamp(pos, lengths);
        long left = Math.max(0, ms);
        int track = p.track();
        long at = p.ms() + left;
        long total = 0;
        for (long l : lengths) total += Math.max(1, l);
        // whole loops of the album don't change where she ends up
        long before = 0;
        for (int i = 0; i < track; i++) before += Math.max(1, lengths[i]);
        long absolute = (before + at) % total;
        int t = 0;
        while (absolute >= Math.max(1, lengths[t])) {
            absolute -= Math.max(1, lengths[t]);
            t = (t + 1) % lengths.length;
        }
        return new Position(t, absolute);
    }

    /** A saved position made valid: a track that doesn't exist becomes the first, a time past the end the next track's start. */
    public static Position clamp(Position pos, long[] lengths) {
        if (lengths.length == 0) return start();
        if (pos == null || pos.track() < 0 || pos.track() >= lengths.length) return start();
        if (pos.ms() < 0) return new Position(pos.track(), 0);
        if (pos.ms() >= lengths[pos.track()]) return new Position((pos.track() + 1) % lengths.length, 0);
        return pos;
    }
}
