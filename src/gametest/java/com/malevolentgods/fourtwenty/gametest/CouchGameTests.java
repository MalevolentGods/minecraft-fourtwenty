package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.CouchBlock;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class CouchGameTests {
    /** Place a couch as a player would: its shape and arms from the couches already round it. */
    private static void place(GameTestHelper h, String colour, int x, int z, Direction facing) {
        BlockPos rel = new BlockPos(x, 2, z);
        BlockState state = ModTerpenes.COUCHES.get(colour).get().defaultBlockState().setValue(CouchBlock.FACING, facing);
        h.setBlock(rel, CouchBlock.connected(state, h.getLevel(), h.absolutePos(rel)));
    }

    private static BlockState at(GameTestHelper h, int x, int z) { return h.getBlockState(new BlockPos(x, 2, z)); }

    private static void expect(GameTestHelper h, int x, int z, StairsShape shape, boolean left, boolean right) {
        BlockState s = at(h, x, z);
        h.assertTrue(s.getValue(CouchBlock.SHAPE) == shape, "(" + x + "," + z + ") shape " + s.getValue(CouchBlock.SHAPE) + ", expected " + shape);
        h.assertTrue(s.getValue(CouchBlock.LEFT_ARM) == left && s.getValue(CouchBlock.RIGHT_ARM) == right,
                "(" + x + "," + z + ") arms left " + s.getValue(CouchBlock.LEFT_ARM) + " right " + s.getValue(CouchBlock.RIGHT_ARM)
                        + ", expected " + left + " " + right);
    }

    @GameTest(template = "empty")
    public static void aRowOfCouchesIsOneSofaWithArmsAtTheEnds(GameTestHelper h) {
        // Backs to the north, so a sitter looks south: their left is east.
        place(h, "natural", 1, 2, Direction.NORTH);
        place(h, "lim_lav_lav", 2, 2, Direction.NORTH);       // colours join too
        place(h, "natural", 3, 2, Direction.NORTH);
        expect(h, 1, 2, StairsShape.STRAIGHT, false, true);
        expect(h, 2, 2, StairsShape.STRAIGHT, false, false);
        expect(h, 3, 2, StairsShape.STRAIGHT, true, false);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void twoRunsMeetInACorner(GameTestHelper h) {
        place(h, "natural", 1, 1, Direction.NORTH);
        place(h, "natural", 2, 1, Direction.NORTH);
        place(h, "natural", 2, 2, Direction.EAST);
        place(h, "natural", 2, 3, Direction.EAST);
        expect(h, 2, 1, StairsShape.INNER_RIGHT, false, false);  // back round the inside of the L
        expect(h, 1, 1, StairsShape.STRAIGHT, false, true);
        expect(h, 2, 2, StairsShape.STRAIGHT, false, false);
        expect(h, 2, 3, StairsShape.STRAIGHT, true, false);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void couchesFacingOppositeWaysKeepTheirArms(GameTestHelper h) {
        place(h, "natural", 2, 2, Direction.NORTH);
        place(h, "natural", 3, 2, Direction.SOUTH);
        expect(h, 2, 2, StairsShape.STRAIGHT, true, true);
        expect(h, 3, 2, StairsShape.STRAIGHT, true, true);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aPlayerSitsOneToACouchAndStandsUpWhenItBreaks(GameTestHelper h) {
        place(h, "natural", 2, 2, Direction.NORTH);
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        var a = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var b = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(com.malevolentgods.fourtwenty.block.SeatEntity.sit(h.getLevel(), pos, a), "couldn't sit");
        h.assertTrue(a.getVehicle() instanceof com.malevolentgods.fourtwenty.block.SeatEntity, "not riding a seat");
        h.assertTrue(!com.malevolentgods.fourtwenty.block.SeatEntity.sit(h.getLevel(), pos, b), "two sat on one couch");
        h.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        h.succeedWhen(() -> {
            h.assertTrue(!a.isPassenger(), "still sitting after the couch broke");
            h.assertTrue(!com.malevolentgods.fourtwenty.block.SeatEntity.occupied(h.getLevel(), pos), "the seat is still there");
        });
    }

    @GameTest(template = "empty")
    public static void sitCommandSitsOnTheGroundAndOnACouch(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        var a = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos feet = h.absolutePos(new BlockPos(1, 2, 1));
        a.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        a.setOnGround(true);
        h.assertTrue(com.malevolentgods.fourtwenty.block.SeatEntity.sitHere(a), "couldn't sit on the ground");
        h.assertTrue(Math.abs(a.getVehicle().getY() - (feet.getY() - 0.15)) < 1e-6, "ground seat at the wrong height: " + a.getVehicle().getY());
        a.stopRiding();
        h.assertTrue(Math.abs(a.getY() - feet.getY()) < 1e-6, "stood up at " + a.getY() + ", not where they sat");

        place(h, "natural", 3, 2, Direction.NORTH);
        var b = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos couch = h.absolutePos(new BlockPos(3, 2, 2));
        b.moveTo(couch.getX() + 0.5, couch.getY() + 0.5, couch.getZ() + 0.5);
        b.setOnGround(true);
        h.assertTrue(com.malevolentgods.fourtwenty.block.SeatEntity.sitHere(b), "/sit on a couch didn't sit");
        h.assertTrue(Math.abs(b.getVehicle().getY() - (couch.getY() + 0.35)) < 1e-6, "couch seat at the wrong height");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aCouchSomeoneElseSitsOnIsTaken(GameTestHelper h) {
        // Another mod's seat (MineColonies sits a citizen on an entity most of a block below the seat).
        place(h, "natural", 2, 2, Direction.NORTH);
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        var seat = net.minecraft.world.entity.EntityType.BOAT.create(h.getLevel());   // a plain (not living) entity, like MineColonies' SittingEntity
        seat.moveTo(pos.getX() + 0.5, pos.getY() - 0.65, pos.getZ() + 0.5);
        seat.setNoGravity(true);
        h.getLevel().addFreshEntity(seat);
        var rider = net.minecraft.world.entity.EntityType.VILLAGER.create(h.getLevel());
        rider.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        h.getLevel().addFreshEntity(rider);
        h.assertTrue(rider.startRiding(seat, true), "test setup: the rider didn't mount");
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(!com.malevolentgods.fourtwenty.block.SeatEntity.sit(h.getLevel(), pos, player), "sat on an occupied couch");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fencesAndWallsDontJoinACouch(GameTestHelper h) {
        place(h, "natural", 2, 2, Direction.NORTH);
        h.setBlock(new BlockPos(3, 2, 2), Blocks.OAK_FENCE);
        h.setBlock(new BlockPos(1, 2, 2), Blocks.COBBLESTONE_WALL);
        h.setBlock(new BlockPos(2, 2, 3), Blocks.GLASS_PANE);
        h.setBlock(new BlockPos(3, 2, 3), Blocks.OAK_FENCE);   // a fence the first one can join, to show joining works here
        BlockState fence = h.getBlockState(new BlockPos(3, 2, 2));
        h.assertTrue(!fence.getValue(FenceBlock.WEST), "the fence joined the couch");
        h.assertTrue(fence.getValue(FenceBlock.SOUTH), "the fence didn't join the fence beside it");
        BlockState wall = h.getBlockState(new BlockPos(1, 2, 2));
        h.assertTrue(wall.getValue(net.minecraft.world.level.block.WallBlock.EAST_WALL) == net.minecraft.world.level.block.state.properties.WallSide.NONE,
                "the wall joined the couch");
        h.assertTrue(!h.getBlockState(new BlockPos(2, 2, 3)).getValue(net.minecraft.world.level.block.IronBarsBlock.NORTH), "the pane joined the couch");
        h.succeed();
    }
}
