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
        return (float) (MAX_VOLUME * gain * MidnightMachineClientConfig.gain());   // the player's own volume and mute
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
        var player = Minecraft.getInstance().player;
        if (player != null) {
            float hf = highs(Math.sqrt(player.distanceToSqr(x, y, z)));
            if (Math.abs(hf - appliedHf) > 0.02f) applyHighs(hf);
        }
    }

    /**
     * Air takes the highs first (the user, 2026-11-14): far off you hear mostly the low end carrying. Nothing changes
     * near or at middle distance, so close by it sounds as mixed; past {@link #HIGHS_FROM} blocks, where the volume is
     * already fading, a gentle low-pass takes the highs down to a quarter by {@link #FAR}.
     */
    static final double HIGHS_FROM = 16;
    static final float HIGHS_AT_FAR = 0.25f;
    private float appliedHf = 1;

    static float highs(double d) {
        if (d <= HIGHS_FROM) return 1;
        double t = Math.min(1, (d - HIGHS_FROM) / (FAR - HIGHS_FROM));
        return (float) (1 - (1 - HIGHS_AT_FAR) * t);
    }

    private void applyHighs(float hf) {
        var engine = ((com.malevolentgods.fourtwenty.compat.minecolonies.mixin.SoundManagerAccessor) Minecraft.getInstance().getSoundManager()).fourtwenty$engine();
        var handle = ((com.malevolentgods.fourtwenty.compat.minecolonies.mixin.SoundEngineAccessor) engine).fourtwenty$channels().get(this);
        if (handle == null) return;   // not playing yet: next tick
        appliedHf = hf;
        handle.execute(channel -> DistanceFilter.apply(((com.malevolentgods.fourtwenty.compat.minecolonies.mixin.ChannelAccessor) channel).fourtwenty$source(), hf));
    }

    /**
     * One shared OpenAL low-pass filter (EFX), set and copied onto a source on the sound thread. Off for good if the
     * device has no EFX; made again after a sound reload drops it. Dynamic Surroundings leaves this sound alone (it
     * skips sounds with no attenuation of their own), so nothing else sets its filter.
     */
    private static final class DistanceFilter {
        private static int filter;
        private static boolean unavailable;

        static void apply(int source, float hf) {
            if (unavailable) return;
            try {
                if (!org.lwjgl.openal.AL.getCapabilities().ALC_EXT_EFX) {
                    unavailable = true;
                    return;
                }
                if (filter == 0 || !org.lwjgl.openal.EXTEfx.alIsFilter(filter)) {
                    filter = org.lwjgl.openal.EXTEfx.alGenFilters();
                    org.lwjgl.openal.EXTEfx.alFilteri(filter, org.lwjgl.openal.EXTEfx.AL_FILTER_TYPE, org.lwjgl.openal.EXTEfx.AL_FILTER_LOWPASS);
                }
                org.lwjgl.openal.EXTEfx.alFilterf(filter, org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAIN, 1f);
                org.lwjgl.openal.EXTEfx.alFilterf(filter, org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAINHF, hf);
                org.lwjgl.openal.AL10.alSourcei(source, org.lwjgl.openal.EXTEfx.AL_DIRECT_FILTER,
                        hf >= 0.999f ? org.lwjgl.openal.EXTEfx.AL_FILTER_NULL : filter);
            } catch (Throwable t) {
                unavailable = true;
            }
        }
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
