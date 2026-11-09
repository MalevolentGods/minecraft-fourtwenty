package com.malevolentgods.fourtwenty.electronics;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import javax.annotation.Nonnull;

/**
 * A Resinator: a component whose value is also in its block state, so its colour bands (the resistor colour code) are drawn from it and
 * change the moment it's stepped. Bands: the first two digits, the multiplier, and a gold tolerance band.
 */
public class ResinatorBlock extends ComponentBlock {
    public static final IntegerProperty VALUE = IntegerProperty.create("value", 0, ComponentType.RESINATOR.values.size() - 1);

    public ResinatorBlock(ComponentType type, Properties properties) {
        super(type, properties);
        registerDefaultState(defaultBlockState().setValue(VALUE, type.defaultIndex()));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VALUE);
    }

    /** The colour of band {@code band} for {@code ohms} (domain/ResistorCode). */
    public static int band(double ohms, int band) { return com.malevolentgods.fourtwenty.domain.ResistorCode.band(ohms, band); }

    /** The value at step {@code index}. */
    public static double ohms(int index) {
        var values = ComponentType.RESINATOR.values;
        return values.get(Math.max(0, Math.min(values.size() - 1, index)));
    }
}
