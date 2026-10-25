package com.malevolentgods.fourtwenty.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
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
 * corner) follow vanilla's stair rule. Its top joins the road round it quarter by quarter
 * ({@link RoadShape}): a border only where the surface steps or ends, and the treads' paving lines up
 * with the road round it. Same speed as its road. Upside-down stairs are the same, mirrored: flush
 * on top, 8/16 under the tread.
 */
public class HempPathStairBlock extends StairBlock {
    public static final MapCodec<HempPathStairBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(b -> b.tier), propertiesCodec()).apply(i, (t, p) -> new HempPathStairBlock(t, null, p)));
    private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private final int tier;

    public HempPathStairBlock(int tier, BlockState road, Properties properties) {
        super(road == null ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : road,
                properties.speedFactor(HempPathBlock.speedFactorFor(HempPathBlock.gainFor(tier))));
        this.tier = tier;
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
}
