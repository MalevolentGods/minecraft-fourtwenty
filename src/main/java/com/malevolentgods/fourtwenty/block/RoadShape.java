package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * How a road's surface is bordered, for roads, road slabs and road stairs alike, quarter by quarter.
 * Every quarter of one of these blocks has a top at 15/16 (a road, a top or double slab, a stair's
 * high quarters, any quarter of an upside-down stair) or 7/16 (a bottom slab, a stair's tread).
 * Wherever two tops sit side by side, the same road at the same height, they read as one bordered
 * surface: a quarter's edge is bordered only where the quarter across it (in its own block or the
 * next) isn't, mitred where two borders meet, with the inner-corner notch where two joined edges
 * meet a gap ({@link #border}). So a stair's steps are each bordered, a flight reads as one, corner
 * stairs join the flights either side and a road's border wraps round where it meets a lower step.
 * A stair's step fronts join the same way ({@link #front}, {@link #frontRunsOn}). The client model
 * (client.RoadModel) reads this as each chunk is drawn, so the blocks need no join properties.
 */
public final class RoadShape {
    public static final int OUTER = 0, NS = 1, EW = 2, INNER = 3, FULL = 4;
    /** {@link #border}'s bits: bordered edges, then notched corners (NW, NE, SW, SE at 16, 32, 64, 128). */
    public static final int N = 1, E = 2, S = 4, W = 8;
    /** A step front's kinds: a tread's (0 to 7, off its downhill sides), a riser (7 to 15, above a tread in the same stair). */
    public static final int TREAD = 1, RISER = 2;
    private static final int[][] CORNERS = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};

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
        if (s.getBlock() instanceof RoadSurface) {
            return s.getBlock() instanceof SlabBlock && s.getValue(SlabBlock.TYPE) == SlabType.TOP ? 8 : 0;
        }
        return s.getValue(StairBlock.HALF) == Half.TOP && !high(s, sx, sz) ? 8 : 0;
    }

    /** A quarter on the half-block grid: a block state and which quarter of it. */
    private record Quarter(BlockState state, int sx, int sz) {}

    /** The quarter (dx, dz) steps (each -1, 0 or 1) along the half-block grid from quarter (sx, sz) of the block s at pos. */
    private static Quarter step(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, int dx, int dz) {
        int gx = (sx < 0 ? 0 : 1) + dx, gz = (sz < 0 ? 0 : 1) + dz;
        int bx = Math.floorDiv(gx, 2), bz = Math.floorDiv(gz, 2);
        BlockState there = bx == 0 && bz == 0 ? s : level.getBlockState(pos.offset(bx, 0, bz));
        return new Quarter(there, Math.floorMod(gx, 2) == 0 ? -1 : 1, Math.floorMod(gz, 2) == 0 ? -1 : 1);
    }

    /** Whether quarter (sx, sz)'s top runs on into the quarter (dx, dz) from it: the same road, its top at the same height. */
    public static boolean joins(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, int dx, int dz) {
        Quarter o = step(level, pos, s, sx, sz, dx, dz);
        return tier(o.state) == tier(s) && top(o.state, o.sx, o.sz) == top(s, sx, sz);
    }

    /** Quarter (sx, sz)'s top: its bordered edges ({@link #N}, {@link #E}, {@link #S}, {@link #W}) and notched corners (bits 4-7). */
    public static int border(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        boolean n = joins(level, pos, s, sx, sz, 0, -1), e = joins(level, pos, s, sx, sz, 1, 0),
                so = joins(level, pos, s, sx, sz, 0, 1), w = joins(level, pos, s, sx, sz, -1, 0);
        int f = (n ? 0 : N) | (e ? 0 : E) | (so ? 0 : S) | (w ? 0 : W);
        for (int i = 0; i < CORNERS.length; i++) {
            int cx = CORNERS[i][0], cz = CORNERS[i][1];
            if ((cz < 0 ? n : so) && (cx < 0 ? w : e) && !joins(level, pos, s, sx, sz, cx, cz)) f |= 16 << i;
        }
        return f;
    }

    /** Quarter (sx, sz)'s case at the block's own edges, as a road quarter's (its underside uses these). */
    public static int caseOf(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        return caseOf(border(level, pos, s, sx, sz), sx, sz);
    }

    /** The case at the block's own edges for {@link #border} bits f. */
    public static int caseOf(int f, int sx, int sz) {
        boolean a = (f & (sz < 0 ? N : S)) == 0, b = (f & (sx < 0 ? W : E)) == 0;
        if (!a && !b) return OUTER;
        if (!a) return NS;
        if (!b) return EW;
        int corner = (sx < 0 ? 0 : 1) + (sz < 0 ? 0 : 2);               // the quarter's own block corner
        return (f & (16 << corner)) != 0 ? INNER : FULL;
    }

    /** The step front of quarter (sx, sz) on side d: {@link #TREAD}, {@link #RISER}, or 0. */
    public static int front(BlockState s, int sx, int sz, Direction d) {
        if (!(s.getBlock() instanceof HempPathStairBlock) || s.getValue(StairBlock.HALF) != Half.BOTTOM) return 0;
        int dx = d.getStepX(), dz = d.getStepZ();
        boolean outer = dx != 0 ? dx == sx : dz == sz;                   // the side is the block's own
        if (!high(s, sx, sz)) return outer && downhill(s, d) ? TREAD : 0;
        return !outer && !high(s, dx != 0 ? -sx : sx, dz != 0 ? -sz : sz) ? RISER : 0;
    }

    /**
     * Whether quarter (sx, sz)'s step front on side d runs on at its end toward e (along the face): into
     * the same front of the quarter that way (the same road), round an outside corner onto its own
     * front on side e, or round an inside corner onto the front facing back along it from the quarter
     * diagonally out and over. Otherwise that end is bordered.
     */
    public static boolean frontRunsOn(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, Direction d, Direction e) {
        int kind = front(s, sx, sz, d);
        if (front(s, sx, sz, e) != 0) return true;
        Quarter o = step(level, pos, s, sx, sz, e.getStepX(), e.getStepZ());
        if (tier(o.state) == tier(s) && front(o.state, o.sx, o.sz, d) == kind) return true;
        Quarter c = step(level, pos, s, sx, sz, e.getStepX() + d.getStepX(), e.getStepZ() + d.getStepZ());
        return tier(c.state) == tier(s) && front(c.state, c.sx, c.sz, e.getOpposite()) == kind;
    }

    /** Whether going d leads down off a right-way-up stair: off its front, and for a corner stair off the side away from its high corner. */
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
     * Whether quarter (sx, sz) reaches up to a full block: another road block stands on it, so the
     * pixel between them is filled and its sides run on up (drawn only; its shape stays 15/16).
     */
    public static boolean capped(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        if (top(s, sx, sz) != 15) return false;
        BlockState above = level.getBlockState(pos.above());
        return tier(above) >= 0 && bottom(above, sx, sz) == 0;
    }

    /** Whether quarter (sx, sz) stands on a road block's full-height quarter (its sides run on down into that one's). */
    public static boolean standsOnRoad(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        if (bottom(s, sx, sz) != 0) return false;
        BlockState below = level.getBlockState(pos.below());
        return tier(below) >= 0 && top(below, sx, sz) == 15;
    }
}
