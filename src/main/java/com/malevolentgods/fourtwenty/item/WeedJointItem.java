package com.malevolentgods.fourtwenty.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import javax.annotation.Nonnull;

public class WeedJointItem extends Item {
    public WeedJointItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nonnull ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // Apply multiple effects to simulate being "high"
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1)); // 30 seconds regeneration II
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0)); // 2 minutes night vision
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1)); // 10 seconds hunger II (makes player hungrier)
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 1800, 0)); // 1.5 minutes luck I (better loot/fishing)
            
            // Add a small chance for confusion effect
            if (level.random.nextFloat() < 0.3f) { // 30% chance
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0)); // 20 seconds confusion
            }
        }
        
        // Consume the item (like food)
        stack.shrink(1);
        return stack;
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.TOOT_HORN; // More appropriate for smoking/inhaling
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity entity) {
        return 32; // Same as food items
    }
}
