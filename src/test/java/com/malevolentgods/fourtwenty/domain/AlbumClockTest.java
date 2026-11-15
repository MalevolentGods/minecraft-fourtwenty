package com.malevolentgods.fourtwenty.domain;

import com.malevolentgods.fourtwenty.domain.AlbumClock.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlbumClockTest {
    // the real album's lengths
    private static final long[] ALBUM = {265_643, 316_087, 207_040, 95_920, 250_037, 195_534, 251_853};

    @Test
    void advancesWithinATrack() {
        assertEquals(new Position(0, 61_000), AlbumClock.advance(new Position(0, 60_000), 1_000, ALBUM));
    }

    @Test
    void rollsIntoTheNextTrack() {
        assertEquals(new Position(1, 357), AlbumClock.advance(new Position(0, 265_000), 1_000, ALBUM));
    }

    @Test
    void rollsPastShortTracksInOneStep() {
        // from the end of track 3 a long step skips the whole of track 4 (1:36) into track 5
        var p = AlbumClock.advance(new Position(2, 207_000), 40 + 95_920 + 5_000, ALBUM);
        assertEquals(new Position(4, 5_000), p);
    }

    @Test
    void loopsAfterTheLastTrack() {
        assertEquals(new Position(0, 147), AlbumClock.advance(new Position(6, 251_000), 1_000, ALBUM));
        long whole = 0;
        for (long l : ALBUM) whole += l;
        assertEquals(new Position(2, 10_000), AlbumClock.advance(new Position(2, 10_000), whole * 3, ALBUM), "whole loops land where she was");
    }

    @Test
    void pausingIsJustNotAdvancing() {
        assertEquals(new Position(3, 42_000), AlbumClock.advance(new Position(3, 42_000), 0, ALBUM));
    }

    @Test
    void badSavedPositionsAreMadeValid() {
        assertEquals(AlbumClock.start(), AlbumClock.clamp(new Position(9, 5), ALBUM), "a track that doesn't exist");
        assertEquals(AlbumClock.start(), AlbumClock.clamp(null, ALBUM));
        assertEquals(new Position(1, 0), AlbumClock.clamp(new Position(1, -50), ALBUM));
        assertEquals(new Position(2, 0), AlbumClock.clamp(new Position(1, 400_000), ALBUM), "past the end: the next track");
        assertEquals(new Position(0, 0), AlbumClock.clamp(new Position(6, 300_000), ALBUM), "past the last: the first");
    }
}
