package com.malevolentgods.fourtwenty;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModCreativeTabs;
import com.malevolentgods.fourtwenty.registry.ModFeatures;
import com.malevolentgods.fourtwenty.util.DelayedEffectManager;

@Mod("fourtwenty")
public class FourTwenty {
    public FourTwenty(IEventBus bus) {
        // Initialize delayed effect system
        DelayedEffectManager.init();
        
        // Register items and blocks first
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        
        // Register world generation features
        ModFeatures.CONFIGURED_FEATURES.register(bus);
        ModFeatures.PLACED_FEATURES.register(bus);
        
        // Then register creative tabs
        ModCreativeTabs.CREATIVE_MODE_TABS.register(bus);
    }
}
