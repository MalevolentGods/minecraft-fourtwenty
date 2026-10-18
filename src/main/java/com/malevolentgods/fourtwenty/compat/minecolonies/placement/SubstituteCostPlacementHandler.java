package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
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
 * Charges a colony builder the {@link BuilderCostSubstitutions} item for blocks it cannot
 * reasonably obtain, while still placing the blueprint's real block.
 *
 * <p>Only the bill changes: placement and the "already built?" check go to whichever handler
 * would own the block without this one (MineColonies' general handler for froglights), and
 * non-builder placement such as creative paste keeps the normal cost. Registered at index 3 by
 * {@link PlacementHandlers#add(IPlacementHandler)}, ahead of the general handlers.
 */
public final class SubstituteCostPlacementHandler implements IPlacementHandler {
    @Override
    public boolean canHandle(Level world, BlockPos pos, BlockState state) {
        return cost(state) != null;
    }

    @Override
    public ActionProcessingResult handle(Level world, BlockPos pos, BlockState state, CompoundTag tileEntityData, IPlacementContext context) {
        return next(world, pos, state).handle(world, pos, state, tileEntityData, context);
    }

    @Override
    public List<ItemStack> getRequiredItems(Level world, BlockPos pos, BlockState state, CompoundTag tileEntityData, IPlacementContext context) {
        // fancyPlacement() is true for survival builders, false for creative paste.
        if (context.fancyPlacement()) return List.of(new ItemStack(cost(state)));
        return next(world, pos, state).getRequiredItems(world, pos, state, tileEntityData, context);
    }

    @Override
    public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, IPlacementContext context) {
        return next(null, BlockPos.ZERO, blueprintState).doesWorldStateMatchBlueprintState(worldState, blueprintState, blockEntityData, context);
    }

    private static net.minecraft.world.item.Item cost(BlockState state) {
        return BuilderCostSubstitutions.costFor(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
                .map(id -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)))
                .orElse(null);
    }

    /** The handler that would place this block if this one were not registered. */
    private IPlacementHandler next(Level world, BlockPos pos, BlockState state) {
        synchronized (PlacementHandlers.handlers) {
            for (IPlacementHandler handler : PlacementHandlers.handlers) {
                if (!(handler instanceof SubstituteCostPlacementHandler) && handler.canHandle(world, pos, state)) return handler;
            }
        }
        return new PlacementHandlers.GeneralBlockPlacementHandler();
    }
}
