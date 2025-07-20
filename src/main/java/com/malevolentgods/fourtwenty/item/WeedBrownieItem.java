package com.malevolentgods.fourtwenty.item;

import net.minecraft.world.food.FoodProperties;

public class WeedBrownieItem extends WeedEdibleItem {
    public WeedBrownieItem(Properties properties) {
        super(
            properties.food(new FoodProperties.Builder()
                .nutrition(4) // More filling than cookie
                .saturationModifier(0.3f) // Better saturation than cookie
                .build()),
            300, // 15 second delay (300 ticks) - longer onset for premium
            6000, // 5 minute base duration (6000 ticks) - longer than cookies
            1, // Higher amplifier (level 2 effects)
            true // Premium edible with additional effects
        );
    }
}
