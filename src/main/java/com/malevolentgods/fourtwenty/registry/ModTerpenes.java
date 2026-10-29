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
 * The terpene system: hemp blocks (Natural Hemp, the 64 colours and the 16 Minecraft colours, each with
 * stairs and a slab), the 84 terpene items, the 16 Minecraft-colour terpenes and the resin, and the
 * Terpinator. Ids come from terpene codes (hemp_myr_pin_lim, terpene_myr_pin_lim, hemp_mc_white,
 * terpene_mc_white); names and colours from tools/terpenes.json.
 */
public final class ModTerpenes {
    private ModTerpenes() {}

    /** A hemp block with its stairs and slab, and their items. */
    public record Hemp(DeferredHolder<Block, Block> block, DeferredHolder<Block, StairBlock> stairs, DeferredHolder<Block, SlabBlock> slab,
                       List<DeferredHolder<Item, BlockItem>> items) {}

    public static final Hemp NATURAL_HEMP = hemp("natural_hemp");

    /** The Minecraft colours' codes, in DyeColor order: mc_white ... mc_black. */
    public static final List<String> MINECRAFT = java.util.Arrays.stream(net.minecraft.world.item.DyeColor.values())
            .map(d -> "mc_" + d.getSerializedName()).toList();

    /** The 64 colours by final terpene code, in codex order, then the 16 Minecraft colours. */
    public static final Map<String, Hemp> HEMP = coloured();

