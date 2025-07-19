package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import com.malevolentgods.fourtwenty.registry.ModItems;
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
        if (!(blockEntity instanceof WeedBongBlockEntity bongEntity)) {
            return InteractionResult.FAIL;
        }

        ItemStack heldItem = player.getMainHandItem();
        
        // Handle water bucket interaction
        if (heldItem.is(Items.WATER_BUCKET) && !state.getValue(HAS_WATER)) {
            level.setBlock(pos, state.setValue(HAS_WATER, true), 3);
            if (!player.isCreative()) {
                player.setItemInHand(player.getUsedItemHand(), new ItemStack(Items.BUCKET));
            }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        
        // Handle bucket interaction to empty water
        if (heldItem.is(Items.BUCKET) && state.getValue(HAS_WATER)) {
            level.setBlock(pos, state.setValue(HAS_WATER, false), 3);
            if (!player.isCreative()) {
                player.setItemInHand(player.getUsedItemHand(), new ItemStack(Items.WATER_BUCKET));
            }
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        // Handle weed bud loading
        if (heldItem.is(ModItems.WEED_BUD.get()) && state.getValue(WEED_LEVEL) < 4) {
            int currentLevel = state.getValue(WEED_LEVEL);
            level.setBlock(pos, state.setValue(WEED_LEVEL, currentLevel + 1), 3);
            if (!player.isCreative()) {
                heldItem.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
            return InteractionResult.SUCCESS;
        }

        // Handle lighting with flint and steel
        if (heldItem.is(Items.FLINT_AND_STEEL) && canLight(state)) {
            bongEntity.lightBong((ServerPlayer) player);
            level.setBlock(pos, state.setValue(LIT, true), 3);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.isCreative()) {
                heldItem.hurtAndBreak(1, player, player.getEquipmentSlotForItem(heldItem));
            }
            return InteractionResult.SUCCESS;
        }

        // Handle using the bong (smoking)
        if (heldItem.isEmpty() && canUse(state)) {
            bongEntity.useBong((ServerPlayer) player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private boolean canLight(BlockState state) {
        return state.getValue(HAS_WATER) && 
               state.getValue(WEED_LEVEL) > 0 && 
               !state.getValue(LIT);
    }

    private boolean canUse(BlockState state) {
        return state.getValue(HAS_WATER) && 
               state.getValue(WEED_LEVEL) > 0 && 
               state.getValue(LIT);
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
            // Drop weed buds when block is broken
            int weedLevel = state.getValue(WEED_LEVEL);
            if (weedLevel > 0) {
                popResource(level, pos, new ItemStack(ModItems.WEED_BUD.get(), weedLevel));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
