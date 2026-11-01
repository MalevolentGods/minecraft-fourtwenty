package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * Two banks, one for each way power can go through it, so what came in on one side can't bounce straight back. A face only takes power
 * when something past the other side can use it: otherwise a Stash would empty itself into the box with nowhere for the power to go.
 */
public class HitBoxBlockEntity extends BIBlockEntity implements MenuProvider {
    private static final long BUFFER = 4_096;

    /** A face of the box: takes power into one bank at that face's tier. */
    private final class Face implements BINode {
        private final boolean high;

        Face(boolean high) { this.high = high; }

        private BIBuffer bank() { return high ? down : up; }

        @Override public BIRules.Tier tier() { return high ? highTier() : highTier().below(); }
        @Override public long stored() { return bank().stored(); }
        @Override public long capacity() { return bank().capacity(); }
        @Override public long receive(long amount) { return (high ? pushDown : pushUp).hasSinks() ? bank().receive(amount) : 0; }
        @Override public long extract(long amount) { return 0; }
        @Override public boolean canReceive() { return true; }
        @Override public boolean canExtract() { return false; }
        @Override public void overload(long now) { down.overload(now); }
    }

    private final BIBuffer down = new BIBuffer(BIRules.Tier.RIP, BUFFER, true, true, this::setChanged);
    private final BIBuffer up = new BIBuffer(BIRules.Tier.RIP, BUFFER, true, true, this::setChanged);
    private final Face highFace = new Face(true), lowFace = new Face(false);
    private final BIPush pushDown = new BIPush(), pushUp = new BIPush();
    private final ContainerData data = BIBlockEntity.data(down, this::gameTime, () -> highTier().ordinal(), BIBlockEntity.KIND_HIT_BOX, 0, () -> 0);

    public HitBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.HIT_BOX_ENTITY.get(), pos, state);
    }

    private long gameTime() { return level == null ? 0 : level.getGameTime(); }

    private BIRules.Tier highTier() { return getBlockState().getValue(HitBoxBlock.TIER); }

    private Direction front() { return getBlockState().getValue(HitBoxBlock.FACING); }

    public BINode node(Direction side) {
        if (side == null) return null;
        return side == front() ? highFace : lowFace;
    }

    public void toggle() {
        BIRules.Tier high = highTier();
        BIRules.Tier next = high == BIRules.Tier.TOKE ? BIRules.Tier.RIP : high == BIRules.Tier.RIP ? BIRules.Tier.BLAZE : BIRules.Tier.TOKE;
        level.setBlock(worldPosition, getBlockState().setValue(HitBoxBlock.TIER, next), 3);
        BIGrid.changed(level);
    }

    @Override public void serverTick() {
        if (overloadTick(down)) {
            lit(false);
            return;
        }
        Direction front = front();
        List<Direction> low = Arrays.stream(Direction.values()).filter(d -> d != front).toList();
        BIRules.Tier high = highTier();
        pushDown.push(level, worldPosition, low, high.below(), high.rate(), down, sink -> true);
        pushUp.push(level, worldPosition, List.of(front), high, high.rate(), up, sink -> true);
        lit(down.stored() > 0 || up.stored() > 0);
    }

    @Override public @Nonnull Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new com.malevolentgods.fourtwenty.power.BIMenu(id, this, data);
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("down", down.stored());
        tag.putLong("up", up.stored());
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        down.set(tag.getLong("down"));
        up.set(tag.getLong("up"));
    }
}
