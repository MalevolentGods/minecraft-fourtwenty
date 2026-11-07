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

    static {
        ALL.add(ModItems.ITEMS.register("hemp_thread", () -> new BlockItem(HEMP_THREAD.get(), new Item.Properties())));
        for (ComponentType t : ComponentType.values()) {
            DeferredHolder<Block, ComponentBlock> block = ModBlocks.BLOCKS.register(t.id, () -> new ComponentBlock(t, component()));
            COMPONENTS.put(t, block);
            ALL.add(ModItems.ITEMS.register(t.id, () -> new BlockItem(block.get(), new Item.Properties())));
        }
    }

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, BlockEntityType<ComponentBlockEntity>> COMPONENT_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("component", () -> BlockEntityType.Builder.of(ComponentBlockEntity::new,
                    COMPONENTS.values().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));

    /** Loads the class, so its registrations happen; call it before the registers go on the bus. */
    public static void init() {}

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.malevolentgods.fourtwenty.power.Network::startTick);
    }

    /** Hemp Thread: Puff tier, but thin. */
    public static final class HempThreadBlock extends WickBlock {
        public HempThreadBlock(Properties properties) { super(BIRules.Tier.PUFF, properties); }

        @Override public double ohmsScale() { return 1000; }

        @Override public double ampsScale() { return 0.01; }
    }
}
