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

    /** Lamps, inverted lamps and lamp trims in all 16 dye colours (block ids as in {@link ModBlocks}). */
    public static final java.util.List<DeferredHolder<Item, BlockItem>> LAMPS = lampItems();

    private static java.util.List<DeferredHolder<Item, BlockItem>> lampItems() {
        var items = new java.util.ArrayList<DeferredHolder<Item, BlockItem>>();
        for (var color : net.minecraft.world.item.DyeColor.values()) {
            items.add(blockItem(color.getName() + "_lamp", ModBlocks.LAMPS.get(color)));
            items.add(blockItem(color.getName() + "_inverted_lamp", ModBlocks.INVERTED_LAMPS.get(color)));
            items.add(blockItem(color.getName() + "_lamp_trim", ModBlocks.LAMP_TRIMS.get(color)));
        }
        return java.util.List.copyOf(items);
    }

    /** Side tables in every wood of {@link ModBlocks#SIDE_TABLE_WOODS}. */
    public static final java.util.List<DeferredHolder<Item, BlockItem>> SIDE_TABLES = ModBlocks.SIDE_TABLE_WOODS.stream()
            .map(wood -> blockItem(wood + "_side_table", ModBlocks.SIDE_TABLES.get(wood))).toList();

    /** Beds in every wood of {@link ModBlocks#SIDE_TABLE_WOODS} (vanilla BedItem: places both halves). */
    public static final java.util.List<DeferredHolder<Item, com.malevolentgods.fourtwenty.item.FancyBedItem>> BEDS = ModBlocks.SIDE_TABLE_WOODS.stream()
            .map(wood -> ITEMS.register(wood + "_bed", () -> new com.malevolentgods.fourtwenty.item.FancyBedItem(ModBlocks.BEDS.get(wood).get(),
                    new Item.Properties().stacksTo(1)))).toList();

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

    public static final java.util.Map<com.malevolentgods.fourtwenty.domain.BongVariety, DeferredHolder<Item, BlockItem>> BONGS = registerBongs();
    public static final DeferredHolder<Item, BlockItem> GLASS_BONG = BONGS.get(com.malevolentgods.fourtwenty.domain.BongVariety.GLASS);

    private static java.util.Map<com.malevolentgods.fourtwenty.domain.BongVariety, DeferredHolder<Item, BlockItem>> registerBongs() {
        var bongs = new java.util.EnumMap<com.malevolentgods.fourtwenty.domain.BongVariety, DeferredHolder<Item, BlockItem>>(
                com.malevolentgods.fourtwenty.domain.BongVariety.class);
        for (var variety : com.malevolentgods.fourtwenty.domain.BongVariety.values()) {
            bongs.put(variety, ITEMS.register(variety.path, () ->
                    new com.malevolentgods.fourtwenty.item.BongItem(variety, ModBlocks.BONGS.get(variety).get(), new Item.Properties().stacksTo(1))));
        }
        return java.util.Collections.unmodifiableMap(bongs);
    }

    public static final DeferredHolder<Item, BlockItem> WEED_BONG =
        ITEMS.register("weed_bong", () ->
            new BlockItem(ModBlocks.WEED_BONG.get(), new Item.Properties().stacksTo(1))); // Single block item
}
