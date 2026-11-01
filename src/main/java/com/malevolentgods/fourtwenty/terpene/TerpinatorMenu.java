package com.malevolentgods.fourtwenty.terpene;

import com.malevolentgods.fourtwenty.infuser.Infuser;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
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

import static com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity.*;

/**
 * The Terpinator's menu. Slots are laid out for {@code TerpinatorScreen}; button 0 starts or stops it, 100 + product
 * picks what to make, which the server allows only for colours the player has caught in the Inspiration Infuser.
 */
public class TerpinatorMenu extends AbstractContainerMenu {
    /** Layout, shared with the screen: the colour grid on the left, the machine on the right, the inventory below. */
    public static final int WIDTH = 256, HEIGHT = 256, GRID_X = 12, GRID_Y = 24, CELL = 14, GRID_GAP = 5,
            X_BUDS = 146, X_OUT = 214, ROW = 92, INV_X = 48, INV_Y = 174;
    public static final int BUTTON_TOGGLE = 0, BUTTON_PICK = 100;

    private final Container container;
    private final ContainerData data;

    public TerpinatorMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModTerpenes.TERPINATOR_MENU.get(), id);
        checkContainerSize(container, SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, BUDS, X_BUDS, ROW) {
            @Override public boolean mayPlace(@Nonnull ItemStack stack) { return container.canPlaceItem(BUDS, stack); }
        });
        addSlot(new Slot(container, OUT, X_OUT, ROW) {
            @Override public boolean mayPlace(@Nonnull ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58));
        addDataSlots(data);
    }

    public TerpinatorMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SIZE), new SimpleContainerData(DATA_COUNT));
    }

    public int progress() { return data.get(DATA_PROGRESS); }

    public int product() { return data.get(DATA_PRODUCT); }

    public boolean running() { return data.get(DATA_RUNNING) != 0; }

    public boolean paused() { return data.get(DATA_PAUSED) != 0; }

    public Container container() { return container; }

    @Override public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (id == BUTTON_TOGGLE) {
            if (container instanceof TerpinatorBlockEntity t) t.toggle();
            else data.set(DATA_RUNNING, running() ? 0 : 1);         // client side: predict, the server's data follows
            return true;
        }
        int product = id - BUTTON_PICK;
        if (!valid(product)) return false;
        if (container instanceof TerpinatorBlockEntity t) {
            if (!Infuser.caught(player, target(product))) return false;
            t.setProduct(product);
        } else {
            data.set(DATA_PRODUCT, product);
        }
        return true;
    }

    @Override public boolean stillValid(@Nonnull Player player) { return container.stillValid(player); }

    @Override public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, SIZE + 36, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (!(container.canPlaceItem(BUDS, stack) && moveItemStackTo(stack, BUDS, BUDS + 1, false))
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
