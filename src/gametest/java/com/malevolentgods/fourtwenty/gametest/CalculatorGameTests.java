package com.malevolentgods.fourtwenty.gametest;

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
 * 1234 + 8765 = 9999; C, 9999 + 9999 = 19998; C, 5 - 8 = -3 (3, the display's minus on); C, 1 2 3 4 5 holds 1234 (the fifth digit
 * refused); C, 7 5 back = 7. Register X, its two Jar Nugs, is what the display shows.
 */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class CalculatorGameTests {
    private CalculatorGameTests() {}

    private static BlockPos at(int[] p) { return new BlockPos(1 + p[0], p[1], 1 + p[2]); }

    private static int jar(GameTestHelper h, int[] p) {
        double[] m = ((ComponentBlockEntity) h.getBlockEntity(at(p))).memory();
        return m.length > 11 ? (int) m[11] & 0xFF : -1;
    }

    private static int x(GameTestHelper h) { return jar(h, CalculatorLayout.XH) << 8 | jar(h, CalculatorLayout.XL); }

    /** The display's minus: the sign Blend Nug's carry wire out. */
    private static boolean negative(GameTestHelper h) {
        return (com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(((ComponentBlockEntity) h.getBlockEntity(at(CalculatorLayout.SIGN))).shown()) & 0x100) != 0;
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

    @GameTest(template = "long", batch = "calculator", timeoutTicks = 2400)
    public static void theCalculatorCalculates(GameTestHelper h) {
        var blocks = h.getLevel().holderLookup(Registries.BLOCK);
        // the machine reaches far past the test's own structure: keep all of its chunks loaded and ticking while it runs
        java.util.Set<net.minecraft.world.level.ChunkPos> forced = new java.util.HashSet<>();
        for (String line : CalculatorLayout.BLOCKS) {
            String[] f = line.split(" ", 4);
            forced.add(new net.minecraft.world.level.ChunkPos(h.absolutePos(new BlockPos(1 + Integer.parseInt(f[0]), 0, 1 + Integer.parseInt(f[2])))));
        }
        for (var c : forced) h.getLevel().setChunkForced(c.x, c.z, true);
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
        h.runAfterDelay(20, () -> type(h, "1234+8765=", () -> {
            h.assertTrue(x(h) == 9999, "1234 + 8765 = 9999: " + x(h));
            type(h, "C9999+9999=", () -> {
                h.assertTrue(x(h) == 19998, "9999 + 9999 = 19998: " + x(h));
                type(h, "C5-8=", () -> {
                    h.assertTrue(x(h) == 3 && negative(h), "5 - 8 = 3, negative: " + x(h) + ", lamp " + negative(h));
                    type(h, "C12345", () -> {
                        h.assertTrue(x(h) == 1234 && !negative(h), "a fifth digit refused: " + x(h));
                        type(h, "C75<", () -> {
                            h.assertTrue(x(h) == 7, "C, 7 5 back = 7: " + x(h));
                            type(h, "C50-8=", () -> {
                                h.assertTrue(x(h) == 42 && !negative(h), "50 - 8 = 42: " + x(h));
                                for (var c : forced) h.getLevel().setChunkForced(c.x, c.z, false);
                                h.succeed();
                            });
                        });
                    });
                });
            });
        }));
    }
}
