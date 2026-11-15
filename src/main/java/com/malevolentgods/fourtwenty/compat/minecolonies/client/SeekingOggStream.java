package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * An OGG Vorbis stream that can start anywhere in a track (specs/014-midnight-machine research R2), so the album
 * resumes mid-song and a player arriving late hears the right place. Decodes with the LWJGL stb_vorbis that Minecraft
 * already bundles, from an in-memory copy of the file; frees its native memory on close.
 */
public final class SeekingOggStream implements AudioStream {
    private final ByteBuffer file;
    private final long decoder;
    private final int channels;
    private final AudioFormat format;
    private boolean closed;

    public SeekingOggStream(byte[] ogg, long startMs) throws IOException {
        file = MemoryUtil.memAlloc(ogg.length);
        file.put(ogg).flip();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var error = stack.mallocInt(1);
            decoder = STBVorbis.stb_vorbis_open_memory(file, error, null);
            if (decoder == MemoryUtil.NULL) {
                MemoryUtil.memFree(file);
                throw new IOException("stb_vorbis could not open the track (error " + error.get(0) + ")");
            }
            var info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);
            channels = info.channels();
            format = new AudioFormat(info.sample_rate(), 16, channels, true, false);
            if (startMs > 0) STBVorbis.stb_vorbis_seek(decoder, (int) Math.min(Integer.MAX_VALUE, startMs * info.sample_rate() / 1000));
        }
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer read(int size) throws IOException {
        if (closed) return BufferUtils.createByteBuffer(0);
        int frames = Math.max(1, size / (2 * channels));
        ShortBuffer pcm = MemoryUtil.memAllocShort(frames * channels);
        try {
            int got = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            ByteBuffer out = BufferUtils.createByteBuffer(got * channels * 2);
            for (int i = 0; i < got * channels; i++) out.putShort(pcm.get(i));
            return out.flip();
        } finally {
            MemoryUtil.memFree(pcm);
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        STBVorbis.stb_vorbis_close(decoder);
        MemoryUtil.memFree(file);
    }
}
