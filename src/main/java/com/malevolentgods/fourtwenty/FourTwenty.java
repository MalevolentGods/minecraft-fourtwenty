package com.malevolentgods.fourtwenty;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModCreativeTabs;
import com.malevolentgods.fourtwenty.registry.ModFeatures;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.registry.ModMenuTypes;
import com.malevolentgods.fourtwenty.util.DelayedEffectManager;

@Mod("fourtwenty")
public class FourTwenty {
    public FourTwenty(IEventBus bus, net.neoforged.fml.ModContainer container) {
        if (net.neoforged.fml.ModList.get().isLoaded("minecolonies")) {
            com.malevolentgods.fourtwenty.compat.minecolonies.ColonyIntegration.register(bus, container);
        }
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.WEED_BONG.get(), (entity, side) -> entity.itemHandler()));

        com.malevolentgods.fourtwenty.util.HotboxNetwork.register(bus);
        bus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent event) ->
            event.register(com.malevolentgods.fourtwenty.blockentity.HotboxBlockEntity.TICKETS));

        // Initialize delayed effect system
        DelayedEffectManager.init();
        
        // Register items and blocks first
        com.malevolentgods.fourtwenty.registry.ModTerpenes.init();
        com.malevolentgods.fourtwenty.terpene.TerpeneNetwork.register(bus);
        ModBlocks.BLOCKS.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.util.BongBuffs::tick);
        ModItems.ITEMS.register(bus);
        com.malevolentgods.fourtwenty.registry.ModDataComponents.COMPONENTS.register(bus);
        com.malevolentgods.fourtwenty.registry.ModStrainData.LOOT_FUNCTIONS.register(bus);
        com.malevolentgods.fourtwenty.registry.ModStrainData.RECIPE_SERIALIZERS.register(bus);
        com.malevolentgods.fourtwenty.util.StrainEffects.register(bus);
        com.malevolentgods.fourtwenty.infuser.Infuser.register(bus);
        
        // Register block entities
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        com.malevolentgods.fourtwenty.registry.ModEntities.ENTITY_TYPES.register(bus);
        
        // Register menus
        ModMenuTypes.MENU_TYPES.register(bus);
        
        // Register world generation features
        ModFeatures.CONFIGURED_FEATURES.register(bus);
        ModFeatures.PLACED_FEATURES.register(bus);
        
        // Then register creative tabs
        ModCreativeTabs.CREATIVE_MODE_TABS.register(bus);
    }
}
