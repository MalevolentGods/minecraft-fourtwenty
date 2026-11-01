package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.CureRules;
import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/** Tier 1 curing: each of the eight spots holds one bud on its own clock, so a part-full rack can be topped up. */
public class CuringRackBlockEntity extends BlockEntity {
    public static final int SLOTS = 8;
    private final NonNullList<ItemStack> buds = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final int[] ticks = new int[SLOTS];

    public CuringRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CURING_RACK.get(), pos, state);
    }

    public NonNullList<ItemStack> buds() { return buds; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CuringRackBlockEntity rack) {
        CureRules.Tier tier = ProcessingConfig.rack();
        boolean changed = false;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack bud = rack.buds.get(i);
            if (!bud.is(ModItems.TRIMMED_BUD.get())) continue;
            if (CureRules.cured(++rack.ticks[i], tier)) {
                rack.buds.set(i, ModDataComponents.withStrain(new ItemStack(ModItems.WEED_BUD.get()), ModDataComponents.strain(bud)));
                changed = true;
            }
        }
        if (changed) rack.changed();
        else if (level.getGameTime() % 100 == 0) rack.setChanged();     // the clocks, saved now and then
    }

    /** Hangs one bud; false if the rack is full. */
    public boolean hang(ItemStack bud) {
        for (int i = 0; i < SLOTS; i++) {
            if (!buds.get(i).isEmpty()) continue;
            buds.set(i, bud.copyWithCount(1));
            ticks[i] = 0;
            changed();
            return true;
        }
        return false;
    }

    /** Takes one bud down, a cured one if {@code cured}, otherwise any; empty if there's none. */
    public ItemStack take(boolean cured) {
        for (int i = 0; i < SLOTS; i++) {
            if (buds.get(i).isEmpty() || (cured && !buds.get(i).is(ModItems.WEED_BUD.get()))) continue;
            ItemStack out = buds.set(i, ItemStack.EMPTY);
            ticks[i] = 0;
            changed();
            return out;
        }
        return ItemStack.EMPTY;
    }

    /** The minutes until the next bud is ready, or -1 with nothing curing. */
    public int minutesLeft() {
        CureRules.Tier tier = ProcessingConfig.rack();
        int best = -1;
        for (int i = 0; i < SLOTS; i++) {
            if (!buds.get(i).is(ModItems.TRIMMED_BUD.get())) continue;
            int left = CureRules.minutesLeft(ticks[i], tier);
            if (best < 0 || left < best) best = left;
        }
        return best;
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, buds, registries);
        tag.putIntArray("ticks", ticks);
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buds.clear();
        ContainerHelper.loadAllItems(tag, buds, registries);
        int[] saved = tag.getIntArray("ticks");
        System.arraycopy(saved, 0, ticks, 0, Math.min(saved.length, SLOTS));
    }

    @Override public @Nonnull CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
