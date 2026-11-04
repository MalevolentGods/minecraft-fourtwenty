package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.CureRules;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.TrimRules;
import com.malevolentgods.fourtwenty.processing.CuringRackBlockEntity;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class ProcessingGameTests {
    private static ItemStack of(net.minecraft.world.item.Item item, int count, Strain strain) {
        return ModDataComponents.withStrain(new ItemStack(item, count), strain);
    }

    private static int count(Player player, net.minecraft.world.item.Item item, Strain strain) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(item) && ModDataComponents.strain(s) == strain) n += s.getCount();
        return n;
    }

    /** The share of random ticks a young plant at {@code pos} grows on. */
    private static double growthRate(GameTestHelper h, BlockPos pos, net.minecraft.world.level.block.state.BlockState young) {
        var level = h.getLevel();
        var at = h.absolutePos(pos);
        int rolls = 4000, grown = 0;
        for (int i = 0; i < rolls; i++) {
            level.setBlock(at, young, 2);
            level.getBlockState(at).randomTick(level, at, level.random);
            if (!level.getBlockState(at).equals(young)) grown++;
        }
        return grown / (double) rolls;
    }

    @GameTest(template = "empty")
    public static void weedTakesTwiceAsLongToGrow(GameTestHelper h) {
        var crop = new BlockPos(2, 3, 2);
        var pot = new BlockPos(4, 3, 2);
        h.setBlock(new BlockPos(3, 3, 2), net.minecraft.world.level.block.Blocks.GLOWSTONE);
        h.setBlock(crop.below(), net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7));
        h.runAfterDelay(5, () -> {
            double planted = growthRate(h, crop, ModBlocks.WEED_CROP.get().defaultBlockState());
            double potted = growthRate(h, pot, com.malevolentgods.fourtwenty.registry.ModTerpenes.POTTED_POT_POTS.values().iterator().next().get().defaultBlockState());
            h.assertTrue(Math.abs(planted - 1 / 14.0) <= 0.02, "crop grew on " + planted + " of its random ticks");
            h.assertTrue(Math.abs(potted - 1 / 16.0) <= 0.02, "pot grew on " + potted + " of its random ticks");
            h.succeed();
        });
    }

    /** The trim a colony's Trim Daddy uses is the player's own: same bud, same strain and traits, same seed odds, nothing consumed. */
    @GameTest(template = "empty")
    public static void aTrayTrimsForAnyoneTheSameWay(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.TRIM_TRAY.get());
        var traits = new com.malevolentgods.fourtwenty.domain.Traits(4, 3, 5);
        ItemStack rough = ModDataComponents.withTraits(of(ModItems.ROUGH_BUD.get(), 5, Strain.CHERRY_PIE), traits);
        ProcessingConfig.overrideChances(new TrimRules.Chances(1, 1, 1, 1, 1));
        java.util.List<ItemStack> always;
        try {
            always = com.malevolentgods.fourtwenty.processing.TrimTrayBlock.trim(h.getLevel(), h.absolutePos(pos), rough);
        } finally {
            ProcessingConfig.overrideChances(null);
        }
        h.assertTrue(rough.getCount() == 5, "the caller uses up the bud, not the tray");
        h.assertTrue(always.size() == 2 && always.get(0).is(ModItems.TRIMMED_BUD.get()) && always.get(1).is(ModItems.WEED_SEEDS.get()), "a trimmed bud and a seed");
        for (ItemStack out : always) {
            h.assertTrue(ModDataComponents.strain(out) == Strain.CHERRY_PIE && ModDataComponents.traits(out).equals(traits), "bred like the bud: " + out);
        }
        ProcessingConfig.overrideChances(new TrimRules.Chances(0, 0, 0, 0, 0));
        try {
            h.assertTrue(com.malevolentgods.fourtwenty.processing.TrimTrayBlock.trim(h.getLevel(), h.absolutePos(pos), rough).size() == 1, "no seed at chance 0");
        } finally {
            ProcessingConfig.overrideChances(null);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void trayTrimsOneBudAndKeepsTheStrain(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.TRIM_TRAY.get());
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, of(ModItems.ROUGH_BUD.get(), 3, Strain.BLUE_DREAM));
        ProcessingConfig.overrideChances(new TrimRules.Chances(1, 1, 1, 1, 1));
        try {
            h.useBlock(pos, player);
        } finally {
            ProcessingConfig.overrideChances(null);
        }
        h.assertTrue(player.getMainHandItem().getCount() == 2, "one bud per click, left " + player.getMainHandItem().getCount());
        h.assertTrue(count(player, ModItems.TRIMMED_BUD.get(), Strain.BLUE_DREAM) == 1, "a Blue Dream trimmed bud");
        h.assertTrue(count(player, ModItems.WEED_SEEDS.get(), Strain.BLUE_DREAM) == 1, "a Blue Dream seed at chance 1");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void traySeedRateFollowsTheFamily(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.TRIM_TRAY.get());
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        int rolls = 1000;
        for (Strain strain : new Strain[] {Strain.LANDRACE, Strain.COUCHLOCK_HUM, Strain.CHERRY_PIE, Strain.STICKY_ICKY, Strain.BLUE_DREAM}) {
            player.getInventory().clearContent();
            for (int i = 0; i < rolls; i++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, of(ModItems.ROUGH_BUD.get(), 1, strain));
                h.useBlock(pos, player);
                if (i % 200 == 199) player.getInventory().items.forEach(s -> { if (s.is(ModItems.TRIMMED_BUD.get())) s.setCount(0); });
            }
            double rate = count(player, ModItems.WEED_SEEDS.get(), strain) / (double) rolls;
            double want = TrimRules.Chances.DEFAULT.of(strain.family());
            h.assertTrue(Math.abs(rate - want) <= 0.05, strain + " seed rate " + rate + ", wanted about " + want);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void roughBudsDoNothingOnARack(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.CURING_RACK.get());
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, of(ModItems.ROUGH_BUD.get(), 1, Strain.LANDRACE));
        h.useBlock(pos, player);
        h.assertTrue(player.getMainHandItem().getCount() == 1, "the rack takes trimmed buds only");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void rackCuresKeepsTheStrainAndDropsWhatItHolds(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.CURING_RACK.get());
        ProcessingConfig.overrideRack(new CureRules.Tier(10, 8, 0));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, of(ModItems.TRIMMED_BUD.get(), 2, Strain.CHERRY_PIE));
        h.useBlock(pos, player);
        h.useBlock(pos, player);
        h.assertTrue(player.getMainHandItem().isEmpty(), "both buds hung");
        h.runAfterDelay(15, () -> {
            ProcessingConfig.overrideRack(null);
            var rack = (CuringRackBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
            h.assertTrue(rack != null, "the rack is there");
            ItemStack bud = rack.take(true);
            h.assertTrue(bud.is(ModItems.WEED_BUD.get()) && ModDataComponents.strain(bud) == Strain.CHERRY_PIE, "a cured Cherry Pie bud, got " + bud);
            var drops = net.minecraft.world.level.block.Block.getDrops(h.getBlockState(pos), h.getLevel(), h.absolutePos(pos), rack);
            h.assertTrue(drops.stream().anyMatch(s -> s.is(ModItems.CURING_RACK.get())), "the rack drops itself");
            h.destroyBlock(pos);
            h.assertItemEntityPresent(ModItems.WEED_BUD.get(), pos, 2);
            h.succeed();
        });
    }
}
