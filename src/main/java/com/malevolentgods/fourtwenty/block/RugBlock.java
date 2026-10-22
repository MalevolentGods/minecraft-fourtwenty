package com.malevolentgods.fourtwenty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nonnull;

/**
 * A rug: a carpet that joins the same rug around it into one larger rug. It knows which of its
 * eight neighbours (sides and diagonals) are the same rug; each quarter of its model is then an
 * outer corner, an edge, an inner corner or interior (tools/generate_palette_blocks.py), so the
 * border runs only round the outside of the whole rug and the pattern flows across the inside.
 * Minecraft only tells a block about its side neighbours, so a rug placed or removed refreshes its
 * diagonal neighbours itself.
 */
public class RugBlock extends CarpetBlock {
    public static final MapCodec<RugBlock> CODEC = simpleCodec(RugBlock::new);
    public static final BooleanProperty NORTH = BooleanProperty.create("north"), EAST = BooleanProperty.create("east"),
            SOUTH = BooleanProperty.create("south"), WEST = BooleanProperty.create("west"),
            NORTH_EAST = BooleanProperty.create("northeast"), SOUTH_EAST = BooleanProperty.create("southeast"),
            SOUTH_WEST = BooleanProperty.create("southwest"), NORTH_WEST = BooleanProperty.create("northwest");
    /** The eight neighbours, as (dx, dz), with their properties. */
    private static final int[][] OFFSETS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}};
    private static final BooleanProperty[] PROPERTIES = {NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST};

    public RugBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty p : PROPERTIES) state = state.setValue(p, false);
        registerDefaultState(state);
    }

    @Override public @Nonnull MapCodec<? extends CarpetBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPERTIES);
    }

    /** This rug's state with its connections to the same rug around it. */
    public BlockState connected(LevelReader level, BlockPos pos, BlockState state) {
        for (int i = 0; i < OFFSETS.length; i++) {
            state = state.setValue(PROPERTIES[i], level.getBlockState(pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1])).is(this));
        }
        return state;
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return connected(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbor,
                                                       @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return updated.is(this) ? connected(level, pos, updated) : updated;
    }

    /** Tell the diagonal neighbours (Minecraft only notifies the side ones). */
    private void refreshDiagonals(Level level, BlockPos pos) {
        for (int i = 4; i < OFFSETS.length; i++) {
            BlockPos p = pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1]);
            BlockState there = level.getBlockState(p);
            if (there.is(this)) {
                BlockState updated = connected(level, p, there);
                if (updated != there) level.setBlock(p, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this)) refreshDiagonals(level, pos);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!level.isClientSide && !newState.is(this)) refreshDiagonals(level, pos);
    }
}
