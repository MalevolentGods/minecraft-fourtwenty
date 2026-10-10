package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nonnull;

public class WeedCropBlock extends CropBlock {
    public WeedCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @Nonnull ItemLike getBaseSeedId() {
        return ModItems.WEED_SEEDS.get();
    }

    /** Roll standard loot once, retaining one seed for the crop that remains planted. */
    @Override
    protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level,
            @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        // An earlier interaction listener may already have harvested this position. The
        // caller can still hold the pre-event state (notably Actually Harvest with an empty hand).
        if (!isMaxAge(state) || !level.getBlockState(pos).equals(state)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            var drops = getDrops(state, server, pos, null, player, player.getMainHandItem());
            if (!level.setBlock(pos, getStateForAge(0), 3)) return InteractionResult.PASS;
            boolean retainedSeed = false;
            for (ItemStack stack : drops) {
                if (!retainedSeed && stack.is(ModItems.WEED_SEEDS.get()) && !stack.isEmpty()) {
                    stack.shrink(1);
                    retainedSeed = true;
                }
                popResource(level, pos, stack);
            }
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
