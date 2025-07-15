package com.malevolentgods.fourtwenty.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "fourtwenty");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOUR_TWENTY_TAB = 
        CREATIVE_MODE_TABS.register("four_twenty_tab", () -> CreativeModeTab.builder()
            .title(Component.literal("Four Twenty"))
            .icon(() -> new ItemStack(ModItems.WEED_SEEDS.get()))
            .displayItems((parameters, output) -> {
                // Add all mod items here
                output.accept(ModItems.WEED_SEEDS.get());
                output.accept(ModItems.WEED_BUD.get());
                output.accept(ModItems.WEED_JOINT.get());
                // Note: Crop block is not added as it's placed by seeds
            })
            .build());
}
