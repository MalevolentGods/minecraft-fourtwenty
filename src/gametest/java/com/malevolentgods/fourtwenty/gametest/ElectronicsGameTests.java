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
            var r = com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(nug));
            h.assertTrue(on(h, nug), "input open (0): the output is high; pins " + r.map(x -> java.util.Arrays.toString(x.pins())).orElse("no reading"));
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
                int third = com.malevolentgods.fourtwenty.domain.electronics.Shown.level(((ComponentBlockEntity) h.getBlockEntity(cap)).shown());
                h.assertTrue(third >= 8 && third <= 11, "it shows about two thirds full: " + third);
                h.runAfterDelay(80, () -> {
                    h.assertTrue(volts.get() > 4.1, "nearly full after five: " + volts.get());
                    h.succeed();
                });
            });
        });
    }
    /**
     * Both FETs straight across a Bench Rig at 4.2 Kb, gates tied to a rail. A Sativa FET facing north: drain (front) on the red rail,
     * source (back) on the black one, its gate (left, east) on a red Wick. An Indica FET facing south: source on red, drain on black, its
     * gate (left, west) on a black Wick. Each is fully on, saturated: K/2·(Vgs − Vt)²·(1 + λ·Vds), out of the Indica's drain.
     */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void fetsConductOnTheSquareLaw(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 8);
        h.setBlock(l.at(1), ModPower.BENCH_RIG.get());
        var rig = (com.malevolentgods.fourtwenty.power.SupplyBlockEntity) h.getBlockEntity(l.at(1));
        while (Math.abs(rig.volts() - 4.2) > 1e-9) rig.stepVolts(rig.volts() < 4.2 ? 1 : -1);
        while (Math.abs(rig.amps() - 1) > 1e-9) rig.stepAmps(1);
        BlockPos sativa = l.at(4), indica = l.at(7);
        place(h, sativa, ComponentType.SATIVA_FET, Direction.NORTH);
        red(h, 5, 2, 2);
        place(h, indica, ComponentType.INDICA_FET, Direction.SOUTH);
        black(h, 6, 2, 2);
        double vt = com.malevolentgods.fourtwenty.domain.electronics.Active.Mosfet.VT, k = com.malevolentgods.fourtwenty.domain.electronics.Active.Mosfet.K,
                lambda = com.malevolentgods.fourtwenty.domain.electronics.Active.Mosfet.LAMBDA;
        h.runAfterDelay(20, () -> {
            for (BlockPos pos : new BlockPos[]{sativa, indica}) {
                var found = com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(pos));
                h.assertTrue(found.isPresent(), "a reading");
                double[] v = found.get().pins();
                boolean p = pos == indica;
                // {source, drain, gate}: the device's own sense is downward for an Indica FET
                double vgs = (p ? -1 : 1) * (v[2] - v[0]), vds = (p ? -1 : 1) * (v[1] - v[0]);
                h.assertTrue(vgs > 4.0, (p ? "Indica" : "Sativa") + " gate fully on: " + vgs);
                double want = (p ? -1 : 1) * k / 2 * (vgs - vt) * (vgs - vt) * (1 + lambda * vds);
                h.assertTrue(Math.abs(found.get().amps() - want) < 1e-4 * Math.abs(want), (p ? "Indica" : "Sativa") + " drain current " + found.get().amps() + " for " + want);
                h.assertTrue(on(h, pos), "shows it conducts");
            }
            h.succeed();
        });
    }

    /**
     * Two Hype Amps on the rails with no feedback, comparing: one facing west has + (left, north) on the red rail and − (right, south)
     * on the black, so it drives its output to near the top of its supply; the other faces east, the other way round, and drives it low.
     */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void hypeAmpsCompare(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 8);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos up = l.at(3), down = l.at(6);
        place(h, up, ComponentType.HYPE_AMP, Direction.WEST);
        place(h, down, ComponentType.HYPE_AMP, Direction.EAST);
        power(h, l, 3);
        power(h, l, 6);
        h.runAfterDelay(20, () -> {
            for (BlockPos pos : new BlockPos[]{up, down}) {
                var found = com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(pos));
                h.assertTrue(found.isPresent(), "a reading");
                double[] v = found.get().pins();
                double supply = v[0] - v[1], out = v[2] - v[1];
                h.assertTrue(supply > 1.5, "powered: " + supply);
                if (pos == up) h.assertTrue(out > 0.9 * supply && on(h, pos), "+ above −: high, " + out + " of " + supply);
                else h.assertTrue(out < 0.1 * supply && !on(h, pos), "+ below −: low, " + out + " of " + supply);
            }
            h.succeed();
        });
    }
    /**
     * A Sampler Nug facing east, off the rails (Ladder at z 0 and 2; the build at z 5). Its input bus (back, west) is an Eighth Ribbon
     * ending in a Roach Clip on channel 2, a red Wick on the clip; its select bus (left, south) a Ribbon and a clip on channel 1, a red
     * Wick on that too. Select 2 picks input 2, which is high: the output is high. Clip the input on channel 3 instead: low.
     */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aSamplerReadsARibbonThroughRoachClips(GameTestHelper h) {
        var l = new Ladder(h, 2, 0).rails(ModPower.HEMP_WICK.get(), 1, 8);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos sampler = new BlockPos(5, 2, 5), inClip = new BlockPos(3, 2, 5), selClip = new BlockPos(5, 2, 7);
        place(h, sampler, ComponentType.SAMPLER_NUG, Direction.EAST);
        for (int z = 0; z <= 7; z++) red(h, 5, 3, z);
        for (int z = 2; z <= 5; z++) black(h, 5, 1, z);
        h.setBlock(new BlockPos(4, 2, 5), ModElectronics.EIGHTH_RIBBON.get());
        h.setBlock(inClip, ModElectronics.ROACH_CLIP.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, 2));
        for (int z = 0; z <= 5; z++) red(h, 3, 3, z);
        h.setBlock(new BlockPos(5, 2, 6), ModElectronics.EIGHTH_RIBBON.get());
        h.setBlock(selClip, ModElectronics.ROACH_CLIP.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, 1));
        java.util.function.Supplier<double[]> pins = () -> com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(sampler))
                .map(com.malevolentgods.fourtwenty.power.Circuits.ComponentReading::pins).orElse(null);
        h.runAfterDelay(20, () -> {
            double[] v = pins.get();
            h.assertTrue(v != null, "a reading");
            double supply = v[0] - v[1];
            h.assertTrue(supply > 3.5, "powered: " + supply);
            h.assertTrue(v[2 + 2] - v[1] > 0.9 * supply, "input 2 high through the clip: " + java.util.Arrays.toString(v));
            h.assertTrue(v[11 + 1] - v[1] > 0.9 * supply && v[11] - v[1] < 0.1 * supply, "select reads 2");
            h.assertTrue(v[20] - v[1] > 0.9 * supply && on(h, sampler), "picks input 2: high");
            h.setBlock(inClip, h.getBlockState(inClip).setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, 3));
            com.malevolentgods.fourtwenty.power.BIGrid.changed(h.getLevel());
            h.runAfterDelay(10, () -> {
                double[] w = pins.get();
                h.assertTrue(w[2 + 3] - w[1] > 0.9 * supply, "now input 3 is high");
                h.assertTrue(w[20] - w[1] < 0.1 * supply && !on(h, sampler), "and input 2, still picked, is low");
                h.succeed();
            });
        });
    }

    /**
     * A Puff-Pass Nug facing east with data (back) tied to VDD, its clock (left, south) and latch (right, north) each on a Ticker at
     * 0.1 s through Hemp Thread, an Eighth Ribbon on its outputs. Ones shift in at every clock: within a few seconds all eight latched
     * outputs and the carry are high.
     */
    @GameTest(template = "long", timeoutTicks = 300)
    public static void aPuffPassNugFillsWithOnes(GameTestHelper h) {
        var l = new Ladder(h, 2, 0).rails(ModPower.HEMP_WICK.get(), 1, 8);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos chip = new BlockPos(5, 2, 5), clock = new BlockPos(5, 2, 7), latch = new BlockPos(5, 2, 3);
        place(h, chip, ComponentType.PUFF_PASS_NUG, Direction.EAST);
        place(h, clock, ComponentType.TICKER, Direction.NORTH);
        place(h, latch, ComponentType.TICKER, Direction.SOUTH);
        for (int z = 0; z <= 7; z++) red(h, 5, 3, z);
        for (int z = 2; z <= 7; z++) black(h, 5, 1, z);
        for (int z = 0; z <= 5; z++) red(h, 4, 3, z);
        red(h, 4, 2, 5);
        h.setBlock(new BlockPos(5, 2, 6), ModElectronics.HEMP_THREAD.get());
        h.setBlock(new BlockPos(5, 2, 4), ModElectronics.HEMP_THREAD.get());
        h.setBlock(new BlockPos(6, 2, 5), ModElectronics.EIGHTH_RIBBON.get());
        ((ComponentBlockEntity) h.getBlockEntity(clock)).step(-3);
        ((ComponentBlockEntity) h.getBlockEntity(latch)).step(-3);
        h.succeedWhen(() -> {
            var found = com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(chip));
            h.assertTrue(found.isPresent(), "a reading");
            double[] v = found.get().pins();
            double supply = v[0] - v[1];
            for (int k = 5; k <= 13; k++) h.assertTrue(v[k] - v[1] > 0.9 * supply, "pin " + k + " high: " + java.util.Arrays.toString(v));
            h.assertTrue(on(h, chip), "lit");
            // what it shows clients: all nine outputs, active; and its Ribbon, all nine wires lit
            int shown = ((ComponentBlockEntity) h.getBlockEntity(chip)).shown();
            h.assertTrue(com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(shown) == 0x1FF && com.malevolentgods.fourtwenty.domain.electronics.Shown.active(shown),
                    "shows its outputs: " + Integer.toBinaryString(shown));
            int wires = ((com.malevolentgods.fourtwenty.electronics.RibbonBlockEntity) h.getBlockEntity(new BlockPos(6, 2, 5))).shown();
            h.assertTrue(com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(wires) == 0x1FF, "its Ribbon glows on every wire: " + Integer.toBinaryString(wires));
        });
    }
    /**
     * A Stacker Nug facing east, off the rails: Roach Clips straight on its bus faces, A's (back, west) on channel 0 and B's (left,
     * south) on channel 1, each under a red Wick, so A = 1 and B = 2. It adds: 3, no carry. Used, as a player would, it subtracts:
     * 1 − 2 wraps to 255 and borrows. (Using a part changes its value; the circuit must follow.)
     */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aStackerNugAddsThenSubtracts(GameTestHelper h) {
        var l = new Ladder(h, 2, 0).rails(ModPower.HEMP_WICK.get(), 1, 8);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos stacker = new BlockPos(5, 2, 5);
        place(h, stacker, ComponentType.STACKER_NUG, Direction.EAST);
        for (int z = 0; z <= 6; z++) red(h, 5, 3, z);
        for (int z = 0; z <= 5; z++) red(h, 4, 3, z);
        for (int z = 2; z <= 5; z++) black(h, 5, 1, z);
        h.setBlock(new BlockPos(4, 2, 5), ModElectronics.ROACH_CLIP.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, 0));
        h.setBlock(new BlockPos(5, 2, 6), ModElectronics.ROACH_CLIP.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, 1));
        java.util.function.ToIntFunction<double[]> sum = v -> {
            int x = 0;
            for (int k = 0; k < 8; k++) if (v[21 + k] - v[1] > 0.5 * (v[0] - v[1])) x |= 1 << k;
            return x;
        };
        java.util.function.Supplier<double[]> pins = () -> com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(stacker))
                .map(com.malevolentgods.fourtwenty.power.Circuits.ComponentReading::pins).orElse(null);
        h.runAfterDelay(20, () -> {
            double[] v = pins.get();
            h.assertTrue(v != null, "a reading");
            h.assertTrue(sum.applyAsInt(v) == 3, "1 + 2 = 3: " + java.util.Arrays.toString(v));
            h.assertTrue(v[29] - v[1] < 0.1 * (v[0] - v[1]), "no carry");
            h.useBlock(stacker);
            h.runAfterDelay(10, () -> {
                double[] w = pins.get();
                h.assertTrue(sum.applyAsInt(w) == 255, "1 - 2 wraps to 255: " + java.util.Arrays.toString(w));
                h.assertTrue(w[29] - w[1] > 0.9 * (w[0] - w[1]), "and borrows");
                h.succeed();
            });
        });
    }
    /**
     * A Kief Cap straight across a Bottomless Stash. Its supply is so stiff that an empty cap would ask thousands of Ig of the first
     * microsecond, and the bad solve tripped the supply, over and over. A new cap starts charged instead, and nothing trips.
     */
    @GameTest(template = "long", timeoutTicks = 100)
    public static void aKiefCapAcrossAStiffSupplyDoesntTripIt(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 6);
        h.setBlock(l.at(1), ModPower.BOTTOMLESS_STASH_PUFF.get());
        BlockPos cap = l.at(4);
        place(h, cap, ComponentType.KIEF_CAP, Direction.NORTH);
        var stash = (com.malevolentgods.fourtwenty.power.SupplyBlockEntity) h.getBlockEntity(l.at(1));
        h.onEachTick(() -> h.assertFalse(stash.node().overloaded(h.getLevel().getGameTime()), "the supply never trips"));
        h.runAfterDelay(30, () -> {
            var r = com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(cap)).orElseThrow();
            double v = Math.abs(r.pins()[0] - r.pins()[1]);
            h.assertTrue(Math.abs(v - 4.2) < 0.01, "charged to the supply: " + v);
            h.succeed();
        });
    }

    /** A Numb Nug fed by a Roach Clip on channel 2, under a red Wick: it reads 4, and shows it powered. */
    @GameTest(template = "long", timeoutTicks = 100)
    public static void aNumbNugReadsARibbon(GameTestHelper h) {
        var l = new Ladder(h, 2, 0).rails(ModPower.HEMP_WICK.get(), 1, 8);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos digit = new BlockPos(5, 2, 5);
        place(h, digit, ComponentType.NUMB_NUG, Direction.EAST);
        for (int z = 0; z <= 5; z++) red(h, 5, 3, z);
        for (int z = 0; z <= 5; z++) red(h, 4, 3, z);
        for (int z = 2; z <= 5; z++) black(h, 5, 1, z);
        h.setBlock(new BlockPos(4, 2, 5), ModElectronics.ROACH_CLIP.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, 2));
        h.runAfterDelay(20, () -> {
            int shown = ((ComponentBlockEntity) h.getBlockEntity(digit)).shown();
            h.assertTrue(com.malevolentgods.fourtwenty.domain.electronics.Shown.active(shown), "powered");
            h.assertTrue(com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(shown) == 4, "reads 4: " + com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(shown));
            h.succeed();
        });
    }

    /** A big chip facing east at (5,2,5), its upper block at (5,3,5), on a Bottomless Stash's rails: VDD over the top, GND underneath. */
    private static BlockPos bigChip(GameTestHelper h, ComponentType type) {
        var l = new Ladder(h, 2, 0).rails(ModPower.HEMP_WICK.get(), 1, 8);
        h.setBlock(l.at(1), ModPower.BOTTOMLESS_STASH_PUFF.get());
        BlockPos chip = new BlockPos(5, 2, 5);
        var state = ModElectronics.COMPONENTS.get(type).get().defaultBlockState().setValue(ComponentBlock.FACING, Direction.EAST);
        h.setBlock(chip, state);
        h.setBlock(chip.above(), state.setValue(com.malevolentgods.fourtwenty.electronics.BigChipBlock.UPPER, true));
        red(h, 5, 3, 0);
        for (int z = 0; z <= 5; z++) red(h, 5, 4, z);
        for (int z = 2; z <= 5; z++) black(h, 5, 1, z);
        return chip;
    }

    private static void clip(GameTestHelper h, BlockPos pos, int channel) {
        h.setBlock(pos, ModElectronics.ROACH_CLIP.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, channel));
    }

    /** The 16-bit word on a big chip's bus pair starting at pin {@code from} (low bus, then high), read from its pin voltages. */
    private static int word(double[] v, int from) {
        int x = 0;
        for (int k = 0; k < 8; k++) {
            if (v[from + k] - v[1] > 0.5 * (v[0] - v[1])) x |= 1 << k;
            if (v[from + 9 + k] - v[1] > 0.5 * (v[0] - v[1])) x |= 1 << (8 + k);
        }
        return x;
    }

    private static boolean high(double[] v, int pin) { return v[pin] - v[1] > 0.5 * (v[0] - v[1]); }

    private static double[] pins(GameTestHelper h, BlockPos pos) {
        return com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(pos))
                .map(com.malevolentgods.fourtwenty.power.Circuits.ComponentReading::pins).orElse(null);
    }

    /**
     * A Manyfier, two blocks tall: A's low byte through a Roach Clip on the lower block's back (channel 2, so 4), B's high byte through
     * one on the upper block's left (channel 1, so 512). 4 × 512 = 2048 in front. Both clips to channel 7: 128 × 32768 = 4,194,304, past
     * 16 bits: 0 in front, 64 on the right's low byte, and the flag.
     */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aManyfierMultipliesSixteenBits(GameTestHelper h) {
        BlockPos chip = bigChip(h, ComponentType.MANYFIER);
        for (int z = 0; z <= 5; z++) red(h, 4, 3, z);
        red(h, 5, 4, 6);
        clip(h, new BlockPos(4, 2, 5), 2);
        clip(h, new BlockPos(5, 3, 6), 1);
        h.runAfterDelay(20, () -> {
            double[] v = pins(h, chip);
            h.assertTrue(v != null, "a reading");
            h.assertTrue(word(v, 2) == 4 && word(v, 20) == 512, "reads A = 4, B = 512: " + word(v, 2) + ", " + word(v, 20) + " " + java.util.Arrays.toString(v));
            h.assertTrue(word(v, 38) == 2048, "4 x 512 = 2048: " + word(v, 38));
            h.assertFalse(high(v, 46), "no overflow");
            clip(h, new BlockPos(4, 2, 5), 7);
            clip(h, new BlockPos(5, 3, 6), 7);
            h.runAfterDelay(10, () -> {
                double[] w = pins(h, chip);
                h.assertTrue(word(w, 38) == 0 && word(w, 56) == 64, "128 x 32768 = 0x400000: " + word(w, 38) + ", " + word(w, 56));
                h.assertTrue(high(w, 46), "the flag: past 65,535");
                h.succeed();
            });
        });
    }

    /**
     * A Fewinator: A's high byte through a Roach Clip on the upper block's back (channel 3, so 2048), nothing on B: dividing by 0 gives
     * 0 and the flag. Then B's low byte through one on the lower block's left (channel 2, so 4): 2048 ÷ 4 = 512, remainder 0.
     */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aFewinatorDivides(GameTestHelper h) {
        BlockPos chip = bigChip(h, ComponentType.FEWINATOR);
        red(h, 4, 4, 5);
        clip(h, new BlockPos(4, 3, 5), 3);
        for (int z = 0; z <= 6; z++) red(h, 6, 3, z);
        red(h, 6, 2, 6);
        h.runAfterDelay(20, () -> {
            double[] v = pins(h, chip);
            h.assertTrue(v != null, "a reading");
            h.assertTrue(word(v, 2) == 2048, "reads A = 2048: " + word(v, 2) + " " + java.util.Arrays.toString(v));
            h.assertTrue(word(v, 38) == 0 && high(v, 46), "divided by 0: 0 and the flag");
            clip(h, new BlockPos(5, 2, 6), 2);
            h.runAfterDelay(10, () -> {
                double[] w = pins(h, chip);
                h.assertTrue(word(w, 38) == 512 && word(w, 56) == 0, "2048 / 4 = 512 r 0: " + word(w, 38) + " r " + word(w, 56));
                h.assertFalse(high(w, 46), "no flag");
                h.succeed();
            });
        });
    }

    /** Breaking either block of a big chip takes the other with it. */
    @GameTest(template = "long", timeoutTicks = 40)
    public static void aBigChipGoesWhole(GameTestHelper h) {
        BlockPos chip = bigChip(h, ComponentType.MANYFIER);
        h.destroyBlock(chip.above());
        h.runAfterDelay(2, () -> {
            h.assertBlockNotPresent(ModElectronics.COMPONENTS.get(ComponentType.MANYFIER).get(), chip);
            h.succeed();
        });
    }
}
