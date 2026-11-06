package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.domain.DaisyLine;
import com.malevolentgods.fourtwenty.domain.LedRules;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.lights.LitBitBlockEntity;
import com.malevolentgods.fourtwenty.lights.ModLights;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Twin and Daisy Wicks in the world (specs/009-twin-daisy-wicks). Rows run along x at y 2; blocks are spaced so they never touch. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class CableGameTests {
    private CableGameTests() {}

    private static final Direction[] AROUND = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static BlockPos at(int x, int z) { return new BlockPos(x, 2, z); }

    private static void cables(GameTestHelper h, Block cable, int z, int x0, int x1) {
        for (int x = x0; x <= x1; x++) h.setBlock(at(x, z), cable);
    }

    private static BlockPos facing(GameTestHelper h, BlockPos pos, Block block, Direction facing) {
        var state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        h.setBlock(pos, state);
        return pos;
    }

    private static BlockPos hookah(GameTestHelper h, BlockPos pos, Block block, Direction facing) {
        facing(h, pos, block, facing);
        var hookah = (WeedBongBlockEntity) h.getBlockEntity(pos);
        hookah.setItem(WeedBongBlockEntity.WEED_SLOT, new ItemStack(ModItems.WEED_BUD.get(), 4));
        hookah.setItem(WeedBongBlockEntity.WATER_SLOT, new ItemStack(Items.WATER_BUCKET));
        hookah.setItem(WeedBongBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL, 16));
        return pos;
    }

    private static BlockPos machine(GameTestHelper h, BlockPos pos, Block block, Direction facing) {
        facing(h, pos, block, facing);
        var m = (MachineBlockEntity) h.getBlockEntity(pos);
        m.setItem(0, ModDataComponents.withStrain(new ItemStack(ModItems.ROUGH_BUD.get(), 64), Strain.LANDRACE));
        return pos;
    }

    private static Circuits.Reading read(GameTestHelper h, BlockPos pos) {
        return Circuits.reading(h.getLevel(), h.absolutePos(pos)).orElseThrow(() -> new AssertionError("no reading at " + pos));
    }

    // ---- Twin Wick ----------------------------------------------------------------------------------------------------------------

    /** A Hookah and four Snip Stations, turned every which way, beside one Twin line: each at full power, as on rails. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aTwinLineBesideARowRunsItInParallel(GameTestHelper h) {
        cables(h, ModPower.HEMP_TWIN_WICK.get(), 1, 1, 9);
        hookah(h, at(1, 2), ModBlocks.WEED_BONG.get(), Direction.SOUTH);
        List<BlockPos> machines = new ArrayList<>();
        for (int i = 0; i < 4; i++) machines.add(machine(h, at(3 + 2 * i, 2), ModPower.SNIP_STATION.get(), AROUND[i]));
        h.runAfterDelay(80, () -> {
            for (BlockPos m : machines) {
                var r = read(h, m);
                h.assertTrue(Math.abs(r.watts() - 105) < 1.5, "about 105 W at " + m + ", got " + r);
                h.assertTrue(r.volts() > 4.1 && r.volts() <= 4.2, "about 4.2 Kb, never reversed, got " + r);
            }
            h.succeed();
        });
    }

    /** Hookah, cable, machine, cable, machine...: inline, the same readings. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aTwinLineThroughARowRunsItInParallel(GameTestHelper h) {
        hookah(h, at(1, 2), ModBlocks.WEED_BONG.get(), Direction.NORTH);
        List<BlockPos> machines = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            h.setBlock(at(2 + 2 * i, 2), ModPower.HEMP_TWIN_WICK.get());
            machines.add(machine(h, at(3 + 2 * i, 2), ModPower.SNIP_STATION.get(), AROUND[(i + 1) % 4]));
        }
        h.runAfterDelay(80, () -> {
            for (BlockPos m : machines) {
                var r = read(h, m);
                h.assertTrue(Math.abs(r.watts() - 105) < 1.5, "about 105 W at " + m + ", got " + r);
            }
            h.succeed();
        });
    }

    /** Eight machines on a Hookah's Twin line brown out evenly. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void eightMachinesOnATwinLineBrownOutEvenly(GameTestHelper h) {
        cables(h, ModPower.HEMP_TWIN_WICK.get(), 1, 1, 17);
        hookah(h, at(1, 2), ModBlocks.WEED_BONG.get(), Direction.EAST);
        List<BlockPos> machines = new ArrayList<>();
        for (int i = 0; i < 8; i++) machines.add(machine(h, at(3 + 2 * i, 2), ModPower.SNIP_STATION.get(), AROUND[i % 4]));
        h.runAfterDelay(80, () -> {
            double first = read(h, machines.get(0)).watts();
            for (BlockPos m : machines) h.assertTrue(Math.abs(read(h, m).watts() - first) < 0.03 * first, "even: " + read(h, m) + " against " + first);
            h.assertTrue(first > 22 && first < 36, "about 29 W, got " + first);
            h.succeed();
        });
    }

    /** A Twin line fed from red and black rails: red joins its + core, black its −. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aTwinLineTakesPowerFromRails(GameTestHelper h) {
        var l = new Ladder(h, 2, 1);
        for (int x = 1; x <= 3; x++) {
            Ladder.wick(h, l.plus(x), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
            Ladder.wick(h, l.minus(x), ModPower.HEMP_WICK.get(), WickBlock.Colour.BLACK);
        }
        l.hookah(1, ModBlocks.WEED_BONG.get());
        for (int z = 1; z <= 3; z++) h.setBlock(at(4, z), ModPower.HEMP_TWIN_WICK.get());
        cables(h, ModPower.HEMP_TWIN_WICK.get(), 2, 5, 9);
        BlockPos a = machine(h, at(6, 1), ModPower.SNIP_STATION.get(), Direction.NORTH), b = machine(h, at(8, 3), ModPower.SNIP_STATION.get(), Direction.WEST);
        h.runAfterDelay(80, () -> {
            for (BlockPos m : List.of(a, b)) h.assertTrue(Math.abs(read(h, m).watts() - 105) < 2, "full power from the rails at " + m + ", got " + read(h, m));
            h.succeed();
        });
    }

    /** A Hemp Twin Wick on a Double Hose's 42 Kb burns, in smoke and ash, and nothing else breaks. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void aTwinWickBurnsOnTooHighAVoltage(GameTestHelper h) {
        for (int x = 1; x <= 3; x++) h.setBlock(new BlockPos(x, 2, 1), ModPower.HEMP_TWIN_WICK.get());
        hookah(h, new BlockPos(1, 2, 2), ModPower.DOUBLE_HOSE.get(), Direction.NORTH);
        machine(h, new BlockPos(3, 2, 2), ModPower.BUD_BARBER.get(), Direction.NORTH);
        h.runAfterDelay(80, () -> {
            boolean burned = false;
            for (int x = 1; x <= 3; x++) burned |= h.getBlockState(new BlockPos(x, 2, 1)).isAir();
            h.assertTrue(burned, "a Twin Wick burned");
            h.assertBlockPresent(ModPower.DOUBLE_HOSE.get(), new BlockPos(1, 2, 2));
            h.assertBlockPresent(ModPower.BUD_BARBER.get(), new BlockPos(3, 2, 2));
            h.succeed();
        });
    }

    // ---- Daisy Wick ---------------------------------------------------------------------------------------------------------------

    private static double[] watts(GameTestHelper h, List<BlockPos> machines) {
        double[] w = new double[machines.size()];
        for (int i = 0; i < w.length; i++) w[i] = read(h, machines.get(i)).watts();
        return w;
    }

    /** A Hookah and three machines beside one Daisy line, turned any way: in series, each a third of the voltage and a ninth of the power. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aDaisyLineRunsARowInSeries(GameTestHelper h) {
        cables(h, ModPower.HEMP_DAISY_WICK.get(), 1, 1, 7);
        hookah(h, at(1, 2), ModBlocks.WEED_BONG.get(), Direction.WEST);
        List<BlockPos> machines = new ArrayList<>();
        for (int i = 0; i < 3; i++) machines.add(machine(h, at(3 + 2 * i, 2), ModPower.SNIP_STATION.get(), AROUND[i]));
        h.runAfterDelay(80, () -> {
            double v0 = read(h, machines.get(0)).volts();
            for (BlockPos m : machines) {
                var r = read(h, m);
                h.assertTrue(Math.abs(r.volts() - v0) < 0.02 * v0 && r.volts() > 0, "equal shares: " + r + " against " + v0);
                h.assertTrue(Math.abs(r.amps() - read(h, machines.get(0)).amps()) < 0.01 * r.amps(), "one current through all: " + r);
            }
            h.assertTrue(v0 > 1.2 && v0 < 1.45, "about a third of 4.2 Kb, got " + v0);
            h.assertTrue(Math.abs(read(h, machines.get(0)).watts() - 105 / 9.0) < 2, "about a ninth of full power, got " + read(h, machines.get(0)));
            h.succeed();
        });
    }

    /** The same string with its source first or in the middle, and inline: the same readings. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aDaisyStringIsTheSameWhereverItsSourceSits(GameTestHelper h) {
        // source first, beside
        cables(h, ModPower.HEMP_DAISY_WICK.get(), 0, 1, 7);
        hookah(h, at(1, 1), ModBlocks.WEED_BONG.get(), Direction.NORTH);
        List<BlockPos> first = new ArrayList<>();
        for (int i = 0; i < 3; i++) first.add(machine(h, at(3 + 2 * i, 1), ModPower.SNIP_STATION.get(), AROUND[i]));
        // source in the middle, inline: machine, cable, Hookah, cable, machine, cable, machine
        machine(h, at(21, 4), ModPower.SNIP_STATION.get(), Direction.EAST);
        h.setBlock(at(22, 4), ModPower.HEMP_DAISY_WICK.get());
        hookah(h, at(23, 4), ModBlocks.WEED_BONG.get(), Direction.SOUTH);
        h.setBlock(at(24, 4), ModPower.HEMP_DAISY_WICK.get());
        machine(h, at(25, 4), ModPower.SNIP_STATION.get(), Direction.NORTH);
        h.setBlock(at(26, 4), ModPower.HEMP_DAISY_WICK.get());
        machine(h, at(27, 4), ModPower.SNIP_STATION.get(), Direction.WEST);
        List<BlockPos> middle = List.of(at(21, 4), at(25, 4), at(27, 4));
        h.runAfterDelay(80, () -> {
            double[] a = watts(h, first), b = watts(h, middle);
            for (int i = 0; i < 3; i++) {
                h.assertTrue(a[i] > 5, "it runs: " + a[i]);
                h.assertTrue(Math.abs(a[i] - b[i]) < 0.03 * a[i], "the same wherever the source sits: " + a[i] + " and " + b[i]);
            }
            h.succeed();
        });
    }

    /** Ten Lit Bits on a Double Hose's Daisy line: each about a tenth of 42 Kb, 4.2 Kb, and lit, like a real LED string. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void tenLitBitsInSeriesOnTokeEachGetPuffVoltage(GameTestHelper h) {
        cables(h, ModPower.ROLLED_DAISY_WICK.get(), 1, 1, 12);
        hookah(h, at(1, 2), ModPower.DOUBLE_HOSE.get(), Direction.SOUTH);
        List<BlockPos> lamps = new ArrayList<>();
        for (int x = 3; x <= 12; x++) {
            BlockPos pos = facing(h, at(x, 2), ModLights.LIT_BIT.get(), Direction.EAST);
            var lamp = (LitBitBlockEntity) h.getBlockEntity(pos);
            lamp.setId(50000 + x);
            lamp.setColour(0xFFFFFF);
            lamps.add(pos);
        }
        h.runAfterDelay(100, () -> {
            for (BlockPos p : lamps) {
                var r = read(h, p);
                h.assertTrue(Math.abs(r.volts() - 4.2) < 0.21, "about 4.2 Kb on " + p + ", got " + r);
                int shown = ((LitBitBlockEntity) h.getBlockEntity(p)).shown();
                h.assertTrue(LedRules.red(shown) > 200, "lit, got " + Integer.toHexString(shown));
            }
            h.succeed();
        });
    }

    /** A Daisy line with a spur carries nothing, and says it can't branch. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aBranchedDaisyLineCarriesNothing(GameTestHelper h) {
        cables(h, ModPower.HEMP_DAISY_WICK.get(), 1, 1, 7);
        h.setBlock(at(4, 0), ModPower.HEMP_DAISY_WICK.get());
        hookah(h, at(1, 2), ModBlocks.WEED_BONG.get(), Direction.SOUTH);
        BlockPos m = machine(h, at(5, 2), ModPower.SNIP_STATION.get(), Direction.NORTH);
        h.runAfterDelay(60, () -> {
            h.assertTrue(read(h, m).watts() < 0.01, "nothing through a branched line, got " + read(h, m));
            var why = Circuits.daisyFailure(h.getLevel(), h.absolutePos(at(4, 1)));
            h.assertTrue(why.orElse(null) == DaisyLine.Reason.BRANCH, "it says it branches, got " + why);
            h.succeed();
        });
    }

    /** A Daisy line from a red rail at one end to a black rail at the other: a series string between them. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aDaisyLineRunsBetweenRedAndBlackRails(GameTestHelper h) {
        var l = new Ladder(h, 2, 1);
        for (int x = 1; x <= 3; x++) {
            Ladder.wick(h, l.plus(x), ModPower.HEMP_WICK.get(), WickBlock.Colour.RED);
            Ladder.wick(h, l.minus(x), ModPower.HEMP_WICK.get(), WickBlock.Colour.BLACK);
        }
        l.hookah(1, ModBlocks.WEED_BONG.get());
        for (int[] c : new int[][]{{4, 1}, {5, 1}, {6, 1}, {6, 2}, {6, 3}, {5, 3}, {4, 3}}) h.setBlock(at(c[0], c[1]), ModPower.HEMP_DAISY_WICK.get());
        List<BlockPos> machines = List.of(machine(h, at(7, 1), ModPower.SNIP_STATION.get(), Direction.NORTH),
                machine(h, at(7, 2), ModPower.SNIP_STATION.get(), Direction.EAST), machine(h, at(7, 3), ModPower.SNIP_STATION.get(), Direction.SOUTH));
        h.runAfterDelay(80, () -> {
            double[] w = watts(h, machines);
            for (double x : w) h.assertTrue(Math.abs(x - 105 / 9.0) < 2.5, "a ninth of full power each, got " + java.util.Arrays.toString(w));
            h.succeed();
        });
    }

    /** A Twin line and a Daisy line side by side don't join: a machine on the Daisy line alone gets nothing from the Twin line's Hookah. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void twinAndDaisyLinesDontJoin(GameTestHelper h) {
        cables(h, ModPower.HEMP_TWIN_WICK.get(), 2, 1, 5);
        cables(h, ModPower.HEMP_DAISY_WICK.get(), 1, 1, 5);
        hookah(h, at(1, 3), ModBlocks.WEED_BONG.get(), Direction.NORTH);
        BlockPos twin = machine(h, at(4, 3), ModPower.SNIP_STATION.get(), Direction.EAST);
        BlockPos daisy = machine(h, at(3, 0), ModPower.SNIP_STATION.get(), Direction.EAST);
        h.runAfterDelay(80, () -> {
            h.assertTrue(read(h, twin).watts() > 100, "the Twin line runs, got " + read(h, twin));
            h.assertTrue(read(h, daisy).watts() < 0.01, "the Daisy line doesn't join it, got " + read(h, daisy));
            h.succeed();
        });
    }

    /** A Double Hose into a Hit Box's front on a Rolled Twin Wick, and a Hemp Twin Wick off its side: the machine runs, stepped down. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aHitBoxStepsDownBetweenTwinLines(GameTestHelper h) {
        hookah(h, at(5, 1), ModPower.DOUBLE_HOSE.get(), Direction.EAST);
        h.setBlock(at(5, 2), ModPower.ROLLED_TWIN_WICK.get());
        h.setBlock(at(5, 3), ModPower.HIT_BOX.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.power.HitBoxBlock.FACING, Direction.NORTH));
        cables(h, ModPower.HEMP_TWIN_WICK.get(), 3, 6, 8);
        BlockPos m = machine(h, at(8, 4), ModPower.SNIP_STATION.get(), Direction.NORTH);
        h.runAfterDelay(100, () -> {
            var r = read(h, m);
            h.assertTrue(r.watts() > 100, "full power through the Hit Box, got " + r);
            h.assertTrue(r.volts() > 4.0 && r.volts() < 4.3, "at Puff voltage, got " + r);
            h.assertBlockPresent(ModPower.HEMP_TWIN_WICK.get(), at(6, 3));
            h.succeed();
        });
    }

    /** The Inspoweedometer's numbers for a Twin Wick: the voltage between its cores, and its line current on each core. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aTwinWickReadsBetweenItsCores(GameTestHelper h) {
        cables(h, ModPower.HEMP_TWIN_WICK.get(), 1, 1, 5);
        hookah(h, at(1, 2), ModBlocks.WEED_BONG.get(), Direction.SOUTH);
        machine(h, at(5, 2), ModPower.SNIP_STATION.get(), Direction.NORTH);
        h.runAfterDelay(80, () -> {
            var r = Circuits.cableReading(h.getLevel(), h.absolutePos(at(3, 1))).orElseThrow();
            h.assertTrue(r.volts() > 4.1 && r.volts() <= 4.21, "about 4.2 Kb between the cores, got " + r);
            h.assertTrue(Math.abs(r.plusAmps() - 25) < 1.5 && Math.abs(r.minusAmps() - 25) < 1.5, "about 25 Ig on each core, got " + r);
            h.succeed();
        });
    }
}
