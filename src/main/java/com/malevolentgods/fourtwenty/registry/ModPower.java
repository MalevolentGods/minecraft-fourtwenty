package com.malevolentgods.fourtwenty.registry;

import com.malevolentgods.fourtwenty.domain.BIRules.Tier;
import com.malevolentgods.fourtwenty.power.BINode;
import com.malevolentgods.fourtwenty.power.GrowOpBlock;
import com.malevolentgods.fourtwenty.power.GrowOpBlockEntity;
import com.malevolentgods.fourtwenty.power.HitBoxBlock;
import com.malevolentgods.fourtwenty.power.HitBoxBlockEntity;
import com.malevolentgods.fourtwenty.power.StashBlock;
import com.malevolentgods.fourtwenty.power.StashBlockEntity;
import com.malevolentgods.fourtwenty.power.WickBlock;
import com.malevolentgods.fourtwenty.processing.MachineBlock;
import com.malevolentgods.fourtwenty.processing.MachineBlock.Job;
import com.malevolentgods.fourtwenty.processing.MachineBlockEntity;
import com.malevolentgods.fourtwenty.processing.MachineMenu;
import com.malevolentgods.fourtwenty.processing.ThinkTankBlock;
import com.malevolentgods.fourtwenty.processing.ThinkTankBlockEntity;
import com.malevolentgods.fourtwenty.processing.ThinkTankMenu;
import com.malevolentgods.fourtwenty.block.WeedBongBlock;
import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Boundless Inspiration and the powered machines. */
public final class ModPower {
    private ModPower() {}

