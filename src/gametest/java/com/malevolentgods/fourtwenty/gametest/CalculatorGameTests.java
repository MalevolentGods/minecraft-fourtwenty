package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.LampBlock;
import com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The keypad calculator built on flatworld (CalculatorLayout, from tools/build_calculator.py), driven as a player would, key by key:
 * 12 + 34 = 46; C, 50 - 8 = 42; C, 7 5 back = 7; C, 200 + 100 = 300 with the overflow lamp lit. Register X is what the display shows.
 */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class CalculatorGameTests {
    private CalculatorGameTests() {}

    private static BlockPos at(int[] p) { return new BlockPos(1 + p[0], p[1], 1 + p[2]); }

    private static int x(GameTestHelper h) {
        double[] m = ((ComponentBlockEntity) h.getBlockEntity(at(CalculatorLayout.JX))).memory();
        return m.length > 11 ? (int) m[11] : -1;
    }

    /** Presses each key of {@code keys} in turn (down 4 ticks, up 4), then runs {@code then}. */
    private static void type(GameTestHelper h, String keys, Runnable then) {
        if (keys.isEmpty()) {
            h.runAfterDelay(4, then);
            return;
        }
        var be = (ComponentBlockEntity) h.getBlockEntity(at(CalculatorLayout.KEYS.get(keys.substring(0, 1))));
        be.closed(true);
        h.runAfterDelay(4, () -> {
            be.closed(false);
            h.runAfterDelay(4, () -> type(h, keys.substring(1), then));
        });
    }

    @GameTest(template = "long", batch = "calculator", timeoutTicks = 1600)
    public static void theCalculatorCalculates(GameTestHelper h) {
        var blocks = h.getLevel().holderLookup(Registries.BLOCK);
        for (String line : CalculatorLayout.BLOCKS) {
            String[] f = line.split(" ", 4);
            BlockPos pos = new BlockPos(1 + Integer.parseInt(f[0]), Integer.parseInt(f[1]), 1 + Integer.parseInt(f[2]));
            try {
                var parsed = BlockStateParser.parseForBlock(blocks, f[3], true);
                h.setBlock(pos, parsed.blockState());
                if (parsed.nbt() != null && h.getBlockEntity(pos) instanceof ComponentBlockEntity be) be.loadWithComponents(parsed.nbt(), h.getLevel().registryAccess());
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                h.fail("bad layout line " + line + ": " + e.getMessage());
            }
        }
        h.runAfterDelay(20, () -> type(h, "12+34=", () -> {
            h.assertTrue(x(h) == 46, "12 + 34 = 46: " + x(h));
            type(h, "C50-8=", () -> {
                h.assertTrue(x(h) == 42, "50 - 8 = 42: " + x(h));
                type(h, "C75<", () -> {
                    h.assertTrue(x(h) == 7, "C, 7 5 back = 7: " + x(h));
                    type(h, "C200+100=", () -> {
                        h.assertTrue(x(h) == 300, "200 + 100 = 300: " + x(h));
                        h.assertTrue(h.getBlockState(at(CalculatorLayout.OVERFLOW)).getValue(LampBlock.LIT), "past 255: the overflow lamp is lit");
                        h.succeed();
                    });
                });
            });
        }));
    }
}
