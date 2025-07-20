package com.malevolentgods.fourtwenty.block;

import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import com.malevolentgods.fourtwenty.registry.ModItems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WeedCropBlock extends CropBlock {
    public WeedCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @Nonnull ItemLike getBaseSeedId() {
        return ModItems.WEED_SEEDS.get();
    }

    @Override
    protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (getAge(state) == getMaxAge()) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005f);
            level.setBlock(pos, this.getStateForAge(0), 2);
            
            // Drop weed items directly
            if (!level.isClientSide()) {
                // Drop 1-3 weed buds
                int budCount = 1 + level.random.nextInt(3);
                popResource(level, pos, new net.minecraft.world.item.ItemStack(ModItems.WEED_BUD.get(), budCount));
                
                // Drop weed seeds with 5% chance
                if (level.random.nextFloat() < 0.05f) {
                    int seedCount = 1 + level.random.nextInt(4);
                    popResource(level, pos, new net.minecraft.world.item.ItemStack(ModItems.WEED_SEEDS.get(), seedCount));
                }
            }
            
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void playerDestroy(@Nonnull Level level, @Nonnull Player player, @Nonnull BlockPos pos, @Nonnull BlockState state, 
                             @Nullable net.minecraft.world.level.block.entity.BlockEntity blockEntity, @Nonnull ItemStack tool) {
        if (!level.isClientSide()) {
            int age = this.getAge(state);
            
            // If not fully grown, drop 1 seed for replanting
            if (age < this.getMaxAge()) {
                popResource(level, pos, new ItemStack(ModItems.WEED_SEEDS.get(), 1));
            }
        }
        
        // Call super for vanilla behavior (without drops since we handle them)
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}
