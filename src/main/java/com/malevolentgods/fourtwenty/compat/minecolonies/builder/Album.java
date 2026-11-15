package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The Midnight Machine album as shipped in the jar (assets/fourtwenty/midnight_machine/album.json, written by
 * tools/convert_album.py and tools/beatmap_album.py): titles, lengths, beat maps, and the VU band levels. Loaded once
 * from the mod's own classpath, so the server (which has no resource manager for assets) reads it too.
 */
public final class Album {
    private Album() {}

    public record BeatMap(double bpm, long[] beatsMs, long[] accentsMs) {
        public static final BeatMap EMPTY = new BeatMap(0, new long[0], new long[0]);
    }

    public record Track(String id, String title, long lengthMs, BeatMap beats) {}

    public static final int BANDS = 6;
    public static final int BAND_MS = 50;

    private static List<Track> tracks;
    private static final byte[][] BANDS_CACHE = new byte[16][];

    public static synchronized List<Track> tracks() {
        if (tracks == null) tracks = load();
        return tracks;
    }

    /** Silence after each track before the next starts (the user, 2026-11-14: "a few seconds of silence"). */
    public static final long GAP_MS = 3000;

    /** How long each track takes up on the album clock: the track and the silence after it. */
    public static long slotMs(int track) {
        return track(track).lengthMs() + GAP_MS;
    }

    public static long[] lengths() {
        var t = tracks();
        long[] out = new long[t.size()];
        for (int i = 0; i < out.length; i++) out[i] = t.get(i).lengthMs() + GAP_MS;
        return out;
    }

    public static Track track(int i) {
        var t = tracks();
        return t.isEmpty() ? new Track("track1", "", 1, BeatMap.EMPTY) : t.get(Math.floorMod(i, t.size()));
    }

    /** One track's VU levels: BANDS bytes a frame, a frame every BAND_MS (empty if missing). */
    public static synchronized byte[] bands(int track) {
        int i = Math.floorMod(track, Math.max(1, tracks().size()));
        if (BANDS_CACHE[i] == null) {
            try (InputStream in = Album.class.getResourceAsStream("/assets/fourtwenty/midnight_machine/" + track(i).id() + ".bands")) {
                BANDS_CACHE[i] = in == null ? new byte[0] : in.readAllBytes();
            } catch (Exception e) {
                BANDS_CACHE[i] = new byte[0];
            }
        }
        return BANDS_CACHE[i];
    }

    private static final com.malevolentgods.fourtwenty.domain.Routine[] ROUTINES = new com.malevolentgods.fourtwenty.domain.Routine[16];

    /** One track's dance routine (trackN.routine.json, from tools/choreograph_album.py); none if missing or broken. */
    public static synchronized com.malevolentgods.fourtwenty.domain.Routine routine(int track) {
        int i = Math.floorMod(track, Math.max(1, tracks().size()));
        if (ROUTINES[i] == null) ROUTINES[i] = loadRoutine(track(i).id());
        return ROUTINES[i];
    }

    private static com.malevolentgods.fourtwenty.domain.Routine loadRoutine(String id) {
        try (InputStream in = Album.class.getResourceAsStream("/assets/fourtwenty/midnight_machine/" + id + ".routine.json")) {
            if (in == null) return com.malevolentgods.fourtwenty.domain.Routine.NONE;
            var root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            var sections = new ArrayList<com.malevolentgods.fourtwenty.domain.Routine.Section>();
            for (var e : root.getAsJsonArray("sections")) {
                var o = e.getAsJsonObject();
                sections.add(new com.malevolentgods.fourtwenty.domain.Routine.Section(o.get("startMs").getAsLong(), o.get("endMs").getAsLong(),
                        o.get("label").getAsString(), o.get("move").getAsString(), o.has("intensity") ? o.get("intensity").getAsDouble() : 1));
            }
            var flourishes = new ArrayList<com.malevolentgods.fourtwenty.domain.Routine.Flourish>();
            for (var e : root.getAsJsonArray("flourishes")) {
                var o = e.getAsJsonObject();
                flourishes.add(new com.malevolentgods.fourtwenty.domain.Routine.Flourish(o.get("ms").getAsLong(), o.get("move").getAsString()));
            }
            flourishes.sort(java.util.Comparator.comparingLong(com.malevolentgods.fourtwenty.domain.Routine.Flourish::ms));
            return new com.malevolentgods.fourtwenty.domain.Routine(List.copyOf(sections), List.copyOf(flourishes));
        } catch (Exception e) {
            return com.malevolentgods.fourtwenty.domain.Routine.NONE;
        }
    }

    /** A band's level (0-1) at a time in a track, eased between frames. */
    public static float level(int track, int band, long ms) {
        byte[] b = bands(track);
        int frames = b.length / BANDS;
        if (frames == 0) return 0;
        double f = Math.max(0, ms) / (double) BAND_MS;
        int k = Math.min(frames - 1, (int) f);
        int k2 = Math.min(frames - 1, k + 1);
        double t = f - (int) f;
        double a = (b[k * BANDS + band] & 0xFF) / 255.0, z = (b[k2 * BANDS + band] & 0xFF) / 255.0;
        return (float) (a + (z - a) * t);
    }

    private static List<Track> load() {
        List<Track> out = new ArrayList<>();
        try (InputStream in = Album.class.getResourceAsStream("/assets/fourtwenty/midnight_machine/album.json")) {
            if (in == null) return out;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : root.getAsJsonArray("tracks")) {
                var t = e.getAsJsonObject();
                BeatMap beats = BeatMap.EMPTY;
                if (t.has("beats_ms")) {
                    beats = new BeatMap(t.has("bpm") ? t.get("bpm").getAsDouble() : 0, longs(t, "beats_ms"), longs(t, "accents_ms"));
                }
                out.add(new Track(t.get("id").getAsString(), t.get("title").getAsString(), t.get("length_ms").getAsLong(), beats));
            }
        } catch (Exception e) {
            BuilderLog.missing("album.json", e);
        }
        return out;
    }

    private static long[] longs(JsonObject o, String key) {
        if (!o.has(key)) return new long[0];
        var a = o.getAsJsonArray(key);
        long[] out = new long[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsLong();
        return out;
    }
}
