package com.malevolentgods.fourtwenty.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import com.malevolentgods.fourtwenty.util.DelayedEffectManager;

import javax.annotation.Nonnull;

public class WeedEdibleItem extends Item {
    private final int delayTicks;
    private final int effectDuration;
    private final int effectAmplifier;
    private final boolean isPremium;

    public WeedEdibleItem(Properties properties, int delayTicks, int effectDuration, int effectAmplifier, boolean isPremium) {
        super(properties);
        this.delayTicks = delayTicks;
        this.effectDuration = effectDuration;
        this.effectAmplifier = effectAmplifier;
        this.isPremium = isPremium;
    }

    @Override
    public @Nonnull ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            if (!DelayedEffectManager.scheduleEffect(player, delayTicks, effectDuration, effectAmplifier, isPremium)) {
                player.stopUsingItem();
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.containerMenu.sendAllDataToRemote();
                }
                return stack;
            }

            // Spawn consumption particles
            if (level instanceof ServerLevel serverLevel) {
                double x = player.getX();
                double y = player.getY() + player.getEyeHeight();
                double z = player.getZ();
                
                // Create eating particles
                for (int i = 0; i < 3; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
                    double offsetY = level.random.nextDouble() * 0.2;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;
                    
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, 
                        x + offsetX, y + offsetY, z + offsetZ, 
                        1, 0, 0, 0, 0);
                }
            }
        }
        
        // Call parent to handle food consumption
        return super.finishUsingItem(stack, level, entity);
    }

    /** Reject a second dose before starting use; finishUsingItem also guards races. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        if (!level.isClientSide() && DelayedEffectManager.hasPendingEffect(player)) {
            return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.EAT; // Eating animation for edibles
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity entity) {
        return 32; // Same as food items
    }
}
