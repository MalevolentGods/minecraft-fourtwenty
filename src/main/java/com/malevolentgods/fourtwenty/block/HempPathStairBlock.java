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
 * ({@link #NORTH}, {@link #EAST}, {@link #SOUTH}, {@link #WEST}): its road beside it, and stairs and
 * slabs of its road whose steps meet its own at the same heights, whichever way they face (so a wide
 * flight reads as one and its corner stairs join the flights either side); the road joins it back
 * ({@link RoadShape}). Same speed as its road. Upside-down stairs are the same, mirrored: flush on
 * top, 8/16 under the tread.
 */
public class HempPathStairBlock extends StairBlock {
    public static final MapCodec<HempPathStairBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(b -> b.tier), propertiesCodec()).apply(i, (t, p) -> new HempPathStairBlock(t, null, p)));
    /** The block on that side carries its road on: no border on that side. */
    public static final BooleanProperty NORTH = BooleanProperty.create("north"), EAST = BooleanProperty.create("east"),
            SOUTH = BooleanProperty.create("south"), WEST = BooleanProperty.create("west");
    private static final BooleanProperty[] SIDES = {NORTH, EAST, SOUTH, WEST};
    private static final Direction[] SIDE_DIRECTIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    /** It stands on a road block's full height: its sides run on down into that road's as one wall. */
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private final int tier;

    public HempPathStairBlock(int tier, BlockState road, Properties properties) {
        super(road == null ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : road,
                properties.speedFactor(HempPathBlock.speedFactorFor(HempPathBlock.gainFor(tier))));
        this.tier = tier;
        BlockState state = defaultBlockState().setValue(DOWN, false);
        for (BooleanProperty side : SIDES) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    @Override protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, EAST, SOUTH, WEST, DOWN);
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
     * Whether the block on side d carries this stair on: its road at full height, or another of its
     * stairs or slabs, whichever way it faces, whose tops meet this one's at the same heights along the
     * shared edge.
     */
    private boolean joins(BlockState state, BlockState beside, Direction d) {
        if (road(beside)) return true;
        if (RoadShape.tier(beside) != tier) return false;
        for (int a : new int[]{-1, 1}) {                // the two quarters along the shared edge
            int sx = d.getStepX() != 0 ? d.getStepX() : a, sz = d.getStepZ() != 0 ? d.getStepZ() : a;
            int mx = d.getStepX() != 0 ? -sx : sx, mz = d.getStepZ() != 0 ? -sz : sz;
            if (RoadShape.top(state, sx, sz) != RoadShape.top(beside, mx, mz)) return false;
        }
        return true;
    }

    private BlockState joined(LevelAccessor level, BlockPos pos, BlockState state) {
        for (int i = 0; i < SIDES.length; i++) {
            state = state.setValue(SIDES[i], joins(state, level.getBlockState(pos.relative(SIDE_DIRECTIONS[i])), SIDE_DIRECTIONS[i]));
        }
        return state.setValue(DOWN, onRoad(level.getBlockState(pos.below())));
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
