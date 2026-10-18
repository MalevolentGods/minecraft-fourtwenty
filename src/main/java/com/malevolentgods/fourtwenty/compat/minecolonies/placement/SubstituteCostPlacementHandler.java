package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/**
 * Charges a colony builder the {@link BuilderCostSubstitutions} item for blocks it cannot
 * reasonably obtain (froglights), while still placing the blueprint's real block.
 */
public final class SubstituteCostPlacementHandler extends CostOnlyPlacementHandler {
    @Override
    public boolean canHandle(Level world, BlockPos pos, BlockState state) {
        return substitute(state) != null;
    }

    @Override
    protected List<ItemStack> builderCost(BlockState state, CompoundTag tileEntityData) {
        return List.of(new ItemStack(substitute(state)));
    }

    private static net.minecraft.world.item.Item substitute(BlockState state) {
        return BuilderCostSubstitutions.costFor(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
                .map(id -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)))
                .orElse(null);
    }
}
