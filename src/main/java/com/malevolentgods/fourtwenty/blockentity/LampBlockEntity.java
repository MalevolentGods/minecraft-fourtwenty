package com.malevolentgods.fourtwenty.blockentity;

import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Lamps and lamp trims carry no data; the block entity only lets the client draw their halo. */
public class LampBlockEntity extends BlockEntity {
    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAMP.get(), pos, state);
    }
}