    /** Blocks a Wick joins to. */
    public static final TagKey<Block> BI_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("fourtwenty", "bi_blocks"));

    /** For the creative tab. */
    public static final List<DeferredHolder<Item, ? extends Item>> ITEMS = new ArrayList<>();

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion().lightLevel(state -> state.getValue(com.malevolentgods.fourtwenty.power.BIBlock.LIT) ? 7 : 0);
    }

    private static BlockBehaviour.Properties wick() {
        return BlockBehaviour.Properties.of().strength(0.3f).sound(SoundType.WOOL).noOcclusion().lightLevel(state -> state.getValue(WickBlock.LIT) ? 4 : 0);
    }

    private static <B extends Block> DeferredHolder<Block, B> block(String name, Supplier<B> make) {
        DeferredHolder<Block, B> holder = ModBlocks.BLOCKS.register(name, make);
        ITEMS.add(ModItems.ITEMS.register(name, () -> new BlockItem(holder.get(), new Item.Properties())));
        return holder;
    }

    /** The Hookah upgraded: the same block at a higher tier. The Hookah itself, tier 1, is ModBlocks.WEED_BONG. */
    private static DeferredHolder<Block, WeedBongBlock> hookah(String name, Tier tier) {
        return block(name, () -> new WeedBongBlock(tier, BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(SoundType.GLASS).noOcclusion()));
    }

    private static DeferredHolder<Block, StashBlock> stash(String name, Tier tier, long capacity, double inset, double height) {
        return block(name, () -> new StashBlock(tier, capacity, Block.box(inset, 0, inset, 16 - inset, height, 16 - inset), metal()));
    }

    private static DeferredHolder<Block, WickBlock> wick(String name, Tier tier) {
        return block(name, () -> new WickBlock(tier, wick()));
    }

    private static DeferredHolder<Block, MachineBlock> machine(String name, Job job, Tier tier) {
        return block(name, () -> new MachineBlock(job, tier, metal()));
    }

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "fourtwenty");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("fourtwenty", name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> SNIP = sound("snip"), BURN_OUT = sound("burn_out"),
            OVERLOAD = sound("overload"), SWITCH = sound("switch");

    public static final DeferredHolder<Item, Item> ASH = ModItems.ITEMS.register("ash", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Block, WickBlock> HEMP_WICK = wick("hemp_wick", Tier.PUFF);
    public static final DeferredHolder<Block, WickBlock> ROLLED_WICK = wick("rolled_wick", Tier.TOKE);
    public static final DeferredHolder<Block, WickBlock> GLASS_WICK = wick("glass_wick", Tier.RIP);
    public static final DeferredHolder<Block, WickBlock> GOLD_WICK = wick("gold_wick", Tier.BLAZE);

    public static final DeferredHolder<Block, WeedBongBlock> DOUBLE_HOSE = hookah("double_hose", Tier.TOKE);
    public static final DeferredHolder<Block, WeedBongBlock> BIG_BERTHA = hookah("big_bertha", Tier.RIP);
    public static final DeferredHolder<Block, WeedBongBlock> GRAND_HOOKAH = hookah("grand_hookah", Tier.BLAZE);

    public static final DeferredHolder<Block, StashBlock> STASH_JAR = stash("stash_jar", Tier.PUFF, 40_000, 4, 12);
    public static final DeferredHolder<Block, StashBlock> STASH_BOX = stash("stash_box", Tier.TOKE, 400_000, 1, 14);
    public static final DeferredHolder<Block, StashBlock> MOTHER_STASH = stash("mother_stash", Tier.RIP, 4_000_000, 0, 16);
    public static final DeferredHolder<Block, StashBlock> THE_VAULT = stash("the_vault", Tier.BLAZE, 40_000_000, 0, 16);

    public static final DeferredHolder<Block, HitBoxBlock> HIT_BOX = block("hit_box", () -> new HitBoxBlock(metal()));

    public static final DeferredHolder<Block, MachineBlock> SNIP_STATION = machine("snip_station", Job.TRIM, Tier.PUFF);
    public static final DeferredHolder<Block, MachineBlock> BUD_BARBER = machine("bud_barber", Job.TRIM, Tier.TOKE);
    public static final DeferredHolder<Block, MachineBlock> TRIM_REAPER = machine("trim_reaper", Job.TRIM, Tier.RIP);
    public static final DeferredHolder<Block, MachineBlock> DRY_BOX = machine("dry_box", Job.CURE, Tier.PUFF);
    public static final DeferredHolder<Block, MachineBlock> CURE_CLOSET = machine("cure_closet", Job.CURE, Tier.TOKE);
    public static final DeferredHolder<Block, MachineBlock> THE_HUMIDOR = machine("the_humidor", Job.CURE, Tier.RIP);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StashBlockEntity>> STASH_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register("stash",
            () -> BlockEntityType.Builder.of(StashBlockEntity::new, STASH_JAR.get(), STASH_BOX.get(), MOTHER_STASH.get(), THE_VAULT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HitBoxBlockEntity>> HIT_BOX_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register("hit_box",
            () -> BlockEntityType.Builder.of(HitBoxBlockEntity::new, HIT_BOX.get()).build(null));
    private static BlockBehaviour.Properties plot(int light) {
        return BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.WOOD).noOcclusion().lightLevel(state -> light);
    }

    public static final DeferredHolder<Block, GrowOpBlock> COSMIC_KUSH_POD = block("cosmic_kush_pod", () -> new GrowOpBlock(5, Tier.BLAZE, null, plot(15)));
    public static final DeferredHolder<Block, GrowOpBlock> AERO_POD = block("aero_pod", () -> new GrowOpBlock(4, Tier.RIP, COSMIC_KUSH_POD, plot(15)));
    public static final DeferredHolder<Block, GrowOpBlock> HYDRO_BED = block("hydro_bed", () -> new GrowOpBlock(3, Tier.TOKE, AERO_POD, plot(15)));
    public static final DeferredHolder<Block, GrowOpBlock> GROW_TENT = block("grow_tent", () -> new GrowOpBlock(2, Tier.PUFF, HYDRO_BED, plot(15)));
    public static final DeferredHolder<Block, GrowOpBlock> CLOSET_GROW = block("closet_grow", () -> new GrowOpBlock(1, null, GROW_TENT, plot(0)));
    public static final DeferredHolder<Block, ThinkTankBlock> THINK_TANK = block("think_tank", () -> new ThinkTankBlock(metal()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrowOpBlockEntity>> GROW_OP_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register("grow_op",
            () -> BlockEntityType.Builder.of(GrowOpBlockEntity::new, CLOSET_GROW.get(), GROW_TENT.get(), HYDRO_BED.get(), AERO_POD.get(), COSMIC_KUSH_POD.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThinkTankBlockEntity>> THINK_TANK_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register("think_tank",
            () -> BlockEntityType.Builder.of(ThinkTankBlockEntity::new, THINK_TANK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> MACHINE_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register("machine",
            () -> BlockEntityType.Builder.of(MachineBlockEntity::new, SNIP_STATION.get(), BUD_BARBER.get(), TRIM_REAPER.get(), DRY_BOX.get(), CURE_CLOSET.get(), THE_HUMIDOR.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<com.malevolentgods.fourtwenty.power.BIMenu>> BI_MENU = ModMenuTypes.MENU_TYPES.register("bi",
            () -> IMenuTypeExtension.create((id, inventory, data) -> new com.malevolentgods.fourtwenty.power.BIMenu(id)));

    public static final DeferredHolder<MenuType<?>, MenuType<ThinkTankMenu>> THINK_TANK_MENU = ModMenuTypes.MENU_TYPES.register("think_tank",
            () -> IMenuTypeExtension.create((id, inventory, data) -> new ThinkTankMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE_MENU = ModMenuTypes.MENU_TYPES.register("machine",
            () -> IMenuTypeExtension.create((id, inventory, data) -> new MachineMenu(id, inventory)));

    /** Loads this class, so its registrations happen; call it before the registers go on the bus. */
    public static void init() {}

    public static void register(IEventBus bus) {
        bus.addListener(ModPower::capabilities);
        com.malevolentgods.fourtwenty.power.GrowOps.register();
        SOUNDS.register(bus);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(BINode.CAPABILITY, ModBlockEntities.WEED_BONG.get(), (WeedBongBlockEntity entity, Direction side) -> entity.node());
        event.registerBlockEntity(BINode.CAPABILITY, STASH_ENTITY.get(), (entity, side) -> entity.node());
        event.registerBlockEntity(BINode.CAPABILITY, HIT_BOX_ENTITY.get(), HitBoxBlockEntity::node);
        event.registerBlockEntity(BINode.CAPABILITY, MACHINE_ENTITY.get(), (entity, side) -> entity.node());
        event.registerBlockEntity(BINode.CAPABILITY, GROW_OP_ENTITY.get(), (entity, side) -> entity.node());
        event.registerBlockEntity(BINode.CAPABILITY, THINK_TANK_ENTITY.get(), (entity, side) -> entity.node());
        event.registerBlockEntity(BINode.CAPABILITY, ModTerpenes.TERPINATOR_ENTITY.get(), (TerpinatorBlockEntity entity, Direction side) -> entity.node());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE_ENTITY.get(), (entity, side) -> side == null ? null : new SidedInvWrapper(entity, side));
    }
}
