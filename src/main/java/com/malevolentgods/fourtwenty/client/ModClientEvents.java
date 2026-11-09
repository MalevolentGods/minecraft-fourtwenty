package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.registry.ModMenuTypes;
import com.malevolentgods.fourtwenty.screen.WeedBongScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {
    /** Every Four Twenty tech item explains itself in its tooltip: the text is tooltip.fourtwenty.item.<id> in the lang file. */
    public static void tooltips(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!"fourtwenty".equals(id.getNamespace())) return;
        String key = "tooltip.fourtwenty.item." + id.getPath();
        // a dyed Wick says what its colour does
        var colour = com.malevolentgods.fourtwenty.power.WickBlock.colourOf(event.getItemStack());
        if (colour != com.malevolentgods.fourtwenty.power.WickBlock.Colour.NONE && event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem bi
                && bi.getBlock() instanceof com.malevolentgods.fourtwenty.power.WickBlock) {
            String role = colour == com.malevolentgods.fourtwenty.power.WickBlock.Colour.RED ? "red" : colour == com.malevolentgods.fourtwenty.power.WickBlock.Colour.BLACK ? "black" : "other";
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.fourtwenty.wick." + role,
                    net.minecraft.network.chat.Component.translatable("color.minecraft." + colour.dye().getName())).withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        // what it really is, in electronics, quietly under its name
        String real = "tooltip.fourtwenty.real." + id.getPath();
        if (net.minecraft.client.resources.language.I18n.exists(real)) {
            event.getToolTip().add(Math.min(1, event.getToolTip().size()), net.minecraft.network.chat.Component.translatable(real)
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
        }
        if (!net.minecraft.client.resources.language.I18n.exists(key)) return;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        for (var line : font.getSplitter().splitLines(net.minecraft.network.chat.Component.translatable(key), 200, net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.ChatFormatting.GRAY))) {
            event.getToolTip().add(net.minecraft.network.chat.Component.literal(line.getString()).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    
    /** The Inspoweedometer's held reading, a line above the live one on the action bar. */
    @SubscribeEvent
    public static void registerLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.OVERLAY_MESSAGE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("fourtwenty", "meter_held"), (graphics, delta) -> {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.player == null || mc.options.hideGui) return;
                    var main = mc.player.getMainHandItem();
                    var stack = main.getItem() instanceof com.malevolentgods.fourtwenty.power.InspoweedometerItem ? main : mc.player.getOffhandItem();
                    if (!(stack.getItem() instanceof com.malevolentgods.fourtwenty.power.InspoweedometerItem)) return;
                    String held = com.malevolentgods.fourtwenty.power.InspoweedometerItem.held(stack);
                    if (held == null) return;
                    // drawn as the action bar draws the live reading (Gui.renderOverlayMessage), one line above it
                    var text = net.minecraft.network.chat.Component.translatable("message.fourtwenty.meter.held", held)
                            .withStyle(s -> s.withColor(com.malevolentgods.fourtwenty.power.InspoweedometerItem.heldColour(stack)));
                    int width = mc.font.width(text);
                    graphics.pose().pushPose();
                    graphics.pose().translate(graphics.guiWidth() / 2f, graphics.guiHeight() - 68 - 12, 0);
                    graphics.drawStringWithBackdrop(mc.font, text, -width / 2, -4, width, 0xFFFFFFFF);
                    graphics.pose().popPose();
                });
    }

    @SubscribeEvent
    public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) { PavePenClient.registerKeys(event); }

    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        PavePenClient.install();
        if (net.neoforged.fml.ModList.get().isLoaded("minecolonies")) {
            event.enqueueWork(com.malevolentgods.fourtwenty.compat.minecolonies.client.DispensaryClient::registerModel);
        }
        // Terpene names stay hidden until discovered; each discovery gets a toast.
        com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge.installClient(key -> net.minecraft.client.Minecraft.getInstance().getToasts()
                .addToast(new com.malevolentgods.fourtwenty.client.terpene.DiscoveryToast(key)));
        // Hemp glass and panes are translucent. Set here rather than in their models (as vanilla's stained glass is):
        // Domum Ornamentum takes a material's faces by render layer, and a model that names its own render type
        // gives none for any other layer, so the glass showed blank in Domum pieces' item icons.
        event.enqueueWork(() -> java.util.stream.Stream.concat(com.malevolentgods.fourtwenty.registry.ModTerpenes.GLASS.values().stream(),
                        com.malevolentgods.fourtwenty.registry.ModTerpenes.GLASS_PANES.values().stream())
                .forEach(b -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(b.get(), net.minecraft.client.renderer.RenderType.translucent())));
        // Bed items show their blanket colour: the item model overrides on this (dye id / 16).
        event.enqueueWork(() -> com.malevolentgods.fourtwenty.registry.ModItems.BEDS.forEach(bed ->
                net.minecraft.client.renderer.item.ItemProperties.register(bed.get(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("fourtwenty", "blanket"),
                        (stack, level, entity, seed) -> com.malevolentgods.fourtwenty.item.FancyBedItem.blanket(stack).getId() / 16f)));
    }

    @SubscribeEvent
    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.malevolentgods.fourtwenty.registry.ModBlockEntities.CURING_RACK.get(), CuringRackRenderer::new);
        event.registerBlockEntityRenderer(com.malevolentgods.fourtwenty.registry.ModBlockEntities.LAMP.get(), LampHaloRenderer::new);
        event.registerBlockEntityRenderer(com.malevolentgods.fourtwenty.electronics.ModElectronics.COMPONENT_ENTITY.get(), ComponentRenderer::new);
        event.registerBlockEntityRenderer(com.malevolentgods.fourtwenty.electronics.ModElectronics.HOP_ENTITY.get(), HopRenderer::new);
        event.registerBlockEntityRenderer(com.malevolentgods.fourtwenty.registry.ModPower.SUPPLY_ENTITY.get(), BenchRigRenderer::new);
        event.registerBlockEntityRenderer(com.malevolentgods.fourtwenty.electronics.ModElectronics.RIBBON_ENTITY.get(), RibbonRenderer::new);
        event.registerEntityRenderer(com.malevolentgods.fourtwenty.registry.ModEntities.BUZZ_BOT.get(), BuzzBotRenderer::new);
        event.registerEntityRenderer(com.malevolentgods.fourtwenty.registry.ModEntities.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BuzzBotRenderer.LAYER, BuzzBotRenderer::layer);
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
        event.registerItem(JointHand.EXTENSIONS, com.malevolentgods.fourtwenty.registry.ModItems.WEED_JOINT.get());
    }

    /** Roads, road slabs and road stairs join quarter by quarter as the chunk is drawn (RoadModel); our other blocks sit down onto them (SunkModel). */
    @SubscribeEvent
    public static void registerPorts(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        PortModel.register(event);
    }

    @SubscribeEvent
    public static void replaceRoadModels(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
        RoadModel.replace(event);
        SunkModel.wrap(event);
        PortModel.wrap(event);
        ComponentModel.wrap(event);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ModClientEvents::tooltips);
        event.register(com.malevolentgods.fourtwenty.registry.ModPower.MACHINE_MENU.get(), MachineScreen::new);
        event.register(com.malevolentgods.fourtwenty.registry.ModPower.PHENO_MENU.get(), PhenoScreen::new);
        event.register(com.malevolentgods.fourtwenty.registry.ModPower.RIG_MENU.get(), RigScreen::new);
        event.register(com.malevolentgods.fourtwenty.registry.ModPower.CONSTRUCTION_MENU.get(), ConstructionScreen::new);
        event.register(com.malevolentgods.fourtwenty.registry.ModPower.BI_MENU.get(), BIScreen::new);
        event.register(com.malevolentgods.fourtwenty.registry.ModPower.THINK_TANK_MENU.get(), ThinkTankScreen::new);
        event.register(ModMenuTypes.WEED_BONG.get(), WeedBongScreen::new);
        event.register(com.malevolentgods.fourtwenty.gear.ModGear.GEAR_MENU.get(), GearScreen::new);
        event.register(com.malevolentgods.fourtwenty.gear.ModGear.RE_UP_MENU.get(), ReUpScreen::new);
        event.register(com.malevolentgods.fourtwenty.registry.ModTerpenes.TERPINATOR_MENU.get(),
                com.malevolentgods.fourtwenty.client.terpene.TerpinatorScreen::new);
    }
}
