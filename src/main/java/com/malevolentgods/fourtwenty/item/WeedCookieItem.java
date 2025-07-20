package com.malevolentgods.fourtwenty.item;

import net.minecraft.world.food.FoodProperties;

public class WeedCookieItem extends WeedEdibleItem {
    public WeedCookieItem(Properties properties) {
        super(
            properties.food(new FoodProperties.Builder()
                .nutrition(2) // Same as vanilla cookie
                .saturationModifier(0.1f) // Same as vanilla cookie
                .build()),
            200, // 10 second delay (200 ticks)
            3600, // 3 minute base duration (3600 ticks)
            0, // Base amplifier (level 1 effects)
            false // Not premium
        );
    }
}
