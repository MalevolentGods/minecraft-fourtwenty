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
        long trackStartMs;            // client clock ms when the current track's 0 ms was
        AlbumSoundInstance sound;
    }

    private static final Map<Integer, Machine> MACHINES = new HashMap<>();

    public static void install() {
        MidnightMachinePayload.clientHandler = MidnightMachineClient::receive;
        NeoForge.EVENT_BUS.addListener(MidnightMachineClient::tick);
    }

    /** Client clock in ms, on the same game-time base as the server. */
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
        long ours = now() - m.trackStartMs;
        boolean resync = !wasPlaying || m.track != p.track() || Math.abs(ours - expected) > DRIFT_MS;
        if (resync) {
            m.track = p.track();
            m.trackStartMs = now() - expected;
        }
        if (!m.playing) {
            stopSound(m);
        } else if (resync || m.sound == null) {
            startSound(p.entity(), m, expected);
        }
        if (!m.lifted && !m.playing) MACHINES.remove(p.entity());
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            MACHINES.values().forEach(MidnightMachineClient::stopSound);
            MACHINES.clear();
            return;
        }
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
            long length = Album.track(m.track).lengthMs();
            long at = now() - m.trackStartMs;
            if (at >= length) {   // on to the next track (the server's next message confirms it)
                m.trackStartMs += length;
                m.track = (m.track + 1) % Math.max(1, Album.tracks().size());
                startSound(e.getKey(), m, now() - m.trackStartMs);
            }
        }
    }

    private static void startSound(int entityId, Machine m, long atMs) {
        stopSound(m);
        var mc = Minecraft.getInstance();
        var entity = mc.level == null ? null : mc.level.getEntity(entityId);
        if (entity == null || Album.tracks().isEmpty()) return;
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
        return Math.max(0, now() + (long) (partialTick * 50) - m.trackStartMs);
    }
}
