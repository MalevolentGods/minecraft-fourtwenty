package com.malevolentgods.fourtwenty.blockentity;

import com.malevolentgods.fourtwenty.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets the client draw a lamp's halo, and holds what a lamp's circuit last said (electronics): not saved, since the circuit says it
 * again every tick it runs.
 */
public class LampBlockEntity extends BlockEntity {
    private boolean circuitHigh;
    private long circuitAt = Long.MIN_VALUE / 2;

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAMP.get(), pos, state);
    }

    public boolean circuitHigh() { return circuitHigh; }

    public long circuitAt() { return circuitAt; }

    public void circuit(boolean high, long now) {
        circuitHigh = high;
        circuitAt = now;
    }
}
