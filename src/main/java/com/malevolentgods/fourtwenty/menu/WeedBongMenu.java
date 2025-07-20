package com.malevolentgods.fourtwenty.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModMenuTypes;

import javax.annotation.Nonnull;

public class WeedBongMenu extends AbstractContainerMenu {
    private static final int WEED_SLOT = 0;
    private static final int WATER_SLOT = 1;
    private static final int BLAZE_POWDER_SLOT = 2;
    private static final int DATA_COUNT = 4; // lit status, burn time, water filled, weed level
    
    private final Container container;
    private final ContainerData data;

    // Constructor for server-side (with real container)
    public WeedBongMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.WEED_BONG.get(), containerId);
        this.container = container;
        this.data = data;
        
        checkContainerSize(container, 3);
        checkContainerDataCount(data, DATA_COUNT);
        
        // Add bong slots
        this.addSlot(new WeedSlot(container, WEED_SLOT, 79, 17)); // Weed buds slot
        this.addSlot(new WaterSlot(container, WATER_SLOT, 79, 53)); // Water bucket slot
        this.addSlot(new BlazePowderSlot(container, BLAZE_POWDER_SLOT, 17, 17)); // Blaze powder slot
        
        // Add player inventory slots
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        
        // Add player hotbar slots
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
        
        this.addDataSlots(data);
    }

    // Constructor for client-side (with dummy container)
    public WeedBongMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(DATA_COUNT));
    }

    // Constructor for network syncing
    public WeedBongMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return this.container.stillValid(player);
    }
    
    public Container getContainer() {
        return this.container;
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            if (index < 3) {
                // Moving from bong slots to player inventory
                if (!this.moveItemStackTo(slotStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to bong slots
                if (slotStack.is(ModItems.WEED_BUD.get())) {
                    if (!this.moveItemStackTo(slotStack, WEED_SLOT, WEED_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.is(Items.WATER_BUCKET)) {
                    if (!this.moveItemStackTo(slotStack, WATER_SLOT, WATER_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.is(Items.BLAZE_POWDER)) {
                    if (!this.moveItemStackTo(slotStack, BLAZE_POWDER_SLOT, BLAZE_POWDER_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 3 && index < 30) {
                    // Player inventory to hotbar
                    if (!this.moveItemStackTo(slotStack, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 30 && index < 39) {
                    // Hotbar to player inventory
                    if (!this.moveItemStackTo(slotStack, 3, 30, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(player, slotStack);
        }
        
        return itemstack;
    }

    // Getter methods for data
    public boolean isLit() {
        return this.data.get(0) != 0;
    }
    
    public int getBurnTime() {
        return this.data.get(1);
    }
    
    public boolean hasWater() {
        return this.data.get(2) != 0;
    }
    
    public int getWeedLevel() {
        return this.data.get(3);
    }

    // Custom slot classes
    static class WeedSlot extends Slot {
        public WeedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return stack.is(ModItems.WEED_BUD.get());
        }

        @Override
        public int getMaxStackSize() {
            return 64; // Allow full stacks of weed
        }
    }

    static class WaterSlot extends Slot {
        public WaterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return stack.is(Items.WATER_BUCKET);
        }

        @Override
        public int getMaxStackSize() {
            return 1; // Only one water bucket at a time
        }
    }

    static class BlazePowderSlot extends Slot {
        public BlazePowderSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return stack.is(Items.BLAZE_POWDER);
        }

        @Override
        public int getMaxStackSize() {
            return 64; // Allow full stacks of blaze powder
        }
    }
}
