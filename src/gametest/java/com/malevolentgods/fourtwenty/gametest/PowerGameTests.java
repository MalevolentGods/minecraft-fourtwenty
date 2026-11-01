package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BIGrid;
import com.malevolentgods.fourtwenty.power.HookahBlockEntity;
import com.malevolentgods.fourtwenty.power.MachinePower;
import com.malevolentgods.fourtwenty.power.StashBlockEntity;
import com.malevolentgods.fourtwenty.processing.MachineBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import com.malevolentgods.fourtwenty.util.StrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Each of these keeps away from a high player: that test has a batch of its own, because a mock high player reaches 21 blocks into its neighbours. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class PowerGameTests {
    private static final BlockPos A = new BlockPos(1, 2, 2), B = new BlockPos(2, 2, 2), C = new BlockPos(3, 2, 2);

    private static ItemStack of(Item item, int count, Strain strain) {
        return ModDataComponents.withStrain(new ItemStack(item, count), strain);
    }

    private static void charge(MachineBlockEntity machine) {
        machine.node().receive(machine.node().capacity());
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void aHookahPowersAMachineThroughAWick(GameTestHelper h) {
        h.setBlock(A, ModPower.SINGLE_HOSE.get());
        h.setBlock(B, ModPower.HEMP_WICK.get());
        h.setBlock(C, ModPower.SNIP_STATION.get());
        var hookah = (HookahBlockEntity) h.getBlockEntity(A);
        hookah.fuel(new ItemStack(ModItems.WEED_BUD.get(), 4));
        var machine = (MachineBlockEntity) h.getBlockEntity(C);
        machine.setItem(0, of(ModItems.ROUGH_BUD.get(), 2, Strain.BLUE_DREAM));
        h.succeedWhen(() -> {
            h.assertTrue(machine.getItem(0).isEmpty(), "both buds trimmed");
            ItemStack out = machine.getItem(MachineBlockEntity.INPUTS);
            h.assertTrue(out.is(ModItems.TRIMMED_BUD.get()) && out.getCount() == 2 && ModDataComponents.strain(out) == Strain.BLUE_DREAM, "two Blue Dream trimmed buds, got " + out);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void withNoPowerAMachinePausesAndKeepsItsProgress(GameTestHelper h) {
        h.setBlock(B, ModPower.SNIP_STATION.get());
        var machine = (MachineBlockEntity) h.getBlockEntity(B);
        machine.setItem(0, of(ModItems.ROUGH_BUD.get(), 1, Strain.LANDRACE));
        machine.node().receive(16 * 5);     // five ticks' worth
        h.runAfterDelay(30, () -> {
            h.assertTrue(machine.progress() == 5, "stopped at 5 of 40, at " + machine.progress());
            h.assertTrue(machine.getItem(0).getCount() == 1, "nothing lost");
            machine.node().receive(16 * 40);
            h.runAfterDelay(25, () -> {
                h.assertTrue(machine.progress() >= 25, "picked up where it left off, at " + machine.progress());
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty", batch = "power_high", timeoutTicks = 100)
    public static void aHighPlayerRunsAMachineFree(GameTestHelper h) {
        h.setBlock(B, ModPower.SNIP_STATION.get());
        var machine = (MachineBlockEntity) h.getBlockEntity(B);
        machine.setItem(0, of(ModItems.ROUGH_BUD.get(), 1, Strain.LANDRACE));
        var player = h.makeMockServerPlayerInLevel();
        player.moveTo(h.absoluteVec(B.above().getCenter()));
        StrainEffects.markHigh(player, 1000);
        h.assertTrue(StrainEffects.isHigh(player), "marked high");
        h.assertTrue(MachinePower.highNear(h.getLevel(), h.absolutePos(B)), "a high player is near");
        h.succeedWhen(() -> h.assertTrue(machine.getItem(0).isEmpty(), "trimmed with no BI at all"));
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void tooMuchPowerBurnsAWickOutAndLeavesAsh(GameTestHelper h) {
        h.setBlock(A, ModPower.DOUBLE_HOSE.get());
        h.setBlock(B, ModPower.HEMP_WICK.get());
        h.setBlock(C, ModPower.STASH_JAR.get());
        ((HookahBlockEntity) h.getBlockEntity(A)).fuel(new ItemStack(ModItems.WEED_BUD.get(), 1));
        h.succeedWhen(() -> {
            h.assertBlockPresent(Blocks.AIR, B);
            h.assertItemEntityPresent(ModPower.ASH.get(), B, 2);
            h.assertBlockPresent(ModPower.STASH_JAR.get(), C);
            h.assertBlockPresent(ModPower.DOUBLE_HOSE.get(), A);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aHitBoxStepsPowerDown(GameTestHelper h) {
        var west = Direction.WEST;
        h.setBlock(A, ModPower.DOUBLE_HOSE.get());
        h.setBlock(B, ModPower.HIT_BOX.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.power.HitBoxBlock.FACING, west));
        h.setBlock(C, ModPower.STASH_JAR.get());
        ((HookahBlockEntity) h.getBlockEntity(A)).fuel(new ItemStack(ModItems.WEED_BUD.get(), 1));
        var stash = (StashBlockEntity) h.getBlockEntity(C);
        h.succeedWhen(() -> {
            h.assertTrue(stash.node().stored() > 0, "power reached the Puff Stash");
            h.assertBlockPresent(ModPower.HIT_BOX.get(), B);
        });
    }

    @GameTest(template = "empty")
    public static void twoStashesSettleLevel(GameTestHelper h) {
        h.setBlock(A, ModPower.STASH_JAR.get());
        h.setBlock(B, ModPower.HEMP_WICK.get());
        h.setBlock(C, ModPower.STASH_JAR.get());
        var full = (StashBlockEntity) h.getBlockEntity(A);
        var empty = (StashBlockEntity) h.getBlockEntity(C);
        ((BIBuffer) full.node()).receive(1_000);
        h.succeedWhen(() -> h.assertTrue(Math.abs(full.node().stored() - empty.node().stored()) <= 64, "settled, " + full.node().stored() + " and " + empty.node().stored()));
    }

    @GameTest(template = "empty")
    public static void drainTakesAllOrNothing(GameTestHelper h) {
        h.setBlock(A, ModPower.STASH_JAR.get());
        h.setBlock(B, ModPower.HEMP_WICK.get());
        h.setBlock(C, Blocks.STONE);
        var stash = (StashBlockEntity) h.getBlockEntity(A);
        ((BIBuffer) stash.node()).receive(100);
        h.assertFalse(BIGrid.drain(h.getLevel(), h.absolutePos(C), 500), "not enough");
        h.assertTrue(stash.node().stored() == 100, "nothing taken");
        h.assertTrue(BIGrid.drain(h.getLevel(), h.absolutePos(C), 60), "enough");
        h.assertTrue(stash.node().stored() == 40, "took it, left " + stash.node().stored());
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void hoppersRunALine(GameTestHelper h) {
        var chestIn = new BlockPos(2, 5, 2);
        var hopperIn = new BlockPos(2, 4, 2);
        var machine = new BlockPos(2, 3, 2);
        var hopperOut = new BlockPos(2, 2, 2);
        var chestOut = new BlockPos(2, 1, 2);
        h.setBlock(chestIn, Blocks.CHEST);
        h.setBlock(hopperIn, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.setBlock(machine, ModPower.TRIM_REAPER.get());
        h.setBlock(hopperOut, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.setBlock(chestOut, Blocks.CHEST);
        var m = (MachineBlockEntity) h.getBlockEntity(machine);
        charge(m);
        var in = (ChestBlockEntity) h.getBlockEntity(chestIn);
        in.setItem(0, of(ModItems.ROUGH_BUD.get(), 2, Strain.BLUE_DREAM));
        in.setItem(1, of(ModItems.ROUGH_BUD.get(), 2, Strain.CHERRY_PIE));
        in.setItem(2, new ItemStack(ModItems.WEED_JOINT.get()));
        var out = (ChestBlockEntity) h.getBlockEntity(chestOut);
        h.succeedWhen(() -> {
            int blue = 0, cherry = 0;
            for (int i = 0; i < out.getContainerSize(); i++) {
                ItemStack s = out.getItem(i);
                if (!s.is(ModItems.TRIMMED_BUD.get())) continue;
                if (ModDataComponents.strain(s) == Strain.BLUE_DREAM) blue += s.getCount();
                if (ModDataComponents.strain(s) == Strain.CHERRY_PIE) cherry += s.getCount();
            }
            h.assertTrue(blue == 2 && cherry == 2, "both strains came out, " + blue + " and " + cherry);
            h.assertTrue(((HopperBlockEntity) h.getBlockEntity(hopperIn)).getItem(0).is(ModItems.WEED_JOINT.get()), "the machine refused a joint");
        });
    }

    @GameTest(template = "empty")
    public static void theItemCapabilityFollowsTheSides(GameTestHelper h) {
        h.setBlock(B, ModPower.DRY_BOX.get());
        var m = (MachineBlockEntity) h.getBlockEntity(B);
        m.setItem(MachineBlockEntity.INPUTS, new ItemStack(ModItems.WEED_BUD.get(), 3));
        var pos = h.absolutePos(B);
        var top = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        var bottom = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        h.assertTrue(top != null && bottom != null, "both sides have a handler");
        ItemStack bud = new ItemStack(ModItems.TRIMMED_BUD.get());
        h.assertTrue(top.insertItem(0, bud.copy(), false).isEmpty(), "the top takes inputs");
        h.assertFalse(bottom.insertItem(0, bud.copy(), true).isEmpty(), "the bottom doesn't");
        h.assertTrue(bottom.extractItem(0, 1, false).is(ModItems.WEED_BUD.get()), "the bottom gives outputs");
        h.assertTrue(top.extractItem(0, 1, true).isEmpty(), "the top gives nothing");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 500)
    public static void theHumidorCuresABatchAtOnce(GameTestHelper h) {
        h.setBlock(B, ModPower.THE_HUMIDOR.get());
        var m = (MachineBlockEntity) h.getBlockEntity(B);
        charge(m);
        m.setItem(0, of(ModItems.TRIMMED_BUD.get(), 64, Strain.STICKY_ICKY));
        h.runAfterDelay(450, () -> {
            ItemStack out = m.getItem(MachineBlockEntity.INPUTS);
            h.assertTrue(out.is(ModItems.WEED_BUD.get()) && out.getCount() == 64 && ModDataComponents.strain(out) == Strain.STICKY_ICKY, "64 cured in one go, got " + out);
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void everyHigherTierIsFaster(GameTestHelper h) {
        var tiers = List.of(ModPower.SNIP_STATION.get(), ModPower.BUD_BARBER.get(), ModPower.TRIM_REAPER.get());
        int last = Integer.MAX_VALUE;
        for (var block : tiers) {
            int ticks = com.malevolentgods.fourtwenty.processing.ProcessingConfig.trim(block.index()).ticks();
            h.assertTrue(ticks < last, block + " takes " + ticks);
            last = ticks;
        }
        h.succeed();
    }
}
