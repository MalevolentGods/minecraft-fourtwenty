package com.malevolentgods.fourtwenty.tiedye;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Tie-dye's blocks and items (specs/018-tie-dye-shack). Core, so a dyed thing loads and keeps its look with or without MineColonies
 * (FR-020). The Tie Dye Shack that makes them colony-side lives under compat/minecolonies/tiedye.
 */
public final class ModTieDye {
    private ModTieDye() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("fourtwenty");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("fourtwenty");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "fourtwenty");

    public static final DeferredHolder<Block, TieDyeBlocks.Hemp> HEMP = BLOCKS.register("tie_dyed_hemp", () -> new TieDyeBlocks.Hemp(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8f).sound(SoundType.WOOL)));
    public static final DeferredHolder<Item, TieDyeItem> HEMP_ITEM = ITEMS.register("tie_dyed_hemp", () -> new TieDyeItem(HEMP.get(), new Item.Properties()));

    /** Every tie-dyed block, for the block entity type and the colour handlers. */
    public static List<Block> blocks() {
        List<Block> out = new ArrayList<>();
        out.add(HEMP.get());
        return out;
    }

    /** Every tie-dyed item, for the colour handlers and item properties. */
    public static List<Item> items() {
        List<Item> out = new ArrayList<>();
        out.add(HEMP_ITEM.get());
        return out;
    }

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TieDyeBlockEntity>> ENTITY = ENTITIES.register("tie_dye",
            () -> BlockEntityType.Builder.of(TieDyeBlockEntity::new, blocks().toArray(Block[]::new)).build(null));

    /** A stack of a tie-dyed item in a look. */
    public static ItemStack stack(Item item, TieDye look, int count) {
        ItemStack s = new ItemStack(item, count);
        s.set(com.malevolentgods.fourtwenty.registry.ModDataComponents.TIE_DYE.get(), look);
        return s;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() != com.malevolentgods.fourtwenty.registry.ModCreativeTabs.FOUR_TWENTY_TAB.getKey()) return;
            for (int p = 0; p < com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules.PATTERNS.size(); p++) {
                event.accept(stack(HEMP_ITEM.get(), new TieDye(p, Colourways.list().get(p % Colourways.list().size()).id()), 1));
            }
        });
    }
}
