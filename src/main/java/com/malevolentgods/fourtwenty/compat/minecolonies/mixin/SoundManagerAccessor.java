package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The sound engine behind the sound manager: for the Midnight Machine's distance filter (AlbumSoundInstance). */
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {
    @Accessor("soundEngine")
    SoundEngine fourtwenty$engine();
}
