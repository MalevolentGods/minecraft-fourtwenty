package com.malevolentgods.fourtwenty.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/** The block entity of a {@link BIBlock}. */
public abstract class BIBlockEntity extends BlockEntity {
    public static final int DATA_STORED = 0, DATA_CAPACITY = 1, DATA_OVERLOADED = 2, DATA_TIER = 3, DATA_KIND = 4, DATA_PLOT = 5, DATA_COUNT = 6;
    public static final int KIND_STASH = 0, KIND_HIT_BOX = 1, KIND_PLOT = 2;
    private static final int FX_EVERY = 20;

    protected BIBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void serverTick();

    protected void lit(boolean on) {
        BlockState state = getBlockState();
        if (level != null && state.getValue(BIBlock.LIT) != on) level.setBlock(worldPosition, state.setValue(BIBlock.LIT, on), 3);
    }

    /** True while this buffer is overloaded, with the smoke and sizzle that go with it. */
    protected boolean overloadTick(BIBuffer buffer) {
        if (!buffer.overloaded(level.getGameTime())) return false;
        if (level.getGameTime() % FX_EVERY == 0) BurnOut.overloadFx(level, worldPosition);
        return true;
    }

    /** What a gauge screen shows of a buffer. */
    static ContainerData data(BIBuffer buffer, LongSupplier now, IntSupplier tier, int kind, int plot) {
        return new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case DATA_STORED -> (int) buffer.stored();
                    case DATA_CAPACITY -> (int) buffer.capacity();
                    case DATA_OVERLOADED -> buffer.overloaded(now.getAsLong()) ? 1 : 0;
                    case DATA_TIER -> tier.getAsInt();
                    case DATA_KIND -> kind;
                    case DATA_PLOT -> plot;
                    default -> 0;
                };
            }

            @Override public void set(int index, int value) {}

            @Override public int getCount() { return DATA_COUNT; }
        };
    }
}
