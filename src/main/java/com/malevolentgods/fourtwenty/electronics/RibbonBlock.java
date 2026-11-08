package com.malevolentgods.fourtwenty.electronics;

import com.malevolentgods.fourtwenty.power.BIGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The Eighth Ribbon: a flat cable of {@link ComponentType#BUS} separate wires, channels 0 to 7 and the carry, striped in the resistor
 * colour code. It joins other Ribbons and Roach Clips, wire for wire, and a chip's bus face; nothing else. Signal wire: no resistance,
 * no rating.
 */
public class RibbonBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    /**
     * Which way a vertical run is turned: wide along x or along z. Taken from the horizontal Ribbon it bends out of (an east-west run is
     * wide along z), and passed up and down a vertical run from Ribbon to Ribbon, so a bend from flat to upright lines up.
     */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction.Axis> VAXIS =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("vaxis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);
    private final Map<BlockState, VoxelShape> shapes = new IdentityHashMap<>();

    public RibbonBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : SIDES.values()) state = state.setValue(side, false);
        registerDefaultState(state.setValue(VAXIS, Direction.Axis.X));
        for (BlockState s : stateDefinition.getPossibleStates()) shapes.put(s, shapeOf(s));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
        builder.add(VAXIS);
    }

    /** Its wires' bits, for the glow (RibbonBlockEntity). */
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) { return new RibbonBlockEntity(pos, state); }

    /** The core's shape: a Ribbon's is the flat cable, a Roach Clip's is bigger. */
    protected VoxelShape core() { return Block.box(3, 7, 3, 14, 9, 14); }

    private VoxelShape shapeOf(BlockState state) {
        VoxelShape shape = core();
        if (state.getValue(SIDES.get(Direction.NORTH))) shape = Shapes.or(shape, Block.box(3, 7, 0, 14, 9, 3));
        if (state.getValue(SIDES.get(Direction.SOUTH))) shape = Shapes.or(shape, Block.box(3, 7, 14, 14, 9, 16));
        if (state.getValue(SIDES.get(Direction.EAST))) shape = Shapes.or(shape, Block.box(14, 7, 3, 16, 9, 14));
        if (state.getValue(SIDES.get(Direction.WEST))) shape = Shapes.or(shape, Block.box(0, 7, 3, 3, 9, 14));
        if (state.getValue(SIDES.get(Direction.UP))) shape = Shapes.or(shape, Block.box(3, 9, 7, 14, 16, 9));
        if (state.getValue(SIDES.get(Direction.DOWN))) shape = Shapes.or(shape, Block.box(3, 0, 7, 14, 7, 9));
        return shape;
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return shapes.getOrDefault(state, core());
    }

    /** Whether a Ribbon reaches toward {@code neighbour} on {@code side}: another Ribbon or Roach Clip, or a chip's bus face. */
    public static boolean links(Direction side, BlockState neighbour) {
        return neighbour.getBlock() instanceof RibbonBlock || ComponentBlock.busAt(neighbour, side.getOpposite()) >= 0;
    }

    private BlockState linked(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) state = state.setValue(SIDES.get(d), links(d, level.getBlockState(pos.relative(d))));
        return state.setValue(VAXIS, vaxis(state, level, pos));
    }

    /** A vertical run's turn: from its own horizontal arms if it has any (east-west wide along z), else from the Ribbon above or below. */
    private static Direction.Axis vaxis(BlockState state, LevelAccessor level, BlockPos pos) {
        boolean ns = state.getValue(SIDES.get(Direction.NORTH)) || state.getValue(SIDES.get(Direction.SOUTH));
        boolean ew = state.getValue(SIDES.get(Direction.EAST)) || state.getValue(SIDES.get(Direction.WEST));
        if (ns) return Direction.Axis.X;
        if (ew) return Direction.Axis.Z;
        for (Direction d : new Direction[]{Direction.DOWN, Direction.UP}) {
            BlockState next = level.getBlockState(pos.relative(d));
            if (state.getValue(SIDES.get(d)) && next.getBlock() instanceof RibbonBlock) return next.getValue(VAXIS);
        }
        return Direction.Axis.X;
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return linked(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState neighbour, @Nonnull LevelAccessor level,
                                                        @Nonnull BlockPos pos, @Nonnull BlockPos neighbourPos) {
        // the whole state again: a neighbour's turn can change this one's, and this one's passes on along a vertical run
        return linked(state, level, pos);
    }

    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moved) {
        if (!old.is(this)) {
            // placed by a command or a paste, it reaches toward its neighbours as a hand-placed one does
            BlockState linked = linked(state, level, pos);
            if (linked != state) level.setBlock(pos, linked, 2);
            BIGrid.changed(level);
        }
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) BIGrid.changed(level);
        super.onRemove(state, level, pos, next, moved);
    }
}
