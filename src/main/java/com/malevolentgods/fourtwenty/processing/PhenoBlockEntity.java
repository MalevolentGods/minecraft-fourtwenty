package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.Climate;
import com.malevolentgods.fourtwenty.domain.Crosses;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
import com.malevolentgods.fourtwenty.power.BIBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BINode;
import com.malevolentgods.fourtwenty.power.MachinePower;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Crosses two seeds. Two parent slots and two outputs; hoppers reach the parents from the top and sides and the outputs from the bottom. One seed of each
 * parent goes in, one comes out. A full output stops it, and changing a parent starts the cross over.
 */
public class PhenoBlockEntity extends BIBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int PARENTS = 2, OUTPUTS = 2, SIZE = PARENTS + OUTPUTS;
    public static final int DATA_PROGRESS = 0, DATA_TOTAL = 1, DATA_PAUSED = 2, DATA_STORED = 3, DATA_CAPACITY = 4, DATA_OVERLOADED = 5, DATA_TIER = 6, DATA_FREE = 7,
            DATA_NEEDS = 8, DATA_MAKES = 9, DATA_TUBE_BLOCKED = 10, DATA_COUNT = 11;
    /** The DATA_NEEDS value for parents that only pollinating can cross. */
    public static final int NEEDS_POLLINATING = 99;
    private static final int[] IN = {0, 1}, OUT = {2, 3};
    private static final int BUFFER_TICKS = 400;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final PhenoBlock block;
    private final BIBuffer buffer;
    private final MachinePower.Meter meter = new MachinePower.Meter();
    private int progress;
    private boolean paused, overloaded, free;
    private final com.malevolentgods.fourtwenty.power.TubeOutput tube = new com.malevolentgods.fourtwenty.power.TubeOutput();

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> loaded() ? ticks() : 0;
                case DATA_PAUSED -> paused ? 1 : 0;
                case DATA_STORED -> (int) buffer.stored();
                case DATA_CAPACITY -> (int) buffer.capacity();
                case DATA_OVERLOADED -> overloaded ? 1 : 0;
                case DATA_TIER -> block.tier().ordinal();
                case DATA_FREE -> free ? 1 : 0;
                case DATA_TUBE_BLOCKED -> tube.blocked() ? 1 : 0;
                case DATA_NEEDS -> !loaded() ? -1 : pollinates() ? NEEDS_POLLINATING : !strongEnough() ? Crosses.chamber(grade()).ordinal() : -1;
                case DATA_MAKES -> settling() != null ? settling().strain().ordinal()
                        : loaded() && !pollinates() ? Crosses.hybrid(first(), second()).map(Enum::ordinal).orElse(-1) : -1;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}

        @Override public int getCount() { return DATA_COUNT; }
    };

    public PhenoBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.PHENO_ENTITY.get(), pos, state);
        block = (PhenoBlock) state.getBlock();
        buffer = new BIBuffer(block.tier(), Math.max(1, (long) Math.ceil(block.tier().machine() * BUFFER_TICKS)), true, false, this::setChanged);
    }

    public BINode node() { return buffer; }

    public int progress() { return progress; }

    private boolean loaded() { return !items.get(0).isEmpty() && !items.get(1).isEmpty() || settling() != null; }

    /** One climate-touched seed alone: the climate it's settling into a biome strain, or null. */
    private @Nullable Climate settling() {
        ItemStack a = items.get(0), b = items.get(1);
        return a.isEmpty() == b.isEmpty() ? null : (a.isEmpty() ? b : a).get(ModDataComponents.CLIMATE.get());
    }

    private Strain first() { return ModDataComponents.strain(items.get(0)); }

    private Strain second() { return ModDataComponents.strain(items.get(1)); }

    private int grade() { return settling() != null ? settling().strain().grade() : Crosses.grade(first(), second()); }

    private int ticks() { return ProcessingConfig.crossTicks(grade()); }

    private boolean strongEnough() { return Crosses.chamber(grade()).ordinal() <= block.tier().ordinal(); }

    private boolean pollinates() { return settling() == null && Crosses.needsPollinating(first(), second()); }

    private boolean outputFree() { return items.get(PARENTS).isEmpty() || items.get(PARENTS + 1).isEmpty(); }

    private void idle() {
        paused = false;
        free = false;
        lit(false);
    }

    @Override public void serverTick() {
        overloaded = overloadTick(buffer);
        if (overloaded) {
            paused = true;
            lit(false);
            return;
        }
        tube.tick(level, worldPosition, this, PARENTS);
        if (tube.blocked()) {
            paused = true;
            free = false;
            lit(false);
            return;
        }
        if (!loaded() || pollinates() || !strongEnough() || !outputFree()) {
            idle();
            return;
        }
        free = MachinePower.highNear(level, worldPosition);
        int steps = MachinePower.steps(level, worldPosition, buffer, block.tier().machine(), meter);
        paused = steps == 0;
        lit(!paused);
        if (paused) return;
        progress += steps;
        setChanged();
        if (progress >= ticks()) finish();
    }

    private void finish() {
        progress = 0;
        Climate climate = settling();
        ItemStack only = climate == null ? null : items.get(items.get(0).isEmpty() ? 1 : 0);
        Strain strain = climate != null ? climate.strain() : Crosses.child(first(), second(), level.random::nextDouble);
        Traits traits = climate != null ? ModDataComponents.traits(only)
                : Traits.cross(ModDataComponents.traits(items.get(0)), ModDataComponents.traits(items.get(1)), level.random::nextDouble);
        items.get(0).shrink(1);
        items.get(1).shrink(1);
        ItemStack seed = ModDataComponents.withTraits(ModDataComponents.withStrain(new ItemStack(ModItems.WEED_SEEDS.get()), strain), traits);
        items.set(items.get(PARENTS).isEmpty() ? PARENTS : PARENTS + 1, seed);
    }

    // Container.

    @Override public int getContainerSize() { return SIZE; }

    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override public @Nonnull ItemStack getItem(int slot) { return items.get(slot); }

    @Override public @Nonnull ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) changed(slot);
        return out;
    }

    @Override public @Nonnull ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = ContainerHelper.takeItem(items, slot);
        if (!out.isEmpty()) changed(slot);
        return out;
    }

    @Override public void setItem(int slot, @Nonnull ItemStack stack) {
        boolean same = ItemStack.isSameItemSameComponents(items.get(slot), stack);
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        if (same) setChanged();
        else changed(slot);
    }

    private void changed(int slot) {
        if (slot < PARENTS && items.get(slot).isEmpty()) progress = 0;
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) { return slot < PARENTS && stack.is(ModItems.WEED_SEEDS.get()); }

    @Override public boolean stillValid(@Nonnull Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override public void clearContent() { items.clear(); }

    @Override public @Nonnull int[] getSlotsForFace(@Nonnull Direction side) { return side == Direction.DOWN ? OUT : IN; }

    @Override public boolean canPlaceItemThroughFace(int slot, @Nonnull ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack) && (items.get(slot).isEmpty() || ItemStack.isSameItemSameComponents(items.get(slot), stack));
    }

    @Override public boolean canTakeItemThroughFace(int slot, @Nonnull ItemStack stack, @Nonnull Direction side) {
        return side == Direction.DOWN && slot >= PARENTS;
    }

    // Menu.

    @Override public @Nonnull Component getDisplayName() { return block.getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new PhenoMenu(id, inventory, this, data);
    }

    // Saving.

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("progress", progress);
        tag.putLong("bi", buffer.stored());
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("progress");
        buffer.set(tag.getLong("bi"));
    }
}
