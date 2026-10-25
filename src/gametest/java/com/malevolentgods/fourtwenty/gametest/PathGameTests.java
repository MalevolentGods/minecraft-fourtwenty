package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.HempPathBlock;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

    @GameTest(template = "empty")
    public static void roadsJoinIntoOneRoad(GameTestHelper h) {
        Block road = ModTerpenes.PATHS.get(0).get();
        // An L: (1,1) (2,1) and (1,2); (2,2) open, so (1,1) has the notch of an L at its south-east.
        for (BlockPos p : new BlockPos[]{new BlockPos(1, 2, 1), new BlockPos(2, 2, 1), new BlockPos(1, 2, 2)}) {
            BlockState s = ((HempPathBlock) road).connected(h.getLevel(), h.absolutePos(p), road.defaultBlockState());
            h.setBlock(p, s);
        }
        BlockState corner = h.getBlockState(new BlockPos(1, 2, 1));
        h.assertTrue(corner.getValue(HempPathBlock.EAST) && corner.getValue(HempPathBlock.SOUTH) && !corner.getValue(HempPathBlock.SOUTH_EAST)
                && !corner.getValue(HempPathBlock.NORTH) && !corner.getValue(HempPathBlock.WEST), "the corner's connections: " + corner);
        BlockState end = h.getBlockState(new BlockPos(2, 2, 1));
        h.assertTrue(end.getValue(HempPathBlock.WEST) && end.getValue(HempPathBlock.SOUTH_WEST) && !end.getValue(HempPathBlock.EAST),
                "the arm end's connections: " + end);
        h.succeed();
    }
}
