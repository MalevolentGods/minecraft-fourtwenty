package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A channel's OpenAL source: for the Midnight Machine's distance filter (AlbumSoundInstance). */
@Mixin(Channel.class)
public interface ChannelAccessor {
    @Accessor("source")
    int fourtwenty$source();
}
