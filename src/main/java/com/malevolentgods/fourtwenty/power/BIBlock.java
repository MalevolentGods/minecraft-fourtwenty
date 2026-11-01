package com.malevolentgods.fourtwenty.power;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A block on a Wick network: faces the player, shows LIT while it's working, tells the grid when it comes and goes, drops what it holds. */
public abstract class BIBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private final VoxelShape shape;

    protected BIBlock(VoxelShape shape, Properties properties) {
        super(properties);
        this.shape = shape;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override protected @Nonnull MapCodec<? extends HorizontalDirectionalBlock> codec() { return MapCodec.unit(this); }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, LIT); }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return shape;
    }

    @Override @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (world, pos, blockState, entity) -> {
            if (entity instanceof BIBlockEntity bi) bi.serverTick();
        };
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) BIGrid.changed(level);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof Container container) Containers.dropContents(level, pos, container);
            BIGrid.changed(level);
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
