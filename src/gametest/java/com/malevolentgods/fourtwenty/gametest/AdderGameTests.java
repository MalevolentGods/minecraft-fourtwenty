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
 * The adding machine built on flatworld (AdderLayout, from its router), driven as a player would: A pressed three times, B five,
 * then =, then reset. Registers A, B and the sum must read 3, 5, 8, then all 0. A whole machine of 3 Jar Nugs, 3 Stacker Nugs, a
 * NOT Nug, 4 Buffer Nugs, 3 One-Hitters, 4 Puff Buttons and 10 Numb Nugs, so it also proves a circuit this size solves, quickly.
 */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class AdderGameTests {
    private AdderGameTests() {}

    /** Register A's Jar Nug, in the test's space; every layout position is relative to it, a level down. */
    private static final BlockPos ORIGIN = new BlockPos(10, 1, 12);

    private static BlockPos at(int dx, int dy, int dz) { return ORIGIN.offset(dx, dy, dz); }

    private static int held(GameTestHelper h, BlockPos jar) {
        double[] m = ((ComponentBlockEntity) h.getBlockEntity(jar)).memory();
        return m.length > 11 ? (int) m[11] : -1;
    }

    private static void press(GameTestHelper h, BlockPos button, int times, Runnable then) {
        if (times == 0) {
            then.run();
            return;
        }
        var be = (ComponentBlockEntity) h.getBlockEntity(button);
        be.closed(true);
        h.runAfterDelay(4, () -> {
            be.closed(false);
            h.runAfterDelay(4, () -> press(h, button, times - 1, then));
        });
    }

    @GameTest(template = "long", batch = "adder", timeoutTicks = 800)
    public static void theAddingMachineAdds(GameTestHelper h) {
        var blocks = h.getLevel().holderLookup(Registries.BLOCK);
        for (String line : AdderLayout.BLOCKS) {
            String[] f = line.split(" ", 4);
            try {
                var state = BlockStateParser.parseForBlock(blocks, f[3], false).blockState();
                h.setBlock(at(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2])), state);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                h.fail("bad layout line " + line + ": " + e.getMessage());
            }
        }
        BlockPos a = at(-1, 1, 11), b = at(3, 1, 11), eq = at(7, 1, 11), reset = at(11, 1, 11);
        BlockPos ja = at(0, 1, 0), jb = at(10, 1, 0), jc = at(7, 1, 4);
        h.runAfterDelay(20, () -> press(h, a, 3, () -> press(h, b, 5, () -> h.runAfterDelay(4, () -> {
            h.assertTrue(held(h, ja) == 3 && held(h, jb) == 5, "A x3 and B x5 hold 3 and 5: " + held(h, ja) + ", " + held(h, jb));
            press(h, eq, 1, () -> h.runAfterDelay(4, () -> {
                h.assertTrue(held(h, jc) == 8, "= holds 3 + 5 = 8: " + held(h, jc));
                press(h, reset, 1, () -> h.runAfterDelay(6, () -> {
                    h.assertTrue(held(h, ja) == 0 && held(h, jb) == 0 && held(h, jc) == 0,
                            "reset clears them all: " + held(h, ja) + ", " + held(h, jb) + ", " + held(h, jc));
                    h.succeed();
                }));
            }));
        }))));
    }
}
