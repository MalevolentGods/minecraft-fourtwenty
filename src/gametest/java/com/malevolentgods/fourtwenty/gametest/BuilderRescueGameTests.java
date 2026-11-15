package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.StandingSpot;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The watchdog's rescue spots, found in real blocks (spec 013 T019a): solid underfoot, two free cells, out of a footprint. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class BuilderRescueGameTests {
    @GameTest(template = "empty")
    public static void rescueSpotsStandOnRealGroundOutsideTheBuilding(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(2, 1, 2));
        for (int dx = -2; dx <= 4; dx++) for (int dz = -2; dz <= 4; dz++) {
            level.setBlockAndUpdate(origin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy <= 4; dy++) level.setBlockAndUpdate(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
        }
        // A glass box round the origin: walls one block out, a lid two up.
        for (int dy = 0; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx != 0 || dz != 0) level.setBlockAndUpdate(origin.offset(dx, dy, dz), Blocks.GLASS.defaultBlockState());
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(origin.offset(dx, 2, dz), Blocks.GLASS.defaultBlockState());

        StandingSpot.Cells solid = (x, y, z) -> level.getBlockState(new BlockPos(x, y, z)).blocksMotion();
        StandingSpot.Cells free = (x, y, z) -> {
            var state = level.getBlockState(new BlockPos(x, y, z));
            return !state.blocksMotion() && state.getFluidState().isEmpty();
        };

        // NEAR_BLOCK from inside the box: the spot inside counts (she's sealed in, but that's where she stands).
        var inside = StandingSpot.nearest(origin.getX(), origin.getY(), origin.getZ(), 4, solid, free, null, 6).orElseThrow();
        h.assertTrue(StandingSpot.standable(inside.x(), inside.y(), inside.z(), solid, free), "the spot found is standable in the world");

        // OUTSIDE: never inside the box's footprint.
        var box = new StandingSpot.Box(origin.getX() - 1, origin.getY() - 1, origin.getZ() - 1, origin.getX() + 1, origin.getY() + 3, origin.getZ() + 1);
        var out = StandingSpot.nearest(origin.getX(), origin.getY(), origin.getZ(), 4, solid, free, box, 6).orElseThrow();
        h.assertTrue(!box.contains(out.x(), out.y(), out.z()), "outside spot is out of the footprint: " + out);
        h.assertTrue(level.getBlockState(new BlockPos(out.x(), out.y() - 1, out.z())).is(Blocks.STONE), "it stands on the stone floor");
        h.assertTrue(level.getBlockState(new BlockPos(out.x(), out.y(), out.z())).isAir()
                && level.getBlockState(new BlockPos(out.x(), out.y() + 1, out.z())).isAir(), "with room for her body");
        h.assertTrue(Math.max(Math.abs(out.x() - origin.getX()), Math.abs(out.z() - origin.getZ())) <= 2, "and right next to the box");
        h.succeed();
    }
}
