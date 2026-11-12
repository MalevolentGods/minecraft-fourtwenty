package com.malevolentgods.fourtwenty.colonytest;

import com.minecolonies.api.blocks.AbstractBlockMinecoloniesRack;
import com.minecolonies.core.entity.pathfinding.PathPointExtended;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveToLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class RackClimbingGameTests {
    @GameTest(template="empty", batch="rack_climbing")
    public static void racksUseVanillaLadderPhysicsAndColonyPaths(GameTestHelper h) throws Exception {
        var bottom = h.absolutePos(new BlockPos(2, 1, 2));
        for (int y = 0; y < 3; y++) {
            h.getLevel().setBlockAndUpdate(bottom.above(y), com.minecolonies.api.blocks.ModBlocks.blockRack.defaultBlockState());
        }
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        player.setPos(bottom.getX() + 0.5, bottom.getY(), bottom.getZ() + 0.5);
        var state = h.getLevel().getBlockState(bottom);
        h.assertTrue(state.isLadder(h.getLevel(), bottom, player), "Racks are ladders to living entities");
        h.assertTrue(player.onClimbable(), "A player inside a rack is climbing");

        for (var direction : Direction.Plane.HORIZONTAL) {
            var rackState = state.setValue(AbstractBlockMinecoloniesRack.FACING, direction);
            var actual = rackState.getCollisionShape(h.getLevel(), bottom, CollisionContext.empty()).toAabbs();
            var expected = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, direction)
                    .getCollisionShape(h.getLevel(), bottom, CollisionContext.empty()).toAabbs();
            h.assertTrue(actual.equals(expected), "Rack collision matches a vanilla ladder facing " + direction);
        }

        var handleOnClimbable = LivingEntity.class.getDeclaredMethod("handleOnClimbable", Vec3.class);
        handleOnClimbable.setAccessible(true);
        var slowed = (Vec3) handleOnClimbable.invoke(player, new Vec3(0, -1, 0));
        h.assertTrue(Math.abs(slowed.y + 0.15) < 0.000001, "Players descend racks at vanilla ladder speed");

        h.assertTrue(com.minecolonies.core.entity.pathfinding.PathfindingUtils.isLadder(state,
                new com.minecolonies.core.entity.pathfinding.PathingOptions()), "Racks are free-climb blocks to colony pathfinding");
        h.assertTrue(!state.blocksMotion(), "Racks are non-solid to colony pathfinding");
        h.getLevel().setBlockAndUpdate(bottom.above(1).north(1), Blocks.STONE.defaultBlockState());
        var job = new PathJobMoveToLocation(h.getLevel(), bottom, bottom.above(2).north(1), 16, null);
        var path = job.call();
        h.assertTrue(path != null && path.canReach(), "Colonists can path up a three-rack stack");
        h.assertTrue(path.getTarget().equals(bottom.above(2).north(1)), "The path reaches the supported exit");
        com.mojang.logging.LogUtils.getLogger().info("RACK_JOB start={} destination={} searched={} status={} pathTarget={} canReach={}",
                bottom, bottom.above(2).north(1), job.getResult().searchedNodes, job.getResult().getStatus(),
                path == null ? null : path.getTarget(), path == null ? null : path.canReach());
        if (path != null) {
            com.mojang.logging.LogUtils.getLogger().info("RACK_PATH count={} nodes={}", path.getNodeCount(),
                    java.util.stream.IntStream.range(0, path.getNodeCount()).mapToObj(n -> {
                        var node = path.getNode(n);
                        return node.toString() + ":ladder=" + (node instanceof PathPointExtended point && point.isOnLadder());
                    }).toList());
            for (int y = 0; y < 3; y++) {
                int rackY = bottom.getY() + y;
                h.assertTrue(java.util.stream.IntStream.range(0, path.getNodeCount()).anyMatch(n -> {
                    var node = path.getNode(n);
                    return node.x == bottom.getX() && node.y == rackY && node.z == bottom.getZ()
                            && (rackY == bottom.getY() || node instanceof PathPointExtended point && point.isOnLadder());
                }), "Colony path uses the rack at y " + rackY);
            }
        }
        h.succeed();
    }
}
