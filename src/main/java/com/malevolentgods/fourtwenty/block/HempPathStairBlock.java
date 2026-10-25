package com.malevolentgods.fourtwenty.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Road stairs, sized to the road: the tread is 7/16 high and the back reaches the road's 15/16, so
 * a ramp of them runs from one road down to the next with no lip. Shapes (straight, inner, outer
 * corner) follow vanilla's stair rule. A straight stair knows whether the stairs beside it carry the
 * ramp on (the same road, facing the same way): its top shows a border only down the ramp's open
 * sides, and the treads' paving lines up with the road round it (tools/generate_paths.py). Same
 * speed as its road. Upside-down stairs are the same, mirrored: flush on top, 8/16 under the tread.
 */
public class HempPathStairBlock extends StairBlock {
    public static final MapCodec<HempPathStairBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(b -> b.tier), propertiesCodec()).apply(i, (t, p) -> new HempPathStairBlock(t, null, p)));
    /** No stairs beside it on its left / right (looking up the stairs): the ramp's edge, with a border. */
    public static final BooleanProperty LEFT_OPEN = BooleanProperty.create("left_open"), RIGHT_OPEN = BooleanProperty.create("right_open");
    private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private final int tier;

    public HempPathStairBlock(int tier, BlockState road, Properties properties) {
        super(road == null ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : road,
                properties.speedFactor(HempPathBlock.speedFactorFor(HempPathBlock.gainFor(tier))));
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(LEFT_OPEN, true).setValue(RIGHT_OPEN, true));
    }

    public int tier() { return tier; }

    @Override public @Nonnull MapCodec<? extends StairBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEFT_OPEN, RIGHT_OPEN);
    }

    /** Whether quarter (sx, sz) (each -1 or +1: west/east, north/south half) is high for this stair. */
    public static boolean high(Direction facing, StairsShape shape, int sx, int sz) {
        boolean back = sx * facing.getStepX() + sz * facing.getStepZ() > 0;
        Direction l = facing.getCounterClockWise();
        boolean left = sx * l.getStepX() + sz * l.getStepZ() > 0;
        return switch (shape) {
            case STRAIGHT -> back;
            case OUTER_LEFT -> back && left;
            case OUTER_RIGHT -> back && !left;
            case INNER_LEFT -> back || left;
            case INNER_RIGHT -> back || !left;
        };
    }

    @Override protected @Nonnull VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPES.computeIfAbsent(state, s -> {
            boolean top = s.getValue(HALF) == Half.TOP;
            VoxelShape out = Shapes.empty();
            for (int sx : new int[]{-1, 1}) {
                for (int sz : new int[]{-1, 1}) {
                    boolean h = high(s.getValue(FACING), s.getValue(SHAPE), sx, sz);
                    double x0 = sx < 0 ? 0 : 8, z0 = sz < 0 ? 0 : 8;
                    double y0 = h || !top ? 0 : 8, y1 = h || top ? 15 : 7;
                    out = Shapes.or(out, Block.box(x0, y0, z0, x0 + 8, y1, z0 + 8));
                }
            }
            return out;
        });
    }

    @Override protected boolean useShapeForLightOcclusion(@Nonnull BlockState state) { return true; }

    /** The ramp's open sides: no stair of this road facing the same way (and the same half) beside it. */
    private BlockState sides(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING);
        return state.setValue(LEFT_OPEN, !continues(state, level.getBlockState(pos.relative(f.getCounterClockWise()))))
                .setValue(RIGHT_OPEN, !continues(state, level.getBlockState(pos.relative(f.getClockWise()))));
    }

    private boolean continues(BlockState state, BlockState beside) {
        return beside.getBlock() instanceof HempPathStairBlock s && s.tier == tier
                && beside.getValue(FACING) == state.getValue(FACING) && beside.getValue(HALF) == state.getValue(HALF);
    }

    /** A road's diagonal joins depend on a ramp's stairs: tell the roads diagonal to this one. */
    @Override protected void onPlace(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.Level level, @Nonnull BlockPos pos, @Nonnull BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide) RoadSurface.refreshDiagonals(level, pos);
    }

    @Override protected void onRemove(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!level.isClientSide && !newState.is(this)) RoadSurface.refreshDiagonals(level, pos);
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : sides(context.getLevel(), context.getClickedPos(), state);
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbor,
                                                       @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return updated.is(this) ? sides(level, pos, updated) : updated;
    }
}
