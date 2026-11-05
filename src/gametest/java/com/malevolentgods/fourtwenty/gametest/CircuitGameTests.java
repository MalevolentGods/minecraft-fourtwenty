package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.power.Circuits;
import com.malevolentgods.fourtwenty.power.HitBoxBlock;
import com.malevolentgods.fourtwenty.processing.MachineBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** The Wick network as a circuit, in the world (specs/006-circuit-solver). Numbers in comments are the hand working. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class CircuitGameTests {
    private CircuitGameTests() {}

    private static void hookah(GameTestHelper h, BlockPos pos, Block block) {
        h.setBlock(pos, block);
        var hookah = (WeedBongBlockEntity) h.getBlockEntity(pos);
        hookah.setItem(WeedBongBlockEntity.WEED_SLOT, new ItemStack(ModItems.WEED_BUD.get(), 4));
        hookah.setItem(WeedBongBlockEntity.WATER_SLOT, new ItemStack(Items.WATER_BUCKET));
        hookah.setItem(WeedBongBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL, 4));
    }

    private static void machine(GameTestHelper h, BlockPos pos, Block block) {
        h.setBlock(pos, block);
        ((MachineBlockEntity) h.getBlockEntity(pos)).setItem(0, ModDataComponents.withStrain(new ItemStack(ModItems.ROUGH_BUD.get(), 64), Strain.LANDRACE));
    }

    private static Circuits.Reading read(GameTestHelper h, BlockPos pos) {
        return Circuits.reading(h.getLevel(), h.absolutePos(pos)).orElseThrow(() -> new AssertionError("no reading at " + pos));
    }

    private static void wicks(GameTestHelper h, Block wick, int y, int z, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) h.setBlock(new BlockPos(x, y, z), wick);
    }

    /** The faces of a Wick at {@code end} other than back along the line (west): up to five spots for machines. */
    private static List<BlockPos> around(BlockPos end) {
        return List.of(end.east(), end.above(), end.below(), end.north(), end.south());
    }

    /** The standard base: one Hookah, a short line, four Snip Stations, each at its full 105 W, about 4.2 Kb and 25 Ig. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aHookahRunsFourMachinesAtFullPower(GameTestHelper h) {
        hookah(h, new BlockPos(1, 2, 3), ModBlocks.WEED_BONG.get());
        wicks(h, ModPower.HEMP_WICK.get(), 2, 3, 2, 5);
        List<BlockPos> machines = around(new BlockPos(5, 2, 3)).subList(0, 4);
        for (BlockPos m : machines) machine(h, m, ModPower.SNIP_STATION.get());
        h.runAfterDelay(80, () -> {
            for (BlockPos m : machines) {
                var r = read(h, m);
                h.assertTrue(Math.abs(r.watts() - 105) < 0.5, "105 W at " + m + ", got " + r.watts());
                h.assertTrue(r.volts() > 4.15 && r.volts() <= 4.2, "about 4.2 Kb, got " + r.volts());
                h.assertTrue(((MachineBlockEntity) h.getBlockEntity(m)).progress() > 0 || ((MachineBlockEntity) h.getBlockEntity(m)).getItem(1).getCount() > 0, "and it's working");
            }
            h.succeed();
        });
    }

    /** Eight machines on a Hookah sized for four: every one slows by the same share, to about half. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void eightMachinesBrownOutEvenly(GameTestHelper h) {
        hookah(h, new BlockPos(1, 2, 3), ModBlocks.WEED_BONG.get());
        wicks(h, ModPower.HEMP_WICK.get(), 2, 3, 2, 5);
        List<BlockPos> machines = new ArrayList<>(around(new BlockPos(5, 2, 3)));
        machines.addAll(List.of(new BlockPos(3, 3, 3), new BlockPos(3, 1, 3), new BlockPos(3, 2, 4)));
        for (BlockPos m : machines) machine(h, m, ModPower.SNIP_STATION.get());
        h.runAfterDelay(80, () -> {
            double first = read(h, machines.get(0)).watts();
            for (BlockPos m : machines) {
                double w = read(h, m).watts();
                h.assertTrue(Math.abs(w - first) < 0.01 * first, "even: " + w + " against " + first);
            }
            h.assertTrue(first > 45 && first < 60, "about half of 105 W, got " + first);
            h.succeed();
        });
    }

    /** 100 Hemp Wicks to four machines at 100 Ig, half the Wick's rating: about 1.5% drop (3% at its full rating), machines still at 105 W. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aLongLineLosesALittle(GameTestHelper h) {
        hookah(h, new BlockPos(1, 2, 3), ModBlocks.WEED_BONG.get());
        wicks(h, ModPower.HEMP_WICK.get(), 2, 3, 2, 101);
        List<BlockPos> machines = around(new BlockPos(101, 2, 3)).subList(0, 4);
        for (BlockPos m : machines) machine(h, m, ModPower.SNIP_STATION.get());
        h.runAfterDelay(80, () -> {
            var near = read(h, new BlockPos(2, 2, 3)); var far = read(h, new BlockPos(101, 2, 3));
            double drop = (near.volts() - far.volts()) / near.volts();
            h.assertTrue(drop > 0.01 && drop < 0.025, "drop " + drop);
            for (BlockPos m : machines) h.assertTrue(Math.abs(read(h, m).watts() - 105) < 0.5, "full power at the far end");
            h.succeed();
        });
    }

    /** The same run stepped up to Toke by Hit Boxes: for each Willard carried, its Wicks lose half what Puff Wicks do. */
    @GameTest(template = "long", timeoutTicks = 300)
    public static void steppingUpHalvesTheLoss(GameTestHelper h) {
        // Puff line along z = 1 for comparison
        hookah(h, new BlockPos(1, 2, 1), ModBlocks.WEED_BONG.get());
        wicks(h, ModPower.HEMP_WICK.get(), 2, 1, 2, 101);
        for (BlockPos m : around(new BlockPos(101, 2, 1)).subList(0, 2)) machine(h, m, ModPower.SNIP_STATION.get());
        // stepped line along z = 5: Hookah, Hemp Wick, Hit Box up (front east), 96 Rolled Wicks, Hit Box down (front west), Hemp Wick, machines
        hookah(h, new BlockPos(1, 2, 5), ModBlocks.WEED_BONG.get());
        h.setBlock(new BlockPos(2, 2, 5), ModPower.HEMP_WICK.get());
        h.setBlock(new BlockPos(3, 2, 5), ModPower.HIT_BOX.get().defaultBlockState().setValue(HitBoxBlock.FACING, Direction.EAST));
        wicks(h, ModPower.ROLLED_WICK.get(), 2, 5, 4, 99);
        h.setBlock(new BlockPos(100, 2, 5), ModPower.HIT_BOX.get().defaultBlockState().setValue(HitBoxBlock.FACING, Direction.WEST));
        h.setBlock(new BlockPos(101, 2, 5), ModPower.HEMP_WICK.get());
        for (BlockPos m : List.of(new BlockPos(102, 2, 5), new BlockPos(101, 3, 5))) machine(h, m, ModPower.SNIP_STATION.get());
        h.runAfterDelay(160, () -> {
            double puff = lossPerWatt(h, new BlockPos(2, 2, 1), new BlockPos(101, 2, 1));
            double toke = lossPerWatt(h, new BlockPos(4, 2, 5), new BlockPos(99, 2, 5));
            h.assertTrue(puff > 0 && toke > 0, "both lines carry power: " + puff + ", " + toke);
            // the Puff line is 100 blocks, the Toke line 96
            double ratio = (toke / 96) / (puff / 100);
            h.assertTrue(ratio > 0.4 && ratio < 0.6, "half per Willard, got " + ratio);
            for (BlockPos m : List.of(new BlockPos(102, 2, 5), new BlockPos(101, 3, 5))) h.assertTrue(read(h, m).watts() > 100, "the far machines run");
            h.succeed();
        });
    }

    private static double lossPerWatt(GameTestHelper h, BlockPos start, BlockPos end) {
        var a = read(h, start); var b = read(h, end);
        double carried = a.volts() * a.amps();
        return carried <= 0 ? 0 : (a.volts() - b.volts()) / a.volts() / carried;
    }

    /** A Double Hose (42 Kb) on a Hemp Wick (insulated for 6.3 Kb): the Wick burns, in smoke and ash, and nothing explodes. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void aWickBurnsOnTooHighAVoltage(GameTestHelper h) {
        hookah(h, new BlockPos(1, 2, 2), ModPower.DOUBLE_HOSE.get());
        h.setBlock(new BlockPos(2, 2, 2), ModPower.HEMP_WICK.get());
        machine(h, new BlockPos(3, 2, 2), ModPower.BUD_BARBER.get());
        h.runAfterDelay(80, () -> {
            h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 2));
            h.assertBlockPresent(ModPower.DOUBLE_HOSE.get(), new BlockPos(1, 2, 2));
            h.assertBlockPresent(ModPower.BUD_BARBER.get(), new BlockPos(3, 2, 2));
            h.succeed();
        });
    }

    /** The meter on one Snip Station: V × I = P and V / I = R, and R = V² / 105 W, to three figures. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void theMeterAgreesWithTheMaths(GameTestHelper h) {
        hookah(h, new BlockPos(1, 2, 2), ModBlocks.WEED_BONG.get());
        h.setBlock(new BlockPos(2, 2, 2), ModPower.HEMP_WICK.get());
        machine(h, new BlockPos(3, 2, 2), ModPower.SNIP_STATION.get());
        h.runAfterDelay(60, () -> {
            var r = read(h, new BlockPos(3, 2, 2));
            h.assertTrue(Math.abs(r.volts() * r.amps() - r.watts()) < 1e-3 * r.watts(), "V × I = P: " + r);
            h.assertTrue(Math.abs(r.volts() / r.amps() - r.ohms()) < 1e-3 * r.ohms(), "V / I = R: " + r);
            h.assertTrue(Math.abs(r.ohms() - r.volts() * r.volts() / 105) < 1e-3 * r.ohms(), "R = V² / P: " + r);
            var wick = read(h, new BlockPos(2, 2, 2));
            h.assertTrue(wick.wick() && Math.abs(wick.amps() - r.amps()) < 1e-3 * r.amps(), "the Wick carries the station's current: " + wick);
            h.assertTrue(wick.ampRating() == 200, "a Hemp Wick is rated 200 Ig");
            h.succeed();
        });
    }

    /**
     * The flatworld demo's Puff row: a Hookah at one end, a full Stash at the other, five machines, a bench, a Re-Up Station charging
     * boots and an Infuser along both sides. A Re-Up Station once drew two Hookahs through the Wick it sat on and burned it; nothing on
     * this row may burn now.
     */
    @GameTest(template = "long", timeoutTicks = 400)
    public static void theDemoRowNeverBurnsAWick(GameTestHelper h) {
        int z = 3;
        hookah(h, new BlockPos(1, 2, z), ModBlocks.WEED_BONG.get());
        wicks(h, ModPower.HEMP_WICK.get(), 2, z, 2, 12);
        h.setBlock(new BlockPos(13, 2, z), ModPower.STASH_JAR.get());
        ((com.malevolentgods.fourtwenty.power.StashBlockEntity) h.getBlockEntity(new BlockPos(13, 2, z))).node().receive(252_000);
        machine(h, new BlockPos(3, 2, z - 1), ModPower.SNIP_STATION.get());
        h.setBlock(new BlockPos(5, 2, z - 1), ModPower.DRY_BOX.get());
        ((MachineBlockEntity) h.getBlockEntity(new BlockPos(5, 2, z - 1))).setItem(0, ModDataComponents.withStrain(new ItemStack(ModItems.TRIMMED_BUD.get(), 64), Strain.LANDRACE));
        h.setBlock(new BlockPos(7, 2, z - 1), ModPower.GROW_TENT.get());
        h.setBlock(new BlockPos(9, 2, z - 1), ModPower.THINK_TANK.get());
        h.setBlock(new BlockPos(3, 2, z + 1), ModPower.CONSTRUCTION_JOINT.get());
        BlockPos station = new BlockPos(5, 2, z + 1);
        h.setBlock(station, com.malevolentgods.fourtwenty.gear.ModGear.RE_UP_STATIONS.get(0).get());
        ((com.malevolentgods.fourtwenty.gear.ReUpStationBlockEntity) h.getBlockEntity(station)).hang(new ItemStack(com.malevolentgods.fourtwenty.gear.ModGear.MOON_BOOTS.get()));
        h.setBlock(new BlockPos(7, 2, z + 1), ModBlocks.INFUSER.get());
        // and a station alone at the far end of a second line, fed only from its start
        hookah(h, new BlockPos(1, 2, 6), ModBlocks.WEED_BONG.get());
        h.setBlock(new BlockPos(2, 2, 6), ModPower.STASH_JAR.get());
        ((com.malevolentgods.fourtwenty.power.StashBlockEntity) h.getBlockEntity(new BlockPos(2, 2, 6))).node().receive(252_000);
        wicks(h, ModPower.HEMP_WICK.get(), 2, 6, 3, 20);
        BlockPos far = new BlockPos(21, 2, 6);
        h.setBlock(far, com.malevolentgods.fourtwenty.gear.ModGear.RE_UP_STATIONS.get(0).get());
        ((com.malevolentgods.fourtwenty.gear.ReUpStationBlockEntity) h.getBlockEntity(far)).hang(new ItemStack(com.malevolentgods.fourtwenty.gear.ModGear.MOON_BOOTS.get()));
        h.runAfterDelay(300, () -> {
            for (int x = 2; x <= 12; x++) h.assertBlockPresent(ModPower.HEMP_WICK.get(), new BlockPos(x, 2, z));
            for (int x = 3; x <= 20; x++) h.assertBlockPresent(ModPower.HEMP_WICK.get(), new BlockPos(x, 2, 6));
            var boots = ((com.malevolentgods.fourtwenty.gear.ReUpStationBlockEntity) h.getBlockEntity(station)).hung();
            h.assertTrue(com.malevolentgods.fourtwenty.gear.Gear.charge(boots) > 0, "the boots charge");
            h.succeed();
        });
    }
}
