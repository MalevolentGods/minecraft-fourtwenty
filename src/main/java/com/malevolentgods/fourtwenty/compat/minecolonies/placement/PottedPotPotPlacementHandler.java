package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import com.ldtteam.structurize.placement.IPlacementContext;
import com.malevolentgods.fourtwenty.block.PottedPotPotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A Potted Pot Pot costs a builder the Pot Pot of its colour ({@link BuilderCostSubstitutions}: the
 * seed is free), and counts as built whatever its plant's age, so a pot that's grown or been
 * harvested isn't knocked down and replaced at the next upgrade.
 */
public final class PottedPotPotPlacementHandler extends CostOnlyPlacementHandler {
    @Override
    public boolean canHandle(Level world, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof PottedPotPotBlock;
    }

    @Override
    protected List<ItemStack> builderCost(Level world, BlockPos pos, BlockState state, CompoundTag tileEntityData, IPlacementContext context) {
        return BuilderCostSubstitutions.costFor(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
                .map(id -> List.of(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)))))
                .orElse(null);
    }

    @Override
    public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, IPlacementContext context) {
        return worldState.is(blueprintState.getBlock());
    }
}
