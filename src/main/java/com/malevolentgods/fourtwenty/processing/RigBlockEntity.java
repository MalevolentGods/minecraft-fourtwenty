package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.Extracts;
import com.malevolentgods.fourtwenty.power.BIBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BINode;
import com.malevolentgods.fourtwenty.power.MachinePower;
import com.malevolentgods.fourtwenty.power.TubeOutput;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/** Runs one step of the extract chain at a time. An input, a slot for Glass Bottles, and two outputs. The extract keeps the strain and traits of what went in. */
public class RigBlockEntity extends BIBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int INPUT = 0, BOTTLE = 1, OUTPUTS = 2, SIZE = 4;
    public static final int DATA_PROGRESS = 0, DATA_TOTAL = 1, DATA_PAUSED = 2, DATA_STORED = 3, DATA_CAPACITY = 4, DATA_OVERLOADED = 5, DATA_FREE = 6, DATA_TUBE_BLOCKED = 7, DATA_COUNT = 8;
    private static final int[] IN = {INPUT, BOTTLE}, OUT = {2, 3};
    private static final int BUFFER_TICKS = 400;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final BIBuffer buffer;
    private final MachinePower.Meter meter = new MachinePower.Meter();
    private final TubeOutput tube = new TubeOutput();
    private int progress;
    private boolean paused, overloaded, free;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> step().map(Extracts.Step::ticks).orElse(0);
                case DATA_PAUSED -> paused ? 1 : 0;
                case DATA_STORED -> (int) buffer.stored();
                case DATA_CAPACITY -> (int) buffer.capacity();
                case DATA_OVERLOADED -> overloaded ? 1 : 0;
                case DATA_FREE -> free ? 1 : 0;
                case DATA_TUBE_BLOCKED -> tube.blocked() ? 1 : 0;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}

        @Override public int getCount() { return DATA_COUNT; }
    };

    public RigBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.RIG_ENTITY.get(), pos, state);
        buffer = new BIBuffer(RigBlock.TIER, Math.max(1, (long) Math.ceil(RigBlock.TIER.machine() * BUFFER_TICKS)), true, false, this::setChanged);
    }

    public BINode node() { return buffer; }

    private static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    /** The step that can run now: enough input, and room for what it makes. */
    private Optional<Extracts.Step> step() {
        ItemStack in = items.get(INPUT);
        if (in.isEmpty()) return Optional.empty();
        return Extracts.step(id(in), items.get(BOTTLE).is(Items.GLASS_BOTTLE)).filter(s -> in.getCount() >= s.count() && room(made(s, in)) != OUTPUTS - 1 + SIZE);
    }

    private static ItemStack made(Extracts.Step step, ItemStack from) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(step.output()));
        return ModDataComponents.bredFrom(new ItemStack(item), from);
    }

    /** The output slot this fits in, or a number past the end if none. */
    private int room(ItemStack stack) {
        for (int i = OUTPUTS; i < SIZE; i++) {
            ItemStack at = items.get(i);
            if (ItemStack.isSameItemSameComponents(at, stack) && at.getCount() < at.getMaxStackSize()) return i;
        }
        for (int i = OUTPUTS; i < SIZE; i++) if (items.get(i).isEmpty()) return i;
        return OUTPUTS - 1 + SIZE;
    }

    @Override public void serverTick() {
        overloaded = overloadTick(buffer);
        if (overloaded) {
            paused = true;
            lit(false);
            return;
        }
        tube.tick(level, worldPosition, this, OUTPUTS);
        Optional<Extracts.Step> step = step();
        if (tube.blocked() || step.isEmpty()) {
            paused = tube.blocked();
            free = false;
            lit(false);
            return;
        }
        free = MachinePower.highNear(level, worldPosition);
        int steps = MachinePower.steps(level, worldPosition, buffer, RigBlock.TIER.machine(), meter);
        paused = steps == 0;
        lit(!paused);
        if (paused) return;
        progress += steps;
        setChanged();
        if (progress >= step.get().ticks()) finish(step.get());
    }

    private void finish(Extracts.Step step) {
        progress = 0;
        ItemStack in = items.get(INPUT);
        ItemStack out = made(step, in);
        int slot = room(out);
        in.shrink(step.count());
        if (step.bottle()) items.get(BOTTLE).shrink(1);
        if (items.get(slot).isEmpty()) items.set(slot, out);
        else items.get(slot).grow(1);
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
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        changed(slot);
    }

    private void changed(int slot) {
        if (slot == INPUT && items.get(INPUT).isEmpty()) progress = 0;
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) {
        return slot == INPUT ? Extracts.refinable(id(stack)) : slot == BOTTLE && stack.is(Items.GLASS_BOTTLE);
    }

    @Override public boolean stillValid(@Nonnull Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override public void clearContent() { items.clear(); }

    @Override public @Nonnull int[] getSlotsForFace(@Nonnull Direction side) { return side == Direction.DOWN ? OUT : IN; }

    @Override public boolean canPlaceItemThroughFace(int slot, @Nonnull ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override public boolean canTakeItemThroughFace(int slot, @Nonnull ItemStack stack, @Nonnull Direction side) {
        return side == Direction.DOWN && slot >= OUTPUTS;
    }

    // Menu.

    @Override public @Nonnull Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new RigMenu(id, inventory, this, data);
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
