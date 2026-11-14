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
 * slider, fading linearly to nothing at the event's 24-block attenuation distance, following her as she flies, and
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
        this.attenuation = Attenuation.LINEAR;
        this.looping = false;
        this.relative = false;
        this.volume = 1;
        follow();
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
