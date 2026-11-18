package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.electronics.ComponentBlock;
import com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity;
import com.malevolentgods.fourtwenty.electronics.ComponentType;
import com.malevolentgods.fourtwenty.electronics.ModElectronics;
import com.malevolentgods.fourtwenty.lights.LitBitBlock;
import com.malevolentgods.fourtwenty.lights.ModLights;
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
 * The Handbook's six example circuits (specs/016-weedcraft-handbook FR-014, SC-004), built block for block as the book shows them, and
 * checked to do what the book says. A Nug shows its output itself: it lights up while its output is 1.
 *
 * <p>Every board: a red + rail (z 0) and a black − rail (z 2) along x at y 2, with a Hookah between them at x 1. A Nug takes its VDD from a
 * red Wick on top and its GND from a black Wick underneath, run to the rails.
 */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class CircuitExampleGameTests {
    private CircuitExampleGameTests() {}

    static void red(GameTestHelper h, int x, int y, int z) { Ladder.wick(h, new BlockPos(x, y, z), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED); }

    static void black(GameTestHelper h, int x, int y, int z) { Ladder.wick(h, new BlockPos(x, y, z), ModPower.HEMP_WICK.get(), WickBlock.Colour.BLACK); }

    static void thread(GameTestHelper h, int x, int y, int z) { h.setBlock(new BlockPos(x, y, z), ModElectronics.HEMP_THREAD.get()); }

    static BlockPos part(GameTestHelper h, int x, int y, int z, ComponentType type, Direction facing) {
        BlockPos pos = new BlockPos(x, y, z);
        h.setBlock(pos, ModElectronics.COMPONENTS.get(type).get().defaultBlockState().setValue(ComponentBlock.FACING, facing));
        return pos;
    }

    static BlockPos litBit(GameTestHelper h, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        h.setBlock(pos, ModLights.LIT_BIT.get().defaultBlockState());
        return pos;
    }

    static boolean lit(GameTestHelper h, BlockPos pos) { return h.getBlockState(pos).getValue(LitBitBlock.LEVEL) > 0; }

    static boolean on(GameTestHelper h, BlockPos pos) { return h.getBlockState(pos).getValue(ComponentBlock.ON); }

    static void flip(GameTestHelper h, BlockPos pos, boolean closed) { ((ComponentBlockEntity) h.getBlockEntity(pos)).closed(closed); }

    static void board(GameTestHelper h, int x1) {
        new Ladder(h, 2, 0).rails(ModPower.HEMP_WICK.get(), 0, x1).hookah(1, ModBlocks.WEED_BONG.get());
    }

    /** A Nug at (x, 2, z), off the board at z >= 3: VDD on a red line overhead from the + rail, GND on a black line underneath from the − rail. */
    static void powerOffBoard(GameTestHelper h, int x, int z) {
        for (int zz = 0; zz <= z; zz++) red(h, x, 3, zz);
        for (int zz = 2; zz <= z; zz++) black(h, x, 1, zz);
    }

    /** A Toggle Joint at (x, 2, z) facing {@code facing}, its back pin fed from the red line at (lineX, 3, z) through red Wicks behind it. */
    static BlockPos switchFrom(GameTestHelper h, int x, int z, Direction facing, int lineX) {
        BlockPos sw = part(h, x, 2, z, ComponentType.TOGGLE_JOINT, facing);
        int bx = x - facing.getStepX(), bz = z - facing.getStepZ();      // behind it
        red(h, bx, 2, bz);
        for (int xx = Math.min(bx, lineX); xx <= Math.max(bx, lineX); xx++) red(h, xx, 3, bz);
        return sw;
    }

    /** 1. A switched light: the + rail runs through a Toggle Joint to a red Wick on a Lit Bit; the Lit Bit's other side is on the − rail. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aSwitchedLight(GameTestHelper h) {
        board(h, 4);
        BlockPos sw = part(h, 5, 2, 0, ComponentType.TOGGLE_JOINT, Direction.WEST);
        red(h, 6, 2, 0);
        red(h, 6, 2, 1);
        BlockPos lamp = litBit(h, 6, 2, 2);
        black(h, 5, 2, 2);
        h.runAfterDelay(40, () -> {
            h.assertFalse(lit(h, lamp), "switch off: dark");
            flip(h, sw, true);
            h.runAfterDelay(30, () -> {
                h.assertTrue(lit(h, lamp), "switch on: lit");
                h.succeed();
            });
        });
    }

    /** 2. An inverter: a NOT Nug lights while its Toggle Joint input is off (0 in, 1 out) and goes dark when it's on. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void anInverter(GameTestHelper h) {
        board(h, 6);
        BlockPos sw = part(h, 5, 2, 1, ComponentType.TOGGLE_JOINT, Direction.EAST);
        red(h, 4, 2, 1);
        BlockPos nug = part(h, 6, 2, 1, ComponentType.NOT_NUG, Direction.EAST);
        red(h, 6, 3, 1);
        red(h, 6, 3, 0);
        black(h, 6, 1, 1);
        black(h, 6, 1, 2);
        h.runAfterDelay(40, () -> {
            h.assertTrue(on(h, nug), "switch off (0): the NOT lights (1)");
            flip(h, sw, true);
            h.runAfterDelay(20, () -> {
                h.assertFalse(on(h, nug), "switch on (1): the NOT goes dark (0)");
                h.succeed();
            });
        });
    }

    /** 3. A two-switch lock: an AND Nug lights only while both its switches are on. */
    @GameTest(template = "long", timeoutTicks = 300)
    public static void aTwoSwitchLock(GameTestHelper h) {
        board(h, 7);
        BlockPos and = part(h, 5, 2, 4, ComponentType.AND_NUG, Direction.NORTH);
        powerOffBoard(h, 5, 4);
        BlockPos a = switchFrom(h, 6, 4, Direction.WEST, 5), b = switchFrom(h, 4, 4, Direction.EAST, 5);
        h.runAfterDelay(40, () -> {
            h.assertFalse(on(h, and), "neither: dark");
            flip(h, a, true);
            h.runAfterDelay(20, () -> {
                h.assertFalse(on(h, and), "only A: dark");
                flip(h, b, true);
                h.runAfterDelay(20, () -> {
                    h.assertTrue(on(h, and), "A and B: lit");
                    flip(h, a, false);
                    h.runAfterDelay(20, () -> {
                        h.assertFalse(on(h, and), "only B: dark");
                        h.succeed();
                    });
                });
            });
        });
    }

    /** 4. A one-bit memory: a Latch Nug lights when its Set switch is flicked, stays lit after, and goes dark only when Reset is flicked. */
    @GameTest(template = "long", timeoutTicks = 300)
    public static void aOneBitMemory(GameTestHelper h) {
        board(h, 7);
        BlockPos latch = part(h, 5, 2, 4, ComponentType.LATCH_NUG, Direction.NORTH);
        powerOffBoard(h, 5, 4);
        BlockPos set = switchFrom(h, 6, 4, Direction.WEST, 5), reset = switchFrom(h, 4, 4, Direction.EAST, 5);
        h.runAfterDelay(30, () -> {
            h.assertFalse(on(h, latch), "starts dark");
            flip(h, set, true);
            h.runAfterDelay(10, () -> {
                flip(h, set, false);
                h.runAfterDelay(10, () -> {
                    h.assertTrue(on(h, latch), "remembers the set");
                    flip(h, reset, true);
                    h.runAfterDelay(10, () -> {
                        flip(h, reset, false);
                        h.runAfterDelay(10, () -> {
                            h.assertFalse(on(h, latch), "remembers the reset");
                            h.succeed();
                        });
                    });
                });
            });
        });
    }

    /** 5. A blinking clock: a Ticker on the board blinks on and off by itself, once a second. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aBlinkingClock(GameTestHelper h) {
        board(h, 6);
        BlockPos ticker = part(h, 5, 2, 1, ComponentType.TICKER, Direction.WEST);
        red(h, 5, 3, 1);
        red(h, 5, 3, 0);
        black(h, 5, 1, 1);
        black(h, 5, 1, 2);
        int[] edges = {0};
        boolean[] last = {false};
        h.onEachTick(() -> {
            boolean now = on(h, ticker);
            if (now != last[0]) edges[0]++;
            last[0] = now;
        });
        h.runAfterDelay(140, () -> {
            h.assertTrue(edges[0] >= 8, "it blinks: " + edges[0] + " changes in 7 s");
            h.succeed();
        });
    }

    /**
     * 6. A half adder: two switches, A and B, feed an XOR Nug (the sum) and an AND Nug (the carry) together. 0+0 = 00, 1+0 = 01, 0+1 = 01,
     * 1+1 = 10: binary addition on two lights.
     */
    @GameTest(template = "long", timeoutTicks = 400)
    public static void aHalfAdder(GameTestHelper h) {
        board(h, 7);
        BlockPos sum = part(h, 5, 2, 4, ComponentType.XOR_NUG, Direction.NORTH);
        BlockPos carry = part(h, 5, 2, 6, ComponentType.AND_NUG, Direction.NORTH);
        powerOffBoard(h, 5, 6);
        // A down the gates' left (east) sides, B down their right (west) sides, on Hemp Thread
        for (int z = 4; z <= 6; z++) {
            thread(h, 6, 2, z);
            thread(h, 4, 2, z);
        }
        BlockPos a = part(h, 7, 2, 5, ComponentType.TOGGLE_JOINT, Direction.WEST);
        red(h, 8, 2, 5);
        red(h, 8, 3, 5);
        red(h, 7, 3, 5);
        red(h, 6, 3, 5);
        BlockPos b = part(h, 3, 2, 5, ComponentType.TOGGLE_JOINT, Direction.EAST);
        red(h, 2, 2, 5);
        red(h, 2, 3, 5);
        red(h, 3, 3, 5);
        red(h, 4, 3, 5);
        int[][] cases = {{0, 0, 0, 0}, {1, 0, 1, 0}, {0, 1, 1, 0}, {1, 1, 0, 1}};
        step(h, a, b, sum, carry, cases, 0);
    }

    private static void step(GameTestHelper h, BlockPos a, BlockPos b, BlockPos sum, BlockPos carry, int[][] cases, int i) {
        if (i == cases.length) { h.succeed(); return; }
        int[] c = cases[i];
        flip(h, a, c[0] == 1);
        flip(h, b, c[1] == 1);
        h.runAfterDelay(i == 0 ? 40 : 20, () -> {
            String what = c[0] + " + " + c[1];
            h.assertTrue(on(h, sum) == (c[2] == 1), what + ": sum should be " + c[2]);
            h.assertTrue(on(h, carry) == (c[3] == 1), what + ": carry should be " + c[3]);
            step(h, a, b, sum, carry, cases, i + 1);
        });
    }
}
