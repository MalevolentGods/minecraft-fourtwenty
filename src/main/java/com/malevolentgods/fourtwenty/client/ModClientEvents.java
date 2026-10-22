package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.registry.ModMenuTypes;
import com.malevolentgods.fourtwenty.screen.WeedBongScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {
    
    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        if (net.neoforged.fml.ModList.get().isLoaded("minecolonies")) {
            event.enqueueWork(com.malevolentgods.fourtwenty.compat.minecolonies.client.DispensaryClient::registerModel);
        }
        // Terpene names stay hidden until discovered; each discovery gets a toast.
        com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge.installClient(key -> net.minecraft.client.Minecraft.getInstance().getToasts()
                .addToast(new com.malevolentgods.fourtwenty.client.terpene.DiscoveryToast(key)));
        // Bed items show their blanket colour: the item model overrides on this (dye id / 16).
        event.enqueueWork(() -> com.malevolentgods.fourtwenty.registry.ModItems.BEDS.forEach(bed ->
                net.minecraft.client.renderer.item.ItemProperties.register(bed.get(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("fourtwenty", "blanket"),
                        (stack, level, entity, seed) -> com.malevolentgods.fourtwenty.item.FancyBedItem.blanket(stack).getId() / 16f)));
    }

    @SubscribeEvent
    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.malevolentgods.fourtwenty.registry.ModBlockEntities.LAMP.get(), LampHaloRenderer::new);
    }

    /** Lit lamp items get their halo in the inventory and hands, like ProjectRed's. */
    @SubscribeEvent
    public static void registerItemRenderers(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        var lit = java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(com.malevolentgods.fourtwenty.registry.ModBlocks.INVERTED_LAMPS.values().stream(),
                        com.malevolentgods.fourtwenty.registry.ModBlocks.LAMP_TRIMS.values().stream()).map(block -> block.get().asItem()),
                java.util.stream.Stream.concat(com.malevolentgods.fourtwenty.registry.ModTerpenes.INVERTED_LAMPS.values().stream(),
                        com.malevolentgods.fourtwenty.registry.ModTerpenes.LAMP_TRIMS.values().stream()).map(block -> block.get().asItem()))
            .toArray(net.minecraft.world.item.Item[]::new);
        event.registerItem(LampItemRenderer.EXTENSIONS, lit);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.WEED_BONG.get(), WeedBongScreen::new);
        event.register(com.malevolentgods.fourtwenty.registry.ModTerpenes.TERPINATOR_MENU.get(),
                com.malevolentgods.fourtwenty.client.terpene.TerpinatorScreen::new);
    }
}
