package com.malevolentgods.fourtwenty.terpene;

import com.malevolentgods.fourtwenty.registry.ModTerpenes;
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
import java.util.List;

import static com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity.*;

/**
 * The Terpinator's menu. Slots are laid out for {@code TerpinatorScreen}; buttons 0..3 set the
 * refining method. Taking from an output records the discovery straight away (the inventory scan
 * would catch it within a second anyway).
 */
public class TerpinatorMenu extends AbstractContainerMenu {
    /** Slot positions, shared with the screen. */
    public static final int ROW_PRESS = 38, ROW_BLEND = 92, X_BUDS = 17, X_RESIN = 71, X_OUT = 143, X_P1 = 17, X_P2 = 37, X_P3 = 57,
            INV_Y = 140;

    private final Container container;
    private final ContainerData data;

    public TerpinatorMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModTerpenes.TERPINATOR_MENU.get(), id);
        checkContainerSize(container, SIZE);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        addSlot(new Input(container, BUDS, X_BUDS, ROW_PRESS));
        addSlot(new Input(container, RESIN, X_RESIN, ROW_PRESS));
        addSlot(new Output(container, BASE_OUT, X_OUT, ROW_PRESS, inventory.player));
        addSlot(new Input(container, PORT1, X_P1, ROW_BLEND));
        addSlot(new Input(container, PORT2, X_P2, ROW_BLEND));
        addSlot(new Input(container, PORT3, X_P3, ROW_BLEND));
        addSlot(new Output(container, BLEND_OUT, X_OUT, ROW_BLEND, inventory.player));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
        addDataSlots(data);
    }

    public TerpinatorMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SIZE), new SimpleContainerData(DATA_COUNT));
    }

    public int progress(int index) { return data.get(index); }

    public int method() { return data.get(DATA_METHOD); }

    public Container container() { return container; }

    @Override public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (id < 0 || id > 3) return false;
        if (container instanceof TerpinatorBlockEntity t) t.setMethod(id);
        else data.set(DATA_METHOD, id);
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
        } else if (!moveIn(stack) && !moveWithinInventory(stack, index)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    /** Buds to the press, resin to the refiner, an accord to port 1, a base to the first port that takes it. */
    private boolean moveIn(ItemStack stack) {
        for (int target : List.of(BUDS, RESIN, PORT1, PORT2, PORT3)) {
            if (!accepts(target, stack)) continue;
            ItemStack there = slots.get(target).getItem();
            if (!there.isEmpty() && !ItemStack.isSameItemSameComponents(there, stack)) continue;
            if (moveItemStackTo(stack, target, target + 1, false)) return true;
        }
        return false;
    }

    private boolean moveWithinInventory(ItemStack stack, int index) {
        return index < SIZE + 27 ? moveItemStackTo(stack, SIZE + 27, SIZE + 36, false) : moveItemStackTo(stack, SIZE, SIZE + 27, false);
    }

    private static class Input extends Slot {
        Input(Container container, int index, int x, int y) { super(container, index, x, y); }

        @Override public boolean mayPlace(@Nonnull ItemStack stack) { return accepts(getContainerSlot(), stack); }
    }

    private static class Output extends Slot {
        private final Player player;

        Output(Container container, int index, int x, int y, Player player) {
            super(container, index, x, y);
            this.player = player;
        }

        @Override public boolean mayPlace(@Nonnull ItemStack stack) { return false; }

        @Override public void onTake(@Nonnull Player taker, @Nonnull ItemStack stack) {
            learn(stack);
            super.onTake(taker, stack);
        }

        @Override protected void onQuickCraft(@Nonnull ItemStack stack, int amount) { learn(stack); }

        private void learn(ItemStack stack) {
            String key = TerpeneDiscovery.key(stack);
            if (key != null && player instanceof ServerPlayer sp) TerpeneDiscovery.learn(sp, List.of(key));
        }
    }
}
