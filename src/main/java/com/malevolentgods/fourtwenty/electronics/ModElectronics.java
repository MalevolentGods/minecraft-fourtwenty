package com.malevolentgods.fourtwenty.electronics;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.power.WickBlock;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The electronics: Hemp Thread and the component blocks (docs/electronics/audit-and-plan.md). */
public final class ModElectronics {
    private ModElectronics() {}

    private static BlockBehaviour.Properties component() {
        return BlockBehaviour.Properties.of().strength(0.6f).sound(SoundType.METAL).noOcclusion()
                .lightLevel(state -> state.getValue(ComponentBlock.ON) ? 3 : 0);
    }

    public static final List<DeferredHolder<Item, ? extends Item>> ALL = new ArrayList<>();
    public static final Map<ComponentType, DeferredHolder<Block, ComponentBlock>> COMPONENTS = new EnumMap<>(ComponentType.class);

    /** A thin single conductor for signals: a Wick of a hundredth the rating and a thousand times the resistance. */
    public static final DeferredHolder<Block, HempThreadBlock> HEMP_THREAD = ModBlocks.BLOCKS.register("hemp_thread",
            () -> new HempThreadBlock(BlockBehaviour.Properties.of().strength(0.1f).sound(SoundType.WOOL).noOcclusion()
                    .lightLevel(state -> state.getValue(WickBlock.LIT) ? 2 : 0)));

    /** The Twister: a wrench that turns components to any of their 24 orientations. */
    public static final DeferredHolder<Item, TwisterItem> TWISTER = ModItems.ITEMS.register("twister", () -> new TwisterItem(new Item.Properties()));

    /** The Eighth Ribbon: nine signal wires in one flat cable. */
    public static final DeferredHolder<Block, RibbonBlock> EIGHTH_RIBBON = ModBlocks.BLOCKS.register("eighth_ribbon",
            () -> new RibbonBlock(BlockBehaviour.Properties.of().strength(0.1f).sound(SoundType.WOOL).noOcclusion()));

    /** The Roach Clip: one channel of a Ribbon out to a single pin. */
    public static final DeferredHolder<Block, RoachClipBlock> ROACH_CLIP = ModBlocks.BLOCKS.register("roach_clip",
            () -> new RoachClipBlock(BlockBehaviour.Properties.of().strength(0.3f).sound(SoundType.METAL).noOcclusion()));

    /** The Rolling Tray: a panel board that buttons, toggles, Numb Nugs and lamps mount on, side by side, each wired on its own. */
    public static final DeferredHolder<Block, RollingTrayBlock> ROLLING_TRAY = ModBlocks.BLOCKS.register("rolling_tray",
            () -> new RollingTrayBlock(BlockBehaviour.Properties.of().strength(0.8f).sound(SoundType.METAL)));

    static {
        ALL.add(ModItems.ITEMS.register("rolling_tray", () -> new BlockItem(ROLLING_TRAY.get(), new Item.Properties())));
        ALL.add(ModItems.ITEMS.register("hemp_thread", () -> new BlockItem(HEMP_THREAD.get(), new Item.Properties())));
        ALL.add(ModItems.ITEMS.register("eighth_ribbon", () -> new BlockItem(EIGHTH_RIBBON.get(), new Item.Properties())));
        ALL.add(ModItems.ITEMS.register("roach_clip", () -> new BlockItem(ROACH_CLIP.get(), new Item.Properties())));
        ALL.add(TWISTER);
        for (ComponentType t : ComponentType.values()) {
            DeferredHolder<Block, ComponentBlock> block = ModBlocks.BLOCKS.register(t.id,
                    () -> t == ComponentType.RESINATOR ? new ResinatorBlock(t, component()) : new ComponentBlock(t, component()));
            COMPONENTS.put(t, block);
            ALL.add(ModItems.ITEMS.register(t.id, () -> new BlockItem(block.get(), new Item.Properties())));
        }
    }

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, BlockEntityType<ComponentBlockEntity>> COMPONENT_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("component", () -> BlockEntityType.Builder.of(ComponentBlockEntity::new,
                    COMPONENTS.values().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, BlockEntityType<RibbonBlockEntity>> RIBBON_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("ribbon", () -> BlockEntityType.Builder.of(RibbonBlockEntity::new, EIGHTH_RIBBON.get(), ROACH_CLIP.get()).build(null));

    /** Loads the class, so its registrations happen; call it before the registers go on the bus. */
    public static void init() {}

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        KeyLabels.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.power.Network::startTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.power.InspoweedometerItem::rightClickBlock);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.power.InspoweedometerItem::playerTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.power.BIGrid::chunkLoaded);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TwisterItem::rightClickBlock);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.power.BIGrid::levelTick);
    }

    /** Hemp Thread: Puff tier, but thin. */
    public static final class HempThreadBlock extends WickBlock {
        public HempThreadBlock(Properties properties) { super(BIRules.Tier.PUFF, properties); }

        @Override public double ohmsScale() { return 1000; }

        @Override public double ampsScale() { return 0.01; }
    }
}
