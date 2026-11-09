package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.GrowRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;

/** The big harvests of the top plots: half as many again of everything but seed, and the crop replants itself. */
public final class GrowOps {
    private GrowOps() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GrowOps::onDrops);
    }

    /** Whether a crop at {@code pos} stands on a plot with big harvests. */
    public static boolean bigHarvest(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof GrowOpBlock plot && plot.plotLevel() >= GrowRules.BIG_HARVEST_TIER;
    }

    /** Raises a harvest's drops in place. */
    public static void raise(BlockState crop, ServerLevel level, BlockPos pos, List<ItemStack> drops) {
        ItemStack seed = crop.getBlock().asItem().getDefaultInstance();
        ItemStack clone = crop.getCloneItemStack(new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false), level, pos, null);
        for (ItemStack stack : drops) {
            if (stack.is(clone.getItem()) || stack.is(seed.getItem())) continue;
            stack.setCount(GrowRules.bigHarvest(stack.getCount()));
        }
    }

    /** The plot tier under {@code pos}, 1 to 5, or 0 if it isn't on a Grow Op. */
    public static int tier(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof GrowOpBlock plot ? plot.plotLevel() : 0;
    }

    /**
     * A Grow Op's extra seed (specs/012-progression-balance): a mature weed crop on a plot of tier t has that tier's chance of one more
     * seed of its strain and traits. Null if none.
     */
    public static @javax.annotation.Nullable ItemStack extraSeed(BlockState state, ServerLevel level, BlockPos pos) {
        if (!(state.getBlock() instanceof com.malevolentgods.fourtwenty.block.WeedCropBlock weed) || !weed.isMaxAge(state)) return null;
        if (!com.malevolentgods.fourtwenty.processing.ProcessingConfig.strainStats().extraSeed(tier(level, pos), level.getRandom().nextDouble())) return null;
        return state.getCloneItemStack(new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false), level, pos, null);
    }

    private static void onDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        ItemStack extra = extraSeed(state, event.getLevel(), pos);
        if (extra != null) {
            event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, extra));
        }
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || !bigHarvest(event.getLevel(), pos)) return;
        var stacks = event.getDrops().stream().map(ItemEntity::getItem).toList();
        raise(state, event.getLevel(), pos, stacks);
        ItemStack seed = state.getCloneItemStack(new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false), event.getLevel(), pos, null);
        for (ItemEntity drop : event.getDrops()) {
            if (!drop.getItem().is(seed.getItem())) continue;
            drop.getItem().shrink(1);
            BlockState young = state.getBlock() instanceof com.malevolentgods.fourtwenty.block.WeedCropBlock weed ? weed.atAge(state, 0) : crop.getStateForAge(0);
            event.getLevel().setBlock(pos, young, 3);
            break;
        }
    }
}
