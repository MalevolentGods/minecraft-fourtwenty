package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.power.WickBlock;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** A dyed Wick takes its dye's colour over its own texture; undyed, it's left as drawn. */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WickColours {
    private WickColours() {}

    /** A Resinator's bands: the resistor colour code of its value (tint 0, 1, 2: first digit, second digit, multiplier). */
    @SubscribeEvent
    public static void resinatorBands(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            if (tint < 0 || tint > 2 || !state.hasProperty(com.malevolentgods.fourtwenty.electronics.ResinatorBlock.VALUE)) return -1;
            double ohms = com.malevolentgods.fourtwenty.electronics.ResinatorBlock.ohms(state.getValue(com.malevolentgods.fourtwenty.electronics.ResinatorBlock.VALUE));
            return 0xFF000000 | com.malevolentgods.fourtwenty.electronics.ResinatorBlock.band(ohms, tint);
        }, com.malevolentgods.fourtwenty.electronics.ModElectronics.COMPONENTS.get(com.malevolentgods.fourtwenty.electronics.ComponentType.RESINATOR).get());
    }

    @SubscribeEvent
    public static void resinatorItemBands(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint < 0 || tint > 2 ? -1
                : 0xFF000000 | com.malevolentgods.fourtwenty.electronics.ResinatorBlock.band(com.malevolentgods.fourtwenty.electronics.ComponentType.RESINATOR.defaultValue, tint),
                com.malevolentgods.fourtwenty.electronics.ModElectronics.COMPONENTS.get(com.malevolentgods.fourtwenty.electronics.ComponentType.RESINATOR).get());
    }

    /** A dyed Wick or cable item shows its colour too. */
    @SubscribeEvent
    public static void itemColours(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            var dye = WickBlock.colourOf(stack).dye();
            return dye == null ? -1 : 0xFF000000 | dye.getTextureDiffuseColor();
        }, ModPower.HEMP_WICK.get(), ModPower.ROLLED_WICK.get(), ModPower.GLASS_WICK.get(), ModPower.GOLD_WICK.get(),
                ModPower.HEMP_TWIN_WICK.get(), ModPower.ROLLED_TWIN_WICK.get(), ModPower.GLASS_TWIN_WICK.get(), ModPower.GOLD_TWIN_WICK.get(),
                ModPower.HEMP_DAISY_WICK.get(), ModPower.ROLLED_DAISY_WICK.get(), ModPower.GLASS_DAISY_WICK.get(), ModPower.GOLD_DAISY_WICK.get(),
                com.malevolentgods.fourtwenty.electronics.ModElectronics.HEMP_THREAD.get());
    }

    @SubscribeEvent
    public static void colours(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            var dye = state.getValue(WickBlock.COLOUR).dye();
            return dye == null ? -1 : dye.getTextureDiffuseColor();
        }, ModPower.HEMP_WICK.get(), ModPower.ROLLED_WICK.get(), ModPower.GLASS_WICK.get(), ModPower.GOLD_WICK.get(),
                ModPower.HEMP_TWIN_WICK.get(), ModPower.ROLLED_TWIN_WICK.get(), ModPower.GLASS_TWIN_WICK.get(), ModPower.GOLD_TWIN_WICK.get(),
                ModPower.HEMP_DAISY_WICK.get(), ModPower.ROLLED_DAISY_WICK.get(), ModPower.GLASS_DAISY_WICK.get(), ModPower.GOLD_DAISY_WICK.get(),
                com.malevolentgods.fourtwenty.electronics.ModElectronics.HEMP_THREAD.get());
    }
}
