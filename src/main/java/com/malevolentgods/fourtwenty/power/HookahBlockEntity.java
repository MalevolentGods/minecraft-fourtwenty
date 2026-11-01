package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Burns weed into BI at its tier's rate while there's room to hold it, and pushes it down the Wicks. Fuel goes in from the top and sides. */
public class HookahBlockEntity extends BIBlockEntity implements WorldlyContainer {
    private static final int[] FUEL_SLOT = {0}, NONE = {};
    private static final int BUFFER_TICKS = 125;
    private static final List<Direction> ALL = List.of(Direction.values());

    private final NonNullList<ItemStack> fuel = NonNullList.withSize(1, ItemStack.EMPTY);
    private final BIRules.Tier tier;
    private final BIBuffer buffer;
    private final BIPush push = new BIPush();
    private long burning;

    public HookahBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.HOOKAH_ENTITY.get(), pos, state);
        tier = ((HookahBlock) state.getBlock()).tier();
        buffer = new BIBuffer(tier, (long) tier.rate() * BUFFER_TICKS, false, true, this::setChanged);
    }

    public BINode node() { return buffer; }

    @Override public void serverTick() {
        if (burning <= 0 && buffer.stored() < buffer.capacity()) {
            ItemStack stack = fuel.get(0);
            long value = ProcessingConfig.fuel(stack);
            if (value > 0) {
                stack.shrink(1);
                burning = value;
                setChanged();
            }
        }
        if (burning > 0) {
            long made = Math.min(tier.rate(), Math.min(burning, buffer.capacity() - buffer.stored()));
            if (made > 0) {
                burning -= made;
                buffer.set(buffer.stored() + made);
                setChanged();
            }
        }
        lit(burning > 0);
        push.push(level, worldPosition, ALL, tier, buffer, node -> true);
    }

    /** Takes what fits of the stack in hand. */
    public void fuel(ItemStack stack) {
        ItemStack slot = fuel.get(0);
        if (slot.isEmpty()) {
            fuel.set(0, stack.split(stack.getCount()));
        } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
            int moved = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
            slot.grow(moved);
            stack.shrink(moved);
        }
        setChanged();
    }

    // Container: fuel in from the top and sides, nothing out.

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return fuel.get(0).isEmpty(); }
    @Override public @Nonnull ItemStack getItem(int slot) { return fuel.get(slot); }
    @Override public @Nonnull ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(fuel, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }
    @Override public @Nonnull ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(fuel, slot); }
    @Override public void setItem(int slot, @Nonnull ItemStack stack) {
        fuel.set(slot, stack);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) { return ProcessingConfig.fuel(stack) > 0; }
    @Override public boolean stillValid(@Nonnull Player player) { return false; }
    @Override public void clearContent() { fuel.clear(); }
    @Override public @Nonnull int[] getSlotsForFace(@Nonnull Direction side) { return side == Direction.DOWN ? NONE : FUEL_SLOT; }
    @Override public boolean canPlaceItemThroughFace(int slot, @Nonnull ItemStack stack, @Nullable Direction side) { return side != Direction.DOWN && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, @Nonnull ItemStack stack, @Nonnull Direction side) { return false; }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, fuel, registries);
        tag.putLong("bi", buffer.stored());
        tag.putLong("burning", burning);
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel.clear();
        ContainerHelper.loadAllItems(tag, fuel, registries);
        buffer.set(tag.getLong("bi"));
        burning = tag.getLong("burning");
    }
}
