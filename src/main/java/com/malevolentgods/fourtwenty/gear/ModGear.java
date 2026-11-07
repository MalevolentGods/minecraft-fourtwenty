package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/** The Kush Suit, the Blazer and the Dab Cell. */
public final class ModGear {
    private ModGear() {}

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "fourtwenty");

    /** Diamond's protection, with the Puff Plate the heaviest piece; it never wears out and always protects. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> KUSH = MATERIALS.register("kush", () -> {
        var defense = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.BODY, 8);
        return new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("fourtwenty", "kush"))), 2.0f, 0.0f);
    });

    public static final DeferredHolder<Item, KushArmorItem> HOTBOX_HELM = ModItems.ITEMS.register("hotbox_helm",
            () -> new KushArmorItem(Piece.HOTBOX_HELM, KUSH, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final DeferredHolder<Item, KushArmorItem> PUFF_PLATE = ModItems.ITEMS.register("puff_plate",
            () -> new KushArmorItem(Piece.PUFF_PLATE, KUSH, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final DeferredHolder<Item, KushArmorItem> COUCHLOCK_GREAVES = ModItems.ITEMS.register("couchlock_greaves",
            () -> new KushArmorItem(Piece.COUCHLOCK_GREAVES, KUSH, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final DeferredHolder<Item, KushArmorItem> MOON_BOOTS = ModItems.ITEMS.register("moon_boots",
            () -> new KushArmorItem(Piece.MOON_BOOTS, KUSH, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final DeferredHolder<Item, BlazerItem> BLAZER = ModItems.ITEMS.register("blazer", () -> new BlazerItem(new Item.Properties()));
    public static final DeferredHolder<Item, DabCellItem> DAB_CELL = ModItems.ITEMS.register("dab_cell", () -> new DabCellItem(new Item.Properties()));

    /** Filled by station(), so declared before the stations. */
    private static final List<DeferredHolder<Item, ? extends Item>> STATION_ITEMS = new java.util.ArrayList<>();
    /** Re-Up Stations, one per tier: Puff, Toke, Rip, Blaze. */
    public static final List<DeferredHolder<net.minecraft.world.level.block.Block, ReUpStationBlock>> RE_UP_STATIONS = java.util.Arrays.stream(
            com.malevolentgods.fourtwenty.domain.BIRules.Tier.values()).map(ModGear::station).toList();
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<ReUpStationBlockEntity>> RE_UP_ENTITY =
            com.malevolentgods.fourtwenty.registry.ModBlockEntities.BLOCK_ENTITIES.register("re_up_station", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    ReUpStationBlockEntity::new, RE_UP_STATIONS.stream().map(DeferredHolder::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    private static DeferredHolder<net.minecraft.world.level.block.Block, ReUpStationBlock> station(com.malevolentgods.fourtwenty.domain.BIRules.Tier tier) {
        String name = "re_up_station_" + tier.getSerializedName();
        var block = com.malevolentgods.fourtwenty.registry.ModBlocks.BLOCKS.register(name, () -> new ReUpStationBlock(tier,
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(net.minecraft.world.level.block.SoundType.METAL)
                        .noOcclusion().lightLevel(state -> state.getValue(com.malevolentgods.fourtwenty.power.BIBlock.LIT) ? 7 : 0)));
        STATION_ITEMS.add(ModItems.ITEMS.register(name, () -> new net.minecraft.world.item.BlockItem(block.get(), new Item.Properties())));
        return block;
    }

    public static final List<DeferredHolder<Item, ? extends Item>> ALL = java.util.stream.Stream.concat(
            java.util.stream.Stream.of(HOTBOX_HELM, PUFF_PLATE, COUCHLOCK_GREAVES, MOON_BOOTS, BLAZER, DAB_CELL), STATION_ITEMS.stream()).toList();

    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<GearMenu>> GEAR_MENU =
            com.malevolentgods.fourtwenty.registry.ModMenuTypes.MENU_TYPES.register("gear", GearMenu::type);

    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<ReUpMenu>> RE_UP_MENU =
            com.malevolentgods.fourtwenty.registry.ModMenuTypes.MENU_TYPES.register("re_up_station",
                    () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inventory, data) -> new ReUpMenu(id, inventory)));

    public static void register(IEventBus bus) {
        GearNetwork.register(bus);
        MATERIALS.register(bus);
        GearPowers.register();
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
                event.registerBlockEntity(com.malevolentgods.fourtwenty.power.BINode.CAPABILITY, RE_UP_ENTITY.get(), (entity, side) -> com.malevolentgods.fourtwenty.power.Terminal.side(entity, entity.node(), side)));
    }
}
