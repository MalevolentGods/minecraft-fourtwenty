package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * Two banks, one for each way power can go through it, so what came in on one side can't bounce straight back. A face only takes power
 * when something past the other side can use it: otherwise a Stash would empty itself into the box with nowhere for the power to go.
 */
public class HitBoxBlockEntity extends BIBlockEntity {
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
    }

    private final BIBuffer down = new BIBuffer(BIRules.Tier.RIP, BUFFER, true, true, this::setChanged);
    private final BIBuffer up = new BIBuffer(BIRules.Tier.RIP, BUFFER, true, true, this::setChanged);
    private final Face highFace = new Face(true), lowFace = new Face(false);
    private final BIPush pushDown = new BIPush(), pushUp = new BIPush();

    public HitBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.HIT_BOX_ENTITY.get(), pos, state);
    }

    private BIRules.Tier highTier() { return getBlockState().getValue(HitBoxBlock.TIER); }

    private Direction front() { return getBlockState().getValue(HitBoxBlock.FACING); }

    public BINode node(Direction side) {
        if (side == null) return null;
        return side == front() ? highFace : lowFace;
    }

    @Override public void serverTick() {
        Direction front = front();
        List<Direction> low = Arrays.stream(Direction.values()).filter(d -> d != front).toList();
        pushDown.push(level, worldPosition, low, highTier().below(), down, sink -> true);
        pushUp.push(level, worldPosition, List.of(front), highTier(), up, sink -> true);
        lit(down.stored() > 0 || up.stored() > 0);
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
