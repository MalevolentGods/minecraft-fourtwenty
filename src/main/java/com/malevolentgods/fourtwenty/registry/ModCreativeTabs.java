package com.malevolentgods.fourtwenty.registry;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.electronics.ModElectronics;
import com.malevolentgods.fourtwenty.power.WickBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * The mod's creative tabs, side by side: Four Twenty (growing, processing, smoking), Strains, Hemp &amp; Terpenes (ModTerpenes), Boundless
 * Inspiration (power), Inspoweednetics (electronics and Lit Bits), Kush Suit (gear) and Décor.
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "fourtwenty");

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String id, Supplier<ItemStack> icon, ResourceKey<CreativeModeTab> after,
                                                                        CreativeModeTab.DisplayItemsGenerator items) {
        return CREATIVE_MODE_TABS.register(id, () -> {
            var builder = CreativeModeTab.builder().title(Component.translatable("itemGroup.fourtwenty." + id)).icon(icon).displayItems(items);
            if (after != null) builder.withTabsBefore(after);
            return builder.build();
        });
    }

    private static ResourceKey<CreativeModeTab> key(String id) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("fourtwenty", id));
    }

    /** Every dyed colour of a wire block (undyed is listed with its block). */
    private static void colours(CreativeModeTab.Output output, Supplier<? extends Block> wire) {
        for (var colour : WickBlock.Colour.values()) {
            if (colour == WickBlock.Colour.NONE) continue;
            ItemStack stack = new ItemStack(wire.get());
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                    net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(WickBlock.COLOUR, colour));
            output.accept(stack);
        }
    }

    /** Growing, processing and smoking (and the colony huts, with MineColonies). */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOUR_TWENTY_TAB = tab("four_twenty_tab", () -> new ItemStack(ModItems.WEED_SEEDS.get()), null,
            (parameters, output) -> {
                output.accept(ModItems.WEED_SEEDS.get());
                output.accept(ModItems.ROUGH_BUD.get());
                output.accept(ModItems.TRIMMED_BUD.get());
                output.accept(ModItems.WEED_BUD.get());
                output.accept(ModItems.TRIM_TRAY.get());
                output.accept(ModItems.CURING_RACK.get());
                output.accept(ModItems.WEED_JOINT.get());
                output.accept(ModItems.WEED_PIPE.get());
                output.accept(ModItems.WEED_BONG.get());
                ModItems.BONGS.values().forEach(bong -> output.accept(bong.get()));
                output.accept(ModItems.HOTBOX.get());
                output.accept(ModItems.WEED_COOKIE.get());
                output.accept(ModItems.WEED_BROWNIE.get());
                ModPower.EXTRACTS.forEach(item -> output.accept(item.get()));
                output.accept(ModItems.INFUSER.get());
                output.accept(ModItems.INFUSER_LAB.get());
                output.accept(ModItems.PAVE_PEN.get());
                output.accept(ModItems.PAVE_PEN_PRO.get());
            });

    /** Every strain's seeds, buds and joints. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STRAINS_TAB = tab("strains",
            () -> ModDataComponents.withStrain(new ItemStack(ModItems.WEED_BUD.get()), Strain.WAKE_AND_BAKE), FOUR_TWENTY_TAB.getKey(),
            (parameters, output) -> {
                for (var strain : Strain.values()) {
                    if (strain == Strain.LANDRACE) continue;
                    for (var item : List.of(ModItems.WEED_SEEDS.get(), ModItems.ROUGH_BUD.get(), ModItems.TRIMMED_BUD.get(), ModItems.WEED_BUD.get(), ModItems.WEED_JOINT.get())) {
                        output.accept(ModDataComponents.withStrain(new ItemStack(item), strain));
                    }
                }
            });

    /** Boundless Inspiration: Hookahs, Wicks in every colour, Stashes, the machines it runs. After Hemp &amp; Terpenes. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> POWER_TAB = tab("boundless_inspiration", () -> new ItemStack(ModPower.HEMP_WICK.get()),
            key("hemp_and_terpenes"), (parameters, output) -> {
                ModPower.ITEMS.forEach(item -> output.accept(item.get()));
                for (var wick : List.<Supplier<? extends Block>>of(ModPower.HEMP_WICK, ModPower.ROLLED_WICK, ModPower.GLASS_WICK, ModPower.GOLD_WICK)) colours(output, wick);
                output.accept(ModPower.ASH.get());
                output.accept(ModPower.BUZZ_BOT_ITEM.get());
            });

    /** Inspoweednetics: components, Hemp Thread and Eighth Ribbons in every colour, the Lit Bits, and the meter. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ELECTRONICS_TAB = tab("inspoweednetics",
            () -> new ItemStack(ModElectronics.COMPONENTS.get(com.malevolentgods.fourtwenty.electronics.ComponentType.DANKODER).get()), POWER_TAB.getKey(),
            (parameters, output) -> {
                output.accept(ModItems.INSPOWEEDOMETER.get());
                ModElectronics.ALL.forEach(item -> output.accept(item.get()));
                colours(output, ModElectronics.HEMP_THREAD);
                colours(output, ModElectronics.EIGHTH_RIBBON);
                com.malevolentgods.fourtwenty.lights.ModLights.ALL.forEach(item -> output.accept(item.get()));
            });

    /** The Kush Suit, the Blazer, and what charges them. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR_TAB = tab("kush_suit",
            () -> new ItemStack(com.malevolentgods.fourtwenty.gear.ModGear.PUFF_PLATE.get()), ELECTRONICS_TAB.getKey(),
            (parameters, output) -> com.malevolentgods.fourtwenty.gear.ModGear.ALL.forEach(item -> output.accept(item.get())));

    /** Lamps, furniture, and the coloured building blocks. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DECOR_TAB = tab("decor",
            () -> new ItemStack(ModTerpenes.COUCHES.values().iterator().next().get()), GEAR_TAB.getKey(),
            (parameters, output) -> {
                ModItems.LAMPS.forEach(lamp -> output.accept(lamp.get()));
                ModTerpenes.LAMPS.values().forEach(b -> output.accept(b.get()));
                ModTerpenes.INVERTED_LAMPS.values().forEach(b -> output.accept(b.get()));
                ModTerpenes.LAMP_TRIMS.values().forEach(b -> output.accept(b.get()));
                ModItems.SIDE_TABLES.forEach(table -> output.accept(table.get()));
                // every blanket colour of every bed, lime (as crafted) first
                ModItems.BEDS.forEach(bed -> {
                    output.accept(bed.get());
                    for (var color : net.minecraft.world.item.DyeColor.values()) {
                        if (color != net.minecraft.world.item.DyeColor.LIME) output.accept(bed.get().withBlanket(color));
                    }
                });
                ModTerpenes.RUGS.values().forEach(b -> output.accept(b.get()));
                ModTerpenes.COUCHES.values().forEach(b -> output.accept(b.get()));
                ModTerpenes.POT_POTS.values().forEach(b -> output.accept(b.get()));
                ModTerpenes.POTTED_POT_POTS.values().forEach(b -> output.accept(b.get()));
                for (int t = 0; t < 3; t++) {
                    output.accept(ModTerpenes.PATHS.get(t).get());
                    output.accept(ModTerpenes.PATH_SLABS.get(t).get());
                    output.accept(ModTerpenes.PATH_STAIRS.get(t).get());
                }
                ModTerpenes.CONCRETE.values().forEach(b -> output.accept(b.get()));
                ModTerpenes.CONCRETE_POWDER.values().forEach(b -> output.accept(b.get()));
                ModTerpenes.GLASS.values().forEach(b -> output.accept(b.get()));
                ModTerpenes.GLASS_PANES.values().forEach(b -> output.accept(b.get()));
            });
}
