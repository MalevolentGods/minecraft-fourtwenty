package com.malevolentgods.fourtwenty.blockentity;

import com.malevolentgods.fourtwenty.menu.DrugCraftingBenchMenu;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.item.WeedGrinderItem;
import com.malevolentgods.fourtwenty.util.CraftingExperienceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.UUID;

public class DrugCraftingBenchBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    
    // Slot indices
    public static final int GRINDER_SLOT = 0;      // Tool slot
    public static final int PAPER_SLOT = 1;       // Papers
    public static final int WEED_INPUT_SLOT = 2;  // Input weed
    public static final int OUTPUT_SLOT = 3;      // Crafted result
    
    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int craftingProgress = 0;
    private int maxCraftingTime = 100; // 5 seconds at 20 ticks/second
    private UUID lastUsedByPlayer = null; // Track who last used this bench
    
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
        // Track the player who opened this container
        if (inventory.player instanceof Player player) {
            this.lastUsedByPlayer = player.getUUID();
            setChanged();
        }
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
                // Apply skill-based speed bonus
                int progressIncrement = 1;
                if (lastUsedByPlayer != null && level != null) {
                    // Get the player who last used this bench
                    Player player = level.getPlayerByUUID(lastUsedByPlayer);
                    if (player instanceof ServerPlayer serverPlayer) {
                        float speedBonus = CraftingExperienceManager.getLevelSpeedBonus(serverPlayer);
                        // Speed bonus increases crafting rate (higher bonus = faster crafting)
                        progressIncrement = Math.max(1, Math.round(1.0f + speedBonus));
                    }
                }
                
                craftingProgress += progressIncrement;
                
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
        ItemStack grinder = this.items.get(GRINDER_SLOT);
        ItemStack paper = this.items.get(PAPER_SLOT);
        ItemStack weed = this.items.get(WEED_INPUT_SLOT);
        ItemStack output = this.items.get(OUTPUT_SLOT);
        
        // For advanced crafting: grinder + paper + weed bud = joint (with efficiency bonus)
        boolean hasGrinder = WeedGrinderItem.canUse(grinder);
        boolean hasPaper = paper.is(net.minecraft.world.item.Items.PAPER) && paper.getCount() >= 1;
        boolean hasWeed = weed.is(ModItems.WEED_BUD.get()) && weed.getCount() >= 1;
        boolean outputEmpty = output.isEmpty() || (output.is(ModItems.WEED_JOINT.get()) && output.getCount() < output.getMaxStackSize());
        
        // All three components required for advanced crafting
        return hasGrinder && hasPaper && hasWeed && outputEmpty;
    }

    private void performCrafting() {
        if (canCraft()) {
            ItemStack grinder = this.items.get(GRINDER_SLOT);
            
            // Consume ingredients
            this.items.get(PAPER_SLOT).shrink(1);
            this.items.get(WEED_INPUT_SLOT).shrink(1);
            
            // Use the grinder (damages it)
            WeedGrinderItem.useGrinder(grinder);
            
            // Calculate output quantity based on grinder efficiency and player skill
            float grinderEfficiency = WeedGrinderItem.getEfficiencyBonus(grinder);
            float playerEfficiency = 0.0f;
            
            // Apply player skill bonuses if we know who crafted this
            Player craftingPlayer = null;
            if (lastUsedByPlayer != null && level != null) {
                craftingPlayer = level.getPlayerByUUID(lastUsedByPlayer);
                if (craftingPlayer != null) {
                    playerEfficiency = CraftingExperienceManager.getLevelEfficiencyBonus(craftingPlayer);
                }
            }
            
            // Combine bonuses (grinder: 20-50%, player skill: up to 50%)
            float totalEfficiency = grinderEfficiency + playerEfficiency;
            int baseOutput = 1;
            int bonusChance = (int)(totalEfficiency * 100); // Convert to percentage
            
            // Chance for bonus joint based on combined efficiency
            int outputCount = baseOutput;
            if (this.level != null && this.level.random.nextInt(100) < bonusChance) {
                outputCount = 2; // Bonus joint from efficient grinding + skill
            }
            
            // Create output
            ItemStack result = new ItemStack(ModItems.WEED_JOINT.get(), outputCount);
            ItemStack currentOutput = this.items.get(OUTPUT_SLOT);
            
            if (currentOutput.isEmpty()) {
                this.items.set(OUTPUT_SLOT, result);
            } else {
                currentOutput.grow(outputCount);
            }
            
            // Award experience to the player who crafted this
            if (craftingPlayer instanceof ServerPlayer serverPlayer) {
                int baseExperience = 10; // Base XP for crafting a joint
                int bonusExperience = (outputCount > 1) ? 5 : 0; // Bonus XP for efficient crafting
                CraftingExperienceManager.awardExperience(serverPlayer, baseExperience + bonusExperience);
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
        
        // Load last used player UUID
        if (tag.hasUUID("LastUsedByPlayer")) {
            this.lastUsedByPlayer = tag.getUUID("LastUsedByPlayer");
        }
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("CraftingProgress", this.craftingProgress);
        
        // Save last used player UUID
        if (this.lastUsedByPlayer != null) {
            tag.putUUID("LastUsedByPlayer", this.lastUsedByPlayer);
        }
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
            case GRINDER_SLOT -> itemStack.is(ModItems.WEED_GRINDER.get());
            case PAPER_SLOT -> itemStack.is(net.minecraft.world.item.Items.PAPER);
            case WEED_INPUT_SLOT -> itemStack.is(ModItems.WEED_BUD.get());
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
