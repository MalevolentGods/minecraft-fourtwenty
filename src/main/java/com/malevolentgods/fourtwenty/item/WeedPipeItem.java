package com.malevolentgods.fourtwenty.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

public class WeedPipeItem extends Item {
    public WeedPipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nonnull ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // The same effects as the joint (shared with the bong), then the exhale.
            if (level instanceof ServerLevel serverLevel) com.malevolentgods.fourtwenty.util.SmokeEffects.apply(serverLevel, player);

            // Add 5 second cooldown (100 ticks = 5 seconds at 20 ticks per second)
            player.getCooldowns().addCooldown(this, 100);

            // Instead of consuming the item, damage it like a tool
            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
        }
        
        return stack;
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        
        // Start spawning smoke particles when beginning to use
        if (level instanceof ServerLevel serverLevel) {
            double x = player.getX();
            double y = player.getY() + player.getEyeHeight() + 0.3;
            double z = player.getZ();
            
            // Spawn initial smoke particles
            for (int i = 0; i < 6; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
                double offsetY = level.random.nextDouble() * 0.2;
                double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;
                
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, 
                    x + offsetX, y + offsetY, z + offsetZ, 
                    1, 0, 0.05, 0, 0.01);
            }
        }
        
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.TOOT_HORN; // Same smoking/inhaling animation as joint
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity entity) {
        return 32; // Same duration as joint
    }

    @Override
    public boolean isValidRepairItem(@Nonnull ItemStack toRepair, @Nonnull ItemStack repair) {
        // Allow repairing with weed buds - thematic and balanced
        return repair.getItem() == com.malevolentgods.fourtwenty.registry.ModItems.WEED_BUD.get();
    }
}
