package com.malevolentgods.fourtwenty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

/**
 * A hemp couch: a seat with a back, arm rests and wooden legs that joins other couches the way
 * stairs join stairs. Its shape (straight, inner or outer corner) comes from the couches in front of
 * and behind it, by vanilla's stair rule, so any arrangement of couches makes a sofa, a sectional or
 * a ring of seats. A straight couch has an arm on each side that doesn't continue into another couch
 * (one facing the same way, or a corner). FACING is the side its back is on, as for stairs.
 *
 * Fences, walls and panes don't join it: it has no sturdy faces (an empty support shape). Players
 * sit on it with an empty-handed right-click ({@link SeatEntity}); sneaking stands them up.
 * Models: tools/generate_couches.py.
 */
public class CouchBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CouchBlock> CODEC = simpleCodec(CouchBlock::new);
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
    /** An arm rest on the sitter's left / right (only on straight couches). */
    public static final BooleanProperty LEFT_ARM = BooleanProperty.create("left_arm"), RIGHT_ARM = BooleanProperty.create("right_arm");

    /** Shapes for a couch whose back faces east (the models' base orientation), turned for the others. */
    private static final VoxelShape SEAT = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape BACK_STRAIGHT = Block.box(12, 8, 0, 16, 15, 16);
    private static final VoxelShape BACK_INNER = Shapes.or(BACK_STRAIGHT, Block.box(0, 8, 12, 12, 15, 16));
    private static final VoxelShape BACK_OUTER = Block.box(12, 8, 12, 16, 15, 16);
    private static final Map<StairsShape, VoxelShape> BASE = new EnumMap<>(Map.of(
            StairsShape.STRAIGHT, Shapes.or(SEAT, BACK_STRAIGHT),
            StairsShape.INNER_LEFT, Shapes.or(SEAT, BACK_INNER), StairsShape.INNER_RIGHT, Shapes.or(SEAT, BACK_INNER),
            StairsShape.OUTER_LEFT, Shapes.or(SEAT, BACK_OUTER), StairsShape.OUTER_RIGHT, Shapes.or(SEAT, BACK_OUTER)));

    public CouchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, StairsShape.STRAIGHT)
                .setValue(LEFT_ARM, true).setValue(RIGHT_ARM, true));
    }

    @Override protected @Nonnull MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE, LEFT_ARM, RIGHT_ARM);
    }

    public static boolean isCouch(BlockState state) { return state.getBlock() instanceof CouchBlock; }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        return connected(state, context.getLevel(), context.getClickedPos());
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbor,
                                                       @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? connected(state, level, pos) : state;
    }

    /** This couch's shape and arms from the couches around it. */
    public static BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
        StairsShape shape = shape(state, level, pos);
        boolean straight = shape == StairsShape.STRAIGHT;
        Direction front = state.getValue(FACING).getOpposite();             // the way a sitter looks
        return state.setValue(SHAPE, shape)
                .setValue(LEFT_ARM, straight && !continues(state, level.getBlockState(pos.relative(front.getCounterClockWise()))))
                .setValue(RIGHT_ARM, straight && !continues(state, level.getBlockState(pos.relative(front.getClockWise()))));
    }

    /** Whether the couch beside this one carries the sofa on (so this side has no arm). */
    private static boolean continues(BlockState state, BlockState beside) {
        return isCouch(beside) && (beside.getValue(FACING) == state.getValue(FACING) || beside.getValue(SHAPE) != StairsShape.STRAIGHT);
    }

    /** Vanilla's StairBlock shape rule, for couches. */
    private static StairsShape shape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockState ahead = level.getBlockState(pos.relative(facing));
        if (isCouch(ahead)) {
            Direction other = ahead.getValue(FACING);
            if (other.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, other.getOpposite())) {
                return other == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
            }
        }
        BlockState behind = level.getBlockState(pos.relative(facing.getOpposite()));
        if (isCouch(behind)) {
            Direction other = behind.getValue(FACING);
            if (other.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, other)) {
                return other == facing.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
            }
        }
        return StairsShape.STRAIGHT;
    }

    private static boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        BlockState there = level.getBlockState(pos.relative(side));
        return !isCouch(there) || there.getValue(FACING) != state.getValue(FACING);
    }

    /** The y rotation (clockwise quarter turns) of a couch's models and shapes: vanilla's stair blockstate. */
    public static int turns(Direction facing, StairsShape shape) {
        int base = switch (facing) { case EAST -> 0; case SOUTH -> 1; case WEST -> 2; default -> 3; };
        boolean left = shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT;
        return Math.floorMod(base - (left ? 1 : 0), 4);
    }

    private static final Map<BlockState, VoxelShape> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPES.computeIfAbsent(state, s -> turn(BASE.get(s.getValue(SHAPE)), turns(s.getValue(FACING), s.getValue(SHAPE))));
    }

    /** No sturdy faces, so fences, walls and panes don't join a couch. */
    @Override protected @Nonnull VoxelShape getBlockSupportShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return Shapes.empty();
    }

    /** Right-click with an empty hand to sit (sneak to stand up). */
    @Override protected @Nonnull net.minecraft.world.InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.Level level,
                                                                                      @Nonnull BlockPos pos, @Nonnull net.minecraft.world.entity.player.Player player,
                                                                                      @Nonnull net.minecraft.world.phys.BlockHitResult hit) {
        if (player.isShiftKeyDown() || player.isPassenger()) return net.minecraft.world.InteractionResult.PASS;
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        return SeatEntity.sit(level, pos, player) ? net.minecraft.world.InteractionResult.CONSUME : net.minecraft.world.InteractionResult.PASS;
    }

    @Override protected boolean isPathfindable(@Nonnull BlockState state, @Nonnull PathComputationType type) { return false; }

    @Override protected @Nonnull BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected @Nonnull BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirror) {
        Direction facing = state.getValue(FACING);
        StairsShape shape = state.getValue(SHAPE);
        boolean flips = (mirror == Mirror.LEFT_RIGHT && facing.getAxis() == Direction.Axis.Z) || (mirror == Mirror.FRONT_BACK && facing.getAxis() == Direction.Axis.X);
        if (!flips) return state.rotate(mirror.getRotation(facing));
        StairsShape mirrored = switch (shape) {
            case INNER_LEFT -> StairsShape.INNER_RIGHT; case INNER_RIGHT -> StairsShape.INNER_LEFT;
            case OUTER_LEFT -> StairsShape.OUTER_RIGHT; case OUTER_RIGHT -> StairsShape.OUTER_LEFT; default -> shape;
        };
        boolean left = state.getValue(LEFT_ARM), right = state.getValue(RIGHT_ARM);
        return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, mirrored).setValue(LEFT_ARM, right).setValue(RIGHT_ARM, left);
    }

    /** A shape turned clockwise (seen from above) by k quarter turns: (x, z) -> (16 - z, x). */
    private static VoxelShape turn(VoxelShape shape, int k) {
        VoxelShape out = shape;
        for (int i = 0; i < k; i++) {
            VoxelShape[] acc = {Shapes.empty()};
            out.forAllBoxes((x0, y0, z0, x1, y1, z1) -> acc[0] = Shapes.or(acc[0], Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
            out = acc[0];
        }
        return out;
    }
}
