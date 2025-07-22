package com.malevolentgods.fourtwenty.blockentity;

import com.malevolentgods.fourtwenty.menu.DrugCraftingBenchMenu;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DrugCraftingBenchBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    
    // Slot indices
    public static final int GRINDER_SLOT = 0;      // Tool slot
    public static final int PAPER_SLOT = 1;       // Papers
    public static final int WEED_INPUT_SLOT = 2;  // Input weed
    public static final int OUTPUT_SLOT = 3;      // Crafted result
    
    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int craftingProgress = 0;
    private int maxCraftingTime = 100; // 5 seconds at 20 ticks/second
    
    // Container data for client-server sync
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> craftingProgress;
                case 1 -> maxCraftingTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> craftingProgress = value;
                case 1 -> maxCraftingTime = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public DrugCraftingBenchBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DRUG_CRAFTING_BENCH.get(), pos, blockState);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fourtwenty.drug_crafting_bench");
    }

    @Override
    @Nonnull
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(@Nonnull NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    @Nonnull
    protected AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory inventory) {
        return new DrugCraftingBenchMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    public void tick() {
        if (this.level != null && !this.level.isClientSide()) {
            boolean hasValidRecipe = canCraft();
            
            if (hasValidRecipe) {
                craftingProgress++;
                
                if (craftingProgress >= maxCraftingTime) {
                    performCrafting();
                    craftingProgress = 0;
                }
                setChanged();
            } else {
                if (craftingProgress > 0) {
                    craftingProgress = 0;
                    setChanged();
                }
            }
        }
    }

    private boolean canCraft() {
        // Check if we have the basic ingredients for joint crafting
        ItemStack paper = this.items.get(PAPER_SLOT);
        ItemStack weed = this.items.get(WEED_INPUT_SLOT);
        ItemStack output = this.items.get(OUTPUT_SLOT);
        
        // For now, basic joint recipe: paper + weed bud = joint
        boolean hasPaper = paper.is(net.minecraft.world.item.Items.PAPER) && paper.getCount() >= 1;
        boolean hasWeed = weed.is(ModItems.WEED_BUD.get()) && weed.getCount() >= 1;
        boolean outputEmpty = output.isEmpty() || (output.is(ModItems.WEED_JOINT.get()) && output.getCount() < output.getMaxStackSize());
        
        return hasPaper && hasWeed && outputEmpty;
    }

    private void performCrafting() {
        if (canCraft()) {
            // Consume ingredients
            this.items.get(PAPER_SLOT).shrink(1);
            this.items.get(WEED_INPUT_SLOT).shrink(1);
            
            // Create output
            ItemStack result = new ItemStack(ModItems.WEED_JOINT.get(), 1);
            ItemStack currentOutput = this.items.get(OUTPUT_SLOT);
            
            if (currentOutput.isEmpty()) {
                this.items.set(OUTPUT_SLOT, result);
            } else {
                currentOutput.grow(1);
            }
            
            setChanged();
        }
    }

    @Override
    public void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.craftingProgress = tag.getInt("CraftingProgress");
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("CraftingProgress", this.craftingProgress);
    }

    // WorldlyContainer implementation for hopper automation
    @Override
    @Nonnull
    public int[] getSlotsForFace(@Nonnull Direction side) {
        // Top: input slots, Bottom: output slot, Sides: input slots
        return switch (side) {
            case DOWN -> new int[]{OUTPUT_SLOT}; // Output only
            case UP -> new int[]{PAPER_SLOT, WEED_INPUT_SLOT}; // Inputs from top
            default -> new int[]{PAPER_SLOT, WEED_INPUT_SLOT}; // Inputs from sides
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, @Nonnull ItemStack itemStack, @Nullable Direction direction) {
        return switch (index) {
            case PAPER_SLOT -> itemStack.is(net.minecraft.world.item.Items.PAPER);
            case WEED_INPUT_SLOT -> itemStack.is(ModItems.WEED_BUD.get());
            case GRINDER_SLOT -> false; // Tools not automatable for now
            case OUTPUT_SLOT -> false; // Output slot
            default -> false;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int index, @Nonnull ItemStack stack, @Nonnull Direction direction) {
        return index == OUTPUT_SLOT; // Only allow taking from output slot
    }

    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
                net.minecraft.world.entity.item.ItemEntity itemEntity = 
                    new net.minecraft.world.entity.item.ItemEntity(level, x, y, z, stack.copy());
                level.addFreshEntity(itemEntity);
            }
        }
    }
}
