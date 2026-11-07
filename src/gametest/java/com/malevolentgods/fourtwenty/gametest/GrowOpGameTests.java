package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.domain.Studies;
import com.malevolentgods.fourtwenty.infuser.Infuser;
import com.malevolentgods.fourtwenty.power.GrowOpBlockEntity;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.processing.ThinkTankBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class GrowOpGameTests {
    private static final BlockPos PLOT = new BlockPos(2, 1, 2), CROP = PLOT.above();

    @GameTest(template = "empty")
    public static void plotsHoldAnyCropWithNoSoil(GameTestHelper h) {
        h.setBlock(PLOT, ModPower.HYDRO_BED.get());
        h.setBlock(CROP, Blocks.WHEAT);
        var pos = h.absolutePos(CROP);
        h.assertTrue(Blocks.WHEAT.defaultBlockState().canSurvive(h.getLevel(), pos), "wheat grows on a plot");
        h.assertTrue(ModBlocks.WEED_CROP.get().defaultBlockState().canSurvive(h.getLevel(), pos), "so does weed");
        h.assertFalse(Blocks.OAK_SAPLING.defaultBlockState().canSurvive(h.getLevel(), pos), "a sapling isn't a crop");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void clickingAPlotWithASeedPlantsIt(GameTestHelper h) {
        h.setBlock(PLOT, ModPower.CLOSET_GROW.get());
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WEED_SEEDS.get(), 2));
        h.useBlock(PLOT, player);
        h.assertBlockPresent(ModBlocks.WEED_CROP.get(), CROP);
        h.assertTrue(player.getMainHandItem().getCount() == 1, "one seed used");
        h.useBlock(PLOT, player);
        h.assertTrue(player.getMainHandItem().getCount() == 1, "a plot with a crop on it takes no more");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void aClosetGrowNeverGlows(GameTestHelper h) {
        h.setBlock(PLOT, ModPower.CLOSET_GROW.get());
        h.setBlock(CROP, ModBlocks.WEED_CROP.get().getStateForAge(1));
        var plot = (GrowOpBlockEntity) h.getBlockEntity(PLOT);
        h.runAfterDelay(20, () -> {
            h.assertTrue(plot.running(), "it grows the crop");
            h.assertFalse(h.getBlockState(PLOT).getValue(com.malevolentgods.fourtwenty.power.BIBlock.LIT), "with no glow, as it has no power");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aWickStaysLitWhileItCarries(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 3);
        ((com.malevolentgods.fourtwenty.power.StashBlockEntity) h.getBlockEntity(l.put(1, ModPower.STASH_JAR.get()))).node().receive(60_000);
        l.put(3, ModPower.SNIP_STATION.get());
        int[] ticks = {0};
        h.onEachTick(() -> {
            if (++ticks[0] <= 20) return;
            h.assertTrue(h.getBlockState(l.plus(2)).getValue(com.malevolentgods.fourtwenty.power.WickBlock.LIT), "dark at tick " + ticks[0]);
            if (ticks[0] >= 80) h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void upgradingKeepsTheCrop(GameTestHelper h) {
        h.setBlock(PLOT, ModPower.CLOSET_GROW.get());
        h.setBlock(CROP, ModBlocks.WEED_CROP.get().getStateForAge(3));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModPower.GROW_TENT.get()));
        h.useBlock(PLOT, player);
        h.assertBlockPresent(ModPower.GROW_TENT.get(), PLOT);
        h.assertTrue(player.getMainHandItem().isEmpty(), "the plot was used up");
        h.assertTrue(ModBlocks.WEED_CROP.get().getAge(h.getBlockState(CROP)) == 3, "the crop stayed");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModPower.AERO_POD.get()));
        h.useBlock(PLOT, player);
        h.assertBlockPresent(ModPower.GROW_TENT.get(), PLOT);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aPoweredPlotWithNoPowerDoesNoHarm(GameTestHelper h) {
        h.setBlock(PLOT, ModPower.GROW_TENT.get());
        h.setBlock(CROP, ModBlocks.WEED_CROP.get().getStateForAge(3));
        var plot = (GrowOpBlockEntity) h.getBlockEntity(PLOT);
        h.runAfterDelay(30, () -> {
            h.assertFalse(plot.running(), "no BI, so no bonus");
            h.assertBlockPresent(ModBlocks.WEED_CROP.get(), CROP);
            plot.node().receive(100);
            h.runAfterDelay(5, () -> {
                h.assertTrue(plot.running(), "with BI it runs");
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void aBigHarvestReplantsAndGivesMore(GameTestHelper h) {
        h.setBlock(PLOT, ModPower.AERO_POD.get());
        h.setBlock(CROP, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        var pos = h.absolutePos(CROP);
        var state = h.getLevel().getBlockState(pos);
        Block.dropResources(state, h.getLevel(), pos);
        var event = new net.neoforged.neoforge.event.level.BlockDropsEvent(h.getLevel(), pos, state, null, new java.util.ArrayList<>(java.util.List.of(
                new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.WHEAT, 2)),
                new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.WHEAT_SEEDS, 2)))), null, ItemStack.EMPTY);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.getDrops().get(0).getItem().getCount() == 3, "half as many again, got " + event.getDrops().get(0).getItem());
        h.assertTrue(event.getDrops().get(1).getItem().getCount() == 1, "a seed went back in");
        h.assertTrue(((CropBlock) Blocks.WHEAT).getAge(h.getBlockState(CROP)) == 0, "replanted");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aTier1StudyRunsEndToEndWithAThinkTankAndAClosetGrow(GameTestHelper h) {
        var tankPos = new BlockPos(5, 2, 2);
        h.setBlock(PLOT, ModPower.CLOSET_GROW.get());
        h.setBlock(tankPos, ModPower.THINK_TANK.get());
        var tank = (ThinkTankBlockEntity) h.getBlockEntity(tankPos);
        tank.node().receive(tank.node().capacity());
        var player = h.makeMockServerPlayerInLevel();
        ProcessingConfig.overrideUnitTicks(1);
        tank.pick(player, 0);
        var study = Studies.ALL.get(0);
        h.assertFalse(Infuser.caught(player, study.colour()), "the colour starts unknown");
        for (int i = 0; i < study.inputs().size(); i++) tank.setItem(i, ThinkTankBlockEntity.stack(study.inputs().get(i)));
        h.runAfterDelay(30, () -> {
            h.assertTrue(tank.status() == ThinkTankBlockEntity.STATUS_NO_PLOT, "no crop yet, so it waits, status " + tank.status());
            h.assertTrue(tank.getItem(0).isEmpty(), "inputs were taken");
            h.setBlock(CROP, ModBlocks.WEED_CROP.get().getStateForAge(1));
            h.succeedWhen(() -> {
                h.assertTrue(tank.status() == ThinkTankBlockEntity.STATUS_DONE, "finished, units " + tank.units());
                ProcessingConfig.overrideUnitTicks(null);
                tank.collect(player);
                h.assertTrue(Infuser.caught(player, study.colour()), "collecting teaches the colour");
                h.assertTrue(tank.status() == ThinkTankBlockEntity.STATUS_DONE || tank.units() == 0, "and clears the study");
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theGrandHookahIsBlazeAndGoldWickCarriesIt(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.GOLD_WICK.get(), 1, 3);
        l.hookah(1, ModPower.GRAND_HOOKAH.get());
        var plot = (GrowOpBlockEntity) h.getBlockEntity(l.put(3, ModPower.COSMIC_KUSH_POD.get()));
        h.succeedWhen(() -> {
            h.assertTrue(plot.node().stored() > 0, "power reached the Cosmic Kush Pod");
            h.assertBlockPresent(ModPower.GOLD_WICK.get(), l.plus(2));
        });
    }
}
