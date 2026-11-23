package com.malevolentgods.fourtwenty.handbook;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;

import javax.annotation.Nonnull;

/**
 * The Stoner Badge Sash (specs/016-weedcraft-handbook FR-035 to FR-040): a cloth sash worn across the chest that shows the wearer's
 * earned badges as patches. Purely cosmetic: no armour, no effects, and no data of its own, so it always shows the badges of whoever
 * wears it. Right-click to put it on (the chest slot) or, with Curios, wear it in the body slot.
 */
public class SashItem extends Item implements Equipable {
    public SashItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nonnull EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }
}
