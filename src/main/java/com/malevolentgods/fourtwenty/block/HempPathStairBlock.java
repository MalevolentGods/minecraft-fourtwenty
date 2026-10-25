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
 * corner) follow vanilla's stair rule. It's drawn as separate steps, each bordered all round on top
 * and down its front (tools/generate_paths.py). It shares its borders with what carries its road on
 * ({@link #LEFT}, {@link #RIGHT}, {@link #BACK}): stairs side by side (so a wide flight reads as one)
 * and its road beside or behind it; and the road joins it back ({@link RoadShape}). Same speed as its road. Upside-down stairs are the same, mirrored: flush on top,
 * 8/16 under the tread.
 */
public class HempPathStairBlock extends StairBlock {
    public static final MapCodec<HempPathStairBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(b -> b.tier), propertiesCodec()).apply(i, (t, p) -> new HempPathStairBlock(t, null, p)));
    /** What's on its left / right (looking up it) or behind it carries it on: no border on that side. */
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right"),
            BACK = BooleanProperty.create("back");
    /** It stands on a road block's full height: its sides run on down into that road's as one wall. */
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private final int tier;

    public HempPathStairBlock(int tier, BlockState road, Properties properties) {
        super(road == null ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : road,
                properties.speedFactor(HempPathBlock.speedFactorFor(HempPathBlock.gainFor(tier))));
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(BACK, false).setValue(DOWN, false));
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEFT, RIGHT, BACK, DOWN);
    }

    public int tier() { return tier; }

    @Override public @Nonnull MapCodec<? extends StairBlock> codec() { return CODEC; }

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

    /** Whether a block is a road block full height all over its top (any tier: a stack's sides read as one wall). */
    private static boolean onRoad(BlockState below) {
        if (RoadShape.tier(below) < 0) return false;
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) if (RoadShape.top(below, sx, sz) != 15) return false;
        return true;
    }

    /** Whether a block is this stair's road at full height (a road, a top or double slab). */
    private boolean road(BlockState b) {
        return b.getBlock() instanceof RoadSurface r && r.tier() == tier && r.surface(b) == 15;
    }

    /**
     * Whether the block on side d carries this stair on: its road, or a stair of it facing the same way,
     * the same half up, its steps at the same heights where the two meet.
     */
    private boolean joins(BlockState state, BlockState beside, Direction d) {
        if (road(beside)) return true;
        if (!(beside.getBlock() instanceof HempPathStairBlock other) || other.tier != tier
                || beside.getValue(FACING) != state.getValue(FACING) || beside.getValue(HALF) != state.getValue(HALF)) {
            return false;
        }
        for (int a : new int[]{-1, 1}) {                // the two quarters along the shared edge
            int sx = d.getStepX() != 0 ? d.getStepX() : a, sz = d.getStepZ() != 0 ? d.getStepZ() : a;
            int mx = d.getStepX() != 0 ? -sx : sx, mz = d.getStepZ() != 0 ? -sz : sz;
            if (RoadShape.high(state, sx, sz) != RoadShape.high(beside, mx, mz)) return false;
        }
        return true;
    }

    private BlockState joined(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING), l = f.getCounterClockWise(), r = f.getClockWise();
        return state.setValue(LEFT, joins(state, level.getBlockState(pos.relative(l)), l))
                .setValue(RIGHT, joins(state, level.getBlockState(pos.relative(r)), r))
                .setValue(BACK, road(level.getBlockState(pos.relative(f))))
                .setValue(DOWN, onRoad(level.getBlockState(pos.below())));
    }

    @Override public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : joined(context.getLevel(), context.getClickedPos(), state);
    }

    @Override protected @Nonnull BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbor,
                                                       @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return updated.is(this) ? joined(level, pos, updated) : updated;
    }
}
