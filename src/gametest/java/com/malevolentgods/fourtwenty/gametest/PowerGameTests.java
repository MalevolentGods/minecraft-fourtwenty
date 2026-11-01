package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BIGrid;
import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
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

    /** Water, a bud and coal: a Hookah lights itself. */
    private static void light(GameTestHelper h, BlockPos pos) {
        var hookah = (WeedBongBlockEntity) h.getBlockEntity(pos);
        hookah.setItem(WeedBongBlockEntity.WEED_SLOT, new ItemStack(ModItems.WEED_BUD.get(), 2));
        hookah.setItem(WeedBongBlockEntity.WATER_SLOT, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        hookah.setItem(WeedBongBlockEntity.FUEL_SLOT, new ItemStack(net.minecraft.world.item.Items.COAL));
    }

    private static void charge(MachineBlockEntity machine) {
        machine.node().receive(machine.node().capacity());
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void aHookahPowersAMachineThroughAWick(GameTestHelper h) {
        h.setBlock(A, com.malevolentgods.fourtwenty.registry.ModBlocks.WEED_BONG.get());
        h.setBlock(B, ModPower.HEMP_WICK.get());
        h.setBlock(C, ModPower.SNIP_STATION.get());
        light(h, A);
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
        machine.node().receive(5);          // a few ticks' worth at 0.875 a tick
        h.runAfterDelay(30, () -> {
            int held = machine.progress();
            h.assertTrue(held >= 4 && held <= 6, "stopped early, at " + held);
            h.assertTrue(machine.getItem(0).getCount() == 1, "nothing lost");
            machine.node().receive(40);
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

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aWireCarryingTooMuchBurnsOutAndLeavesAsh(GameTestHelper h) {
        h.setBlock(A, ModPower.BIG_BERTHA.get());
        h.setBlock(B, ModPower.HEMP_WICK.get());
        h.setBlock(C, ModPower.MOTHER_STASH.get());
        light(h, A);
        h.succeedWhen(() -> {
            h.assertBlockPresent(Blocks.AIR, B);
            h.assertItemEntityPresent(ModPower.ASH.get(), B, 2);
            h.assertBlockPresent(ModPower.MOTHER_STASH.get(), C);
            h.assertBlockPresent(ModPower.BIG_BERTHA.get(), A);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aWireCarriesTwoBatteriesButNotThree(GameTestHelper h) {
        var wick = new BlockPos(2, 2, 2);
        var sink = new BlockPos(3, 2, 2);
        h.setBlock(wick, ModPower.HEMP_WICK.get());
        h.setBlock(sink, ModPower.TRIM_REAPER.get());
        for (BlockPos jar : List.of(new BlockPos(1, 2, 2), new BlockPos(2, 3, 2), new BlockPos(2, 1, 2))) {
            h.setBlock(jar, ModPower.STASH_JAR.get());
            ((StashBlockEntity) h.getBlockEntity(jar)).node().receive(5_000);
        }
        var okWick = new BlockPos(2, 2, 5);
        var okSink = new BlockPos(3, 2, 5);
        h.setBlock(okWick, ModPower.HEMP_WICK.get());
        h.setBlock(okSink, ModPower.TRIM_REAPER.get());
        for (BlockPos jar : List.of(new BlockPos(1, 2, 5), new BlockPos(2, 3, 5))) {
            h.setBlock(jar, ModPower.STASH_JAR.get());
            ((StashBlockEntity) h.getBlockEntity(jar)).node().receive(5_000);
        }
        h.runAfterDelay(60, () -> {
            h.assertBlockPresent(Blocks.AIR, wick);
            h.assertBlockPresent(ModPower.HEMP_WICK.get(), okWick);
            h.assertTrue(((MachineBlockEntity) h.getBlockEntity(okSink)).node().stored() > 0, "two batteries got through");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aBlockOverloadsButNeverBreaks(GameTestHelper h) {
        h.setBlock(A, ModPower.STASH_BOX.get());
        h.setBlock(B, ModPower.ROLLED_WICK.get());
        h.setBlock(C, ModPower.SNIP_STATION.get());
        ((StashBlockEntity) h.getBlockEntity(A)).node().receive(1_000);
        var machine = (MachineBlockEntity) h.getBlockEntity(C);
        machine.setItem(0, of(ModItems.ROUGH_BUD.get(), 1, Strain.LANDRACE));
        machine.node().receive(100);
        h.runAfterDelay(30, () -> {
            h.assertBlockPresent(ModPower.SNIP_STATION.get(), C);
            h.assertBlockPresent(ModPower.ROLLED_WICK.get(), B);
            h.assertTrue(machine.createDataForTest() == 1, "it says it's overloaded");
            h.assertTrue(machine.progress() == 0, "it stopped working");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void aBatteryOfItsTierFeedsAMachine(GameTestHelper h) {
        h.setBlock(A, ModPower.STASH_JAR.get());
        h.setBlock(B, ModPower.HEMP_WICK.get());
        h.setBlock(C, ModPower.SNIP_STATION.get());
        ((StashBlockEntity) h.getBlockEntity(A)).node().receive(1_000);
        var machine = (MachineBlockEntity) h.getBlockEntity(C);
        h.succeedWhen(() -> h.assertTrue(machine.node().stored() > 0, "power arrived"));
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void upgradedHookahsAreStillHookahs(GameTestHelper h) {
        h.setBlock(B, ModPower.BIG_BERTHA.get());
        light(h, B);
        var hookah = (WeedBongBlockEntity) h.getBlockEntity(B);
        h.succeedWhen(() -> {
            h.assertTrue(hookah.isLit(), "it lights like a Hookah");
            h.assertTrue(hookah.node().stored() >= 100, "and makes Rip power, at " + hookah.node().stored());
            h.assertTrue(hookah.node().tier() == com.malevolentgods.fourtwenty.domain.BIRules.Tier.RIP, "Big Bertha is Rip");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aHitBoxStepsPowerDown(GameTestHelper h) {
        var west = Direction.WEST;
        h.setBlock(A, ModPower.DOUBLE_HOSE.get());
        h.setBlock(B, ModPower.HIT_BOX.get().defaultBlockState().setValue(com.malevolentgods.fourtwenty.power.HitBoxBlock.FACING, west));
        h.setBlock(C, ModPower.STASH_JAR.get());
        light(h, A);
        var stash = (StashBlockEntity) h.getBlockEntity(C);
        h.succeedWhen(() -> {
            h.assertTrue(stash.node().stored() > 0, "power reached the Puff Stash");
            h.assertBlockPresent(ModPower.HIT_BOX.get(), B);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void twoStashesSettleLevel(GameTestHelper h) {
        h.setBlock(A, ModPower.STASH_JAR.get());
        h.setBlock(B, ModPower.HEMP_WICK.get());
        h.setBlock(C, ModPower.STASH_JAR.get());
        var full = (StashBlockEntity) h.getBlockEntity(A);
        var empty = (StashBlockEntity) h.getBlockEntity(C);
        ((BIBuffer) full.node()).receive(1_000);
        h.succeedWhen(() -> h.assertTrue(Math.abs(full.node().stored() - empty.node().stored()) <= 8, "settled, " + full.node().stored() + " and " + empty.node().stored()));
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
