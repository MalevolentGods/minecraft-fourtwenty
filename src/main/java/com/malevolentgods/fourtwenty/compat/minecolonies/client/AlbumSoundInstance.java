package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * One album track playing from a Builder (specs/014-midnight-machine FR-002, FR-003): on the Jukebox/Note Blocks
 * slider, falling off with distance (inverse cube, silent past 48 blocks), following her as she flies, and
 * starting at {@code startMs} through {@link SeekingOggStream}.
 */
public final class AlbumSoundInstance extends AbstractTickableSoundInstance {
    private final Entity builder;
    private final long startMs;
    final int track;

    public AlbumSoundInstance(Entity builder, int track, String trackId, long startMs) {
        super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("fourtwenty", "midnight_machine." + trackId)),
                SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.builder = builder;
        this.track = track;
        this.startMs = Math.max(0, startMs);
        // Our own steep falloff (below) instead of OpenAL's linear one, which still sounds loud halfway out; the
        // source stays positioned, so it still pans and sounds like it comes from her.
        this.attenuation = Attenuation.NONE;
        this.looping = false;
        this.relative = false;
        this.volume = 0;
        follow();
        this.volume = falloff();
    }

    /**
     * Falling off faster than open air would (inverse-square intensity is SPL - 20 log10(r2/r1), -6 dB per doubling):
     * inverse cube, -9 dB per doubling, so the gain goes with (NEAR/d)^1.5 (the user, 2026-11-14: inverse-square
     * carried too far). Full volume within {@link #NEAR} blocks (capped at {@link #MAX_VOLUME}, so close up it sits
     * under the player's other sounds), and a smooth fade to nothing from {@link #FADE} to {@link #FAR}.
     */
    static final double NEAR = 4, FADE = 36, FAR = 48;
    static final double MAX_VOLUME = 0.6;

    private float falloff() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        double d = Math.sqrt(player.distanceToSqr(x, y, z));
        if (d >= FAR) return 0;
        double gain = Math.pow(NEAR / Math.max(NEAR, d), 1.5);   // inverse cube: -9 dB per doubling of distance
        if (d > FADE) {
            double t = (FAR - d) / (FAR - FADE);
            gain *= t * t * (3 - 2 * t);
        }
        return (float) (MAX_VOLUME * gain);
    }

    @Override
    public boolean canStartSilent() {
        return true;   // she may start a track while you're out of earshot
    }

    private void follow() {
        x = builder.getX();
        y = builder.getY() + 1.2;
        z = builder.getZ();
    }

    @Override
    public void tick() {
        if (builder.isRemoved() || !builder.isAlive()) {
            stop();
            return;
        }
        follow();
        volume = falloff();
    }

    public void end() {
        stop();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        ResourceLocation path = sound.getPath();
        return CompletableFuture.supplyAsync(() -> {
            try (var in = Minecraft.getInstance().getResourceManager().open(path)) {
                return new SeekingOggStream(in.readAllBytes(), startMs);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.backgroundExecutor());
    }
}
