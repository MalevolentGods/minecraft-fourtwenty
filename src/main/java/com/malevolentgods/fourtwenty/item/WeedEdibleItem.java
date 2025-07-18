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
            // Check if player already has pending edible effects to prevent stacking/overdose
            if (DelayedEffectManager.hasPendingEffect(player)) {
                // Player already has pending edible effects, don't allow another
                return stack;
            }

            // Schedule delayed effect application
            DelayedEffectManager.scheduleEffect(player, delayTicks, effectDuration, effectAmplifier, isPremium);

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

    @Override
    public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.EAT; // Eating animation for edibles
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity entity) {
        return 32; // Same as food items
    }
}
