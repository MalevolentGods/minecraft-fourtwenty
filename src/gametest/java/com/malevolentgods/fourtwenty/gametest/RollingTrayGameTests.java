package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.LampBlock;
import com.malevolentgods.fourtwenty.electronics.ComponentBlock;
import com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity;
import com.malevolentgods.fourtwenty.electronics.ComponentType;
import com.malevolentgods.fourtwenty.electronics.ModElectronics;
import com.malevolentgods.fourtwenty.electronics.RollingTrayBlock;
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

/**
 * Rolling Trays: a row of five, facing north, powered once (a red Wick on the first, black Wicks under the row), each part wired on its
 * own from its tray's back. Parts sit on the trays' fronts at z 4; the trays at z 5; their backs at z 6.
 */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class RollingTrayGameTests {
    private RollingTrayGameTests() {}

    private static final int Z = 5;

    /** Trays at x 3 to 7, a Bottomless Stash at x 1 feeding the board: red into the first tray's side, black under them all. */
    private static void board(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 2, Z), ModPower.BOTTOMLESS_STASH_PUFF.get());
        Ladder.wick(h, new BlockPos(2, 2, Z), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
        for (int x = 1; x <= 7; x++) Ladder.wick(h, new BlockPos(x, 1, Z), ModPower.HEMP_WICK.get(), WickBlock.Colour.BLACK);
        for (int x = 3; x <= 7; x++) h.setBlock(new BlockPos(x, 2, Z), ModElectronics.ROLLING_TRAY.get().defaultBlockState().setValue(RollingTrayBlock.FACING, Direction.NORTH));
    }

    private static BlockPos mount(GameTestHelper h, int x, ComponentType type) {
        BlockPos at = new BlockPos(x, 2, Z - 1);
        h.setBlock(at, ModElectronics.COMPONENTS.get(type).get().defaultBlockState().setValue(ComponentBlock.FACING, Direction.NORTH).setValue(ComponentBlock.MOUNTED, true));
        return at;
    }

    private static boolean lit(GameTestHelper h, BlockPos pos) { return h.getBlockState(pos).getValue(LampBlock.LIT); }

    /**
     * Two toggles and two lamps on one board, each toggle threaded to its own lamp behind: the closed toggle's lamp lights, the open one's
     * stays dark until it closes. Neighbours on the board share power, never signals.
     */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void togglesOnABoardLightTheirOwnLamps(GameTestHelper h) {
        board(h);
        BlockPos closed = mount(h, 3, ComponentType.TOGGLE_JOINT), open = mount(h, 7, ComponentType.TOGGLE_JOINT);
        BlockPos lampA = new BlockPos(4, 2, Z - 1), lampB = new BlockPos(6, 2, Z - 1);
        h.setBlock(lampA, ModBlocks.LAMPS.get(DyeColor.LIME).get());
        h.setBlock(lampB, ModBlocks.LAMPS.get(DyeColor.RED).get());
        // tray 3's back to tray 4's, tray 7's to tray 6's; tray 5 between with nothing on it keeps the two threads apart
        for (int x : new int[]{3, 4, 6, 7}) h.setBlock(new BlockPos(x, 2, Z + 1), ModElectronics.HEMP_THREAD.get());
        ((ComponentBlockEntity) h.getBlockEntity(closed)).closed(true);
        h.runAfterDelay(30, () -> {
            h.assertTrue(lit(h, lampA), "the closed toggle's lamp is lit");
            h.assertFalse(lit(h, lampB), "the open toggle's lamp is dark");
            ((ComponentBlockEntity) h.getBlockEntity(open)).closed(true);
            h.runAfterDelay(15, () -> {
                h.assertTrue(lit(h, lampB), "closing the second toggle lights its lamp");
                ((ComponentBlockEntity) h.getBlockEntity(closed)).closed(false);
                h.runAfterDelay(15, () -> {
                    h.assertFalse(lit(h, lampA), "opening the first puts its lamp out");
                    h.assertTrue(lit(h, lampB), "and leaves the other lit");
                    h.succeed();
                });
            });
        });
    }

    /** A mounted Numb Nug reads its tray's back: a Roach Clip there on channel 2, a red Wick on it, reads 4, with the board's power. */
    @GameTest(template = "long", timeoutTicks = 100)
    public static void aMountedNumbNugReadsItsTraysBack(GameTestHelper h) {
        board(h);
        BlockPos digit = mount(h, 5, ComponentType.NUMB_NUG);
        h.setBlock(new BlockPos(5, 2, Z + 1), ModElectronics.ROACH_CLIP.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.electronics.RoachClipBlock.CHANNEL, 2));
        // the clip's channel from the board's +: a red Wick from the first tray's red, up and over
        for (int x = 2; x <= 5; x++) Ladder.wick(h, new BlockPos(x, 3, Z + 1), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
        Ladder.wick(h, new BlockPos(2, 3, Z), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
        h.setBlock(new BlockPos(5, 3, Z + 1), ModPower.HEMP_WICK.get().defaultBlockState().setValue(WickBlock.COLOUR, WickBlock.Colour.RED));
        h.runAfterDelay(30, () -> {
            int shown = ((ComponentBlockEntity) h.getBlockEntity(digit)).shown();
            h.assertTrue(com.malevolentgods.fourtwenty.domain.electronics.Shown.active(shown), "powered from the board");
            h.assertTrue(com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(shown) == 4, "reads 4: " + com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(shown));
            h.succeed();
        });
    }

    /** Two Puff Buttons on channels 0 and 1 of the board's bus; a Numb Nug on the same board, its back on a Ribbon to the board: 3. */
    @GameTest(template = "long", timeoutTicks = 100)
    public static void buttonsOnTheBoardBusReachANumbNug(GameTestHelper h) {
        board(h);
        BlockPos a = mount(h, 3, ComponentType.PUFF_BUTTON), b = mount(h, 4, ComponentType.PUFF_BUTTON);
        h.setBlock(new BlockPos(3, 2, Z), h.getBlockState(new BlockPos(3, 2, Z)).setValue(RollingTrayBlock.CHANNEL, 0));
        h.setBlock(new BlockPos(4, 2, Z), h.getBlockState(new BlockPos(4, 2, Z)).setValue(RollingTrayBlock.CHANNEL, 1));
        BlockPos digit = mount(h, 6, ComponentType.NUMB_NUG);
        // a Ribbon from tray 7's back (the board bus: tray 7 has no part) round to the Numb Nug's tray's back
        h.setBlock(new BlockPos(7, 2, Z + 1), ModElectronics.EIGHTH_RIBBON.get());
        h.setBlock(new BlockPos(7, 2, Z + 2), ModElectronics.EIGHTH_RIBBON.get());
        h.setBlock(new BlockPos(6, 2, Z + 2), ModElectronics.EIGHTH_RIBBON.get());
        h.setBlock(new BlockPos(6, 2, Z + 1), ModElectronics.EIGHTH_RIBBON.get());
        ((ComponentBlockEntity) h.getBlockEntity(a)).closed(true);
        ((ComponentBlockEntity) h.getBlockEntity(b)).closed(true);
        h.runAfterDelay(30, () -> {
            int shown = ((ComponentBlockEntity) h.getBlockEntity(digit)).shown();
            h.assertTrue(com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(shown) == 3, "both buttons on the bus: 3, read " + com.malevolentgods.fourtwenty.domain.electronics.Shown.bits(shown));
            h.succeed();
        });
    }

    /** A mounted part falls off when its tray goes. */
    @GameTest(template = "long", timeoutTicks = 40)
    public static void aMountedPartFallsOffWithoutItsTray(GameTestHelper h) {
        board(h);
        BlockPos button = mount(h, 5, ComponentType.PUFF_BUTTON);
        h.runAfterDelay(2, () -> {
            h.setBlock(new BlockPos(5, 2, Z), net.minecraft.world.level.block.Blocks.AIR);
            h.runAfterDelay(2, () -> {
                h.assertBlockNotPresent(ModElectronics.COMPONENTS.get(ComponentType.PUFF_BUTTON).get(), button);
                h.succeed();
            });
        });
    }
}
