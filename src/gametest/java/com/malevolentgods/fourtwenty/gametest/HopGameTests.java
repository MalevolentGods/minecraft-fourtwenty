package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.LampBlock;
import com.malevolentgods.fourtwenty.electronics.HopBlockEntity;
import com.malevolentgods.fourtwenty.electronics.ModElectronics;
import com.malevolentgods.fourtwenty.power.WickBlock;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Nah, Skip Me, Bro: two wires of one colour crossing in one block, each carrying its own signal, neither touching the other. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class HopGameTests {
    private HopGameTests() {}

    /**
     * Two Eighth Ribbons crossing in a hop: a Numb Nug's data comes through it along x from a Roach Clip on channel 2 (4), and a Ribbon
     * along z through the same hop carries channel 0 (1). The digit reads 4: had they touched, 5.
     */
    @GameTest(template = "long", timeoutTicks = 100)
    public static void twoRibbonsCrossWithoutTouching(GameTestHelper h) {
        // a Bottomless Stash beside the digit: + on the red line above it, − down past its front (no pin there) to the black underneath
        h.setBlock(new BlockPos(6, 3, 5), ModPower.BOTTOMLESS_STASH_PUFF.get());
        black(h, 6, 2, 5);
        black(h, 6, 1, 5);
        BlockPos digit = new BlockPos(5, 2, 5);
        h.setBlock(digit, ModElectronics.COMPONENTS.get(com.malevolentgods.fourtwenty.electronics.ComponentType.NUMB_NUG).get().defaultBlockState()
                .setValue(com.malevolentgods.fourtwenty.electronics.ComponentBlock.FACING, Direction.EAST));
        for (int z = 0; z <= 5; z++) Ladder.wick(h, new BlockPos(5, 3, z), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
        for (int z = 0; z <= 5; z++) Ladder.wick(h, new BlockPos(4, 3, z), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
        Ladder.wick(h, new BlockPos(3, 3, 5), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
        for (int z = 2; z <= 5; z++) black(h, 5, 1, z);
        BlockPos hop = new BlockPos(4, 2, 5);
        h.setBlock(hop, ModElectronics.SKIP_ME_BRO.get());
        var be = (HopBlockEntity) h.getBlockEntity(hop);
        be.add(new HopBlockEntity.Wire(ModElectronics.EIGHTH_RIBBON.get(), WickBlock.Colour.NONE, Direction.Axis.X));
        be.add(new HopBlockEntity.Wire(ModElectronics.EIGHTH_RIBBON.get(), WickBlock.Colour.NONE, Direction.Axis.Z));
        var clip = ModElectronics.ROACH_CLIP.get().defaultBlockState();
        h.setBlock(new BlockPos(3, 2, 5), clip.setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, 2));
        h.setBlock(new BlockPos(4, 2, 4), ModElectronics.EIGHTH_RIBBON.get());
        h.setBlock(new BlockPos(4, 2, 6), ModElectronics.EIGHTH_RIBBON.get());
        h.setBlock(new BlockPos(4, 2, 3), clip.setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, 0));
        com.malevolentgods.fourtwenty.power.BIGrid.changed(h.getLevel());
        h.runAfterDelay(30, () -> {
            int shown = ((com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity) h.getBlockEntity(digit)).shown();
            int bits = com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(shown);
            h.assertTrue(bits == 4, "the digit reads 4 through the hop (5 would mean the crossing Ribbon touched): " + bits + " active " + com.malevolentgods.fourtwenty.domain.electronics.Shown.active(shown) + " pins " + com.malevolentgods.fourtwenty.power.Circuits.componentReading(h.getLevel(), h.absolutePos(digit)).map(r -> java.util.Arrays.toString(r.pins())).orElse("none") + " hop " + ((HopBlockEntity) h.getBlockEntity(hop)).wire(0) + " / " + ((HopBlockEntity) h.getBlockEntity(hop)).wire(1));
            h.succeed();
        });
    }

    private static void thread(GameTestHelper h, int x, int y, int z, WickBlock.Colour colour) {
        h.setBlock(new BlockPos(x, y, z), ModElectronics.HEMP_THREAD.get().defaultBlockState().setValue(WickBlock.COLOUR, colour));
    }

    private static void black(GameTestHelper h, int x, int y, int z) { Ladder.wick(h, new BlockPos(x, y, z), ModPower.HEMP_WICK.get(), WickBlock.Colour.BLACK); }

    /**
     * A red thread from a Bottomless Stash's + along x to a lamp; another red thread along z, from nothing, to a second lamp; they cross in
     * a hop at (5, 2, 4). The first lamp lights, the second stays dark: had they touched, both would.
     */
    @GameTest(template = "long", timeoutTicks = 100)
    public static void twoThreadsCrossWithoutTouching(GameTestHelper h) {
        h.setBlock(new BlockPos(0, 2, 4), ModPower.BOTTOMLESS_STASH_PUFF.get());
        BlockPos hop = new BlockPos(5, 2, 4);
        h.setBlock(hop, ModElectronics.SKIP_ME_BRO.get());
        var be = (HopBlockEntity) h.getBlockEntity(hop);
        be.add(new HopBlockEntity.Wire(ModElectronics.HEMP_THREAD.get(), WickBlock.Colour.RED, Direction.Axis.X));
        be.add(new HopBlockEntity.Wire(ModElectronics.HEMP_THREAD.get(), WickBlock.Colour.RED, Direction.Axis.Z));
        for (int x = 1; x <= 9; x++) if (x != 5) thread(h, x, 2, 4, WickBlock.Colour.RED);
        for (int z = 1; z <= 7; z++) if (z != 4) thread(h, 5, 2, z, WickBlock.Colour.RED);
        BlockPos lit = new BlockPos(10, 2, 4), dark = new BlockPos(5, 2, 8);
        h.setBlock(lit, ModBlocks.LAMPS.get(DyeColor.LIME).get());
        h.setBlock(dark, ModBlocks.LAMPS.get(DyeColor.RED).get());
        // the return: black under the x line from the Stash to the first lamp, and across to the second
        for (int x = 0; x <= 10; x++) black(h, x, 1, 4);
        for (int z = 5; z <= 8; z++) black(h, 5, 1, z);
        com.malevolentgods.fourtwenty.power.BIGrid.changed(h.getLevel());
        h.runAfterDelay(30, () -> {
            h.assertTrue(h.getBlockState(lit).getValue(LampBlock.LIT), "the powered line's lamp is lit, through the hop");
            h.assertFalse(h.getBlockState(dark).getValue(LampBlock.LIT), "the crossing line's lamp is dark: the hop keeps them apart");
            h.succeed();
        });
    }
}
