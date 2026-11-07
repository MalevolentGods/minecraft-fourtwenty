package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.electronics.ComponentBlock;
import com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity;
import com.malevolentgods.fourtwenty.electronics.ComponentType;
import com.malevolentgods.fourtwenty.electronics.ModElectronics;
import com.malevolentgods.fourtwenty.power.WickBlock;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Electronics in the world (docs/electronics/audit-and-plan.md): components on a Hookah's red and black rails, wired by their pin faces.
 * Each Nug sits on the row facing west: front (output) to the west, back (input) to the east, VDD on top and GND below.
 */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class ElectronicsGameTests {
    private ElectronicsGameTests() {}

    private static BlockPos component(GameTestHelper h, BlockPos pos, ComponentType type) {
        h.setBlock(pos, ModElectronics.COMPONENTS.get(type).get().defaultBlockState().setValue(ComponentBlock.FACING, Direction.WEST));
        return pos;
    }

    /** VDD from the red rail over the top, GND from the black rail underneath, for a Nug at x on a Ladder's row. */
    private static void power(GameTestHelper h, Ladder l, int x) {
        Ladder.wick(h, l.at(x).above(), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
        Ladder.wick(h, l.plus(x).above(), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
        Ladder.wick(h, l.at(x).below(), ModPower.HEMP_WICK.get(), WickBlock.Colour.BLACK);
        Ladder.wick(h, l.minus(x).below(), ModPower.HEMP_WICK.get(), WickBlock.Colour.BLACK);
    }

    private static boolean on(GameTestHelper h, BlockPos pos) { return h.getBlockState(pos).getValue(ComponentBlock.ON); }

    /** A NOT Nug with a Toggle Joint on its input: high while the switch is open, low once it's closed, as its truth table says. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aNotNugInvertsAToggleJoint(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 7);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos nug = component(h, l.at(5), ComponentType.NOT_NUG);
        power(h, l, 5);
        BlockPos toggle = component(h, l.at(6), ComponentType.TOGGLE_JOINT);
        // the toggle's back pin to the red rail, through a red Wick on its east side
        Ladder.wick(h, l.at(7), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
        h.setBlock(l.at(4), ModElectronics.HEMP_THREAD.get());
        h.runAfterDelay(40, () -> {
            h.assertTrue(on(h, nug), "input open (0): the output is high");
            ((ComponentBlockEntity) h.getBlockEntity(toggle)).closed(true);
            h.runAfterDelay(10, () -> {
                h.assertFalse(on(h, nug), "input closed to VDD (1): the output is low");
                h.succeed();
            });
        });
    }

    /** A Ticker at 0.1 s on the rails: its output turns over every tick (50 ms), and keeps on doing it. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aTickerTicks(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 7);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos ticker = component(h, l.at(5), ComponentType.TICKER);
        power(h, l, 5);
        var be = (ComponentBlockEntity) h.getBlockEntity(ticker);
        // from 1 s (its default) down to 0.1 s
        be.step(-3);
        int[] changes = {0};
        boolean[] last = {false};
        h.onEachTick(() -> {
            boolean now = on(h, ticker);
            if (now != last[0]) changes[0]++;
            last[0] = now;
        });
        h.runAfterDelay(100, () -> {
            h.assertTrue(changes[0] >= 30, "it ticks: " + changes[0] + " edges in 100 ticks");
            h.succeed();
        });
    }

    /** A Bench Rig set to 3.3 Kb across a 1 kΦ Resinator between its rails: 3.3 mIg flows, by Ohm's law. (Hemp rails are insulated to 6.3 Kb.) */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aBenchRigDrivesOhmsLaw(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 5);
        h.setBlock(l.at(1), ModPower.BENCH_RIG.get());
        var rig = (com.malevolentgods.fourtwenty.power.SupplyBlockEntity) h.getBlockEntity(l.at(1));
        while (Math.abs(rig.volts() - 3.3) > 1e-9) rig.stepVolts(1);
        // facing north: its front pin to the red rail (north), its back pin to the black one (south)
        BlockPos r = l.at(4);
        h.setBlock(r, ModElectronics.COMPONENTS.get(ComponentType.RESINATOR).get().defaultBlockState().setValue(ComponentBlock.FACING, Direction.NORTH));
        h.runAfterDelay(20, () -> {
            var found = com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(r));
            h.assertTrue(found.isPresent(), "a reading");
            var reading = found.get();
            double across = Math.abs(reading.pins()[0] - reading.pins()[1]);
            h.assertTrue(Math.abs(across - 3.3) < 0.02, "3.3 Kb across it, got " + across);
            h.assertTrue(Math.abs(Math.abs(reading.amps()) - 0.0033) < 3e-5, "3.3 mIg through it, got " + reading.amps());
            h.succeed();
        });
    }

    /** A Bottomless Stash powers a NOT Nug with nothing on its input: its output is high, and it never runs dry. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aBottomlessStashPowersLogic(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 7);
        h.setBlock(l.at(1), ModPower.BOTTOMLESS_STASH_PUFF.get());
        BlockPos nug = component(h, l.at(5), ComponentType.NOT_NUG);
        power(h, l, 5);
        h.runAfterDelay(20, () -> {
            h.assertTrue(on(h, nug), "powered, input low: output high");
            h.succeed();
        });
    }

    /** A 1 Φ Resinator across a Bench Rig at 4.2 Kb dissipates 17.6 W, over its 10 W rating: it burns out in smoke, and nothing else breaks. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void anOverloadedResinatorBurnsOut(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 5);
        h.setBlock(l.at(1), ModPower.BENCH_RIG.get());
        var rig = (com.malevolentgods.fourtwenty.power.SupplyBlockEntity) h.getBlockEntity(l.at(1));
        while (Math.abs(rig.amps() - 10) > 1e-9) rig.stepAmps(1);
        BlockPos r = l.at(4);
        h.setBlock(r, ModElectronics.COMPONENTS.get(ComponentType.RESINATOR).get().defaultBlockState().setValue(ComponentBlock.FACING, Direction.NORTH));
        ((ComponentBlockEntity) h.getBlockEntity(r)).step(-((ComponentBlockEntity) h.getBlockEntity(r)).type().defaultIndex());
        h.succeedWhen(() -> {
            h.assertTrue(h.getBlockState(r).isAir(), "the Resinator burned out");
            h.assertBlockPresent(ModPower.BENCH_RIG.get(), l.at(1));
        });
    }
    private static void place(GameTestHelper h, BlockPos pos, ComponentType type, Direction facing) {
        h.setBlock(pos, ModElectronics.COMPONENTS.get(type).get().defaultBlockState().setValue(ComponentBlock.FACING, facing));
    }

    private static void red(GameTestHelper h, int x, int y, int z) { Ladder.wick(h, new BlockPos(x, y, z), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED); }

    private static void black(GameTestHelper h, int x, int y, int z) { Ladder.wick(h, new BlockPos(x, y, z), ModPower.HEMP_WICK.get(), WickBlock.Colour.BLACK); }

    /**
     * A Latch Nug facing north south of the rails: its left (set) is east and its right (reset) west, each through a Toggle Joint to VDD.
     * Set, release: it holds high. Reset, release: it holds low.
     */
    @GameTest(template = "long", timeoutTicks = 300)
    public static void aLatchNugRemembers(GameTestHelper h) {
        var l = new Ladder(h, 2, 0).rails(ModPower.HEMP_WICK.get(), 1, 7);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos latch = new BlockPos(5, 2, 4), set = new BlockPos(6, 2, 4), reset = new BlockPos(4, 2, 4);
        place(h, latch, ComponentType.LATCH_NUG, Direction.NORTH);
        for (int z = 0; z <= 4; z++) red(h, 5, 3, z);
        for (int z = 2; z <= 4; z++) black(h, 5, 1, z);
        place(h, set, ComponentType.TOGGLE_JOINT, Direction.EAST);
        place(h, reset, ComponentType.TOGGLE_JOINT, Direction.EAST);
        red(h, 7, 2, 4);
        red(h, 7, 3, 4);
        red(h, 6, 3, 4);
        red(h, 3, 2, 4);
        red(h, 3, 3, 4);
        red(h, 4, 3, 4);
        h.setBlock(new BlockPos(5, 2, 3), ModElectronics.HEMP_THREAD.get());
        var s = (ComponentBlockEntity) h.getBlockEntity(set);
        var r = (ComponentBlockEntity) h.getBlockEntity(reset);
        h.runAfterDelay(30, () -> {
            h.assertFalse(on(h, latch), "starts reset");
            s.closed(true);
            h.runAfterDelay(10, () -> {
                h.assertTrue(on(h, latch), "set");
                s.closed(false);
                h.runAfterDelay(10, () -> {
                    h.assertTrue(on(h, latch), "holds after the set is released");
                    r.closed(true);
                    h.runAfterDelay(10, () -> {
                        h.assertFalse(on(h, latch), "reset");
                        r.closed(false);
                        h.runAfterDelay(10, () -> {
                            h.assertFalse(on(h, latch), "holds reset");
                            h.succeed();
                        });
                    });
                });
            });
        });
    }

    /**
     * Along the row from the Bench Rig (4.2 Kb): a red Wick, a Toggle Joint, a 1 kΦ Resinator, a 1 mGa Kief Cap, a black Wick. Close the
     * toggle: after one time constant (1 s, 20 ticks) the cap has about 63% of the supply, after five nearly all of it.
     */
    @GameTest(template = "long", timeoutTicks = 300)
    public static void aKiefCapChargesOnItsTimeConstant(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 7);
        h.setBlock(l.at(1), ModPower.BENCH_RIG.get());
        red(h, 2, 2, 2);
        BlockPos toggle = l.at(3), cap = l.at(5);
        place(h, toggle, ComponentType.TOGGLE_JOINT, Direction.EAST);
        place(h, l.at(4), ComponentType.RESINATOR, Direction.EAST);
        place(h, cap, ComponentType.KIEF_CAP, Direction.EAST);
        black(h, 6, 2, 2);
        java.util.function.Supplier<Double> volts = () -> {
            var reading = com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(cap));
            return reading.map(r -> Math.abs(r.pins()[0] - r.pins()[1])).orElse(-1.0);
        };
        h.runAfterDelay(10, () -> {
            h.assertTrue(volts.get() >= 0 && volts.get() < 0.05, "starts empty: " + volts.get());
            ((ComponentBlockEntity) h.getBlockEntity(toggle)).closed(true);
            h.runAfterDelay(20, () -> {
                double one = volts.get(), want = 4.2 * (1 - Math.exp(-1));
                h.assertTrue(Math.abs(one - want) < 0.25, "about 63% after a second: " + one + " for " + want);
                h.runAfterDelay(80, () -> {
                    h.assertTrue(volts.get() > 4.1, "nearly full after five: " + volts.get());
                    h.succeed();
                });
            });
        });
    }
}
