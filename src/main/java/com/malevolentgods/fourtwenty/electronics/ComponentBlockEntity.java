package com.malevolentgods.fourtwenty.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * A component's settings and memory: its value (which step of its type's values), whether a switch is closed, and what its part
 * remembers between ticks (a Kief Cap's charge, a Nug's output and pending change), saved with the world so a circuit carries on after a
 * chunk reloads or the server restarts.
 */
public class ComponentBlockEntity extends BlockEntity {
    private int index = -1;
    private boolean closed;
    private double[] memory = new double[0];

    public ComponentBlockEntity(BlockPos pos, BlockState state) {
        super(ModElectronics.COMPONENT_ENTITY.get(), pos, state);
    }

    public ComponentType type() { return getBlockState().getBlock() instanceof ComponentBlock c ? c.type() : null; }

    /** Its value, in its type's unit (Φ, Ga, Ma, Kb or seconds). */
    public double value() {
        ComponentType t = type();
        if (t == null || t.values.isEmpty()) return 0;
        if (index < 0 || index >= t.values.size()) index = t.defaultIndex();
        return t.values.get(index);
    }

    /** Which of its type's values it's at. */
    public int index() {
        value();
        return index;
    }

    public void step(int by) {
        ComponentType t = type();
        if (t == null || t.values.isEmpty()) return;
        value();
        index = Math.floorMod(index + by, t.values.size());
        setChanged();
    }

    /** A Resinator placed before its bands showed its value catches its bands up when it loads. */
    @Override public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (state.hasProperty(ResinatorBlock.VALUE) && state.getValue(ResinatorBlock.VALUE) != index()) {
            level.setBlock(worldPosition, state.setValue(ResinatorBlock.VALUE, index()), 3);
        }
    }

    public boolean closed() { return closed; }

    public void closed(boolean closed) {
        this.closed = closed;
        setChanged();
    }

    public double[] memory() { return memory; }

    /** What its part remembers, after a tick; saved with the world. */
    public void memory(double[] memory) {
        this.memory = memory == null ? new double[0] : memory.clone();
        setChanged();
    }

    /** A value with an SI prefix and its unit: 4.7 kΦ, 1 mGa, 0.5 s. */
    public static String format(double v, String unit) {
        if (v == 0) return "0 " + unit;
        String[] prefixes = {"p", "n", "µ", "m", "", "k", "M", "G"};
        int p = 4;
        double x = v;
        while (Math.abs(x) >= 1000 && p < prefixes.length - 1) {
            x /= 1000;
            p++;
        }
        while (Math.abs(x) < 1 && p > 0) {
            x *= 1000;
            p--;
        }
        String num = Math.abs(x - Math.round(x)) < 1e-6 ? Long.toString(Math.round(x)) : String.format(java.util.Locale.ROOT, "%.2g", x);
        return num + " " + prefixes[p] + unit;
    }

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("value", index);
        tag.putBoolean("closed", closed);
        ListTag list = new ListTag();
        for (double d : memory) list.add(DoubleTag.valueOf(d));
        tag.put("memory", list);
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        index = tag.contains("value") ? tag.getInt("value") : -1;
        closed = tag.getBoolean("closed");
        ListTag list = tag.getList("memory", Tag.TAG_DOUBLE);
        memory = new double[list.size()];
        for (int i = 0; i < memory.length; i++) memory[i] = list.getDouble(i);
    }
}
