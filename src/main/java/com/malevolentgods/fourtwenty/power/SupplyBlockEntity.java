package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.CircuitRules;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A creative supply: a Bottomless Stash (its tier's voltage, ten Hookahs' current, never empty) or the Bench Rig (a variable DC supply:
 * the voltage and current limit it's set to, holding that current past the limit, as a bench supply does). Both give energy from
 * nowhere: test tools for building circuits.
 */
public class SupplyBlockEntity extends BIBlockEntity {
    /** The Bench Rig's voltages, in Kb, and current limits, in Ig. */
    public static final List<Double> VOLTS = List.of(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.3, 4.2, 5.0, 6.0, 9.0, 12.0, 24.0, 42.0, 100.0, 420.0, 4200.0);
    public static final List<Double> AMPS = List.of(0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 1000.0);
    private static final int HOOKAHS = 10;

    private int volts = VOLTS.indexOf(4.2), amps = AMPS.indexOf(1.0);
    private long overloadedUntil;

    private final BINode node = new BINode() {
        @Override public BIRules.Tier tier() { return block().tier(); }
        @Override public long stored() { return Long.MAX_VALUE / 4; }
        @Override public long capacity() { return Long.MAX_VALUE / 4; }
        @Override public long receive(long amount) { return amount; }
        @Override public long extract(long amount) { return amount; }
        @Override public boolean canReceive() { return false; }
        @Override public boolean canExtract() { return true; }
        @Override public Role role() { return Role.SOURCE; }
        @Override public boolean sourceReady() { return true; }
        @Override public double sourceWatts() { return HOOKAHS * CircuitRules.hookahWatts(tier()); }
        @Override public double emf() { return rig() ? VOLTS.get(volts) : Double.NaN; }
        @Override public double maxAmps() { return rig() ? AMPS.get(amps) : Double.NaN; }
        @Override public void overload(long now) { overloadedUntil = now + 40; }
        @Override public boolean overloaded(long now) { return now < overloadedUntil; }
    };

    public SupplyBlockEntity(BlockPos pos, BlockState state) {
        super(ModPower.SUPPLY_ENTITY.get(), pos, state);
    }

    private SupplyBlock block() { return (SupplyBlock) getBlockState().getBlock(); }

    private boolean rig() { return block().rig(); }

    public BINode node() { return node; }

    public double volts() { return VOLTS.get(volts); }

    public double amps() { return AMPS.get(amps); }

    public void stepVolts(int by) {
        volts = Math.floorMod(volts + by, VOLTS.size());
        setChanged();
    }

    public void stepAmps(int by) {
        amps = Math.floorMod(amps + by, AMPS.size());
        setChanged();
    }

    @Override public void serverTick() {
        lit(true);
        Circuits.tick(level, node, worldPosition);
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("volts", volts);
        tag.putInt("amps", amps);
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("volts")) volts = Math.max(0, Math.min(VOLTS.size() - 1, tag.getInt("volts")));
        if (tag.contains("amps")) amps = Math.max(0, Math.min(AMPS.size() - 1, tag.getInt("amps")));
    }
}
