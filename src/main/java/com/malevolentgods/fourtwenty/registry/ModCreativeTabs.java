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
                output.accept(ModItems.INFUSER.get());
                output.accept(ModItems.INFUSER_LAB.get());
                output.accept(ModItems.WEED_SEEDS.get());
                output.accept(ModItems.ROUGH_BUD.get());
                output.accept(ModItems.TRIMMED_BUD.get());
                output.accept(ModItems.WEED_BUD.get());
                output.accept(ModItems.TRIM_TRAY.get());
                output.accept(ModItems.CURING_RACK.get());
                com.malevolentgods.fourtwenty.registry.ModPower.ITEMS.forEach(item -> output.accept(item.get()));
                output.accept(com.malevolentgods.fourtwenty.registry.ModPower.ASH.get());
                output.accept(com.malevolentgods.fourtwenty.registry.ModPower.BUZZ_BOT_ITEM.get());
                output.accept(ModItems.WEED_JOINT.get());
                output.accept(ModItems.WEED_PIPE.get());
                for (var strain : com.malevolentgods.fourtwenty.domain.Strain.values()) {
                    if (strain == com.malevolentgods.fourtwenty.domain.Strain.LANDRACE) continue;
                    for (var item : java.util.List.of(ModItems.WEED_SEEDS.get(), ModItems.ROUGH_BUD.get(), ModItems.TRIMMED_BUD.get(), ModItems.WEED_BUD.get(), ModItems.WEED_JOINT.get()))
                        output.accept(ModDataComponents.withStrain(new ItemStack(item), strain));
                }
                output.accept(ModItems.WEED_COOKIE.get());
                output.accept(ModItems.WEED_BROWNIE.get());
                output.accept(ModItems.WEED_BONG.get());
                output.accept(ModItems.HOTBOX.get());
                ModItems.BONGS.values().forEach(bong -> output.accept(bong.get()));
                ModItems.LAMPS.forEach(lamp -> output.accept(lamp.get()));
                ModItems.SIDE_TABLES.forEach(table -> output.accept(table.get()));
                // Every blanket colour of every bed, lime (as crafted) first.
                ModItems.BEDS.forEach(bed -> {
                    output.accept(bed.get());
                    for (var color : net.minecraft.world.item.DyeColor.values()) {
                        if (color != net.minecraft.world.item.DyeColor.LIME) output.accept(bed.get().withBlanket(color));
                    }
                });
                // Note: Crop block is not added as it's placed by seeds
            })
            .build());
}
