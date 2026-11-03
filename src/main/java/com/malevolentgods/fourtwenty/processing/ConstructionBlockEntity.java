package com.malevolentgods.fourtwenty.processing;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.ConstructionRules;
import com.malevolentgods.fourtwenty.power.BIBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BINode;
import com.malevolentgods.fourtwenty.power.MachinePower;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * A Construction bench: a reserve of BI, shown as a bar, that every craft draws from. The Wicks fill a small intake, and the reserve charges
 * from it at a steady rate for the tier; with a high player near, it charges at that rate for nothing.
 */
public class ConstructionBlockEntity extends BIBlockEntity implements MenuProvider {
    public static final int DATA_STORED = 0, DATA_CAPACITY = 1, DATA_YIELD = 2, DATA_OVERLOADED = 3, DATA_STATE = 4, DATA_RATE = 5, DATA_TIER = 6, DATA_COUNT = 7;
    public static final int STATE_FULL = 0, STATE_PAID = 1, STATE_FREE = 2, STATE_WAITING = 3;

    private final ConstructionJointBlock block;
    private final BIBuffer intake;
    private final BIBuffer reserve;
    private boolean overloaded;
    private int state = STATE_WAITING;
    private double owed;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_STORED -> (int) reserve.stored();
                case DATA_CAPACITY -> (int) reserve.capacity();
                case DATA_YIELD -> ConstructionRules.factor(tier());
                case DATA_OVERLOADED -> overloaded ? 1 : 0;
                case DATA_STATE -> state;
                case DATA_RATE -> (int) Math.round(ConstructionRules.chargeRate(tier()) * BIRules.TICKS_PER_SECOND);
                case DATA_TIER -> tier().ordinal();
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}

        @Override public int getCount() { return DATA_COUNT; }
    };

    public ConstructionBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.CONSTRUCTION_ENTITY.get(), pos, state);
        block = (ConstructionJointBlock) state.getBlock();
        intake = new BIBuffer(block.tier(), ConstructionRules.intake(block.tier()), true, false, this::setChanged);
        reserve = new BIBuffer(block.tier(), ConstructionRules.CAPACITY, false, false, this::setChanged);
    }

    /** What the Wicks fill. */
    public BINode node() { return intake; }

    /** The bar crafts draw from. */
    public BIBuffer reserve() { return reserve; }

    public BIRules.Tier tier() { return block.tier(); }

    public boolean overloaded() { return overloaded; }

    /** Whether the reserve covers a craft costing {@code cost}. */
    public boolean affords(long cost) { return !overloaded && reserve.stored() >= cost; }

    public void pay(long cost) { reserve.extract(cost); }

    @Override public void serverTick() {
        overloaded = overloadTick(intake);
        if (!overloaded) charge();
        lit(!overloaded && reserve.stored() > 0);
    }

    private void charge() {
        long room = reserve.capacity() - reserve.stored();
        if (room <= 0) {
            state = STATE_FULL;
            owed = 0;
            return;
        }
        owed += ConstructionRules.chargeRate(tier());
        long due = Math.min(room, (long) owed);
        owed -= (long) owed;
        if (MachinePower.highNear(level, worldPosition)) {
            state = STATE_FREE;
            reserve.receive(due);
        } else {
            long got = intake.extract(due);
            state = got > 0 || due == 0 && intake.stored() > 0 ? STATE_PAID : STATE_WAITING;
            reserve.receive(got);
        }
    }

    @Override public @Nonnull Component getDisplayName() { return block.getName(); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new ConstructionMenu(id, inventory, ContainerLevelAccess.create(level, worldPosition), this, data);
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("bi", reserve.stored());
        tag.putLong("intake", intake.stored());
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reserve.set(tag.getLong("bi"));
        intake.set(tag.getLong("intake"));
    }
}
