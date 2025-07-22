package com.malevolentgods.fourtwenty.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nonnull;
import java.util.List;

public class WeedGrinderItem extends Item {
    
    public WeedGrinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> tooltipComponents, @Nonnull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Add tooltip information
        tooltipComponents.add(Component.translatable("item.fourtwenty.weed_grinder.tooltip.line1")
            .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.fourtwenty.weed_grinder.tooltip.line2")
            .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        
        // Show durability information
        if (stack.isDamageableItem()) {
            int durability = stack.getMaxDamage() - stack.getDamageValue();
            int maxDurability = stack.getMaxDamage();
            tooltipComponents.add(Component.translatable("item.fourtwenty.weed_grinder.tooltip.durability", 
                durability, maxDurability).withStyle(net.minecraft.ChatFormatting.BLUE));
        }
    }

    @Override
    public boolean isDamageable(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 1; // Allow basic enchantments
    }

    /**
     * Check if this grinder can be used (has durability remaining)
     */
    public static boolean canUse(ItemStack grinderStack) {
        return !grinderStack.isEmpty() && 
               grinderStack.getItem() instanceof WeedGrinderItem && 
               grinderStack.getDamageValue() < grinderStack.getMaxDamage();
    }

    /**
     * Use the grinder (damage it by 1)
     */
    public static void useGrinder(ItemStack grinderStack) {
        if (canUse(grinderStack)) {
            grinderStack.setDamageValue(grinderStack.getDamageValue() + 1);
            if (grinderStack.getDamageValue() >= grinderStack.getMaxDamage()) {
                grinderStack.shrink(1); // Break the grinder when durability reaches 0
            }
        }
    }

    /**
     * Get the efficiency bonus this grinder provides
     */
    public static float getEfficiencyBonus(ItemStack grinderStack) {
        if (!canUse(grinderStack)) {
            return 0.0f;
        }
        
        // Higher durability remaining = better efficiency
        float durabilityRatio = (float)(grinderStack.getMaxDamage() - grinderStack.getDamageValue()) / grinderStack.getMaxDamage();
        return 0.2f + (durabilityRatio * 0.3f); // 20% to 50% efficiency bonus
    }
}
