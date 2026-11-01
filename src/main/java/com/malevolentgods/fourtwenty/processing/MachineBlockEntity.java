package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.CureRules;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.TrimRules;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A powered trimming or curing machine. Four input slots and six outputs, shared with seeds; hoppers and pipes reach the inputs from the top
 * and sides and the outputs from the bottom. Each input slot holds one strain, so strains never mix. A full output stops it and loses nothing.
 */
public class MachineBlockEntity extends BIBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int INPUTS = 4, OUTPUTS = 6, SIZE = INPUTS + OUTPUTS;
    public static final int DATA_PROGRESS = 0, DATA_TOTAL = 1, DATA_PAUSED = 2, DATA_STORED = 3, DATA_CAPACITY = 4, DATA_OVERLOADED = 5, DATA_TIER = 6, DATA_JOB = 7, DATA_COUNT = 8;
    private static final int[] IN = {0, 1, 2, 3}, OUT = {4, 5, 6, 7, 8, 9};
    private static final int BUFFER_TICKS = 400;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final MachineBlock block;
    private final BIBuffer buffer;
    private final MachinePower.Meter meter = new MachinePower.Meter();
    private int progress;
    private boolean paused, overloaded;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> ticks();
                case DATA_PAUSED -> paused ? 1 : 0;
                case DATA_STORED -> (int) buffer.stored();
                case DATA_CAPACITY -> (int) buffer.capacity();
                case DATA_OVERLOADED -> overloaded ? 1 : 0;
                case DATA_TIER -> block.tier().ordinal();
                case DATA_JOB -> block.job().ordinal();
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}

        @Override public int getCount() { return DATA_COUNT; }
    };

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.MACHINE_ENTITY.get(), pos, state);
        block = (MachineBlock) state.getBlock();
        buffer = new BIBuffer(block.tier(), Math.max(1, (long) Math.ceil(block.tier().machine() * BUFFER_TICKS)), true, false, this::setChanged);
    }

    public BINode node() { return buffer; }

    public MachineBlock block() { return block; }

    public int progress() { return progress; }

    /** For tests: 1 while overloaded. */
    public int createDataForTest() { return data.get(DATA_OVERLOADED); }

    private boolean trims() { return block.job() == MachineBlock.Job.TRIM; }

    private CureRules.Tier cure() { return ProcessingConfig.cure(block.index()); }

    private TrimRules.Tier trim() { return ProcessingConfig.trim(block.index()); }

    private int ticks() { return trims() ? trim().ticks() : cure().ticks(); }

    private double cost() { return trims() ? trim().cost() : cure().cost(); }

    private Item input() { return trims() ? ModItems.ROUGH_BUD.get() : ModItems.TRIMMED_BUD.get(); }

    private Item output() { return trims() ? ModItems.TRIMMED_BUD.get() : ModItems.WEED_BUD.get(); }

    /** The first input slot whose result has room, or -1. */
    private int job() {
        for (int i = 0; i < INPUTS; i++) {
            ItemStack in = items.get(i);
            if (in.isEmpty()) continue;
            Strain strain = ModDataComponents.strain(in);
            List<ItemStack> out = new ArrayList<>();
            out.add(strained(output(), batch(in), strain));
            if (trims()) out.add(strained(ModItems.WEED_SEEDS.get(), 1, strain));
            if (fits(out)) return i;
        }
        return -1;
    }

    private int batch(ItemStack in) {
        return trims() ? 1 : Math.min(in.getCount(), cure().batch());
    }

    private static ItemStack strained(Item item, int count, Strain strain) {
        return ModDataComponents.withStrain(new ItemStack(item, count), strain);
    }

    /** Whether all of these would go into the outputs at once. */
    private boolean fits(List<ItemStack> stacks) {
        NonNullList<ItemStack> copy = NonNullList.withSize(OUTPUTS, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUTS; i++) copy.set(i, items.get(INPUTS + i).copy());
        for (ItemStack stack : stacks) if (!insert(copy, 0, stack.copy()).isEmpty()) return false;
        return true;
    }

    /** Puts a stack into {@code list}; what's left over comes back. */
    private static ItemStack insert(List<ItemStack> list, int offset, ItemStack stack) {
        for (int i = offset; i < list.size() && !stack.isEmpty(); i++) {
            ItemStack at = list.get(i);
            if (ItemStack.isSameItemSameComponents(at, stack)) {
                int moved = Math.min(stack.getCount(), at.getMaxStackSize() - at.getCount());
                at.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int i = offset; i < list.size() && !stack.isEmpty(); i++) {
            if (list.get(i).isEmpty()) {
                list.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack;
    }

    @Override public void serverTick() {
        overloaded = overloadTick(buffer);
        if (overloaded) {
            paused = true;
            lit(false);
            return;
        }
        int slot = job();
        if (slot < 0) {
            paused = false;
            lit(false);
            return;
        }
        int steps = MachinePower.steps(level, worldPosition, buffer, cost(), meter);
        paused = steps == 0;
        lit(!paused);
        if (paused) return;
        progress += steps;
        setChanged();
        if (progress < ticks()) return;
        progress = 0;
        ItemStack in = items.get(slot);
        Strain strain = ModDataComponents.strain(in);
        int count = batch(in);
        in.shrink(count);
        insert(items, INPUTS, strained(output(), count, strain));
        if (trims() && TrimRules.seeded(ProcessingConfig.chances(), strain, level.random.nextDouble())) {
            insert(items, INPUTS, strained(ModItems.WEED_SEEDS.get(), 1, strain));
        }
    }

    // Container.

    @Override public int getContainerSize() { return SIZE; }

    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override public @Nonnull ItemStack getItem(int slot) { return items.get(slot); }

    @Override public @Nonnull ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }

    @Override public @Nonnull ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override public void setItem(int slot, @Nonnull ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) { return slot < INPUTS && stack.is(input()); }

    @Override public boolean stillValid(@Nonnull Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override public void clearContent() { items.clear(); }

    @Override public @Nonnull int[] getSlotsForFace(@Nonnull Direction side) { return side == Direction.DOWN ? OUT : IN; }

    @Override public boolean canPlaceItemThroughFace(int slot, @Nonnull ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override public boolean canTakeItemThroughFace(int slot, @Nonnull ItemStack stack, @Nonnull Direction side) {
        return side == Direction.DOWN && slot >= INPUTS;
    }

    // Menu.

    @Override public @Nonnull Component getDisplayName() { return block.getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new MachineMenu(id, inventory, this, data);
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
