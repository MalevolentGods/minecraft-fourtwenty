package com.malevolentgods.fourtwenty.registry;

import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.terpene.TerpeneItem;
import com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge;
import com.malevolentgods.fourtwenty.terpene.TerpinatorBlock;
import com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity;
import com.malevolentgods.fourtwenty.terpene.TerpinatorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The terpene system: hemp blocks (Natural Hemp and the 64 colours, each with stairs and a slab),
 * the 84 terpene items and the resin, and the Terpinator. Ids come from terpene codes
 * (hemp_myr_pin_lim, terpene_myr_pin_lim); names and colours from tools/terpenes.json.
 */
public final class ModTerpenes {
    private ModTerpenes() {}

    /** A hemp block with its stairs and slab, and their items. */
    public record Hemp(DeferredHolder<Block, Block> block, DeferredHolder<Block, StairBlock> stairs, DeferredHolder<Block, SlabBlock> slab,
                       List<DeferredHolder<Item, BlockItem>> items) {}

    public static final Hemp NATURAL_HEMP = hemp("natural_hemp");

    /** The 64 colours, by final terpene code, in codex order. */
    public static final Map<String, Hemp> HEMP = coloured();

    private static Map<String, Hemp> coloured() {
        Map<String, Hemp> out = new LinkedHashMap<>();
        for (Terpenes.Code code : Terpenes.finals()) out.put(code.id(), hemp("hemp_" + code.id()));
        return Collections.unmodifiableMap(out);
    }

    private static Hemp hemp(String id) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8f).sound(SoundType.WOOL);
        DeferredHolder<Block, Block> block = ModBlocks.BLOCKS.register(id, () -> new Block(props));
        DeferredHolder<Block, StairBlock> stairs = ModBlocks.BLOCKS.register(id + "_stairs",
                () -> new StairBlock(block.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(block.get())));
        DeferredHolder<Block, SlabBlock> slab = ModBlocks.BLOCKS.register(id + "_slab",
                () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(block.get())));
        List<DeferredHolder<Item, BlockItem>> items = List.of(
                ModItems.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())),
                ModItems.ITEMS.register(id + "_stairs", () -> new BlockItem(stairs.get(), new Item.Properties())),
                ModItems.ITEMS.register(id + "_slab", () -> new BlockItem(slab.get(), new Item.Properties())));
        return new Hemp(block, stairs, slab, items);
    }

    /** Sticky Situation: pressed from weed buds, refined into base terpenes. Hidden until first pressed. */
    public static final DeferredHolder<Item, Item> RESIN = ModItems.ITEMS.register("sticky_situation", () -> new Item(new Item.Properties()) {
        @Override public @Nonnull Component getName(@Nonnull ItemStack stack) {
            return TerpeneKnowledge.knows(TerpeneKnowledge.RESIN) ? super.getName(stack) : Component.translatable("item.fourtwenty.terpene.unknown.resin");
        }
    });

    /** All 84 terpenes by code. */
    public static final Map<String, DeferredHolder<Item, TerpeneItem>> TERPENES = terpenes();

    private static Map<String, DeferredHolder<Item, TerpeneItem>> terpenes() {
        Map<String, DeferredHolder<Item, TerpeneItem>> out = new LinkedHashMap<>();
        for (Terpenes.Code code : Terpenes.all()) {
            out.put(code.id(), ModItems.ITEMS.register("terpene_" + code.id(), () -> new TerpeneItem(code, new Item.Properties())));
        }
        return Collections.unmodifiableMap(out);
    }

    public static TerpeneItem terpene(Terpenes.Code code) { return TERPENES.get(code.id()).get(); }

    public static final DeferredHolder<Block, TerpinatorBlock> TERPINATOR = ModBlocks.BLOCKS.register("terpinator",
            () -> new TerpinatorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 6f).sound(SoundType.COPPER)
                    .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> state.getValue(TerpinatorBlock.LIT) ? 7 : 2)));

    public static final DeferredHolder<Item, BlockItem> TERPINATOR_ITEM = ModItems.ITEMS.register("terpinator",
            () -> new BlockItem(TERPINATOR.get(), new Item.Properties()));

    @SuppressWarnings("null")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerpinatorBlockEntity>> TERPINATOR_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("terpinator",
                    () -> BlockEntityType.Builder.of(TerpinatorBlockEntity::new, TERPINATOR.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<TerpinatorMenu>> TERPINATOR_MENU = ModMenuTypes.MENU_TYPES.register("terpinator",
            () -> IMenuTypeExtension.create((id, inventory, data) -> new TerpinatorMenu(id, inventory)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = ModCreativeTabs.CREATIVE_MODE_TABS.register("hemp_and_terpenes",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fourtwenty.hemp_and_terpenes"))
                    .icon(() -> new ItemStack(TERPINATOR_ITEM.get()))
                    .withTabsBefore(ModCreativeTabs.FOUR_TWENTY_TAB.getKey())
                    .displayItems((parameters, output) -> {
                        output.accept(TERPINATOR_ITEM.get());
                        output.accept(RESIN.get());
                        TERPENES.values().forEach(t -> output.accept(t.get()));
                        List<Hemp> all = new ArrayList<>();
                        all.add(NATURAL_HEMP);
                        all.addAll(HEMP.values());
                        all.forEach(h -> output.accept(h.items().get(0).get()));
                        all.forEach(h -> output.accept(h.items().get(1).get()));
                        all.forEach(h -> output.accept(h.items().get(2).get()));
                    })
                    .build());

    /** Loads this class, so everything above is registered before the registers fire. */
    public static void init() {}
}
