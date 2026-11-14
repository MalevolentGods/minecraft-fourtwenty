package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Empties a storage block the Builder is about to break into her hut (specs/013-lifted-builders FR-016, clarification
 * Q3): the hut's own pickups carry spare items on to the Warehouse. Whatever doesn't fit drops at the hut, never where
 * the block stood.
 */
public final class StorageTransfer {
    private StorageTransfer() {}

    /** Moves everything out of {@code from}; returns how many items moved (into the hut or dropped at it). */
    public static int move(IItemHandler from, IItemHandler into, Level level, BlockPos dropAt) {
        int moved = 0;
        for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack stack = from.extractItem(slot, from.getStackInSlot(slot).getCount(), false);
            if (stack.isEmpty()) continue;
            moved += stack.getCount();
            ItemStack left = into == null ? stack : ItemHandlerHelper.insertItemStacked(into, stack, false);
            if (!left.isEmpty()) Containers.dropItemStack(level, dropAt.getX() + 0.5, dropAt.getY() + 1, dropAt.getZ() + 0.5, left);
        }
        return moved;
    }
}
