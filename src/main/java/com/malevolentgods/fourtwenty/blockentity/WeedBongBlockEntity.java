package com.malevolentgods.fourtwenty.blockentity;

import com.malevolentgods.fourtwenty.block.WeedBongBlock;
import com.malevolentgods.fourtwenty.domain.BongFuel;
import net.minecraft.core.registries.BuiltInRegistries;
import com.malevolentgods.fourtwenty.menu.WeedBongMenu;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/** One inventory backs menus, hoppers and every capability side. */
public class WeedBongBlockEntity extends BlockEntity implements Container, MenuProvider {
    /** One charcoal's burn, and the length assumed for older saves. */
    public static final int BURN_DURATION = 1200;
    public static final int WEED_SLOT = 0;
    public static final int WATER_SLOT = 1;
    /** Takes charcoal, the {@link BongFuel}. */
    public static final int FUEL_SLOT = 2;
    /** Older name for {@link #FUEL_SLOT}. */
    public static final int BLAZE_POWDER_SLOT = FUEL_SLOT;
    /**
     * Told once a second while a bong burns. Set by the MineColonies integration (bong session
     * happiness); stays null without MineColonies, so the core mod has no colony references.
     */
    public static volatile java.util.function.BiConsumer<net.minecraft.server.level.ServerLevel, BlockPos> burningListener;
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int burnTime;
    private int burnDuration = BURN_DURATION;
    private boolean isLit;
    private boolean chargePaid;
    private boolean removing;
    private double made;
    /** BI it has made and not yet sent down the Wicks: output only, and a Hookah can't be overloaded by what it makes. */
    private final com.malevolentgods.fourtwenty.power.BIBuffer buffer;
    private final com.malevolentgods.fourtwenty.power.BIPush push = new com.malevolentgods.fourtwenty.power.BIPush();
    private static final java.util.List<net.minecraft.core.Direction> ALL = java.util.List.of(net.minecraft.core.Direction.values());

    private final ContainerData dataAccess = new ContainerData() {
        public int get(int index) {
            return switch (index) {
                case 0 -> isLit ? 1 : 0;
                case 1 -> burnTime;
                case 2 -> hasWater() ? 1 : 0;
                case 3 -> visualWeedLevel();
                case 4 -> burnDuration;
                case 5 -> (int) buffer.stored();
                case 6 -> (int) buffer.capacity();
                case 7 -> buffer.tier().ordinal();
                default -> 0;
            };
        }
        public void set(int index, int value) {
            if (index == 0) isLit = value != 0;
            if (index == 1) burnTime = value;
        }
        public int getCount() { return 8; }
    };

