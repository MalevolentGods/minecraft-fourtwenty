package com.malevolentgods.fourtwenty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * How a road's faces are bordered, for roads, road slabs and road stairs alike, quarter by quarter.
 * Every quarter of one of these blocks is a column from its underside (0, or 7 for a top slab and an
 * upside-down stair's low quarters) to its top (15, or 7 for a bottom slab and a stair's tread).
 *
 * The rule: wherever two faces sit side by side they read as one bordered space, and a border runs
 * wherever the next face rises, drops, is covered or is another block.
 * <ul>
 * <li>Tops ({@link #border}): a quarter's edge joins the quarter across it (in its own block or the
 * next) when that's the same road, its top at the same height, and neither top is covered by a block
 * standing on it. Borders mitre where they meet; where two joined edges meet a gap, the inner-corner
 * notch carries the border round.</li>
 * <li>Undersides ({@link #underBorder}): the same, by underside heights, where nothing holds them up.</li>
 * <li>Step faces ({@link #stepFace}): where a quarter rises above a lower step of the same road (in its
 * block or the next), the face between them is a bordered strip; so is a stair tread's front off its
 * downhill side. Strips run on into the same strip beside them and round inside and outside corners
 * ({@link #stepRunsOn}). Every other side face is the cut through the road.</li>
 * </ul>
 * The client model (client.RoadModel) reads this as each chunk is drawn, so the blocks need no join
 * properties.
 */
public final class RoadShape {
    public static final int OUTER = 0, NS = 1, EW = 2, INNER = 3, FULL = 4;
    /** {@link #border}'s bits: bordered edges, then notched corners (NW, NE, SW, SE at 16, 32, 64, 128). */
    public static final int N = 1, E = 2, S = 4, W = 8;
    private static final int[][] CORNERS = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
    private static final double PIXEL = 1 / 16.0;

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

    /** The height of quarter (sx, sz)'s underside in pixels: 0, or 7 for a top slab and an upside-down stair's low quarters. */
    public static int bottom(BlockState s, int sx, int sz) {
        if (s.getBlock() instanceof RoadSurface) {
            return s.getBlock() instanceof SlabBlock && s.getValue(SlabBlock.TYPE) == SlabType.TOP ? 7 : 0;
        }
        return s.getValue(StairBlock.HALF) == Half.TOP && !high(s, sx, sz) ? 7 : 0;
    }

    /** A quarter on the half-block grid: its block's position and state, and which quarter of it. */
    private record Quarter(BlockPos pos, BlockState state, int sx, int sz) {}

    /** The quarter (dx, dz) steps (each -1, 0 or 1) along the half-block grid from quarter (sx, sz) of the block s at pos. */
    private static Quarter step(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, int dx, int dz) {
        int gx = (sx < 0 ? 0 : 1) + dx, gz = (sz < 0 ? 0 : 1) + dz;
        int bx = Math.floorDiv(gx, 2), bz = Math.floorDiv(gz, 2);
        BlockPos p = bx == 0 && bz == 0 ? pos : pos.offset(bx, 0, bz);
        return new Quarter(p, p == pos ? s : level.getBlockState(p), Math.floorMod(gx, 2) == 0 ? -1 : 1, Math.floorMod(gz, 2) == 0 ? -1 : 1);
    }

    /** Whether a shape fills the quarter's (sx, sz) square across its one-pixel layer from height y. */
    private static boolean fills(VoxelShape shape, int sx, int sz, double y) {
        double x0 = sx < 0 ? 0 : 0.5, z0 = sz < 0 ? 0 : 0.5;
        return !shape.isEmpty() && !Shapes.joinIsNotEmpty(Shapes.box(x0, y, z0, x0 + 0.5, y + PIXEL, z0 + 0.5), shape, BooleanOp.ONLY_FIRST);
    }

    /** Whether something stands on quarter (sx, sz)'s full-height top: a road block's underside, or any block solid over it. */
    public static boolean covered(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        if (top(s, sx, sz) != 15) return false;
        BlockPos ap = pos.above();
        BlockState above = level.getBlockState(ap);
        if (tier(above) >= 0) return bottom(above, sx, sz) == 0;
        return fills(above.getCollisionShape(level, ap), sx, sz, 0);
    }

    /** Whether a road block stands on quarter (sx, sz): a stack, whose sides read as one wall. */
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

    /** Whether quarter (sx, sz)'s underside rests on something: a road block's full-height top, or any block solid under it. */
    public static boolean supported(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        if (bottom(s, sx, sz) != 0) return false;
        BlockPos bp = pos.below();
        BlockState below = level.getBlockState(bp);
        if (tier(below) >= 0) return top(below, sx, sz) == 15;
        return fills(below.getCollisionShape(level, bp), sx, sz, 1 - PIXEL);
    }

    // ---- tops and undersides ---------------------------------------------------------------------

    /** Whether quarter (sx, sz)'s top runs on into the quarter (dx, dz) from it: the same road, its top at the same height, neither covered. */
    public static boolean joins(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, int dx, int dz) {
        Quarter o = step(level, pos, s, sx, sz, dx, dz);
        return tier(o.state) == tier(s) && top(o.state, o.sx, o.sz) == top(s, sx, sz)
                && !covered(level, pos, s, sx, sz) && !covered(level, o.pos, o.state, o.sx, o.sz);
    }

    /** Whether quarter (sx, sz)'s underside runs on into the quarter (dx, dz) from it: the same road, its underside at the same height, neither resting on anything. */
    public static boolean joinsUnder(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, int dx, int dz) {
        Quarter o = step(level, pos, s, sx, sz, dx, dz);
        return tier(o.state) == tier(s) && bottom(o.state, o.sx, o.sz) == bottom(s, sx, sz)
                && !supported(level, pos, s, sx, sz) && !supported(level, o.pos, o.state, o.sx, o.sz);
    }

    /** A joining rule between a quarter and the one (dx, dz) from it. */
    private interface Joins {
        boolean test(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, int dx, int dz);
    }

    private static int border(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, Joins joins) {
        boolean n = joins.test(level, pos, s, sx, sz, 0, -1), e = joins.test(level, pos, s, sx, sz, 1, 0),
                so = joins.test(level, pos, s, sx, sz, 0, 1), w = joins.test(level, pos, s, sx, sz, -1, 0);
        int f = (n ? 0 : N) | (e ? 0 : E) | (so ? 0 : S) | (w ? 0 : W);
        for (int i = 0; i < CORNERS.length; i++) {
            int cx = CORNERS[i][0], cz = CORNERS[i][1];
            if ((cz < 0 ? n : so) && (cx < 0 ? w : e) && !joins.test(level, pos, s, sx, sz, cx, cz)) f |= 16 << i;
        }
        return f;
    }

    /** Quarter (sx, sz)'s top: its bordered edges ({@link #N}, {@link #E}, {@link #S}, {@link #W}) and notched corners (bits 4-7). */
    public static int border(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        return border(level, pos, s, sx, sz, RoadShape::joins);
    }

    /** Quarter (sx, sz)'s underside, as {@link #border} (in world directions). */
    public static int underBorder(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        return border(level, pos, s, sx, sz, RoadShape::joinsUnder);
    }

    /** Quarter (sx, sz)'s case at the block's own edges, as a road quarter's. */
    public static int caseOf(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        int f = border(level, pos, s, sx, sz);
        boolean a = (f & (sz < 0 ? N : S)) == 0, b = (f & (sx < 0 ? W : E)) == 0;
        if (!a && !b) return OUTER;
        if (!a) return NS;
        if (!b) return EW;
        int corner = (sx < 0 ? 0 : 1) + (sz < 0 ? 0 : 2);               // the quarter's own block corner
        return (f & (16 << corner)) != 0 ? INNER : FULL;
    }

    // ---- sides -------------------------------------------------------------------------------------

    /** What's across side d of a quarter, in its block or the next: the same road's quarter (its underside and top), or nothing of it. */
    public record Across(boolean road, int bottom, int top, boolean covered) {
        static final Across NONE = new Across(false, 0, 0, false);
    }

    public static Across across(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, Direction d) {
        Quarter o = step(level, pos, s, sx, sz, d.getStepX(), d.getStepZ());
        if (tier(o.state) != tier(s)) return Across.NONE;
        return new Across(true, bottom(o.state, o.sx, o.sz), top(o.state, o.sx, o.sz), covered(level, o.pos, o.state, o.sx, o.sz));
    }

    /**
     * Quarter (sx, sz)'s step face on side d, as (from << 5 | to) in pixels, or 0: from the top of a
     * lower step of the same road across it up to this quarter's top (unless something stands on this
     * one: then it's a wall, the cut); or a stair tread's front off its downhill side, where nothing of
     * the road is beside it (0 to 7).
     */
    public static int stepFace(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, Direction d) {
        int hi = top(s, sx, sz);
        Across a = across(level, pos, s, sx, sz, d);
        if (a.road()) return a.top() < hi && !covered(level, pos, s, sx, sz) ? a.top() << 5 | hi : 0;
        boolean outer = d.getStepX() != 0 ? d.getStepX() == sx : d.getStepZ() == sz;
        if (outer && s.getBlock() instanceof HempPathStairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM && !high(s, sx, sz) && downhill(s, d)) {
            return 7;                                                   // 0 to 7
        }
        return 0;
    }

    /**
     * Whether quarter (sx, sz)'s step face on side d runs on at its end toward e (along the face): into
     * the same step face of the quarter that way, round an outside corner onto its own same face on
     * side e, or round an inside corner onto the same face looking back along it from the quarter
     * diagonally out and over. Otherwise that end is bordered.
     */
    public static boolean stepRunsOn(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, Direction d, Direction e) {
        int face = stepFace(level, pos, s, sx, sz, d);
        if (stepFace(level, pos, s, sx, sz, e) == face) return true;
        Quarter o = step(level, pos, s, sx, sz, e.getStepX(), e.getStepZ());
        if (tier(o.state) == tier(s) && stepFace(level, o.pos, o.state, o.sx, o.sz, d) == face) return true;
        Quarter c = step(level, pos, s, sx, sz, e.getStepX() + d.getStepX(), e.getStepZ() + d.getStepZ());
        return tier(c.state) == tier(s) && stepFace(level, c.pos, c.state, c.sx, c.sz, e.getOpposite()) == face;
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

    /** How high a quarter's side wall reaches: its top, or on up (99) where a road block stands on it (a stack is one wall). */
    private static int wallTop(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz) {
        return capped(level, pos, s, sx, sz) ? 99 : top(s, sx, sz);
    }

    /**
     * The corner band at the end toward e of quarter (sx, sz)'s side wall on side d (the cut through the
     * road), as the height it runs up from: 0 (the whole height), 7 or 15; or -1, none. A wall reads as
     * one face, bordered round its outline: the band runs up an end where the wall turns an outside
     * corner or meets another block, and where the wall beside it in the same face is lower, from that
     * wall's top up (its top border running into the band).
     */
    public static int wallEnd(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, Direction d, Direction e) {
        Quarter o = step(level, pos, s, sx, sz, e.getStepX(), e.getStepZ());
        if (tier(o.state) >= 0) {
            if (tier(o.state) != tier(s)) return 0;                     // another road: a border between them
            Across front = across(level, o.pos, o.state, o.sx, o.sz, d);
            if (front.road() && front.bottom() <= bottom(o.state, o.sx, o.sz) && front.top() >= top(o.state, o.sx, o.sz)) return -1;   // an inside corner
            return wallTop(level, o.pos, o.state, o.sx, o.sz) < wallTop(level, pos, s, sx, sz) ? top(o.state, o.sx, o.sz) : -1;
        }
        VoxelShape shape = o.state.getCollisionShape(level, o.pos);
        if (!shape.isEmpty()) return 0;                                 // the wall runs into another block
        return open(level, pos, s, sx, sz, e) ? 0 : -1;                 // an outside corner, or it meets a wall round it
    }

    /**
     * Whether quarter (sx, sz)'s side d is open to view over its whole height (lo to hi): nothing of the
     * road across covers it and no other block fills it. Side faces take corner quoins where two open
     * ones meet at the block's corner.
     */
    public static boolean open(BlockGetter level, BlockPos pos, BlockState s, int sx, int sz, Direction d) {
        int lo = bottom(s, sx, sz), hi = top(s, sx, sz);
        Across a = across(level, pos, s, sx, sz, d);
        if (a.road()) return a.bottom() > lo || a.top() < hi;
        boolean outer = d.getStepX() != 0 ? d.getStepX() == sx : d.getStepZ() == sz;
        if (!outer) return false;
        BlockPos np = pos.relative(d);
        VoxelShape shape = level.getBlockState(np).getCollisionShape(level, np);
        if (shape.isEmpty()) return true;
        // the neighbour's layer against this quarter's side, over its height
        double x0 = d.getStepX() > 0 ? 0 : d.getStepX() < 0 ? 1 - PIXEL : (sx < 0 ? 0 : 0.5);
        double z0 = d.getStepZ() > 0 ? 0 : d.getStepZ() < 0 ? 1 - PIXEL : (sz < 0 ? 0 : 0.5);
        double x1 = d.getStepX() != 0 ? x0 + PIXEL : x0 + 0.5, z1 = d.getStepZ() != 0 ? z0 + PIXEL : z0 + 0.5;
        return Shapes.joinIsNotEmpty(Shapes.box(x0, lo * PIXEL, z0, x1, hi * PIXEL, z1), shape, BooleanOp.ONLY_FIRST);
    }
}
