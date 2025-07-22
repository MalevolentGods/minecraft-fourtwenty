package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.registry.ModMenuTypes;
import com.malevolentgods.fourtwenty.screen.WeedBongScreen;
import com.malevolentgods.fourtwenty.screen.DrugCraftingBenchScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {
    
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.WEED_BONG.get(), WeedBongScreen::new);
        event.register(ModMenuTypes.DRUG_CRAFTING_BENCH.get(), DrugCraftingBenchScreen::new);
    }
}
