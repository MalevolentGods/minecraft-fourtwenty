package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.Album;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.Lifted;
import com.malevolentgods.fourtwenty.domain.AlbumClock;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Each Builder's place in the Midnight Machine album survives a save (spec 014 T013), and the album ships complete. */
@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class AlbumPositionGameTests {
    @GameTest(template = "empty")
    public static void albumPositionsSurviveASaveAndLoad(GameTestHelper h) {
        var lifted = new Lifted();
        lifted.position(3, 7, new AlbumClock.Position(4, 61_250));
        lifted.position(4, 7, new AlbumClock.Position(1, 12_000));
        var back = Lifted.load(lifted.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(back.position(3, 7).equals(new AlbumClock.Position(4, 61_250)), "track and time kept");
        h.assertTrue(back.position(4, 7).equals(new AlbumClock.Position(1, 12_000)), "the same citizen id in another colony is kept apart");
        h.assertTrue(back.position(9, 9).equals(AlbumClock.start()), "a Builder never Lifted starts at track 1");

        var old = new CompoundTag();
        old.put("buffs", new ListTag());   // a save from before spec 014: no album positions
        h.assertTrue(Lifted.load(old, h.getLevel().registryAccess()).position(3, 7).equals(AlbumClock.start()), "old saves load, starting at track 1");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theAlbumShipsCompleteWithItsBeatsAndBars(GameTestHelper h) {
        var tracks = Album.tracks();
        h.assertTrue(tracks.size() == 7, "seven tracks: " + tracks.size());
        for (int i = 0; i < tracks.size(); i++) {
            var t = tracks.get(i);
            h.assertTrue(t.lengthMs() > 60_000, t.id() + " has its length");
            h.assertTrue(t.beats().beatsMs().length > 100 && t.beats().bpm() > 40, t.id() + " has its beat map");
            int frames = Album.bands(i).length / Album.BANDS;
            h.assertTrue(Math.abs(frames * (long) Album.BAND_MS - t.lengthMs()) < 2_000, t.id() + " has VU levels for the whole track: " + frames);
        }
        h.succeed();
    }
}
