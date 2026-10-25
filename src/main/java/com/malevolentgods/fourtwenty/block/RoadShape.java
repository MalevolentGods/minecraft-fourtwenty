package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * How a road's top is drawn, quarter by quarter, for roads, road slabs and road stairs alike. Each
 * quarter of one of these blocks has a top at 15/16 (a road, a top or double slab, a stair's high
 * quarters, any quarter of an upside-down stair) or 7/16 (a bottom slab, a stair's tread). A quarter's
 * outer side joins the quarter across it when that's the same road at the same height, and its case
 * (outer corner, edge, inner corner, field) follows from its two sides and its diagonal, as for a
 * road ({@link HempPathBlock}). So a straight stair beside a corner stair, or a road beside a stair's
 * tread, gets its border exactly where the surface steps. A road beside its own stairs joins them
 * whatever their height: the stair draws its own borders and shares them with the road.
 *
 * A ramp carries the road between levels: a tread joins the road (or the stair's back) a level down
 * off its downhill sides, the front of the stair, and for a corner stair also the side away from its
 * high corner; and that lower road joins the tread back. The client model
 * (client.RoadModel) reads this as each chunk is drawn, so the blocks need no join properties.
 */
public final class RoadShape {
    public static final int OUTER = 0, NS = 1, EW = 2, INNER = 3, FULL = 4;

    private RoadShape() {}

    /** The tier of a road block (road, slab or stair), or -1 for anything else. */
    public static int tier(BlockState s) {
        if (s.getBlock() instanceof RoadSurface r) return r.tier();
        if (s.getBlock() instanceof HempPathStairBlock st) return st.tier();
        return -1;
    }

    /** Whether quarter (sx, sz) (each -1 or +1: west/east, north/south half) is a stair's high part, or a whole road. */
    public static boolean high(BlockState s, int sx, int sz) {
        if (s.getBlock() instanceof HempPathStairBlock) return HempPathStairBlock.high(s.getValue(StairBlock.FACING), s.getValue(StairBlock.SHAPE), sx, sz);
        return s.getBlock() instanceof RoadSurface r && r.surface(s) == 15;
    }

    /** The height of quarter (sx, sz)'s top in pixels: 15 or 7. */
    public static int top(BlockState s, int sx, int sz) {
        if (s.getBlock() instanceof RoadSurface r) return r.surface(s);
        return s.getValue(StairBlock.HALF) == Half.TOP || high(s, sx, sz) ? 15 : 7;
    }

    /** The height of quarter (sx, sz)'s underside in pixels: 0, or 8 for a top slab and an upside-down stair's tread. */
    public static int bottom(BlockState s, int sx, int sz) {
        if (s.getBlock() instanceof RoadSurface r) return r.surface(s) == 15 && s.getBlock() instanceof net.minecraft.world.level.block.SlabBlock
                && s.getValue(net.minecraft.world.level.block.SlabBlock.TYPE) == net.minecraft.world.level.block.state.properties.SlabType.TOP ? 8 : 0;
        return s.getValue(StairBlock.HALF) == Half.TOP && !high(s, sx, sz) ? 8 : 0;
    }

    /**
     * Whether quarter (sx, sz) of a road or slab at pos reaches up to a full block: another road block
     * stands on it, so the pixel between them is filled (drawn only; its shape stays 15/16).
     */
    public static boolean capped(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        if (!(s.getBlock() instanceof RoadSurface r) || r.surface(s) != 15) return false;
        BlockState above = level.getBlockState(pos.above());
        return tier(above) >= 0 && bottom(above, sx, sz) == 0;
    }

    /** Whether quarter (sx, sz) of a road or slab stands on a road block's full-height quarter (drawn as one wall with it). */
    public static boolean standsOnRoad(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        if (!(s.getBlock() instanceof RoadSurface) || bottom(s, sx, sz) != 0) return false;
        BlockState below = level.getBlockState(pos.below());
        return tier(below) >= 0 && top(below, sx, sz) == 15 && bottom(below, sx, sz) == 0;
    }

    /** Whether quarter (sx, sz) is the tread of a right-way-up stair. */
    public static boolean tread(BlockState s, int sx, int sz) {
        return s.getBlock() instanceof HempPathStairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM && !high(s, sx, sz);
    }

    /** Whether going (dx, dz) (either part, for a diagonal) leads down off a right-way-up stair. */
    static boolean downhill(BlockState s, int dx, int dz) {
        return (dx != 0 && downhill(s, dx > 0 ? Direction.EAST : Direction.WEST)) || (dz != 0 && downhill(s, dz > 0 ? Direction.SOUTH : Direction.NORTH));
    }

    private static boolean downhill(BlockState s, Direction d) {
        Direction f = s.getValue(StairBlock.FACING);
        if (d == f.getOpposite()) return true;
        return switch (s.getValue(StairBlock.SHAPE)) {
            case OUTER_LEFT, INNER_LEFT -> d == f.getClockWise();
            case OUTER_RIGHT, INNER_RIGHT -> d == f.getCounterClockWise();
            default -> false;
        };
    }

    /**
     * Whether quarter (sx, sz) of the road block s at pos carries on into the block at (dx, dz) (each
     * 0 or the quarter's own sx / sz: one of its outer sides or its diagonal).
     */
    public static boolean joins(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, int dx, int dz) {
        int tier = tier(s), top = top(s, sx, sz);
        int mx = dx != 0 ? -sx : sx, mz = dz != 0 ? -sz : sz;           // the quarter across, in its block
        BlockPos n = pos.offset(dx, 0, dz);
        BlockState o = level.getBlockState(n);
        if (tier(o) >= 0) return tier(o) == tier && (top(o, mx, mz) == top || o.getBlock() instanceof HempPathStairBlock);   // shares a stair's borders
        // No road beside: a ramp carries it on a level down (off a tread) or up (onto one).
        if (tread(s, sx, sz) && downhill(s, dx, dz)) {
            BlockState below = level.getBlockState(n.below());
            return tier(below) == tier && top(below, mx, mz) == 15;
        }
        if (top == 15) {
            BlockState above = level.getBlockState(n.above());
            return tier(above) == tier && tread(above, mx, mz) && downhill(above, -dx, -dz);
        }
        return false;
    }

    /** Quarter (sx, sz)'s case: {@link #OUTER}, {@link #NS} (its north/south side open), {@link #EW}, {@link #INNER} or {@link #FULL}. */
    public static int caseOf(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        boolean a = joins(level, pos, s, sx, sz, 0, sz), b = joins(level, pos, s, sx, sz, sx, 0);
        if (!a && !b) return OUTER;
        if (!a) return NS;
        if (!b) return EW;
        return joins(level, pos, s, sx, sz, sx, sz) ? FULL : INNER;
    }
}
