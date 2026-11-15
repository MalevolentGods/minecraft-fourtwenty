package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.util.FaceClimbing;
import com.minecolonies.core.entity.pathfinding.PathPointExtended;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveToLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Racks stay solid and are climbed from outside, by players and by colonists' route-finding. */
@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class RackClimbingGameTests {
    @GameTest(template = "empty", batch = "rack_climbing")
    public static void racksAreSolidAndClimbedFromOutside(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var bottom = h.absolutePos(new BlockPos(2, 1, 2));
        for (int dx = -2; dx <= 6; dx++) for (int dz = -2; dz <= 5; dz++) {
            level.setBlockAndUpdate(bottom.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy <= 5; dy++) level.setBlockAndUpdate(bottom.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
        }
        for (int y = 0; y < 3; y++) level.setBlockAndUpdate(bottom.above(y), com.minecolonies.api.blocks.ModBlocks.blockRack.defaultBlockState());
        var rack = level.getBlockState(bottom);

        h.assertTrue(FaceClimbing.isFace(rack), "Racks are in #fourtwenty:climbable_faces");
        h.assertTrue(rack.getCollisionShape(level, bottom, CollisionContext.empty()).equals(Shapes.block()), "A rack is a full solid block: nobody walks into it");
        h.assertTrue(!rack.is(net.minecraft.tags.BlockTags.CLIMBABLE), "A rack is not climbable from inside");

        // A player beside the stack, on the floor south of it.
        var beside = bottom.south();
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.2);   // body touching the rack's south face
        player.setOnGround(true);
        player.horizontalCollision = false;
        h.assertTrue(!player.onClimbable(), "Standing beside a rack without pushing in is ordinary walking");
        player.horizontalCollision = true;
        h.assertTrue(player.onClimbable(), "Pushing into a rack's side climbs it");
        player.setPos(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.8);   // a step back: not touching
        h.assertTrue(!player.onClimbable(), "A rack a step away isn't climbed");

        // Colonists: a route from the floor beside the stack up onto its top goes up the outside.
        var top = bottom.above(3);
        var start = beside.south(2);
        var job = new PathJobMoveToLocation(level, start, top, 16, null);
        var path = job.call();
        com.mojang.logging.LogUtils.getLogger().info("RACK_PATH status={} searched={} target={} reach={} nodes={}", job.getResult().getStatus(),
                job.getResult().searchedNodes, path == null ? null : path.getTarget(), path == null ? null : path.canReach(),
                path == null ? "none" : java.util.stream.IntStream.range(0, path.getNodeCount()).mapToObj(n -> {
                    var node = path.getNode(n);
                    return node.x + "," + node.y + "," + node.z + (node instanceof PathPointExtended pe && pe.isOnLadder() ? "L" + pe.getLadderFacing() : "");
                }).toList());
        h.assertTrue(path != null && path.canReach() && path.getTarget().equals(top), "Colonists can climb the outside of a three-rack stack onto its top");
        boolean climbedOutside = false;
        for (int n = 0; n < path.getNodeCount(); n++) {
            var node = path.getNode(n);
            h.assertTrue(!(node.x == bottom.getX() && node.z == bottom.getZ() && node.y < top.getY()), "The route never passes through a rack: " + node);
            if (node.x == beside.getX() && node.z == beside.getZ() && node.y > beside.getY()
                    && node instanceof PathPointExtended point && point.isOnLadder() && point.getLadderFacing() == Direction.SOUTH) climbedOutside = true;
        }
        h.assertTrue(climbedOutside, "The route climbs the cells beside the stack, facing the rack");

        // A floor cell beside a single rack is ordinary ground, not a climb.
        var lone = bottom.east(4);
        level.setBlockAndUpdate(lone, com.minecolonies.api.blocks.ModBlocks.blockRack.defaultBlockState());
        h.assertTrue(FaceClimbing.pathFace(Blocks.AIR.defaultBlockState(), lone.south().getX(), lone.south().getY(), lone.south().getZ(),
                (x, y, z) -> level.getBlockState(new BlockPos(x, y, z))) == null, "Beside a single rack on the floor is not a climb");
        h.succeed();
    }
}
