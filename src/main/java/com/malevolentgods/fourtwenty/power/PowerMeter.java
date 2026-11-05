package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.Units;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/** Two extra data slots on a BI screen: the voltage and current at the block, from its circuit, sent as float bits. */
public final class PowerMeter {
    public static final int SLOTS = 3;

    private PowerMeter() {}

    /** A menu that shows the meter line under its gauge. */
    public interface Shown {
        ContainerData meter();
    }

    /** The server's live slots for {@code owner}, or empty slots for the client to fill. */
    public static ContainerData of(@Nullable Object owner) {
        if (!(owner instanceof BlockEntity be) || be.getLevel() == null || be.getLevel().isClientSide) return new SimpleContainerData(SLOTS);
        return new ContainerData() {
            @Override public int get(int index) {
                float[] m = read(be);
                return Float.floatToIntBits(index >= 0 && index < m.length ? m[index] : 0);
            }

            @Override public void set(int index, int value) {}

            @Override public int getCount() { return SLOTS; }
        };
    }

    private static float[] read(BlockEntity be) {
        var level = be.getLevel();
        for (Direction d : Direction.values()) {
            BINode node = level.getCapability(BINode.CAPABILITY, be.getBlockPos(), d);
            if (node != null) return Circuits.meter(level, node, be.getBlockPos());
        }
        return new float[SLOTS];
    }

    /** "4.20 Kb · 25.0 Ig · 168 mΦ · 105 W" from a menu's meter slots; the resistance is V / I. */
    public static Component line(ContainerData meter) {
        float volts = Float.intBitsToFloat(meter.get(0)), amps = Float.intBitsToFloat(meter.get(1)), watts = Float.intBitsToFloat(meter.get(2));
        String ohms = amps > 0 ? Units.format(volts / amps, Units.OHMS) : "\u221e " + Units.OHMS;
        return Component.translatable("gui.fourtwenty.meter", Units.format(volts, Units.VOLTS), Units.format(amps, Units.AMPS), ohms, Units.format(watts, Units.WATTS));
    }
}
