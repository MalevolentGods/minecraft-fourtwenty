package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.Circuit;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.power.Circuits;
import com.malevolentgods.fourtwenty.power.WickBlock;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The Wick network as a circuit, in the world (specs/006-circuit-solver, specs/007-polarity-circuits). Every circuit here is a closed
 * loop, mostly a {@link Ladder}'s + and − rails with blocks between them. Numbers in comments are the hand working.
 */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class CircuitGameTests {
    private CircuitGameTests() {}

    private static void stock(GameTestHelper h, BlockPos pos, Block block) {
        var m = (MachineBlockEntity) h.getBlockEntity(pos);
        boolean cures = block == ModPower.DRY_BOX.get() || block == ModPower.CURE_CLOSET.get() || block == ModPower.THE_HUMIDOR.get();
        m.setItem(0, ModDataComponents.withStrain(new ItemStack(cures ? ModItems.TRIMMED_BUD.get() : ModItems.ROUGH_BUD.get(), 64), Strain.LANDRACE));
    }

    private static BlockPos machine(Ladder l, int x, Block block) {
        BlockPos pos = l.put(x, block);
        stock(l.h, pos, block);
        return pos;
    }

    private static Circuits.Reading read(GameTestHelper h, BlockPos pos) {
        return Circuits.reading(h.getLevel(), h.absolutePos(pos)).orElseThrow(() -> new AssertionError("no reading at " + pos));
    }

    /** The standard base: one Hookah, four Snip Stations in parallel, each at its full 105 W, about 4.2 Kb and 25 Ig. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aHookahRunsFourMachinesAtFullPower(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 9);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        List<BlockPos> machines = new ArrayList<>();
        for (int x = 3; x <= 9; x += 2) machines.add(machine(l, x, ModPower.SNIP_STATION.get()));
        h.runAfterDelay(80, () -> {
            for (BlockPos m : machines) {
                var r = read(h, m);
                h.assertTrue(Math.abs(r.watts() - 105) < 1.5, "about 105 W at " + m + ", got " + r);
                h.assertTrue(r.volts() > 4.1 && r.volts() <= 4.2, "about 4.2 Kb across it, got " + r);
                var be = (MachineBlockEntity) h.getBlockEntity(m);
                h.assertTrue(be.progress() > 0 || be.getItem(MachineBlockEntity.INPUTS).getCount() > 0, "and it's working");
            }
            h.succeed();
        });
    }

    /**
     * Eight machines on a Hookah sized for four: it holds its 105 Ig limit, the line sags under the machines' rated 4.2 Kb, and each is
     * its rated 0.168 Φ: eight in parallel are 0.021 Φ, so about 2.2 Kb and 29 W each, all alike.
     */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void eightMachinesBrownOutEvenly(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 17);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        List<BlockPos> machines = new ArrayList<>();
        for (int x = 3; x <= 17; x += 2) machines.add(machine(l, x, ModPower.SNIP_STATION.get()));
        h.runAfterDelay(80, () -> {
            double first = read(h, machines.get(0)).watts();
            for (BlockPos m : machines) {
                double w = read(h, m).watts();
                h.assertTrue(Math.abs(w - first) < 0.03 * first, "even: " + w + " against " + first);
            }
            h.assertTrue(first > 22 && first < 36, "about 29 W, got " + first);
            h.succeed();
        });
    }

    /** 100 blocks of + and − rail to four machines at 100 Ig: each rail drops about 1.5%, about 3% round the loop. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aLongLineLosesALittle(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 107);
        BlockPos hookah = l.hookah(1, ModBlocks.WEED_BONG.get());
        List<BlockPos> machines = new ArrayList<>();
        for (int x = 101; x <= 107; x += 2) machines.add(machine(l, x, ModPower.SNIP_STATION.get()));
        h.runAfterDelay(80, () -> {
            double source = read(h, hookah).volts(), far = read(h, machines.get(0)).volts();
            double drop = 1 - far / source;
            h.assertTrue(drop > 0.02 && drop < 0.045, "drop " + drop);
            for (BlockPos m : machines) h.assertTrue(read(h, m).watts() > 95, "near full power at the far end: " + read(h, m));
            h.succeed();
        });
    }

    /**
     * The same 420 W carried 100 blocks at Toke (two Bud Barbers off a Double Hose) loses half the share it does at Puff (four Snip
     * Stations off a Hookah): a tenth of the current, through Wicks of fifty times the resistance.
     */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void steppingUpHalvesTheLoss(GameTestHelper h) {
        var puff = new Ladder(h, 2, 0).rails(ModPower.HEMP_WICK.get(), 1, 107);
        BlockPos hookah = puff.hookah(1, ModBlocks.WEED_BONG.get());
        List<BlockPos> snips = new ArrayList<>();
        for (int x = 101; x <= 107; x += 2) snips.add(machine(puff, x, ModPower.SNIP_STATION.get()));
        var toke = new Ladder(h, 2, 4).rails(ModPower.ROLLED_WICK.get(), 1, 103);
        BlockPos hose = toke.hookah(1, ModPower.DOUBLE_HOSE.get());
        List<BlockPos> barbers = new ArrayList<>();
        for (int x = 101; x <= 103; x += 2) barbers.add(machine(toke, x, ModPower.BUD_BARBER.get()));
        h.runAfterDelay(100, () -> {
            double puffLoss = 1 - read(h, snips.get(0)).volts() / read(h, hookah).volts();
            double tokeLoss = 1 - read(h, barbers.get(0)).volts() / read(h, hose).volts();
            double ratio = tokeLoss / puffLoss;
            h.assertTrue(ratio > 0.4 && ratio < 0.6, "half the loss: Puff " + puffLoss + ", Toke " + tokeLoss);
            h.succeed();
        });
    }

    /** A Double Hose (42 Kb) on Hemp Wicks (insulated for 6.3 Kb): a Wick burns, in smoke and ash, and nothing explodes. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void aWickBurnsOnTooHighAVoltage(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 3);
        l.hookah(1, ModPower.DOUBLE_HOSE.get());
        machine(l, 3, ModPower.BUD_BARBER.get());
        h.runAfterDelay(80, () -> {
            boolean burned = false;
            for (int x = 1; x <= 3; x++) burned |= h.getBlockState(l.plus(x)).isAir() || h.getBlockState(l.minus(x)).isAir();
            h.assertTrue(burned, "a Hemp Wick burned");
            h.assertBlockPresent(ModPower.DOUBLE_HOSE.get(), l.at(1));
            h.assertBlockPresent(ModPower.BUD_BARBER.get(), l.at(3));
            h.succeed();
        });
    }

    /** The meter on one Snip Station: V × I = P, V / I = R, R = V² / P, and its + rail carries its current. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void theMeterAgreesWithTheMaths(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 3);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos snip = machine(l, 3, ModPower.SNIP_STATION.get());
        h.runAfterDelay(60, () -> {
            var r = read(h, snip);
            h.assertTrue(Math.abs(r.volts() * r.amps() - r.watts()) < 1e-3 * r.watts(), "V × I = P: " + r);
            h.assertTrue(Math.abs(r.volts() / r.amps() - r.ohms()) < 1e-3 * r.ohms(), "V / I = R: " + r);
            h.assertTrue(Math.abs(r.ohms() - r.volts() * r.volts() / r.watts()) < 1e-3 * r.ohms(), "R = V² / P: " + r);
            var wick = read(h, l.plus(2));
            h.assertTrue(wick.wick() && Math.abs(wick.amps() - r.amps()) < 1e-2 * r.amps(), "the + rail carries the station's current: " + wick + " vs " + r);
            h.assertTrue(wick.ampRating() == 200, "a Hemp Wick is rated 200 Ig");
            h.succeed();
        });
    }

    /**
     * The flatworld demo's Puff row, as a ladder: a Hookah, a full Stash, machines, a bench and a Re-Up Station charging a Blazer. A Re-Up
     * Station once drew two Hookahs through its Wick and burned it; it fills at one Hookah now, and no Wick may burn.
     */
    @GameTest(template = "long", timeoutTicks = 500)
    public static void theDemoRowNeverBurnsAWick(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 23);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        machine(l, 3, ModPower.SNIP_STATION.get());
        machine(l, 5, ModPower.DRY_BOX.get());
        l.put(7, ModPower.GROW_TENT.get());
        l.put(9, ModPower.THINK_TANK.get());
        l.put(11, com.malevolentgods.fourtwenty.registry.ModTerpenes.TERPINATOR.get());
        l.put(13, ModPower.CONSTRUCTION_JOINT.get());
        BlockPos station = l.put(15, com.malevolentgods.fourtwenty.gear.ModGear.RE_UP_STATIONS.get(0).get());
        ((com.malevolentgods.fourtwenty.gear.ReUpStationBlockEntity) h.getBlockEntity(station)).hang(new ItemStack(com.malevolentgods.fourtwenty.gear.ModGear.BLAZER.get()));
        BlockPos jar = l.put(23, ModPower.STASH_JAR.get());
        ((com.malevolentgods.fourtwenty.power.StashBlockEntity) h.getBlockEntity(jar)).node().receive(252_000);
        double[] most = new double[24];
        h.onEachTick(() -> {
            for (int x = 1; x <= 23; x++) for (BlockPos w : new BlockPos[]{l.plus(x), l.minus(x)}) {
                var r = Circuits.reading(h.getLevel(), h.absolutePos(w));
                if (r.isPresent()) most[x] = Math.max(most[x], r.get().amps());
            }
        });
        h.runAfterDelay(400, () -> {
            for (int x = 1; x <= 23; x++) {
                h.assertBlockPresent(ModPower.HEMP_WICK.get(), l.plus(x));
                h.assertBlockPresent(ModPower.HEMP_WICK.get(), l.minus(x));
                h.assertTrue(most[x] < 190, "the most through any Wick at x=" + x + ": " + most[x]);
            }
            var s = (com.malevolentgods.fourtwenty.gear.ReUpStationBlockEntity) h.getBlockEntity(station);
            h.assertTrue(s.node().stored() > 0 || com.malevolentgods.fourtwenty.gear.Gear.charge(s.hung()) > 0, "the station fills and charges");
            // the row asks more than its Hookah and Stash can give while the bench and station fill, so it browns out evenly, but runs
            h.assertTrue(read(h, l.at(3)).watts() > 20, "the machines run: " + read(h, l.at(3)));
            h.succeed();
        });
    }

    /** Only red and black Wicks plug into a block: a machine with an undyed Wick for its return gets nothing, and the meter says so. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void aMachineWithNoBlackWickGetsNothing(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 3);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        h.setBlock(l.minus(3), ModPower.HEMP_WICK.get());
        BlockPos snip = l.at(3);
        h.setBlock(snip, ModPower.SNIP_STATION.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        stock(h, snip, ModPower.SNIP_STATION.get());
        h.runAfterDelay(60, () -> {
            var r = read(h, snip);
            h.assertTrue(r.status() == Circuit.NO_LOOP && r.watts() == 0, "no return, nothing: " + r);
            h.succeed();
        });
    }

    /** A supply line only, as built before polarity: the machine gets nothing, nothing burns, and the meter says "no return path". */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void aSupplyLineAloneCarriesNothing(GameTestHelper h) {
        var l = new Ladder(h, 2, 1);
        for (int x = 1; x <= 3; x++) h.setBlock(l.plus(x), ModPower.HEMP_WICK.get());
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos snip = machine(l, 3, ModPower.SNIP_STATION.get());
        h.runAfterDelay(60, () -> {
            var r = read(h, snip);
            h.assertTrue(r.status() == Circuit.NO_LOOP && r.watts() == 0, "no return path: " + r);
            for (int x = 1; x <= 3; x++) h.assertBlockPresent(ModPower.HEMP_WICK.get(), l.plus(x));
            h.succeed();
        });
    }


    /** A red supply and a black return touching all the way don't join, so the machine runs. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void colouredLinesSideBySideStayApart(GameTestHelper h) {
        var red = WickBlock.Colour.RED;
        var black = WickBlock.Colour.BLACK;
        var l = new Ladder(h, 2, 1);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos snip = machine(l, 3, ModPower.SNIP_STATION.get());
        for (int x = 1; x <= 3; x++) Ladder.wick(h, l.plus(x), ModPower.HEMP_WICK.get(), red);
        // the return climbs from the station's −, runs back over the + rail touching it all the way, and comes down to the Hookah's −
        Ladder.wick(h, l.minus(3), ModPower.HEMP_WICK.get(), black);
        Ladder.wick(h, l.minus(1), ModPower.HEMP_WICK.get(), black);
        for (int z = l.minusZ(); z >= l.plusZ; z--) {
            Ladder.wick(h, new BlockPos(3, 3, z), ModPower.HEMP_WICK.get(), black);
            Ladder.wick(h, new BlockPos(1, 3, z), ModPower.HEMP_WICK.get(), black);
        }
        Ladder.wick(h, new BlockPos(2, 3, l.plusZ), ModPower.HEMP_WICK.get(), black);
        h.runAfterDelay(60, () -> {
            h.assertTrue(read(h, snip).watts() > 100, "runs: " + read(h, snip));
            h.succeed();
        });
    }
}
