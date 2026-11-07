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
}
