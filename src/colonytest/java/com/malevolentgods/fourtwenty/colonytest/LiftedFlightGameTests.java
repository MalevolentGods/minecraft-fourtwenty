package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.Lifted;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.LiftedFlight;
import com.malevolentgods.fourtwenty.domain.FlightPlanner;
import com.malevolentgods.fourtwenty.domain.Strain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Lifted flight in the world (spec 013 T032): routes through real blocks, gentle landings, and the saved buff. */
@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class LiftedFlightGameTests {
    private static BlockPos floor(GameTestHelper h) {
        var origin = h.absolutePos(new BlockPos(2, 1, 2));
        for (int dx = -2; dx <= 6; dx++) for (int dz = -2; dz <= 6; dz++) {
            h.getLevel().setBlockAndUpdate(origin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy <= 12; dy++) h.getLevel().setBlockAndUpdate(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
        }
        return origin;
    }

    @GameTest(template = "empty")
    public static void flightRoutesGoOverWallsAndNeverIntoSealedRooms(GameTestHelper h) {
        var level = h.getLevel();
        var o = floor(h);
        for (int y = 0; y < 3; y++) for (int dz = -2; dz <= 6; dz++) level.setBlockAndUpdate(o.offset(2, y, dz), Blocks.STONE.defaultBlockState());
        FlightPlanner.Free free = (x, y, z) -> {
            var s = level.getBlockState(new BlockPos(x, y, z));
            return !s.blocksMotion() && s.getFluidState().isEmpty();
        };
        var route = FlightPlanner.plan(new FlightPlanner.Cell(o.getX(), o.getY(), o.getZ()), new FlightPlanner.Cell(o.getX() + 4, o.getY(), o.getZ()), free, 24, 4000);
        h.assertTrue(!route.isEmpty(), "a route over the wall");
        h.assertTrue(route.stream().anyMatch(c -> c.y() >= o.getY() + 3), "it goes over the top");
        h.assertTrue(route.stream().allMatch(c -> FlightPlanner.fits(free, c.x(), c.y(), c.z())), "her body fits all the way");

        // A sealed glass box: no way in.
        var box = o.offset(4, 5, 4);
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++) {
            if (Math.abs(dx) == 1 || Math.abs(dz) == 1 || dy == -1 || dy == 2) level.setBlockAndUpdate(box.offset(dx, dy, dz), Blocks.GLASS.defaultBlockState());
        }
        h.assertTrue(FlightPlanner.plan(new FlightPlanner.Cell(o.getX(), o.getY(), o.getZ()), new FlightPlanner.Cell(box.getX(), box.getY(), box.getZ()),
                free, 24, 4000).isEmpty(), "no route into a sealed room");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aLandingFromTenUpNeverHurts(GameTestHelper h) {
        var o = floor(h);
        var mob = EntityType.VILLAGER.create(h.getLevel());
        h.assertTrue(mob != null, "a villager to drop");
        mob.moveTo(o.getX() + 0.5, o.getY() + 10, o.getZ() + 0.5);
        h.getLevel().addFreshEntity(mob);
        float health = mob.getHealth();
        h.onEachTick(() -> LiftedFlight.landStep(h.getLevel(), mob, o));
        h.succeedWhen(() -> {
            h.assertTrue(mob.onGround() && !mob.isNoGravity(), "still coming down: y=" + String.format("%.2f", mob.getY() - o.getY()) + " onGround=" + mob.onGround() + " noGravity=" + mob.isNoGravity() + " dy=" + String.format("%.3f", mob.getDeltaMovement().y));
            h.assertTrue(mob.getHealth() == health, "a gentle landing does no damage");
        });
    }

    @GameTest(template = "empty")
    public static void liftedBuffsSurviveASaveAndLoad(GameTestHelper h) {
        var lifted = new Lifted();
        lifted.start(3, 7, Strain.GOLDEN_GOD, 9000);
        lifted.start(3, 8, Strain.LANDRACE, 1200);
        lifted.start(4, 7, Strain.LANDRACE, 600);
        var tag = lifted.save(new CompoundTag(), h.getLevel().registryAccess());
        var back = Lifted.load(tag, h.getLevel().registryAccess());
        h.assertTrue(back.get(3, 7) != null && back.get(3, 7).strain() == Strain.GOLDEN_GOD && back.get(3, 7).ticksLeft() == 9000, "strain and time kept");
        h.assertTrue(back.get(4, 7) != null && back.get(4, 7).ticksLeft() == 600, "the same citizen id in another colony is kept apart");
        h.assertTrue(back.tick(3, 8, 1200) && back.get(3, 8) == null, "running out removes her buff");
        h.succeed();
    }
}
