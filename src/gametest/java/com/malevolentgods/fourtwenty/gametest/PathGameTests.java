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
            h.setBlock(p, stairs.defaultBlockState());
            double stair = h.getBlockState(p).getShape(h.getLevel(), h.absolutePos(p)).max(Direction.Axis.Y);
            h.assertTrue(Math.abs(bottom - 7 / 16.0) < 1e-6 && Math.abs(full - 15 / 16.0) < 1e-6 && Math.abs(stair - 15 / 16.0) < 1e-6,
                    "tier " + (t + 1) + " heights: slab " + bottom * 16 + ", double " + full * 16 + ", stairs " + stair * 16);
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
        // Its fronts: the tread fronts south run on across the stair, and round the corner.
        h.assertTrue(RoadShape.front(h.getBlockState(corner), 1, 1, Direction.SOUTH) == RoadShape.TREAD
                && RoadShape.frontRunsOn(h.getLevel(), h.absolutePos(corner), h.getBlockState(corner), 1, 1, Direction.SOUTH, Direction.EAST),
                "the corner's tread front doesn't turn the corner");
        h.succeed();
    }
}
