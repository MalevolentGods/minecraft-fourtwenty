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

    public static final List<DeferredHolder<Item, ? extends Item>> ALL = List.of(HOTBOX_HELM, PUFF_PLATE, COUCHLOCK_GREAVES, MOON_BOOTS, BLAZER, DAB_CELL);

    public static void register(IEventBus bus) {
        MATERIALS.register(bus);
        GearPowers.register();
    }
}
