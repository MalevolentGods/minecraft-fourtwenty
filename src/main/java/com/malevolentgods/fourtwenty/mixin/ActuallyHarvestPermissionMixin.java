package com.malevolentgods.fourtwenty.mixin;

import com.malevolentgods.fourtwenty.compat.ColonyHarvestPermission;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Guard each target, including area targets, before Actually Harvest mutates it. */
@Pseudo
@Mixin(targets = "actuallyharvest.event.HarvestEventHandler", remap = false)
public abstract class ActuallyHarvestPermissionMixin {
    @Inject(method = "tryHarvest", at = @At("HEAD"), cancellable = true, require = 1)
    private static void fourtwenty$checkCropPermission(Level level, BlockPos pos, LivingEntity entity,
            InteractionHand hand, boolean hoe, CallbackInfoReturnable<Boolean> result) {
        if (!level.isClientSide() && level.getBlockState(pos).is(ModBlocks.WEED_CROP.get()) && entity instanceof Player player
                && !ColonyHarvestPermission.allows(player, level, pos, hand)) result.setReturnValue(false);
    }

    /** The installed mod reserves a seed even when replanting is off. Keep FourTwenty's
     * normal loot intact in that case; the original harvest still rolls and removes once. */
    @Inject(method = "lambda$harvestAndReplant$0", at = @At("HEAD"), cancellable = true, require = 1)
    private static void fourtwenty$keepUnplantedSeed(net.minecraft.world.item.Item seed,
            org.apache.commons.lang3.mutable.MutableBoolean retained, Level level, BlockPos pos,
            net.minecraft.world.item.ItemStack drop, CallbackInfo callback) {
        if (seed == com.malevolentgods.fourtwenty.registry.ModItems.WEED_SEEDS.get()
                && level.getBlockState(pos).is(ModBlocks.WEED_CROP.get())
                && !com.malevolentgods.fourtwenty.compat.ActuallyHarvestSettings.replants()) {
            net.minecraft.world.level.block.Block.popResource(level, pos, drop);
            callback.cancel();
        }
    }
}
