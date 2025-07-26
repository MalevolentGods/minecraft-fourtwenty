package com.malevolentgods.fourtwenty.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.MenuProvider;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.block.WeedBongBlock;
import com.malevolentgods.fourtwenty.menu.WeedBongMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class WeedBongBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final int EFFECT_RADIUS = 4; // 4 block radius for area effects
    private static final int BURN_DURATION = 1200; // 1 minute (1200 ticks) per bud
    private static final int USE_COOLDOWN = 100; // 5 seconds between uses
    
    private static final int WEED_SLOT = 0;
    private static final int WATER_SLOT = 1;
    private static final int BLAZE_POWDER_SLOT = 2;
    
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int burnTime = 0;
    private int lastUseTick = 0;
    private boolean isLit = false;
    
    // Container data for syncing with client
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> isLit ? 1 : 0;
                case 1 -> burnTime;
                case 2 -> WeedBongBlockEntity.this.hasWater() ? 1 : 0;
                case 3 -> WeedBongBlockEntity.this.getWeedCount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> isLit = value != 0;
                case 1 -> burnTime = value;
                case 2 -> {} // Water status is read-only
                case 3 -> {} // Weed count is read-only
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };
    
    public WeedBongBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.WEED_BONG.get(), pos, blockState);
    }

    // Container implementation methods
    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Nonnull
    public ItemStack getItem(int index) {
        return this.items.get(index);
    }

    @Override
    @Nonnull
    public ItemStack removeItem(int index, int count) {
        ItemStack result = ContainerHelper.removeItem(this.items, index, count);
        if (!result.isEmpty()) {
            this.setChanged();
            this.updateBlockState();
        }
        return result;
    }

    @Override
    @Nonnull
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(this.items, index);
    }

    @Override
    public void setItem(int index, @Nonnull ItemStack stack) {
        this.items.set(index, stack);
        if (stack.getCount() > this.getMaxStackSize(index)) {
            stack.setCount(this.getMaxStackSize(index));
        }
        this.setChanged();
        this.updateBlockState();
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this &&
               player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.updateBlockState();
    }
    
    @Override
    public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) {
        // Limit weed slot to maximum of 4 items
        if (slot == WEED_SLOT) {
            ItemStack currentStack = this.getItem(WEED_SLOT);
            // Don't allow any items if we already have 4
            if (currentStack.getCount() >= 4) {
                return false;
            }
            // Don't allow if adding this stack would exceed 4
            return currentStack.getCount() + stack.getCount() <= 4;
        }
        return true;
    }
    
    @Override
    public int getMaxStackSize() {
        return 64; // Default for most slots
    }
    
    // Custom method to get max stack size for specific slot
    public int getMaxStackSize(int slot) {
        if (slot == WEED_SLOT) {
            return 4; // Limit weed slot to 4 items
        }
        return getMaxStackSize();
    }
    
    // Update block state to match inventory contents
    private void updateBlockState() {
        if (this.level != null && !this.level.isClientSide) {
            // Check if this block entity is still valid and the block still exists
            BlockState state = this.getBlockState();
            if (state.getBlock() instanceof WeedBongBlock) {
                boolean hasWater = hasWater();
                int weedLevel = Math.min(getWeedCount(), 4); // Cap at 4 for block states
                boolean lit = isLit;
                
                BlockState newState = state
                    .setValue(WeedBongBlock.HAS_WATER, hasWater)
                    .setValue(WeedBongBlock.WEED_LEVEL, weedLevel)
                    .setValue(WeedBongBlock.LIT, lit);
                
                if (!state.equals(newState)) {
                    this.level.setBlock(this.worldPosition, newState, 3);
                }
            }
        }
    }

    // MenuProvider implementation methods
    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("container.fourtwenty.weed_bong");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new WeedBongMenu(containerId, inventory, this, this.dataAccess);
    }

    // Helper methods for container data
    private boolean hasWater() {
        return this.getItem(WATER_SLOT).is(Items.WATER_BUCKET);
    }

    private int getWeedCount() {
        return this.getItem(WEED_SLOT).getCount();
    }

    private boolean hasBlazePowder() {
        return !this.getItem(BLAZE_POWDER_SLOT).isEmpty();
    }

    // Check if bong can be lit
    public boolean canLight() {
        return hasWater() && getWeedCount() > 0 && hasBlazePowder() && !isLit;
    }

    // Check if bong can be used
    public boolean canUse() {
        return hasWater() && getWeedCount() > 0 && isLit;
    }
    
    public boolean isLit() {
        return isLit;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WeedBongBlockEntity entity) {
        if (level.isClientSide()) {
            entity.clientTick(level, pos, state);
        } else {
            entity.serverTick(level, pos, state);
        }
    }

    private void clientTick(Level level, BlockPos pos, BlockState state) {
        // Client-side particle effects when lit
        if (state.getValue(WeedBongBlock.LIT) && level.random.nextFloat() < 0.3f) {
            // Add smoke particles
            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
            
            level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                x, y, z, 0.0, 0.1, 0.0);
        }
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        // Check if we can auto-light the bong
        if (!isLit && canLight()) {
            autoLight(level, pos, state);
        }
        
        if (isLit) {
            burnTime++;
            
            // Apply area effects every 20 ticks (1 second)
            if (burnTime % 20 == 0) {
                applyAreaEffects(level, pos);
            }
            
            // Consume weed and extinguish after burn duration
            if (burnTime >= BURN_DURATION) {
                consumeWeed(level, pos, state);
                burnTime = 0;
                isLit = false;
                setChanged();
                updateBlockState();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        }
    }
    
    private void autoLight(Level level, BlockPos pos, BlockState state) {
        // Consume blaze powder to light the bong
        ItemStack blazePowder = getItem(BLAZE_POWDER_SLOT);
        if (!blazePowder.isEmpty()) {
            blazePowder.shrink(1);
            setItem(BLAZE_POWDER_SLOT, blazePowder);
            
            isLit = true;
            burnTime = 0;
            setChanged();
            updateBlockState();
            
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.playSound(null, worldPosition, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
    }

    private void applyAreaEffects(Level level, BlockPos pos) {
        AABB effectArea = new AABB(pos).inflate(EFFECT_RADIUS);
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, effectArea);
        
        for (Player player : nearbyPlayers) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Apply enhanced effects for group session
                applyBongEffects(serverPlayer);
            }
        }
    }

    private void applyBongEffects(ServerPlayer player) {
        // Enhanced effects compared to individual consumption
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1)); // 20s, level 2
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0)); // 1 minute
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 0)); // 30s
        
        // Bonus effects for group sessions
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0)); // 15s
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0)); // 15s
        
        // Slight hunger effect (munchies)
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0)); // 10s
    }

    public void lightBong(ServerPlayer player) {
        if (!canLight()) return;
        
        // Consume one blaze powder
        ItemStack blazePowder = this.getItem(BLAZE_POWDER_SLOT);
        blazePowder.shrink(1);
        this.setItem(BLAZE_POWDER_SLOT, blazePowder);
        
        isLit = true;
        burnTime = 0;
        setChanged();
        
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.playSound(null, worldPosition, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.2f);
        }
    }

    // public void useBong(ServerPlayer player) {
    //     if (level == null || !canUse()) return;
        
    //     long currentTick = level.getGameTime();
        
    //     // Check cooldown
    //     if (currentTick - lastUseTick < USE_COOLDOWN) {
    //         return;
    //     }
        
    //     lastUseTick = (int) currentTick;
        
    //     // Apply immediate strong effects to the user
    //     player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2)); // 30s, level 3
    //     player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0)); // 1.5 minutes
    //     player.addEffect(new MobEffectInstance(MobEffects.LUCK, 900, 1)); // 45s, level 2
    //     player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 2)); // 5s food restoration
        
    //     // Play use sound
    //     if (level != null) {
    //         level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.8f, 0.8f);
    //     }
        
    //     setChanged();
    // }

    private void consumeWeed(Level level, BlockPos pos, BlockState state) {
        ItemStack weedStack = this.getItem(WEED_SLOT);
        if (!weedStack.isEmpty()) {
            weedStack.shrink(1);
            this.setItem(WEED_SLOT, weedStack);
            
            if (weedStack.isEmpty()) {
                isLit = false;
            }
            
            setChanged();
            updateBlockState();
            if (level != null) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("LastUseTick", lastUseTick);
        tag.putBoolean("IsLit", isLit);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Override
    protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("BurnTime");
        lastUseTick = tag.getInt("LastUseTick");
        isLit = tag.getBoolean("IsLit");
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @Nonnull
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
