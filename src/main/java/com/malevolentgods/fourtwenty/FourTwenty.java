package com.malevolentgods.fourtwenty;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModCreativeTabs;

@Mod("fourtwenty")
public class FourTwenty {
    public FourTwenty(IEventBus bus) {
        // Register items and blocks first
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        
        // Then register creative tabs
        ModCreativeTabs.CREATIVE_MODE_TABS.register(bus);
    }
}
