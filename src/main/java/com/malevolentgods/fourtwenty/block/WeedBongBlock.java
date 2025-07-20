package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WeedBongBlock extends BaseEntityBlock {
    public static final MapCodec<WeedBongBlock> CODEC = simpleCodec(WeedBongBlock::new);
    
    // Properties for the bong state
    public static final BooleanProperty HAS_WATER = BooleanProperty.create("has_water");
    public static final IntegerProperty WEED_LEVEL = IntegerProperty.create("weed_level", 0, 4); // 0 = empty, 4 = full
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    
    // Bong shape - roughly 3x3x5 blocks in size but as a single block
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    
    public WeedBongBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HAS_WATER, false)
            .setValue(WEED_LEVEL, 0)
            .setValue(LIT, false));
    }

    @Override
    @Nonnull
    public MapCodec<WeedBongBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_WATER, WEED_LEVEL, LIT);
    }

    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Nonnull
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WeedBongBlockEntity bongEntity) {
            player.openMenu(bongEntity);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new WeedBongBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.WEED_BONG.get(), WeedBongBlockEntity::tick);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WeedBongBlockEntity bongEntity) {
                // Drop all items from the inventory
                for (int i = 0; i < bongEntity.getContainerSize(); i++) {
                    ItemStack stack = bongEntity.getItem(i);
                    if (!stack.isEmpty()) {
                        popResource(level, pos, stack);
                    }
                }
                bongEntity.clearContent();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
