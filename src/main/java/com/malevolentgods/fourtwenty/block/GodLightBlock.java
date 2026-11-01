package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/** The light around a Golden God: air that shines. It clears itself a moment after it was last renewed, so nothing is ever left behind. */
public class GodLightBlock extends AirBlock {
    public GodLightBlock(Properties properties) { super(properties); }

    @Override protected void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        level.removeBlock(pos, false);
    }
}
