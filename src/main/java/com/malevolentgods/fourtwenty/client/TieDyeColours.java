package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.tiedye.Colourways;
import com.malevolentgods.fourtwenty.tiedye.ModTieDye;
import com.malevolentgods.fourtwenty.tiedye.TieDye;
import com.malevolentgods.fourtwenty.tiedye.TieDyeBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Tie-dye on screen (specs/018-tie-dye-shack research R1): each pattern is four greyscale slot masks with tint indices 0-3, and these
 * handlers colour slot k with the colourway's k-th colour, from the block entity for a placed block and from the item's component for an
 * item. Items pick their pattern's model by the `fourtwenty:pattern` property (pattern / 8).
 */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TieDyeColours {
    private TieDyeColours() {}

    private static final int UNDYED = 0xFFFFFF;

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            if (tint < 0 || level == null || pos == null) return UNDYED;
            return level.getBlockEntity(pos) instanceof TieDyeBlockEntity be ? Colourways.slotRgb(be.colourway(), tint) : UNDYED;
        }, ModTieDye.blocks().toArray(net.minecraft.world.level.block.Block[]::new));
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            TieDye look = stack.get(ModDataComponents.TIE_DYE.get());
            return tint < 0 || look == null ? 0xFFFFFFFF : 0xFF000000 | Colourways.slotRgb(look.colourway(), tint);
        }, ModTieDye.items().toArray(net.minecraft.world.level.ItemLike[]::new));
    }

    /** Tie-dyed hemp spreads its pattern over squares of blocks (research R11): its baked models are wrapped to draw by fit. */
    @SubscribeEvent
    public static void models(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
        TieDyeModel.clearCache();
        for (var state : ModTieDye.HEMP.get().getStateDefinition().getPossibleStates()) {
            var location = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
            var model = event.getModels().get(location);
            if (model != null) event.getModels().put(location, new TieDyeModel(model));
        }
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            var id = ResourceLocation.fromNamespaceAndPath("fourtwenty", "pattern");
            for (var item : ModTieDye.items()) {
                net.minecraft.client.renderer.item.ItemProperties.register(item, id, (stack, level, entity, seed) -> {
                    TieDye look = stack.get(ModDataComponents.TIE_DYE.get());
                    return look == null ? 0 : look.pattern() / 8f;
                });
            }
        });
    }
}
