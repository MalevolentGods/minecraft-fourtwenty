package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/**
 * A Structurize placement handler that only changes what a colony builder pays for a block.
 *
 * <p>Placement and the "already built?" check go to whichever handler would own the block without
 * this one (normally MineColonies' general handler), and non-builder placement such as creative
 * paste keeps the normal cost. Register with {@link PlacementHandlers#add(IPlacementHandler)},
 * which inserts at index 3, ahead of the general handlers.
 */
public abstract class CostOnlyPlacementHandler implements IPlacementHandler {
    /** What a builder pays instead of the normal cost; null to keep the normal cost. */
    protected abstract List<ItemStack> builderCost(BlockState state, CompoundTag tileEntityData);

    @Override
    public ActionProcessingResult handle(Level world, BlockPos pos, BlockState state, CompoundTag tileEntityData, IPlacementContext context) {
        return next(world, pos, state).handle(world, pos, state, tileEntityData, context);
    }

    @Override
    public List<ItemStack> getRequiredItems(Level world, BlockPos pos, BlockState state, CompoundTag tileEntityData, IPlacementContext context) {
        // fancyPlacement() is true for survival builders, false for creative paste.
        if (context.fancyPlacement()) {
            var cost = builderCost(state, tileEntityData);
            if (cost != null) return cost;
        }
        return next(world, pos, state).getRequiredItems(world, pos, state, tileEntityData, context);
    }

    @Override
    public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, IPlacementContext context) {
        return next(null, BlockPos.ZERO, blueprintState).doesWorldStateMatchBlueprintState(worldState, blueprintState, blockEntityData, context);
    }

    /** The handler that would place this block if no cost-only handler were registered. */
    private IPlacementHandler next(Level world, BlockPos pos, BlockState state) {
        synchronized (PlacementHandlers.handlers) {
            for (IPlacementHandler handler : PlacementHandlers.handlers) {
                if (!(handler instanceof CostOnlyPlacementHandler) && handler.canHandle(world, pos, state)) return handler;
            }
        }
        return new PlacementHandlers.GeneralBlockPlacementHandler();
    }
}
