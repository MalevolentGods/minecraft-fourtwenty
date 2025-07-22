package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.blockentity.DrugCraftingBenchBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DrugCraftingBenchBlock extends BaseEntityBlock {
    public static final MapCodec<DrugCraftingBenchBlock> CODEC = simpleCodec(DrugCraftingBenchBlock::new);
    
    public DrugCraftingBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new DrugCraftingBenchBlockEntity(pos, state);
    }

    @Override
    @Nonnull
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nonnull
    protected InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DrugCraftingBenchBlockEntity craftingBench) {
                MenuProvider menuProvider = new MenuProvider() {
                    @Override
                    @Nonnull
                    public Component getDisplayName() {
                        return Component.translatable("container.fourtwenty.drug_crafting_bench");
                    }

                    @Override
                    @Nullable
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, 
                            @Nonnull net.minecraft.world.entity.player.Inventory playerInventory, @Nonnull Player player) {
                        return craftingBench.createMenu(containerId, playerInventory, player);
                    }
                };
                
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(menuProvider, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        
        return createTickerHelper(blockEntityType, ModBlockEntities.DRUG_CRAFTING_BENCH.get(), 
            (level1, pos, state1, blockEntity) -> blockEntity.tick());
    }

    @Override
    protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DrugCraftingBenchBlockEntity craftingBench) {
                craftingBench.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
