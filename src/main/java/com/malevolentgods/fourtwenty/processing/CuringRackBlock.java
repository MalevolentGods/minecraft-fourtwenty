package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Hang trimmed buds with a right-click; an empty hand takes cured ones down, and sneaking takes any bud back. */
public class CuringRackBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CuringRackBlock> CODEC = simpleCodec(CuringRackBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public CuringRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected @Nonnull MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new CuringRackBlockEntity(pos, state);
    }

    @Override @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CURING_RACK.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<CuringRackBlockEntity>) CuringRackBlockEntity::serverTick;
    }

    @Override protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                 @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (!stack.is(ModItems.TRIMMED_BUD.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CuringRackBlockEntity rack) {
            if (rack.hang(stack)) stack.consume(1, player);
            else player.displayClientMessage(Component.translatable("block.fourtwenty.curing_rack.full"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                                                  @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CuringRackBlockEntity rack) {
            ItemStack bud = rack.take(!player.isSecondaryUseActive());
            if (!bud.isEmpty()) {
                if (!player.getInventory().add(bud)) player.drop(bud, false);
            } else if (rack.minutesLeft() >= 0) {
                player.displayClientMessage(Component.translatable("block.fourtwenty.curing_rack.status", Math.max(1, rack.minutesLeft())), true);
            } else {
                player.displayClientMessage(Component.translatable("block.fourtwenty.curing_rack.empty"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CuringRackBlockEntity rack) Containers.dropContents(level, pos, rack.buds());
        super.onRemove(state, level, pos, next, moved);
    }
}
