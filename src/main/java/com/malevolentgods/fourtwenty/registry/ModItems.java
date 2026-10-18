package com.malevolentgods.fourtwenty.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.malevolentgods.fourtwenty.item.WeedJointItem;
import com.malevolentgods.fourtwenty.item.WeedPipeItem;
import com.malevolentgods.fourtwenty.item.WeedCookieItem;
import com.malevolentgods.fourtwenty.item.WeedBrownieItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(Registries.ITEM, "fourtwenty");

    public static final DeferredHolder<Item, ItemNameBlockItem> WEED_SEEDS =
        ITEMS.register("weed_seeds", () ->
            new ItemNameBlockItem(com.malevolentgods.fourtwenty.registry.ModBlocks.WEED_CROP.get(),
                new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> LIME_LAMP = blockItem("lime_lamp", ModBlocks.LIME_LAMP);
    public static final DeferredHolder<Item, BlockItem> GREEN_LAMP = blockItem("green_lamp", ModBlocks.GREEN_LAMP);
    public static final DeferredHolder<Item, BlockItem> WHITE_LAMP = blockItem("white_lamp", ModBlocks.WHITE_LAMP);
    public static final DeferredHolder<Item, BlockItem> LIME_LAMP_TRIM = blockItem("lime_lamp_trim", ModBlocks.LIME_LAMP_TRIM);

    private static DeferredHolder<Item, BlockItem> blockItem(String name, DeferredHolder<? extends net.minecraft.world.level.block.Block, ? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static final DeferredHolder<Item, Item> WEED_BUD =
        ITEMS.register("weed_bud", () ->
            new Item(new Item.Properties()));

    public static final DeferredHolder<Item, WeedJointItem> WEED_JOINT =
        ITEMS.register("weed_joint", () ->
            new WeedJointItem(new Item.Properties().stacksTo(16))); // Limited stack size for balance

    public static final DeferredHolder<Item, WeedPipeItem> WEED_PIPE =
        ITEMS.register("weed_pipe", () ->
            new WeedPipeItem(new Item.Properties().durability(4).stacksTo(1))); // 4 uses, doesn't stack

    public static final DeferredHolder<Item, WeedCookieItem> WEED_COOKIE =
        ITEMS.register("weed_cookie", () ->
            new WeedCookieItem(new Item.Properties().stacksTo(16))); // Standard stack size for food

    public static final DeferredHolder<Item, WeedBrownieItem> WEED_BROWNIE =
        ITEMS.register("weed_brownie", () ->
            new WeedBrownieItem(new Item.Properties().stacksTo(16))); // Standard stack size for food

    public static final DeferredHolder<Item, BlockItem> WEED_BONG =
        ITEMS.register("weed_bong", () ->
            new BlockItem(ModBlocks.WEED_BONG.get(), new Item.Properties().stacksTo(1))); // Single block item
}
