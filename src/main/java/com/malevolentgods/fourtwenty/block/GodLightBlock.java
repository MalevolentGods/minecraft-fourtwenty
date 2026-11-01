package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.util.GoldenGod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/** The light around a Golden God: air that shines. It keeps checking for its god and clears itself once they've moved on, so nothing is ever left behind. */
public class GodLightBlock extends AirBlock {
    public GodLightBlock(Properties properties) { super(properties); }

    @Override protected void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (GoldenGod.lighting(level, pos)) level.scheduleTick(pos, this, GoldenGod.LIGHT_LASTS);
        else level.removeBlock(pos, false);
    }
}