    // A caller retaining an old handler after movement must not mutate the old inventory.
    private final IItemHandler handler = new InvWrapper(this) {
        @Override public ItemStack getStackInSlot(int slot) {
            return live() ? super.getStackInSlot(slot).copy() : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return slotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return live() && accepts(slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return live() ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!live() || amount <= 0) return ItemStack.EMPTY;
            // Legacy invalid/oversized stacks must support partial recovery too. InvWrapper
            // writes the remainder through setItem, whose insertion validation rejects them.
            var existing = getItem(slot);
            int count = Math.min(amount, Math.min(existing.getCount(), existing.getMaxStackSize()));
            if (count == 0) return ItemStack.EMPTY;
            return simulate ? existing.copyWithCount(count) : removeItem(slot, count);
        }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            if (live()) super.setStackInSlot(slot, stack);
        }
    };

    public WeedBongBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEED_BONG.get(), pos, state);
        var tier = ((WeedBongBlock) state.getBlock()).tier();
        buffer = new com.malevolentgods.fourtwenty.power.BIBuffer(tier, tier.total(), false, true, () -> {});
    }

    public com.malevolentgods.fourtwenty.power.BINode node() { return buffer; }

    public IItemHandler itemHandler() { return handler; }

    private boolean live() {
        return !removing && !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this;
    }

    public static int slotLimit(int slot) {
        return switch (slot) {
            case WEED_SLOT -> 4;
            case WATER_SLOT -> 1;
            case BLAZE_POWDER_SLOT -> 64;
            default -> throw new IndexOutOfBoundsException(slot);
        };
    }

    /** Type validation is independent of incoming stack size so partial insertions work. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case WEED_SLOT -> stack.is(ModItems.WEED_BUD.get());
            case WATER_SLOT -> stack.is(Items.WATER_BUCKET);
            case FUEL_SLOT -> BongFuel.isFuel(id(stack));
            default -> false;
        };
    }
    private static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    @Override public int getContainerSize() { return 3; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return live() && accepts(slot, stack) && getItem(slot).getCount() < slotLimit(slot);
    }
    @Override public ItemStack removeItem(int slot, int count) {
        if (!live() || count <= 0) return ItemStack.EMPTY;
        var result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (!live()) return ItemStack.EMPTY;
        var result = ContainerHelper.takeItem(items, slot);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (!live()) return;
        if (!stack.isEmpty() && (!accepts(slot, stack) || stack.getCount() > Math.min(slotLimit(slot), stack.getMaxStackSize()))) {
            throw new IllegalArgumentException("Invalid bong stack for slot " + slot + "; use itemHandler insertion for partial transfer");
        }
        items.set(slot, stack.copy());
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return live() && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }
    @Override public void clearContent() {
        if (!live()) return;
        items.clear();
        setChanged();
    }

    /** Destruction transfers ownership to drops; do not rewrite the replacement block's state. */
    public void beginRemoval() { removing = true; }

    @Override public void setChanged() {
        super.setChanged();
        if (live() && !level.isClientSide()) {
            if (isLit && !hasWater()) extinguish();
            updateBlockState();
        }
    }
    @Override public void setRemoved() {
        super.setRemoved();
        if (level != null) level.invalidateCapabilities(worldPosition);
    }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WeedBongMenu(id, inventory, this, dataAccess);
    }

    private boolean hasWater() { return getItem(WATER_SLOT).is(Items.WATER_BUCKET); }
    private int queuedBuds() { return getItem(WEED_SLOT).is(ModItems.WEED_BUD.get()) ? getItem(WEED_SLOT).getCount() : 0; }
    private int visualWeedLevel() { return Math.min(4, queuedBuds() + (isLit && chargePaid ? 1 : 0)); }
    public boolean isLit() { return isLit; }
    public int burnTime() { return burnTime; }
    public boolean canLight() {
        return live() && hasWater() && queuedBuds() > 0 && BongFuel.isFuel(id(getItem(FUEL_SLOT))) && !isLit;
    }
    public boolean canUse() { return live() && isLit && chargePaid && hasWater(); }

    private void updateBlockState() {
        var state = getBlockState();
        if (!(state.getBlock() instanceof WeedBongBlock)) return;
        var updated = state.setValue(WeedBongBlock.HAS_WATER, hasWater())
            .setValue(WeedBongBlock.WEED_LEVEL, visualWeedLevel()).setValue(WeedBongBlock.LIT, isLit);
        if (!updated.equals(state)) level.setBlock(worldPosition, updated, 3);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WeedBongBlockEntity entity) {
        if (level.isClientSide()) {
            if (state.getValue(WeedBongBlock.LIT) && level.random.nextFloat() < .3f) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + .5 + (level.random.nextDouble() - .5) * .3, pos.getY() + 1,
                    pos.getZ() + .5 + (level.random.nextDouble() - .5) * .3, 0, .1, 0);
            }
        } else if (entity.live()) entity.serverTick();
    }

    /** Legacy lit saves paid fuel at ignition but left their bud in the inventory. */
    private void migrateLegacyBurn() {
        if (isLit && !chargePaid) {
            if (hasWater() && queuedBuds() > 0 && burnTime >= 0 && burnTime < burnDuration) {
                getItem(WEED_SLOT).shrink(1);
                chargePaid = true;
            } else extinguish();
            setChanged();
        }
    }

    private void serverTick() {
        migrateLegacyBurn();
        if (!isLit && canLight()) lightBong(null);
        var tier = buffer.tier();
        if (isLit && hasWater()) {
            made += tier.rate();
            buffer.set(buffer.stored() + (long) made);
            made -= (long) made;
        }
        push.push(level, worldPosition, ALL, tier, tier.rate(), buffer, node -> true);
        if (!isLit) return;
        if (!hasWater()) { extinguish(); setChanged(); return; }
        burnTime++;
        if (burnTime % 20 == 0) {
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(4))) {
                if (player.isAlive() && !player.isSpectator()) applyBongEffects(player);
            }
            var listener = burningListener;
            if (listener != null) listener.accept((net.minecraft.server.level.ServerLevel) level, worldPosition);
        }
        if (burnTime >= burnDuration) extinguish();
        // Mark every elapsed tick durable; dirty marking does not itself write the chunk to disk.
        setChanged();
    }

    /** Inputs are paid atomically before the first effect, independently of later extraction. */
    public void lightBong(ServerPlayer player) {
        if (!canLight() || level.isClientSide()) return;
        var fuel = getItem(FUEL_SLOT);
        int duration = BongFuel.burnTicks(id(fuel)).orElseThrow();
        getItem(WEED_SLOT).shrink(1);
        fuel.shrink(1);
        isLit = true;
        chargePaid = true;
        burnTime = 0;
        burnDuration = duration;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1, 1);
    }

    private void extinguish() { isLit = false; chargePaid = false; burnTime = 0; burnDuration = BURN_DURATION; }
    public int burnDuration() { return burnDuration; }

    private void applyBongEffects(ServerPlayer player) {
        com.malevolentgods.fourtwenty.util.StrainEffects.markHigh(player, 1200);
        player.addEffect(com.malevolentgods.fourtwenty.util.SmokeEffects.quiet(MobEffects.REGENERATION, 400, 1));
        player.addEffect(com.malevolentgods.fourtwenty.util.SmokeEffects.quiet(MobEffects.NIGHT_VISION, 1200, 0));
        player.addEffect(com.malevolentgods.fourtwenty.util.SmokeEffects.quiet(MobEffects.LUCK, 600, 0));
        player.addEffect(com.malevolentgods.fourtwenty.util.SmokeEffects.quiet(MobEffects.DAMAGE_BOOST, 300, 0));
        player.addEffect(com.malevolentgods.fourtwenty.util.SmokeEffects.quiet(MobEffects.MOVEMENT_SPEED, 300, 0));
        player.addEffect(com.malevolentgods.fourtwenty.util.SmokeEffects.quiet(MobEffects.HUNGER, 200, 0));
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        tag.putBoolean("IsLit", isLit);
        tag.putBoolean("ChargePaid", chargePaid);
        tag.putLong("bi", buffer.stored());
        ContainerHelper.saveAllItems(tag, items, registries);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.set(tag.getLong("bi"));
        items = NonNullList.withSize(3, ItemStack.EMPTY);
        // Loading bypasses insertion validation so old invalid contents remain recoverable.
        ContainerHelper.loadAllItems(tag, items, registries);
        burnTime = tag.getInt("BurnTime");
        isLit = tag.getBoolean("IsLit");
        chargePaid = tag.getBoolean("ChargePaid");
        // Saves from before per-fuel burn lengths were always blaze powder sessions.
        burnDuration = tag.contains("BurnDuration") ? tag.getInt("BurnDuration") : BURN_DURATION;
        if (burnDuration <= 0 || burnDuration > BURN_DURATION) burnDuration = BURN_DURATION;
        if (!isLit || burnTime < 0 || burnTime >= burnDuration) extinguish();
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
}
