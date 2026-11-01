package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

import static com.malevolentgods.fourtwenty.processing.RigBlockEntity.*;

/** The Rosin Rig's menu: what's being refined and any bottles on the left, the extracts on the right. */
public class RigMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 166, X_IN = 44, X_OUT = 108, Y_FIRST = 20, Y_SECOND = 44, INV_X = 8, INV_Y = 84;

    private final Container container;
    private final ContainerData data;

    public RigMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModPower.RIG_MENU.get(), id);
        checkContainerSize(container, SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        for (int i = 0; i < OUTPUTS; i++) {
            addSlot(new Slot(container, i, X_IN, i == 0 ? Y_FIRST : Y_SECOND) {
                @Override public boolean mayPlace(@Nonnull ItemStack stack) { return container.canPlaceItem(getContainerSlot(), stack); }
            });
        }
        for (int i = OUTPUTS; i < SIZE; i++) {
            addSlot(new Slot(container, i, X_OUT, i == OUTPUTS ? Y_FIRST : Y_SECOND) {
                @Override public boolean mayPlace(@Nonnull ItemStack stack) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58));
        addDataSlots(data);
    }

    public RigMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SIZE), new SimpleContainerData(DATA_COUNT));
    }

    public int progress() { return data.get(DATA_PROGRESS); }

    public int total() { return data.get(DATA_TOTAL); }

    public boolean paused() { return data.get(DATA_PAUSED) != 0; }

    public boolean overloaded() { return data.get(DATA_OVERLOADED) != 0; }

    public int stored() { return data.get(DATA_STORED); }

    public int capacity() { return data.get(DATA_CAPACITY); }

    public boolean free() { return data.get(DATA_FREE) != 0; }

    public boolean tubeBlocked() { return data.get(DATA_TUBE_BLOCKED) != 0; }

    @Override public boolean stillValid(@Nonnull Player player) { return container.stillValid(player); }

    @Override public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, SIZE + 36, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (!(container.canPlaceItem(INPUT, stack) && moveItemStackTo(stack, INPUT, INPUT + 1, false))
                && !(container.canPlaceItem(BOTTLE, stack) && moveItemStackTo(stack, BOTTLE, BOTTLE + 1, false))
                && !(index < SIZE + 27 ? moveItemStackTo(stack, SIZE + 27, SIZE + 36, false) : moveItemStackTo(stack, SIZE, SIZE + 27, false))) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }
}
