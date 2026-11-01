package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.TubeOutput;
import com.malevolentgods.fourtwenty.processing.MachineBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class TubeGameTests {
    private static final BlockPos TRIM = new BlockPos(1, 2, 1), CURE = new BlockPos(5, 2, 1);

    private static void line(GameTestHelper h) {
        h.setBlock(TRIM, ModPower.SNIP_STATION.get());
        h.setBlock(CURE, ModPower.DRY_BOX.get());
        for (int x = 2; x < 5; x++) h.setBlock(new BlockPos(x, 2, 1), ModPower.TRANSPORT_TUBE.get());
    }

    private static MachineBlockEntity machine(GameTestHelper h, BlockPos pos) {
        return (MachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    @GameTest(template = "empty")
    public static void aTubeCarriesOutputToTheNextMachine(GameTestHelper h) {
        line(h);
        var trim = machine(h, TRIM);
        var cure = machine(h, CURE);
        ItemStack buds = ModDataComponents.withStrain(new ItemStack(ModItems.TRIMMED_BUD.get(), 3), Strain.BLUE_DREAM);
        trim.setItem(MachineBlockEntity.INPUTS, buds);
        h.succeedWhen(() -> {
            h.assertTrue(trim.getItem(MachineBlockEntity.INPUTS).isEmpty(), "it left the output");
            ItemStack arrived = cure.getItem(0);
            h.assertTrue(arrived.getCount() == 3 && ModDataComponents.strain(arrived) == Strain.BLUE_DREAM, "and arrived whole: " + arrived);
        });
    }

    @GameTest(template = "empty")
    public static void aTubeOnlyDeliversWhatTheEndWants(GameTestHelper h) {
        line(h);
        var trim = machine(h, TRIM);
        trim.setItem(MachineBlockEntity.INPUTS, new ItemStack(ModItems.WEED_SEEDS.get(), 2));
        h.runAfterDelay(30, () -> {
            h.assertTrue(trim.getItem(MachineBlockEntity.INPUTS).getCount() == 2, "a Dry Box doesn't take seeds, so they stay");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void aTubeIntoAnUnloadedChunkBlocksTheLine(GameTestHelper h) {
        line(h);
        var start = h.absolutePos(TRIM);
        var far = h.absolutePos(new BlockPos(4, 2, 1));
        h.assertFalse(TubeOutput.walk(h.getLevel(), start, h.getLevel()::hasChunkAt).blocked(), "all loaded, it's clear");
        h.assertTrue(TubeOutput.walk(h.getLevel(), start, pos -> !pos.equals(far)).blocked(), "a tube into an unloaded chunk blocks it");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aTubeIgnoresChests(GameTestHelper h) {
        line(h);
        h.setBlock(new BlockPos(3, 3, 1), Blocks.CHEST);
        var ends = TubeOutput.walk(h.getLevel(), h.absolutePos(TRIM), h.getLevel()::hasChunkAt).ends();
        h.assertTrue(ends.size() == 1 && ends.get(0).equals(h.absolutePos(CURE)), "only the Dry Box is on the line: " + ends);
        h.succeed();
    }

    /** The same network works both ways, and output is shared out. */
    @GameTest(template = "empty")
    public static void aNetworkSharesBothWays(GameTestHelper h) {
        line(h);
        var second = new BlockPos(3, 2, 3);
        h.setBlock(new BlockPos(3, 2, 2), ModPower.TRANSPORT_TUBE.get());
        h.setBlock(second, ModPower.DRY_BOX.get());
        var trim = machine(h, TRIM);
        trim.setItem(MachineBlockEntity.INPUTS, new ItemStack(ModItems.TRIMMED_BUD.get(), 8));
        var cure = machine(h, CURE);
        cure.setItem(MachineBlockEntity.INPUTS, new ItemStack(ModItems.ROUGH_BUD.get(), 2));
        h.succeedWhen(() -> {
            h.assertTrue(cure.getItem(0).getCount() == 4 && machine(h, second).getItem(0).getCount() == 4, "the Trimmed Buds split evenly: " + cure.getItem(0) + ", " + machine(h, second).getItem(0));
            h.assertTrue(trim.getItem(0).getCount() == 2 && trim.getItem(0).is(ModItems.ROUGH_BUD.get()), "and Rough Buds came back the other way through the same tubes");
        });
    }

    /** A stopped machine keeps its work and carries on when the chunk is back. */
    @GameTest(template = "empty")
    public static void aMachineStillRunsWithAClearTube(GameTestHelper h) {
        line(h);
        var trim = machine(h, TRIM);
        ((BIBuffer) trim.node()).set(100_000);
        trim.setItem(0, new ItemStack(ModItems.ROUGH_BUD.get()));
        h.succeedWhen(() -> h.assertTrue(machine(h, CURE).getItem(0).is(ModItems.TRIMMED_BUD.get()), "trimmed and sent on"));
    }
}
