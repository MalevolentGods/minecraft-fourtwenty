package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.Album;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.MidnightMachinePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

/**
 * The Midnight Machine on the client (specs/014-midnight-machine FR-002 to FR-005, FR-011): which Builders wear the
 * boombox, which are playing, and each one's playback clock, kept in step with the server. Starts, re-syncs and stops
 * her {@link AlbumSoundInstance}, and moves on to the next track itself. The VU bars and the dancing read the clock.
 */
public final class MidnightMachineClient {
    private MidnightMachineClient() {}

    private static final long DRIFT_MS = 2000;

    private static final class Machine {
        boolean lifted, playing;
        int track;
        long trackStartMs;            // music clock (wall()) ms when the current track's 0 ms was
        AlbumSoundInstance sound;
    }

    private static final Map<Integer, Machine> MACHINES = new HashMap<>();
    /** The one boombox this player hears: the nearest playing one (1.0.3). A colony's Builders all play the same moment
     * of the album, so the rest stay quiet here rather than echo it; walking between them hands the music over in place. */
    private static int audible = -1;
    /** How much nearer (blocks, squared) another boombox must be before the music moves to it, so it doesn't flicker. */
    private static final double SWITCH_SQ = 16;

    public static void install() {
        MidnightMachinePayload.clientHandler = MidnightMachineClient::receive;
        NeoForge.EVENT_BUS.addListener(MidnightMachineClient::tick);
        DanceAnimation.install();
        MidnightMachineSettings.install();
        HighStatusClient.install();
    }

    /** Client clock in ms, on the same game-time base as the server. */
    /**
     * The music's clock: real time, as the audio plays, less any time the game sat paused (the audio pauses too).
     * The dance and VU bars read it. They followed the game's tick clock before, which falls behind real time whenever
     * the client lags, so the dance slid behind the music until a 2 s resync (2026-11-15).
     */
    private static long pausedMs, lastRawMs = -1;

    private static long wall() {
        long raw = System.nanoTime() / 1_000_000;
        long pausing = lastRawMs >= 0 && Minecraft.getInstance().isPaused() ? raw - lastRawMs : 0;
        return raw - pausedMs - pausing;
    }

    private static void trackPause() {
        long raw = System.nanoTime() / 1_000_000;
        if (lastRawMs >= 0 && Minecraft.getInstance().isPaused()) pausedMs += raw - lastRawMs;
        lastRawMs = raw;
    }

    /** The game's tick clock, for how long a server message took to arrive. */
    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime() * 50L;
    }

    private static void receive(MidnightMachinePayload p) {
        var m = MACHINES.computeIfAbsent(p.entity(), id -> new Machine());
        m.lifted = p.lifted();
        long expected = p.positionMs() + Math.max(0, now() - p.sentAt() * 50L);
        boolean wasPlaying = m.playing;
        m.playing = p.playing() && p.lifted();
        long ours = wall() - m.trackStartMs;
        boolean resync = !wasPlaying || m.track != p.track() || Math.abs(ours - expected) > DRIFT_MS;
        if (resync) {
            m.track = p.track();
            m.trackStartMs = wall() - expected;
        }
        if (!m.playing || p.entity() != audible) {
            stopSound(m);
        } else if (resync || m.sound == null) {
            startSound(p.entity(), m, expected);
        }
        if (!m.lifted && !m.playing) MACHINES.remove(p.entity());
    }

    private static void tick(ClientTickEvent.Post event) {
        trackPause();
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            MACHINES.values().forEach(MidnightMachineClient::stopSound);
            MACHINES.clear();
            return;
        }
        pickAudible(mc);
        var it = MACHINES.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            var m = e.getValue();
            if (mc.level.getEntity(e.getKey()) == null) {
                stopSound(m);
                it.remove();
                continue;
            }
            if (!m.playing) continue;
            if (e.getKey() != audible) stopSound(m);
            else if (m.sound == null && wall() - m.trackStartMs < Album.track(m.track).lengthMs()) startSound(e.getKey(), m, wall() - m.trackStartMs);
            long slot = Album.slotMs(m.track);   // the track, then a few seconds of silence
            long at = wall() - m.trackStartMs;
            if (at >= slot) {   // on to the next track (the server's next message confirms it)
                m.trackStartMs += slot;
                m.track = (m.track + 1) % Math.max(1, Album.tracks().size());
                if (e.getKey() == audible) startSound(e.getKey(), m, wall() - m.trackStartMs);
            }
        }
    }

    /** The nearest playing boombox to the player, kept unless another is clearly nearer. */
    private static void pickAudible(Minecraft mc) {
        if (mc.player == null) { audible = -1; return; }
        int best = -1;
        double bestSq = Double.MAX_VALUE, currentSq = Double.MAX_VALUE;
        for (var e : MACHINES.entrySet()) {
            if (!e.getValue().playing) continue;
            var entity = mc.level.getEntity(e.getKey());
            if (entity == null) continue;
            double sq = entity.distanceToSqr(mc.player);
            if (e.getKey() == audible) currentSq = sq;
            if (sq < bestSq) { bestSq = sq; best = e.getKey(); }
        }
        if (best == -1) audible = -1;
        else if (currentSq == Double.MAX_VALUE || bestSq + SWITCH_SQ < currentSq) audible = best;
    }

    private static void startSound(int entityId, Machine m, long atMs) {
        stopSound(m);
        var mc = Minecraft.getInstance();
        var entity = mc.level == null ? null : mc.level.getEntity(entityId);
        if (entity == null || Album.tracks().isEmpty()) return;
        if (atMs >= Album.track(m.track).lengthMs()) return;   // in the silence after it: the next track starts itself
        m.sound = new AlbumSoundInstance(entity, m.track, Album.track(m.track).id(), atMs);
        mc.getSoundManager().play(m.sound);
    }

    private static void stopSound(Machine m) {
        if (m.sound != null) {
            m.sound.end();
            Minecraft.getInstance().getSoundManager().stop(m.sound);
            m.sound = null;
        }
    }

    // What the boombox and the dancing read

    public static boolean lifted(int entityId) {
        var m = MACHINES.get(entityId);
        return m != null && m.lifted;
    }

    public static boolean playing(int entityId) {
        var m = MACHINES.get(entityId);
        return m != null && m.playing;
    }

    public static int track(int entityId) {
        var m = MACHINES.get(entityId);
        return m == null ? 0 : m.track;
    }

    /** Where in her track the music is, in ms, with the frame's partial tick for smooth animation. */
    public static long positionMs(int entityId, float partialTick) {
        var m = MACHINES.get(entityId);
        if (m == null) return 0;
        return Math.max(0, wall() - m.trackStartMs);
    }
}
