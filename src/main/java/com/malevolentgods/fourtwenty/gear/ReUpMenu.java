package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.power.PowerMeter;
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

import static com.malevolentgods.fourtwenty.gear.ReUpStationBlockEntity.*;

/** The Re-Up Station's screen: one slot for the piece being charged, the battery gauge, what comes in and what goes out. */
public class ReUpMenu extends AbstractContainerMenu implements PowerMeter.Shown {
    public static final int WIDTH = 176, HEIGHT = 166, SLOT_X = 26, SLOT_Y = 34, INV_X = 8, INV_Y = 84;
    private final Container container;
    private final ContainerData data, meter;

    public ReUpMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModGear.RE_UP_MENU.get(), id);
        checkContainerSize(container, 1);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, 0, SLOT_X, SLOT_Y) {
            @Override public boolean mayPlace(@Nonnull ItemStack stack) { return container.canPlaceItem(0, stack); }

            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58));
        addDataSlots(data);
        meter = PowerMeter.of(container);
        addDataSlots(meter);
    }

    public ReUpMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
    }

    @Override public ContainerData meter() { return meter; }

    public int stored() { return data.get(DATA_STORED); }

    public int capacity() { return data.get(DATA_CAPACITY); }

    public boolean overloaded() { return data.get(DATA_OVERLOADED) != 0; }

    public int tier() { return data.get(DATA_TIER); }

    public int inWatts() { return data.get(DATA_IN); }

    public int outWatts() { return data.get(DATA_OUT); }

    public int state() { return data.get(DATA_STATE); }

    @Override public boolean stillValid(@Nonnull Player player) { return container.stillValid(player); }

    @Override public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (!(container.canPlaceItem(0, stack) && moveItemStackTo(stack, 0, 1, false))) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
