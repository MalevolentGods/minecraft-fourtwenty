package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import com.ldtteam.structurize.placement.IPlacementContext;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A king bed's extension has no item (it's part of the bed), so a builder would wait forever for
 * one: it costs nothing, and the bed columns around it are paid for as beds.
 */
public final class BedExtensionPlacementHandler extends CostOnlyPlacementHandler {
    @Override
    public boolean canHandle(Level world, BlockPos pos, BlockState state) {
        return state.is(ModBlocks.BED_EXTENSION.get());
    }

    @Override
    protected List<ItemStack> builderCost(Level world, BlockPos pos, BlockState state, CompoundTag tileEntityData, IPlacementContext context) {
        return List.of();
    }
}
