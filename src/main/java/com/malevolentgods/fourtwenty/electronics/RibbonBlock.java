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
    /**
     * A one-block vertical run between two Ribbons turned different ways (one out of a north-south run, one into an east-west): it twists a
     * quarter turn, as ribbon cable does, from its own VAXIS at the bottom to the other at the top.
     */
    public static final BooleanProperty TWIST = BooleanProperty.create("twist");
    /** Undyed or one of the 16 dyes: a Ribbon joins only Ribbons of its colour, as Wicks do (Roach Clips, bus faces and trays take any). */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<com.malevolentgods.fourtwenty.power.WickBlock.Colour> COLOUR = com.malevolentgods.fourtwenty.power.WickBlock.COLOUR;
    private final Map<BlockState, VoxelShape> shapes = new IdentityHashMap<>();

    public RibbonBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : SIDES.values()) state = state.setValue(side, false);
        registerDefaultState(state.setValue(VAXIS, Direction.Axis.X).setValue(TWIST, false).setValue(COLOUR, com.malevolentgods.fourtwenty.power.WickBlock.Colour.NONE));
        for (BlockState s : stateDefinition.getPossibleStates()) shapes.put(s, shapeOf(s));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
        builder.add(VAXIS, TWIST, COLOUR);
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

    /** Two Ribbons join: one is a Roach Clip (it takes any colour), or they're one colour. */
    public static boolean joins(BlockState a, BlockState b) {
        if (!(a.getBlock() instanceof RibbonBlock) || !(b.getBlock() instanceof RibbonBlock)) return false;
        return a.getBlock() instanceof RoachClipBlock || b.getBlock() instanceof RoachClipBlock || a.getValue(COLOUR) == b.getValue(COLOUR);
    }

    /** Whether a Ribbon ({@code self}) reaches toward {@code neighbour} on {@code side}: a Ribbon it joins, a Roach Clip, or a bus face. */
    public static boolean links(BlockState self, Direction side, BlockState neighbour) {
        // a Rolling Tray takes a Ribbon on any face but its front: on its back for its part, else on the board's bus
        if (neighbour.getBlock() instanceof RollingTrayBlock) return RollingTrayBlock.front(neighbour) != side.getOpposite();
        if (neighbour.getBlock() instanceof RibbonBlock) return joins(self, neighbour);
        return ComponentBlock.busAt(neighbour, side.getOpposite()) >= 0;
    }

    /** As {@link #links(Direction, BlockState)}, and into a Nah, Skip Me, Bro whose Ribbon runs this way. */
    public static boolean links(BlockState self, Direction side, BlockState neighbour, net.minecraft.world.level.BlockGetter level, BlockPos neighbourPos) {
        if (neighbour.getBlock() instanceof HopBlock) {
            var w = HopBlockEntity.facing(level, neighbourPos, side.getOpposite());
            return w != null && w.ribbon() && (self.getBlock() instanceof RoachClipBlock || w.colour() == self.getValue(COLOUR));
        }
        return links(self, side, neighbour);
    }

    private BlockState linked(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) state = state.setValue(SIDES.get(d), links(state, d, level.getBlockState(pos.relative(d)), level, pos.relative(d)));
        state = state.setValue(VAXIS, vaxis(state, level, pos));
        return state.setValue(TWIST, twist(state, level, pos));
    }

    /** A vertical run's block whose Ribbon above is turned the other way from it (it took its turn from below): a twist. */
    private static boolean twist(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) if (state.getValue(SIDES.get(d))) return false;
        if (!state.getValue(SIDES.get(Direction.UP)) || !state.getValue(SIDES.get(Direction.DOWN))) return false;
        BlockState above = level.getBlockState(pos.above()), below = level.getBlockState(pos.below());
        if (!joins(state, below) || !joins(state, above) || below.getValue(VAXIS) != state.getValue(VAXIS)) return false;
        return above.getValue(VAXIS) != state.getValue(VAXIS);
    }

    /** A vertical run's turn: from its own horizontal arms if it has any (east-west wide along z), else from the Ribbon above or below. */
    private static Direction.Axis vaxis(BlockState state, LevelAccessor level, BlockPos pos) {
        boolean ns = state.getValue(SIDES.get(Direction.NORTH)) || state.getValue(SIDES.get(Direction.SOUTH));
        boolean ew = state.getValue(SIDES.get(Direction.EAST)) || state.getValue(SIDES.get(Direction.WEST));
        if (ns) return Direction.Axis.X;
        if (ew) return Direction.Axis.Z;
        for (Direction d : new Direction[]{Direction.DOWN, Direction.UP}) {
            BlockState next = level.getBlockState(pos.relative(d));
            if (state.getValue(SIDES.get(d)) && joins(state, next)) return next.getValue(VAXIS);
        }
        return Direction.Axis.X;
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return linked(defaultBlockState().setValue(COLOUR, com.malevolentgods.fourtwenty.power.WickBlock.colourOf(context.getItemInHand())), context.getLevel(), context.getClickedPos());
    }

    /** Use a dye on a Ribbon to colour it; it then joins only Ribbons of that colour. */
    @Override protected @Nonnull net.minecraft.world.ItemInteractionResult useItemOn(@Nonnull net.minecraft.world.item.ItemStack stack, @Nonnull BlockState state,
            @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.world.entity.player.Player player, @Nonnull net.minecraft.world.InteractionHand hand,
            @Nonnull net.minecraft.world.phys.BlockHitResult hit) {
        if (this instanceof RoachClipBlock || !(stack.getItem() instanceof net.minecraft.world.item.DyeItem dye)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        var colour = com.malevolentgods.fourtwenty.power.WickBlock.Colour.of(dye.getDyeColor());
        if (state.getValue(COLOUR) == colour) return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            BlockState next = linked(state.setValue(COLOUR, colour), level, pos);
            level.setBlock(pos, next, 3);
            for (Direction d : Direction.values()) {
                BlockPos at = pos.relative(d);
                BlockState around = level.getBlockState(at);
                BlockState updated = around.updateShape(d.getOpposite(), next, level, at, pos);
                if (updated != around) level.setBlock(at, updated, 3);
            }
            BIGrid.changed(level);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
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
