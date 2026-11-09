package com.malevolentgods.fourtwenty.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A big chip, two blocks tall (ComponentType.Layout.BIG_CHIP): the Manyfier and the Fewinator. Placed, it stands its upper block on its
 * own top; either block broken takes the other with it, and only one drops. Its pins are numbered as one part's; the upper block's faces
 * carry the high bytes and VDD.
 */
public class BigChipBlock extends ComponentBlock {
    public static final BooleanProperty UPPER = BooleanProperty.create("upper");

    public BigChipBlock(ComponentType type, Properties properties) {
        super(type, properties);
        registerDefaultState(defaultBlockState().setValue(UPPER, false));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(UPPER);
    }

    public static boolean upper(BlockState state) { return state.hasProperty(UPPER) && state.getValue(UPPER); }

    /** The way from this block to its other half: up its top from the lower, down from the upper. */
    public static Direction toPartner(BlockState state) {
        Direction top = Orientation.toWorld(state.getValue(FACING), state.getValue(SPIN), ComponentType.Face.TOP);
        return upper(state) ? top.getOpposite() : top;
    }

    /** The lower block of the big chip at {@code pos} (itself if it is the lower). */
    public static BlockPos lower(BlockState state, BlockPos pos) { return upper(state) ? pos.relative(toPartner(state)) : pos; }

    @Override public @Nullable BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        BlockPos above = context.getClickedPos().relative(toPartner(state));
        Level level = context.getLevel();
        if (!level.isInWorldBounds(above) || !level.getBlockState(above).canBeReplaced(context)) return null;
        return state.setValue(UPPER, false);
    }

    @Override public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!upper(state)) level.setBlock(pos.relative(toPartner(state)), state.setValue(UPPER, true), 3);
    }

    /** Its other half gone (or turned away), it goes too. */
    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState neighbour,
                                                        @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighbourPos) {
        if (side == toPartner(state) && !partnerOf(state, neighbour)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, side, neighbour, level, pos, neighbourPos);
    }

    /** Whether {@code other} is the other half of {@code state}: the same chip, turned the same way, the other half. */
    public static boolean partnerOf(BlockState state, BlockState other) {
        return other.is(state.getBlock()) && upper(other) != upper(state) && other.getValue(FACING) == state.getValue(FACING)
                && other.getValue(SPIN).equals(state.getValue(SPIN));
    }
}