    private static Map<String, Hemp> coloured() {
        Map<String, Hemp> out = new LinkedHashMap<>();
        for (Terpenes.Code code : Terpenes.finals()) out.put(code.id(), hemp("hemp_" + code.id()));
        for (String code : MINECRAFT) out.put(code, hemp("hemp_" + code));
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

    /** Concrete in the 64 colours and the 16 Minecraft colours, by code (one flat colour, like vanilla's). */
    public static final Map<String, DeferredHolder<Block, Block>> CONCRETE = paletteAndMinecraft("concrete_",
            code -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.8f).requiresCorrectToolForDrops()));

    /**
     * Concrete powder: vanilla's ConcretePowderBlock, so it falls and sets into its concrete in water,
     * and MineColonies' Concrete Mixer (which looks for a ConcretePowderBlock) can set it in its pond.
     */
    public static final Map<String, DeferredHolder<Block, Block>> CONCRETE_POWDER = paletteAndMinecraft("concrete_powder_",
            code -> new net.minecraft.world.level.block.ConcretePowderBlock(CONCRETE.get(code).get(),
                    BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND)));

    /** Stained glass and glass panes in the 64 colours (translucent; drop only with silk touch). */
    public static final Map<String, DeferredHolder<Block, Block>> GLASS = palette("glass_",
            code -> new net.minecraft.world.level.block.TransparentBlock(glass()));
    public static final Map<String, DeferredHolder<Block, Block>> GLASS_PANES = palette("glass_pane_",
            code -> new net.minecraft.world.level.block.IronBarsBlock(glass()));

    private static BlockBehaviour.Properties glass() {
        return BlockBehaviour.Properties.of().strength(0.3f).sound(SoundType.GLASS).noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false);
    }

    /**
     * Rugs in the 64 colours, Natural Hemp ("natural") and the 16 Minecraft colours: one pixel thick, inset a pixel round the
     * outside with rounded corners, joining the same rug around them into one larger rug
     * ({@link com.malevolentgods.fourtwenty.block.RugBlock}); carpets in every other way.
     */
    public static final Map<String, DeferredHolder<Block, Block>> RUGS = rugs();

    private static Map<String, DeferredHolder<Block, Block>> rugs() {
        Map<String, DeferredHolder<Block, Block>> out = new LinkedHashMap<>(palette("rug_", code -> rug()));
        out.put("natural", register("rug_natural", ModTerpenes::rug));
        for (String code : MINECRAFT) out.put(code, register("rug_" + code, ModTerpenes::rug));
        return Collections.unmodifiableMap(out);
    }

    private static Block rug() {
        return new com.malevolentgods.fourtwenty.block.RugBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.1f)
                .sound(SoundType.WOOL).ignitedByLava().noOcclusion());
    }

    /**
     * Couches in the 64 colours and Natural Hemp ("natural"): hemp seats with a back, arm rests and
     * wooden legs that join each other like stairs ({@link com.malevolentgods.fourtwenty.block.CouchBlock}).
     */
    public static final Map<String, DeferredHolder<Block, Block>> COUCHES = couches();

    private static Map<String, DeferredHolder<Block, Block>> couches() {
        Map<String, DeferredHolder<Block, Block>> out = new LinkedHashMap<>(palette("couch_", code -> couch()));
        out.put("natural", register("couch_natural", ModTerpenes::couch));
        return Collections.unmodifiableMap(out);
    }

    private static Block couch() {
        return new com.malevolentgods.fourtwenty.block.CouchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8f)
                .sound(SoundType.WOOL).ignitedByLava().noOcclusion());
    }

    /**
     * Pot Pots in the 64 colours: terpene-concrete plant pots holding soil, a design of their own per
     * colour ({@link com.malevolentgods.fourtwenty.block.PotPotBlock}); with a weed seed planted, Potted Pot
     * Pots, whose plant grows and is harvested like the crop ({@link com.malevolentgods.fourtwenty.block.PottedPotPotBlock}).
     */
    public static final Map<String, DeferredHolder<Block, Block>> POTTED_POT_POTS = palette("potted_pot_pot_",
            code -> new com.malevolentgods.fourtwenty.block.PottedPotPotBlock(potProperties()));
    public static final Map<String, DeferredHolder<Block, Block>> POT_POTS = palette("pot_pot_",
            code -> new com.malevolentgods.fourtwenty.block.PotPotBlock(() -> POTTED_POT_POTS.get(code).get(), potProperties()));

    private static BlockBehaviour.Properties potProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.5f).sound(SoundType.STONE).noOcclusion();
    }

    /**
     * Roads, in three tiers ({@link com.malevolentgods.fourtwenty.block.HempPathBlock}): a cobble path,
     * a paved road and a yellow brick road, 5, 10 and 15% faster to walk on, joining into whole roads
     * with a border round the outside.
     */
    public static final java.util.List<DeferredHolder<Block, Block>> PATHS = java.util.List.of(
            register("cobble_path", () -> path(1)), register("paved_road", () -> path(2)), register("yellow_brick_road", () -> path(3)));

    /** Road slabs and stairs, sized to the roads (HempPathSlabBlock, HempPathStairBlock). */
    public static final java.util.List<DeferredHolder<Block, Block>> PATH_SLABS = java.util.List.of(
            register("cobble_path_slab", () -> new com.malevolentgods.fourtwenty.block.HempPathSlabBlock(1, roadProperties(1))),
            register("paved_road_slab", () -> new com.malevolentgods.fourtwenty.block.HempPathSlabBlock(2, roadProperties(2))),
            register("yellow_brick_road_slab", () -> new com.malevolentgods.fourtwenty.block.HempPathSlabBlock(3, roadProperties(3))));
    public static final java.util.List<DeferredHolder<Block, Block>> PATH_STAIRS = java.util.List.of(
            register("cobble_path_stairs", () -> new com.malevolentgods.fourtwenty.block.HempPathStairBlock(1, PATHS.get(0).get().defaultBlockState(), roadProperties(1))),
            register("paved_road_stairs", () -> new com.malevolentgods.fourtwenty.block.HempPathStairBlock(2, PATHS.get(1).get().defaultBlockState(), roadProperties(2))),
            register("yellow_brick_road_stairs", () -> new com.malevolentgods.fourtwenty.block.HempPathStairBlock(3, PATHS.get(2).get().defaultBlockState(), roadProperties(3))));

    private static Block path(int tier) {
        return new com.malevolentgods.fourtwenty.block.HempPathBlock(tier, roadProperties(tier));
    }

    /** The light a road gives off by tier, without a visible glow: Cobble Path dim (under a redstone torch's 7), Paved Road between, Yellow Brick Road a glowstone's 15. */
    public static final int[] ROAD_LIGHT = {0, 6, 10, 15};

    private static BlockBehaviour.Properties roadProperties(int tier) {
        return BlockBehaviour.Properties.of().mapColor(tier == 3 ? MapColor.GOLD : MapColor.STONE)
                .strength(1.2f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(state -> ROAD_LIGHT[tier]);
    }

    /** Lamps, inverted lamps and lamp trims in the 64 colours (halos in their own colour). */
    public static final Map<String, DeferredHolder<Block, Block>> LAMPS = palette("lamp_",
            code -> new com.malevolentgods.fourtwenty.block.LampBlock(rgb(code), false, ModBlocks.lampProperties()));
    public static final Map<String, DeferredHolder<Block, Block>> INVERTED_LAMPS = palette("inverted_lamp_",
            code -> new com.malevolentgods.fourtwenty.block.LampBlock(rgb(code), true, ModBlocks.lampProperties()));
    public static final Map<String, DeferredHolder<Block, Block>> LAMP_TRIMS = palette("lamp_trim_",
            code -> new com.malevolentgods.fourtwenty.block.LampTrimBlock(rgb(code), ModBlocks.lampProperties().noOcclusion()));

    private static int rgb(String code) { return com.malevolentgods.fourtwenty.domain.TerpenePalette.color(Terpenes.Code.parse(code).orElseThrow()); }

    /** A block (and its item) per palette colour: prefix + final terpene code. */
    private static Map<String, DeferredHolder<Block, Block>> palette(String prefix, java.util.function.Function<String, Block> make) {
        Map<String, DeferredHolder<Block, Block>> out = new LinkedHashMap<>();
        for (Terpenes.Code code : Terpenes.finals()) out.put(code.id(), register(prefix + code.id(), () -> make.apply(code.id())));
        return Collections.unmodifiableMap(out);
    }

    /** {@link #palette} and then the 16 Minecraft colours (mc_white ...). */
    private static Map<String, DeferredHolder<Block, Block>> paletteAndMinecraft(String prefix, java.util.function.Function<String, Block> make) {
        Map<String, DeferredHolder<Block, Block>> out = new LinkedHashMap<>(palette(prefix, make));
        for (String code : MINECRAFT) out.put(code, register(prefix + code, () -> make.apply(code)));
        return Collections.unmodifiableMap(out);
    }

    private static DeferredHolder<Block, Block> register(String id, java.util.function.Supplier<Block> block) {
        DeferredHolder<Block, Block> holder = ModBlocks.BLOCKS.register(id, block);
        ModItems.ITEMS.register(id, () -> new BlockItem(holder.get(), new Item.Properties()));
        return holder;
    }

    /** Every palette lamp, inverted lamp and trim (for the halo block entity and item renderer). */
    public static java.util.stream.Stream<Block> paletteLamps() {
        return java.util.stream.Stream.of(LAMPS, INVERTED_LAMPS, LAMP_TRIMS).flatMap(m -> m.values().stream()).map(DeferredHolder::get);
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

    /** The 16 Minecraft-colour terpenes by code (mc_white ...), in DyeColor order. */
    public static final Map<String, DeferredHolder<Item, com.malevolentgods.fourtwenty.terpene.MinecraftTerpeneItem>> MINECRAFT_TERPENES = minecraftTerpenes();

    private static Map<String, DeferredHolder<Item, com.malevolentgods.fourtwenty.terpene.MinecraftTerpeneItem>> minecraftTerpenes() {
        Map<String, DeferredHolder<Item, com.malevolentgods.fourtwenty.terpene.MinecraftTerpeneItem>> out = new LinkedHashMap<>();
        for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
            String code = "mc_" + dye.getSerializedName();
            out.put(code, ModItems.ITEMS.register("terpene_" + code,
                    () -> new com.malevolentgods.fourtwenty.terpene.MinecraftTerpeneItem(dye, new Item.Properties())));
        }
        return Collections.unmodifiableMap(out);
    }

    /** The Terpinator in two finishes, Steel and Lab: the same machine (one block entity type, one menu). */
    public static final DeferredHolder<Block, TerpinatorBlock> TERPINATOR = ModBlocks.BLOCKS.register("terpinator", ModTerpenes::terpinator);
    public static final DeferredHolder<Block, TerpinatorBlock> TERPINATOR_LAB = ModBlocks.BLOCKS.register("terpinator_lab", ModTerpenes::terpinator);

    private static TerpinatorBlock terpinator() {
        return new TerpinatorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 6f).sound(SoundType.METAL)
                .noOcclusion().requiresCorrectToolForDrops().lightLevel(state -> state.getValue(TerpinatorBlock.LIT) ? 9 : 4));
    }

    public static final DeferredHolder<Item, BlockItem> TERPINATOR_ITEM = ModItems.ITEMS.register("terpinator",
            () -> new BlockItem(TERPINATOR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> TERPINATOR_LAB_ITEM = ModItems.ITEMS.register("terpinator_lab",
            () -> new BlockItem(TERPINATOR_LAB.get(), new Item.Properties()));

    @SuppressWarnings("null")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerpinatorBlockEntity>> TERPINATOR_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("terpinator",
                    () -> BlockEntityType.Builder.of(TerpinatorBlockEntity::new, TERPINATOR.get(), TERPINATOR_LAB.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<TerpinatorMenu>> TERPINATOR_MENU = ModMenuTypes.MENU_TYPES.register("terpinator",
            () -> IMenuTypeExtension.create((id, inventory, data) -> new TerpinatorMenu(id, inventory)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = ModCreativeTabs.CREATIVE_MODE_TABS.register("hemp_and_terpenes",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fourtwenty.hemp_and_terpenes"))
                    .icon(() -> new ItemStack(TERPINATOR_ITEM.get()))
                    .withTabsBefore(ModCreativeTabs.FOUR_TWENTY_TAB.getKey())
                    .displayItems((parameters, output) -> {
                        output.accept(TERPINATOR_ITEM.get());
                        output.accept(TERPINATOR_LAB_ITEM.get());
                        // Resin, bases and accords stay registered for old worlds; the Terpinator makes only finals now.
                        TERPENES.values().stream().filter(t -> t.get().code().isFinal()).forEach(t -> output.accept(t.get()));
                        MINECRAFT_TERPENES.values().forEach(t -> output.accept(t.get()));
                        List<Hemp> all = new ArrayList<>();
                        all.add(NATURAL_HEMP);
                        all.addAll(HEMP.values());
                        all.forEach(h -> output.accept(h.items().get(0).get()));
                        all.forEach(h -> output.accept(h.items().get(1).get()));
                        all.forEach(h -> output.accept(h.items().get(2).get()));
                        RUGS.values().forEach(b -> output.accept(b.get()));
                        COUCHES.values().forEach(b -> output.accept(b.get()));
                        POT_POTS.values().forEach(b -> output.accept(b.get()));
                        POTTED_POT_POTS.values().forEach(b -> output.accept(b.get()));
                        for (int t = 0; t < 3; t++) {
                            output.accept(PATHS.get(t).get());
                            output.accept(PATH_SLABS.get(t).get());
                            output.accept(PATH_STAIRS.get(t).get());
                        }
                        CONCRETE.values().forEach(b -> output.accept(b.get()));
                        CONCRETE_POWDER.values().forEach(b -> output.accept(b.get()));
                        GLASS.values().forEach(b -> output.accept(b.get()));
                        GLASS_PANES.values().forEach(b -> output.accept(b.get()));
                        LAMPS.values().forEach(b -> output.accept(b.get()));
                        INVERTED_LAMPS.values().forEach(b -> output.accept(b.get()));
                        LAMP_TRIMS.values().forEach(b -> output.accept(b.get()));
                    })
                    .build());

    /** Loads this class, so everything above is registered before the registers fire. */
    public static void init() {}
}
