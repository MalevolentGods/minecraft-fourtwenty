package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.Studies;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.server.level.ServerPlayer;
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

import static com.malevolentgods.fourtwenty.processing.ThinkTankBlockEntity.*;

/** The Think Tank: the study grid on the left, its inputs on the right. Button 0 collects a finished study; 1 + n picks study n. */
public class ThinkTankMenu extends AbstractContainerMenu {
    public static final int X_IN = 98, Y_IN = 20, BUTTON_COLLECT = 0, BUTTON_PICK = 1;

    private final Container container;
    private final ContainerData data;

    public ThinkTankMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModPower.THINK_TANK_MENU.get(), id);
        checkContainerSize(container, SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        for (int i = 0; i < SLOTS; i++) {
            addSlot(new Slot(container, i, X_IN + i * 18, Y_IN) {
                @Override public boolean mayPlace(@Nonnull ItemStack stack) { return container.canPlaceItem(getContainerSlot(), stack); }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public ThinkTankMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SLOTS), new SimpleContainerData(DATA_COUNT));
    }

    public int study() { return data.get(DATA_STUDY); }

    public int status() { return data.get(DATA_STATUS); }

    public int progress() { return data.get(DATA_PROGRESS); }

    public int total() { return data.get(DATA_TOTAL); }

    public int stored() { return data.get(DATA_STORED); }

    public int capacity() { return data.get(DATA_CAPACITY); }

    public boolean overloaded() { return data.get(DATA_OVERLOADED) != 0; }

    public int minutes() { return data.get(DATA_MINUTES); }

    @Override public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (!(container instanceof ThinkTankBlockEntity tank) || !(player instanceof ServerPlayer server)) return false;
        if (id == BUTTON_COLLECT) tank.collect(server);
        else if (id - BUTTON_PICK < Studies.ALL.size()) tank.pick(server, id - BUTTON_PICK);
        return true;
    }

    @Override public boolean stillValid(@Nonnull Player player) { return container.stillValid(player); }

    @Override public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, SLOTS + 36, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }
}
