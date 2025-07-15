package com.malevolentgods.fourtwenty.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.malevolentgods.fourtwenty.item.WeedJointItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(Registries.ITEM, "fourtwenty");

    public static final DeferredHolder<Item, ItemNameBlockItem> WEED_SEEDS =
        ITEMS.register("weed_seeds", () ->
            new ItemNameBlockItem(com.malevolentgods.fourtwenty.registry.ModBlocks.WEED_CROP.get(),
                new Item.Properties()));

    public static final DeferredHolder<Item, Item> WEED_BUD =
        ITEMS.register("weed_bud", () ->
            new Item(new Item.Properties()));

    public static final DeferredHolder<Item, WeedJointItem> WEED_JOINT =
        ITEMS.register("weed_joint", () ->
            new WeedJointItem(new Item.Properties().stacksTo(16))); // Limited stack size for balance
}
