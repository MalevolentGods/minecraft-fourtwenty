package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.HempPathBlock;
import com.malevolentgods.fourtwenty.block.HempPathStairBlock;
import com.malevolentgods.fourtwenty.block.RoadShape;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class PathGameTests {
    /**
     * A pig walked straight along a strip of floor: its speed once it has settled (blocks per tick,
     * ticks 10 to 22). It walks slowly (the gain is a ratio, so speed doesn't matter) so it stays
     * within its few blocks of the test's area.
     */
    private static double walk(GameTestHelper h, Block floor, int x) {
        for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), floor);
        Pig pig = EntityType.PIG.create(h.getLevel());
        BlockPos start = h.absolutePos(new BlockPos(x, 2, 0));
        pig.moveTo(start.getX() + 0.5, start.getY() + 0.1, start.getZ() + 0.5, 0, 0);   // facing south, +z
        // Not NoAI: vanilla only moves an entity with effective AI. The loop runs inside one tick, so its AI never acts.
        h.getLevel().addFreshEntity(pig);
        double z10 = 0;
        for (int tick = 0; tick <= 22; tick++) {
            pig.setSpeed(0.04f);
            pig.travel(new Vec3(0, 0, 1));
            if (tick == 10) z10 = pig.getZ();
        }
        double speed = (pig.getZ() - z10) / 12;
        pig.discard();
        return speed;
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void roadsMakeWalkersFiveTenAndFifteenPercentFaster(GameTestHelper h) {
        double base = walk(h, Blocks.STONE, 0);
        for (int tier = 1; tier <= 3; tier++) {
            double on = walk(h, ModTerpenes.PATHS.get(tier - 1).get(), tier * 2);
            double gain = on / base - 1;
            double want = HempPathBlock.gainFor(tier);
            h.assertTrue(base > 0.05 && Math.abs(gain - want) < 0.01, "tier " + tier + ": " + Math.round(gain * 1000) / 10.0 + "% faster, not " + Math.round(want * 100) + "% (speeds " + base + " on stone, " + on + " on the road)");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aLampBesideARoadStillShowsItsTopStrip(GameTestHelper h) {
        BlockPos lamp = new BlockPos(1, 2, 1), road = new BlockPos(2, 2, 1), stone = new BlockPos(1, 2, 2);
        h.setBlock(lamp, Blocks.GLOWSTONE);
        h.setBlock(road, ModTerpenes.PATHS.get(2).get());
        h.setBlock(stone, Blocks.STONE);
        BlockState glow = h.getBlockState(lamp);
        h.assertTrue(Block.shouldRenderFace(glow, h.getLevel(), h.absolutePos(lamp), Direction.EAST, h.absolutePos(road)),
                "the glowstone's side beside the road is hidden");
        h.assertTrue(!Block.shouldRenderFace(glow, h.getLevel(), h.absolutePos(lamp), Direction.SOUTH, h.absolutePos(stone)),
                "test setup: a full block should hide the face");
        h.succeed();
    }

    /** Quarter (sx, sz)'s case at a test position. */
    private static int quarter(GameTestHelper h, BlockPos p, int sx, int sz) {
        return RoadShape.caseOf(h.getLevel(), h.absolutePos(p), h.getBlockState(p), sx, sz);
    }

    @GameTest(template = "empty")
    public static void roadsJoinIntoOneRoad(GameTestHelper h) {
        Block road = ModTerpenes.PATHS.get(0).get();
        // An L: (1,1) (2,1) and (1,2); (2,2) open, so (1,1) has the notch of an L at its south-east.
        for (BlockPos p : new BlockPos[]{new BlockPos(1, 2, 1), new BlockPos(2, 2, 1), new BlockPos(1, 2, 2)}) h.setBlock(p, road);
        BlockPos corner = new BlockPos(1, 2, 1), end = new BlockPos(2, 2, 1);
        h.assertTrue(quarter(h, corner, 1, 1) == RoadShape.INNER && quarter(h, corner, -1, -1) == RoadShape.OUTER
                && quarter(h, corner, 1, -1) == RoadShape.NS, "the corner's quarters");
        h.assertTrue(quarter(h, end, -1, 1) == RoadShape.NS && quarter(h, end, 1, -1) == RoadShape.OUTER, "the arm end's quarters");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void slabsAndStairsAreSizedToTheRoadAndAsFast(GameTestHelper h) {
        for (int t = 0; t < 3; t++) {
            Block road = ModTerpenes.PATHS.get(t).get(), slab = ModTerpenes.PATH_SLABS.get(t).get(), stairs = ModTerpenes.PATH_STAIRS.get(t).get();
            h.assertTrue(slab.getSpeedFactor() == road.getSpeedFactor() && stairs.getSpeedFactor() == road.getSpeedFactor(), "tier " + (t + 1) + " speeds differ");
            BlockPos p = new BlockPos(1, 2, 1);
            h.setBlock(p, slab.defaultBlockState());
            double bottom = h.getBlockState(p).getShape(h.getLevel(), h.absolutePos(p)).max(Direction.Axis.Y);
            h.setBlock(p, slab.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.DOUBLE));
            double full = h.getBlockState(p).getShape(h.getLevel(), h.absolutePos(p)).max(Direction.Axis.Y);
            h.setBlock(p, slab.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
            double under = h.getBlockState(p).getShape(h.getLevel(), h.absolutePos(p)).min(Direction.Axis.Y);
            h.setBlock(p, stairs.defaultBlockState());
            double stair = h.getBlockState(p).getShape(h.getLevel(), h.absolutePos(p)).max(Direction.Axis.Y);
            h.assertTrue(Math.abs(bottom - 7 / 16.0) < 1e-6 && Math.abs(full - 15 / 16.0) < 1e-6 && Math.abs(stair - 15 / 16.0) < 1e-6
                    && Math.abs(under - 7 / 16.0) < 1e-6,
                    "tier " + (t + 1) + " heights: slab " + bottom * 16 + ", double " + full * 16 + ", top slab from " + under * 16 + ", stairs " + stair * 16);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void topSlabsJoinRoadsAndRoadsJoinTheirRamps(GameTestHelper h) {
        Block road = ModTerpenes.PATHS.get(1).get(), slab = ModTerpenes.PATH_SLABS.get(1).get();
        HempPathStairBlock stairs = (HempPathStairBlock) ModTerpenes.PATH_STAIRS.get(1).get();
        BlockPos r = new BlockPos(2, 2, 1);
        h.setBlock(new BlockPos(1, 2, 1), slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        h.setBlock(new BlockPos(3, 2, 1), slab.defaultBlockState());                       // a bottom slab: a step lower, no join
        // A two-wide ramp rising north to the road, down to a road a level lower; and north of the
        // road a stair facing east: the road joins its high back, and borders its lower tread.
        BlockState up = stairs.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        h.setBlock(new BlockPos(2, 2, 2), up);
        h.setBlock(new BlockPos(3, 2, 2), up);
        h.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
        h.setBlock(new BlockPos(2, 1, 3), road);
        h.setBlock(new BlockPos(3, 1, 3), road);
        h.setBlock(new BlockPos(2, 2, 0), stairs.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        h.setBlock(r, road);
        h.assertTrue(quarter(h, r, -1, -1) == RoadShape.NS, "road nw: joins the top slab, not the stair's tread: " + quarter(h, r, -1, -1));
        h.assertTrue(quarter(h, r, 1, -1) == RoadShape.EW, "road ne: joins the stair's back, not the bottom slab: " + quarter(h, r, 1, -1));
        h.assertTrue(quarter(h, r, -1, 1) == RoadShape.INNER, "road sw: joins the ramp and the slab: " + quarter(h, r, -1, 1));
        h.assertTrue(quarter(h, r, 1, 1) == RoadShape.EW, "road se: joins the ramp: " + quarter(h, r, 1, 1));
        h.succeed();
    }

    /** Quarter (sx, sz)'s bordered edges at a test position. */
    private static int edges(GameTestHelper h, BlockPos p, int sx, int sz) {
        return RoadShape.border(h.getLevel(), h.absolutePos(p), h.getBlockState(p), sx, sz) & 15;
    }

    @GameTest(template = "empty")
    public static void stairsShareBordersWhereverTheirTopsAreLevel(GameTestHelper h) {
        HempPathStairBlock stairs = (HempPathStairBlock) ModTerpenes.PATH_STAIRS.get(2).get();
        Block road = ModTerpenes.PATHS.get(2).get(), other = ModTerpenes.PATHS.get(0).get();
        BlockState up = stairs.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        // A flight of two (west to east), its road behind the west one, a cobble path east of the east one.
        h.setBlock(new BlockPos(1, 2, 0), road);
        h.setBlock(new BlockPos(3, 2, 1), other);
        BlockPos a = new BlockPos(1, 2, 1), b = new BlockPos(2, 2, 1);
        h.setBlock(a, up);
        h.setBlock(b, up);
        h.assertTrue((edges(h, a, 1, -1) & RoadShape.E) == 0 && (edges(h, a, 1, 1) & RoadShape.E) == 0, "the flight's steps don't join across");
        h.assertTrue(edges(h, a, -1, -1) == (RoadShape.W | RoadShape.S), "the west stair's back step: joins the road behind and its flight: " + edges(h, a, -1, -1));
        h.assertTrue(edges(h, a, -1, 1) == (RoadShape.W | RoadShape.N | RoadShape.S), "the west stair's tread: bordered but toward its flight: " + edges(h, a, -1, 1));
        h.assertTrue((edges(h, b, 1, -1) & (RoadShape.N | RoadShape.E)) == (RoadShape.N | RoadShape.E), "the east stair's back step: open behind, another road east");
        // The road behind wraps its border round the tread-less side: it joins the high step only.
        h.assertTrue((edges(h, new BlockPos(1, 2, 0), -1, 1) & RoadShape.S) == 0, "the road doesn't join the step level with it");
        // A stair facing the other way beside: its steps meet these at other heights, so both are bordered.
        h.setBlock(new BlockPos(0, 2, 1), up.setValue(StairBlock.FACING, Direction.SOUTH));
        h.assertTrue((edges(h, a, -1, -1) & RoadShape.W) != 0, "a stair facing south joined the flight");
        // A corner, as round a ring: a stair facing west north of a stair facing north makes that one an
        // outer corner, whose high quarter joins both flights though they face other ways.
        BlockPos corner = new BlockPos(4, 2, 3);
        h.setBlock(new BlockPos(4, 2, 2), up.setValue(StairBlock.FACING, Direction.WEST));
        h.setBlock(new BlockPos(3, 2, 3), up);
        h.setBlock(corner, Block.updateFromNeighbourShapes(up, h.getLevel(), h.absolutePos(corner)));
        h.assertTrue(h.getBlockState(corner).getValue(StairBlock.SHAPE) == StairsShape.OUTER_LEFT, "test setup: " + h.getBlockState(corner));
        h.assertTrue(edges(h, corner, -1, -1) == (RoadShape.E | RoadShape.S), "the corner's high step: " + edges(h, corner, -1, -1));
        // Its fronts: the tread front south runs on round the corner.
        BlockPos at = h.absolutePos(corner);
        BlockState cs = h.getBlockState(corner);
        h.assertTrue(RoadShape.stepFace(h.getLevel(), at, cs, 1, 1, Direction.SOUTH) == 7
                && RoadShape.stepRunsOn(h.getLevel(), at, cs, 1, 1, Direction.SOUTH, Direction.EAST), "the corner's tread front doesn't turn the corner");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aCoveredRoadIsAWallToTheRoadBesideIt(GameTestHelper h) {
        Block road = ModTerpenes.PATHS.get(0).get();
        BlockPos a = new BlockPos(1, 2, 1), b = new BlockPos(2, 2, 1);
        h.setBlock(a, road);
        h.setBlock(b, road);
        h.assertTrue((edges(h, a, 1, -1) & RoadShape.E) == 0, "test setup: two roads side by side don't join");
        h.setBlock(new BlockPos(2, 3, 1), Blocks.STONE);
        h.assertTrue((edges(h, a, 1, -1) & RoadShape.E) != 0, "a road beside a covered road isn't bordered");
        h.assertTrue(RoadShape.covered(h.getLevel(), h.absolutePos(b), h.getBlockState(b), -1, -1), "stone on a road doesn't cover it");
        // Furniture stands on the road: a rug or a couch doesn't cover it.
        h.setBlock(new BlockPos(2, 3, 1), ModTerpenes.RUGS.get("natural").get());
        h.assertTrue(!RoadShape.covered(h.getLevel(), h.absolutePos(b), h.getBlockState(b), -1, -1), "a rug covers a road");
        h.setBlock(new BlockPos(2, 3, 1), ModTerpenes.COUCHES.values().iterator().next().get());
        h.assertTrue(!RoadShape.covered(h.getLevel(), h.absolutePos(b), h.getBlockState(b), -1, -1), "a couch covers a road");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void stepFacesRunBetweenBlocks(GameTestHelper h) {
        Block road = ModTerpenes.PATHS.get(1).get(), slab = ModTerpenes.PATH_SLABS.get(1).get();
        // Two roads, two bottom slabs east of them: the roads' faces above the slabs are one bordered strip.
        for (int z = 1; z <= 2; z++) {
            h.setBlock(new BlockPos(1, 2, z), road);
            h.setBlock(new BlockPos(2, 2, z), slab.defaultBlockState());
        }
        BlockPos r = h.absolutePos(new BlockPos(1, 2, 1));
        BlockState rs = h.getBlockState(new BlockPos(1, 2, 1));
        h.assertTrue(RoadShape.stepFace(h.getLevel(), r, rs, 1, 1, Direction.EAST) == (7 << 5 | 15), "no step face above the slab");
        h.assertTrue(RoadShape.stepRunsOn(h.getLevel(), r, rs, 1, 1, Direction.EAST, Direction.SOUTH), "the step face doesn't run on into the next road's");
        h.assertTrue(!RoadShape.stepRunsOn(h.getLevel(), r, rs, 1, -1, Direction.EAST, Direction.NORTH), "the step face isn't bordered where it ends");
        // A top slab beside a bottom slab: flush underneath, its face above the bottom slab a step face too.
        BlockPos t = new BlockPos(1, 2, 4), u = new BlockPos(2, 2, 4);
        h.setBlock(t, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        h.setBlock(u, slab.defaultBlockState());
        h.assertTrue(RoadShape.stepFace(h.getLevel(), h.absolutePos(t), h.getBlockState(t), 1, -1, Direction.EAST) == (7 << 5 | 15), "no step face on the top slab");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void undersidesBorderByTheirOwnHeights(GameTestHelper h) {
        Block road = ModTerpenes.PATHS.get(1).get(), slab = ModTerpenes.PATH_SLABS.get(1).get();
        BlockPos t = new BlockPos(1, 3, 1), r = new BlockPos(2, 3, 1), r2 = new BlockPos(3, 3, 1);
        h.setBlock(t, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        h.setBlock(r, road);
        h.setBlock(r2, road);
        int slabUnder = RoadShape.underBorder(h.getLevel(), h.absolutePos(t), h.getBlockState(t), 1, -1);
        int roadUnder = RoadShape.underBorder(h.getLevel(), h.absolutePos(r), h.getBlockState(r), -1, -1);
        int roadUnder2 = RoadShape.underBorder(h.getLevel(), h.absolutePos(r), h.getBlockState(r), 1, -1);
        h.assertTrue((slabUnder & RoadShape.E) != 0 && (roadUnder & RoadShape.W) != 0, "a top slab's underside joined a road's");
        h.assertTrue((roadUnder2 & RoadShape.E) == 0, "two roads' undersides don't join");
        h.assertTrue((edges(h, t, 1, -1) & RoadShape.E) == 0, "test setup: the top slab's top joins the road's");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wallsAreBorderedRoundTheirOutline(GameTestHelper h) {
        Block road = ModTerpenes.PATHS.get(1).get();
        // A wall two roads long, a second road stacked on its west end: seen from the south.
        h.setBlock(new BlockPos(1, 2, 1), road);
        h.setBlock(new BlockPos(2, 2, 1), road);
        h.setBlock(new BlockPos(1, 3, 1), road);
        net.minecraft.server.level.ServerLevel level = h.getLevel();
        BlockPos west = h.absolutePos(new BlockPos(1, 2, 1)), east = h.absolutePos(new BlockPos(2, 2, 1)), top = h.absolutePos(new BlockPos(1, 3, 1));
        BlockState r = road.defaultBlockState();
        // The low wall's east end turns a corner: a band the whole height; its west end meets the stack, which rises above it.
        h.assertTrue(RoadShape.wallEnd(level, east, r, 1, 1, Direction.SOUTH, Direction.EAST) == 0, "no corner band at the wall's end");
        h.assertTrue(RoadShape.wallEnd(level, east, r, -1, 1, Direction.SOUTH, Direction.WEST) == -1, "a band where the wall runs on into the stack's lower block");
        h.assertTrue(RoadShape.wallEnd(level, west, r, 1, 1, Direction.SOUTH, Direction.EAST) == 15, "no band up the stack from the lower wall's top: "
                + RoadShape.wallEnd(level, west, r, 1, 1, Direction.SOUTH, Direction.EAST));
        h.assertTrue(RoadShape.wallEnd(level, top, r, 1, 1, Direction.SOUTH, Direction.EAST) == 0, "no corner band up the stack's top block");
        h.assertTrue(RoadShape.wallEnd(level, west, r, 1, 1, Direction.SOUTH, Direction.WEST) == -1, "a band inside one block's wall");
        h.succeed();
    }

    /** Every road block state to try against every other: roads of two tiers, every slab, every stair; and stone and air. */
    private static java.util.List<BlockState> everyShape() {
        java.util.List<BlockState> out = new java.util.ArrayList<>();
        out.add(ModTerpenes.PATHS.get(0).get().defaultBlockState());
        out.add(ModTerpenes.PATHS.get(1).get().defaultBlockState());
        for (SlabType type : SlabType.values()) out.add(ModTerpenes.PATH_SLABS.get(0).get().defaultBlockState().setValue(SlabBlock.TYPE, type));
        for (Direction f : Direction.Plane.HORIZONTAL) {
            for (net.minecraft.world.level.block.state.properties.Half half : net.minecraft.world.level.block.state.properties.Half.values()) {
                for (StairsShape shape : StairsShape.values()) {
                    out.add(ModTerpenes.PATH_STAIRS.get(0).get().defaultBlockState().setValue(StairBlock.FACING, f)
                            .setValue(StairBlock.HALF, half).setValue(StairBlock.SHAPE, shape));
                }
            }
        }
        out.add(Blocks.STONE.defaultBlockState());
        out.add(Blocks.AIR.defaultBlockState());
        return out;
    }

    /**
     * Every pair of shapes, side by side both ways: joins are two-sided (tops and undersides), a border
     * running along a joined edge's side carries on across it or turns into a notch, never just stops,
     * and step faces that run on into each other agree.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void everyPairOfShapesBordersAsOneSurface(GameTestHelper h) {
        java.util.List<BlockState> shapes = everyShape();
        net.minecraft.server.level.ServerLevel level = h.getLevel();
        BlockPos a = h.absolutePos(new BlockPos(2, 2, 2));
        java.util.List<String> faults = new java.util.ArrayList<>();
        for (Direction way : new Direction[]{Direction.EAST, Direction.SOUTH}) {
            BlockPos b = a.relative(way);
            for (BlockState sa : shapes) {
                for (BlockState sb : shapes) {
                    level.setBlock(a, sa, 2 | 16);
                    level.setBlock(b, sb, 2 | 16);
                    check(level, a, way, faults, sa, sb);
                    if (faults.size() > 20) break;
                }
            }
            level.setBlock(b, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        level.setBlock(a, Blocks.AIR.defaultBlockState(), 2 | 16);
        h.assertTrue(faults.isEmpty(), faults.size() + " faults, e.g. " + String.join(" | ", faults.subList(0, Math.min(5, faults.size()))));
        h.succeed();
    }

    /** Checks every quarter edge across the pair at a (the other block `way` from it) and inside each block. */
    private static void check(net.minecraft.server.level.ServerLevel level, BlockPos a, Direction way, java.util.List<String> faults, BlockState sa, BlockState sb) {
        // the edges between quarter P and quarter Q, `way` from P: across the pair, and inside both blocks
        for (BlockPos p : new BlockPos[]{a, a.relative(way)}) {
            BlockState ps = level.getBlockState(p);
            if (RoadShape.tier(ps) < 0) continue;
            for (int sx : new int[]{-1, 1}) {
                for (int sz : new int[]{-1, 1}) {
                    int dx = way.getStepX(), dz = way.getStepZ();
                    // Q: the quarter `way` from P on the half-block grid
                    int gx = (sx < 0 ? 0 : 1) + dx, gz = (sz < 0 ? 0 : 1) + dz;
                    BlockPos qp = p.offset(Math.floorDiv(gx, 2), 0, Math.floorDiv(gz, 2));
                    BlockState qs = level.getBlockState(qp);
                    int qx = Math.floorMod(gx, 2) == 0 ? -1 : 1, qz = Math.floorMod(gz, 2) == 0 ? -1 : 1;
                    if (RoadShape.tier(qs) < 0) continue;
                    String where = sa.toString().replace("fourtwenty:", "") + " / " + sb.toString().replace("fourtwenty:", "") + " " + way
                            + " at " + (p.equals(a) ? "a" : "b") + "(" + sx + "," + sz + ")";
                    boolean pq = RoadShape.joins(level, p, ps, sx, sz, dx, dz), qp2 = RoadShape.joins(level, qp, qs, qx, qz, -dx, -dz);
                    if (pq != qp2) faults.add("one-sided top join " + where);
                    boolean upq = RoadShape.joinsUnder(level, p, ps, sx, sz, dx, dz), uqp = RoadShape.joinsUnder(level, qp, qs, qx, qz, -dx, -dz);
                    if (upq != uqp) faults.add("one-sided underside join " + where);
                    if (pq) continuity(faults, "top " + where, way, RoadShape.border(level, p, ps, sx, sz), RoadShape.border(level, qp, qs, qx, qz));
                    if (upq) continuity(faults, "underside " + where, way, RoadShape.underBorder(level, p, ps, sx, sz), RoadShape.underBorder(level, qp, qs, qx, qz));
                }
            }
        }
    }

    /** Across a joined edge (P to Q, going `way`): each border along it on either side carries on in Q, or one of them turns there. */
    private static void continuity(java.util.List<String> faults, String where, Direction way, int fp, int fq) {
        boolean eastWest = way.getAxis() == Direction.Axis.X;
        int[] sides = eastWest ? new int[]{RoadShape.N, RoadShape.S} : new int[]{RoadShape.W, RoadShape.E};
        for (int side : sides) {
            boolean bp = (fp & side) != 0, bq = (fq & side) != 0;
            if (bp == bq) continue;
            // the shared corner on that side: P's corner toward Q, Q's toward P (bits NW 16, NE 32, SW 64, SE 128)
            int pCorner, qCorner;
            if (eastWest) {
                pCorner = side == RoadShape.N ? 32 : 128;               // P's NE / SE
                qCorner = side == RoadShape.N ? 16 : 64;                // Q's NW / SW
            } else {
                pCorner = side == RoadShape.W ? 64 : 128;               // P's SW / SE
                qCorner = side == RoadShape.W ? 16 : 32;                // Q's NW / NE
            }
            if ((fp & pCorner) == 0 && (fq & qCorner) == 0) faults.add("border stops short (" + side + ") " + where);
        }
    }
}
