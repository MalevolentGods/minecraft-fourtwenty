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
 * The transformer: two windings, solved with both their circuits as one two-port (docs/electronics/audit-and-plan.md 2.1), turns ratio the
 * two tiers' voltages, loss its winding resistance. It stores nothing; what goes in one side comes out the other, less its loss, in the
 * same tick. (Its old banks are kept only so saves load.)
 */
public class HitBoxBlockEntity extends BIBlockEntity implements MenuProvider {
    private static final long BUFFER = 24_576;

    /**
     * A face of the box at that face's tier. It gives from one bank when its own side wants power, and otherwise takes into the other bank
     * when the far side wants it, losing a little: a converter between tiers with an energy store on each side.
     */
    private final class Face implements BINode {
        private final boolean high;

        Face(boolean high) { this.high = high; }

        /** Power that came in this face goes to this bank, to leave by the other face. */
        private BIBuffer in() { return high ? down : up; }

        /** Power leaving by this face comes from this bank. */
        private BIBuffer out() { return high ? up : down; }

        private Face other() { return high ? lowFace : highFace; }

        private double rate() { return BIRules.watts(highTier().rate()); }

        @Override public BIRules.Tier tier() { return high ? highTier() : highTier().below(); }
        @Override public long stored() { return in().stored(); }
        @Override public long capacity() { return in().capacity(); }
        @Override public long receive(long amount) { return in().receive(amount); }
        @Override public long extract(long amount) { return out().extract(amount); }
        // a winding: passive, its power flows straight through the two-port the circuit solves (Network: coupled windings)
        @Override public boolean canReceive() { return false; }
        @Override public boolean canExtract() { return false; }
        @Override public void overload(long now) { down.overload(now); }
        @Override public boolean overloaded(long now) { return down.overloaded(now); }
        @Override public double efficiency() { return 1; }
        @Override public double sourceWatts() { return 0; }
        @Override public boolean sourceReady() { return false; }
        @Override public double demandWatts() { return 0; }
        @Override public Role role() { return Role.NONE; }
        @Override public BINode coupled() { return other(); }
        @Override public boolean primary() { return high; }
    }

    private final BIBuffer down = new BIBuffer(BIRules.Tier.RIP, BUFFER, true, true, this::setChanged);
    private final BIBuffer up = new BIBuffer(BIRules.Tier.RIP, BUFFER, true, true, this::setChanged);
    private final Face highFace = new Face(true), lowFace = new Face(false);
    private final ContainerData data = BIBlockEntity.data(down, this::gameTime, () -> highTier().ordinal(), BIBlockEntity.KIND_HIT_BOX, 0, () -> 0);

    public HitBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.HIT_BOX_ENTITY.get(), pos, state);
    }

    private long gameTime() { return level == null ? 0 : level.getGameTime(); }

    private BIRules.Tier highTier() { return getBlockState().getValue(HitBoxBlock.TIER); }

    private Direction front() { return getBlockState().getValue(HitBoxBlock.FACING); }

    /** Its two windings: the high one on its front and back, the low one on its left and right; top and bottom have none. */
    public BINode node(Direction side) {
        if (side == null || !side.getAxis().isHorizontal()) return null;
        return com.malevolentgods.fourtwenty.domain.Terminals.highWinding(front(), side) != null ? highFace : lowFace;
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
        Circuits.tick(level, highFace, worldPosition);
        lit(Circuits.meter(level, highFace, worldPosition)[1] > 1e-6);
    }

    @Override public @Nonnull Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new com.malevolentgods.fourtwenty.power.BIMenu(id, this, data);
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ws_down", down.stored());
        tag.putLong("ws_up", up.stored());
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        down.set(BIBuffer.loadWs(tag, "ws_down", "down"));
        up.set(BIBuffer.loadWs(tag, "ws_up", "up"));
    }
}
