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
 * slider, loud only close to her (a square falloff to silence at 20 blocks), following her as she flies, and
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

    /** Full volume within {@link #NEAR} blocks of her, falling off on a square curve to silence at {@link #FAR}. */
    static final double NEAR = 2, FAR = 20;
    /** Right beside her: loud, not full blast (the user, 2026-11-14). */
    static final double MAX_VOLUME = 0.8;

    private float falloff() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        double d = Math.sqrt(player.distanceToSqr(x, y, z));
        double t = Math.max(0, Math.min(1, (FAR - d) / (FAR - NEAR)));
        return (float) (MAX_VOLUME * t * t);
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
