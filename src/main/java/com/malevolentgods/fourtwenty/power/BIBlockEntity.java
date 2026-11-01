package com.malevolentgods.fourtwenty.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The block entity of a {@link BIBlock}. */
public abstract class BIBlockEntity extends BlockEntity {
    protected BIBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void serverTick();

    protected void lit(boolean on) {
        BlockState state = getBlockState();
        if (level != null && state.getValue(BIBlock.LIT) != on) level.setBlock(worldPosition, state.setValue(BIBlock.LIT, on), 3);
    }
}
