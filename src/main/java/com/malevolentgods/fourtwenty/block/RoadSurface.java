package com.malevolentgods.fourtwenty.block;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A block with a road's whole top: a road ({@link HempPathBlock}) or a road slab ({@link HempPathSlabBlock}).
 * A road, a double slab and a top slab all reach 15/16; a bottom slab 7/16. How they join each other
 * and the road stairs is {@link RoadShape}.
 */
public interface RoadSurface {
    int tier();

    /** The height of this state's top in pixels (15 or 7). */
    int surface(BlockState state);
}
