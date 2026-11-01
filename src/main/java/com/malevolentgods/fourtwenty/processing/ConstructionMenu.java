package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/** The Construction Joint: a crafting grid that makes twice as many of the mod's building blocks. */
public class ConstructionMenu extends CraftingMenu {
    private final ContainerLevelAccess access;

    public ConstructionMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(id, inventory, access);
        this.access = access;
    }

    public ConstructionMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    /** What the bench makes of a recipe's result. */
    public static ItemStack doubled(ItemStack result) {
        if (!result.is(ModPower.CONSTRUCTION)) return result;
        return result.copyWithCount(Math.min(result.getMaxStackSize(), result.getCount() * 2));
    }

    @Override public @Nonnull MenuType<?> getType() { return ModPower.CONSTRUCTION_MENU.get(); }

    @Override public boolean stillValid(@Nonnull Player player) {
        return stillValid(access, player, ModPower.CONSTRUCTION_JOINT.get());
    }

    @Override public void slotsChanged(@Nonnull Container container) {
        super.slotsChanged(container);
        access.execute((level, pos) -> {
            var result = getSlot(getResultSlotIndex());
            if (result.hasItem()) {
                result.set(doubled(result.getItem()));
                broadcastChanges();
            }
        });
    }
}
