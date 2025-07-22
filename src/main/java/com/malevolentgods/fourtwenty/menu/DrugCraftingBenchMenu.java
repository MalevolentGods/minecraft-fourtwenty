package com.malevolentgods.fourtwenty.menu;

import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

import javax.annotation.Nonnull;

public class DrugCraftingBenchMenu extends AbstractContainerMenu {
    private static final int GRINDER_SLOT = 0;
    private static final int PAPER_SLOT = 1;
    private static final int WEED_INPUT_SLOT = 2;
    private static final int OUTPUT_SLOT = 3;
    
    private final Container container;
    private final ContainerData data;

    // Client constructor
    public DrugCraftingBenchMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(4), new SimpleContainerData(2));
    }

    // Server constructor
    public DrugCraftingBenchMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.DRUG_CRAFTING_BENCH.get(), containerId);
        this.container = container;
        this.data = data;

        // Grinder slot (tool) - position TBD based on GUI layout
        this.addSlot(new Slot(container, GRINDER_SLOT, 30, 35) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return stack.is(ModItems.WEED_GRINDER.get());
            }
        });

        // Paper slot
        this.addSlot(new Slot(container, PAPER_SLOT, 56, 35) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return stack.is(Items.PAPER);
            }
        });

        // Weed input slot
        this.addSlot(new Slot(container, WEED_INPUT_SLOT, 82, 35) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return stack.is(ModItems.WEED_BUD.get());
            }
        });

        // Output slot
        this.addSlot(new Slot(container, OUTPUT_SLOT, 124, 35) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return false; // Output slot - no placing allowed
            }
        });

        // Player inventory slots
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Player hotbar slots
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return this.container.stillValid(player);
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack resultStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            resultStack = stackInSlot.copy();
            
            if (index == OUTPUT_SLOT) {
                // Moving from output slot to player inventory
                if (!this.moveItemStackTo(stackInSlot, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, resultStack);
            } else if (index >= 4 && index < 40) {
                // Moving from player inventory to crafting slots
                if (stackInSlot.is(ModItems.WEED_GRINDER.get())) {
                    if (!this.moveItemStackTo(stackInSlot, GRINDER_SLOT, GRINDER_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(Items.PAPER)) {
                    if (!this.moveItemStackTo(stackInSlot, PAPER_SLOT, PAPER_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.WEED_BUD.get())) {
                    if (!this.moveItemStackTo(stackInSlot, WEED_INPUT_SLOT, WEED_INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 31) {
                    // Main inventory to hotbar
                    if (!this.moveItemStackTo(stackInSlot, 31, 40, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Hotbar to main inventory
                    if (!this.moveItemStackTo(stackInSlot, 4, 31, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(stackInSlot, 4, 40, false)) {
                return ItemStack.EMPTY;
            }
            
            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return resultStack;
    }

    public int getCraftingProgress() {
        return this.data.get(0);
    }

    public int getMaxCraftingTime() {
        return this.data.get(1);
    }
}
