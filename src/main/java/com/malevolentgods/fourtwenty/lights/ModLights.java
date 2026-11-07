package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.Programs;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/** Lit Bits, the Sticky Finger, the Weedweeno and Bit Chits (specs/008-lit-bits): their blocks, items, block entities and components. */
public final class ModLights {
    private ModLights() {}

    public static final Codec<Programs.Program> PROGRAM_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Programs.Program::name),
            Codec.STRING.fieldOf("code").forGetter(Programs.Program::code)).apply(i, Programs.Program::new));
    public static final StreamCodec<ByteBuf, Programs.Program> PROGRAM_STREAM = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), Programs.Program::name, ByteBufCodecs.stringUtf8(Programs.MAX_CODE), Programs.Program::code, Programs.Program::new);

    /** A Bit Chit's program; a blank one has none. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Programs.Program>> PROGRAM = ModDataComponents.COMPONENTS.register("program",
            () -> DataComponentType.<Programs.Program>builder().persistent(PROGRAM_CODEC).networkSynchronized(PROGRAM_STREAM).build());
    /** A broken Weedweeno's programs, carried on its item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Programs.Program>>> PROGRAMS = ModDataComponents.COMPONENTS.register("weedweeno_programs",
            () -> DataComponentType.<List<Programs.Program>>builder().persistent(PROGRAM_CODEC.listOf()).build());
    /** The Sticky Finger's next ID. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NEXT_ID = ModDataComponents.COMPONENTS.register("next_id",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private static BlockBehaviour.Properties lamp() {
        return BlockBehaviour.Properties.of().strength(0.5f).sound(SoundType.GLASS).noOcclusion();
    }

    public static final DeferredHolder<net.minecraft.world.level.block.Block, LitBitBlock> LIT_BIT = ModBlocks.BLOCKS.register("lit_bit", () -> new LitBitBlock(lamp()));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, WeedweenoBlock> WEEDWEENO = ModBlocks.BLOCKS.register("weedweeno",
            () -> new WeedweenoBlock(BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> LIT_BIT_ITEM = ModItems.ITEMS.register("lit_bit", () -> new BlockItem(LIT_BIT.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WEEDWEENO_ITEM = ModItems.ITEMS.register("weedweeno", () -> new BlockItem(WEEDWEENO.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, StickyFingerItem> STICKY_FINGER = ModItems.ITEMS.register("sticky_finger", () -> new StickyFingerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, BitChitItem> BIT_CHIT = ModItems.ITEMS.register("bit_chit", () -> new BitChitItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LitBitBlockEntity>> LIT_BIT_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register("lit_bit",
            () -> BlockEntityType.Builder.of(LitBitBlockEntity::new, LIT_BIT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeedweenoBlockEntity>> WEEDWEENO_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register("weedweeno",
            () -> BlockEntityType.Builder.of(WeedweenoBlockEntity::new, WEEDWEENO.get()).build(null));

    public static final List<DeferredHolder<Item, ? extends Item>> ALL = List.of(LIT_BIT_ITEM, STICKY_FINGER, WEEDWEENO_ITEM, BIT_CHIT);

    /** Loads the class, so its registrations happen; call it before the registers go on the bus. */
    public static void init() {
        BitChitCopyRecipe.init();
    }

    public static void register(IEventBus bus) {
        LightsNetwork.register(bus);
        bus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(com.malevolentgods.fourtwenty.power.BINode.CAPABILITY, LIT_BIT_ENTITY.get(),
                    (entity, side) -> com.malevolentgods.fourtwenty.power.Terminal.side(entity, entity.node(), side));
            event.registerBlockEntity(com.malevolentgods.fourtwenty.power.BINode.CAPABILITY, WEEDWEENO_ENTITY.get(),
                    (entity, side) -> com.malevolentgods.fourtwenty.power.Terminal.side(entity, entity.node(), side));
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(LampSync::flush);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(WeedweenoBlockEntity::startTick);
    }
}
