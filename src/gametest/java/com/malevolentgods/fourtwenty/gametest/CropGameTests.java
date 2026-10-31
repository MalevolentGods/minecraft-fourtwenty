package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class CropGameTests {
    @GameTest(template = "empty")
    public static void allAgesUseStandardLoot(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (int age = 0; age <= 7; age++) {
            for (int repeat = 0; repeat < 100; repeat++) {
                var drops = Block.getDrops(ModBlocks.WEED_CROP.get().getStateForAge(age), h.getLevel(), pos,
                    null, player, ItemStack.EMPTY);
                int seeds = drops.stream().filter(s -> s.is(ModItems.WEED_SEEDS.get())).mapToInt(ItemStack::getCount).sum();
                int buds = drops.stream().filter(s -> s.is(ModItems.ROUGH_BUD.get())).mapToInt(ItemStack::getCount).sum();
                h.assertTrue(drops.stream().allMatch(s -> s.is(ModItems.WEED_SEEDS.get()) || s.is(ModItems.ROUGH_BUD.get())), "Only intended crop loot");
                h.assertTrue(age == 7 ? seeds == 1 && buds >= 1 && buds <= 3 : seeds == 1 && buds == 0,
                    "Growth stage " + age + " has valid renewable harvest");
            }
            var wild = Block.getDrops(ModBlocks.WILD_WEED.get().getStateForAge(age), h.getLevel(), pos, null, player, ItemStack.EMPTY);
            h.assertTrue(wild.stream().allMatch(s -> s.is(ModItems.WEED_SEEDS.get())), "Wild weed drops only seeds");
            int count = wild.stream().mapToInt(ItemStack::getCount).sum();
            h.assertTrue(count >= 1 && count <= (age < 4 ? 1 : age < 6 ? 2 : age < 7 ? 3 : 4), "Wild age-dependent seed bounds");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rightClickHarvestIsSingleAndReplants(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos.below(), Blocks.FARMLAND);
        h.setBlock(pos, ModBlocks.WEED_CROP.get().getStateForAge(7));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.useBlock(pos, player);
        h.assertTrue(ModBlocks.WEED_CROP.get().getAge(h.getBlockState(pos)) == 0, "Harvest replants at zero");
        h.assertItemEntityPresent(ModItems.ROUGH_BUD.get(), pos, 3);
        int before = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(3))
            .stream().mapToInt(e -> e.getItem().getCount()).sum();
        h.useBlock(pos, player);
        int after = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(3))
            .stream().mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(before == after, "Repeated click awards nothing");
        h.assertItemEntityNotPresent(Items.DIAMOND, pos, 3);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void staleInteractionCannotHarvestAgainOrResurrectCrop(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        var absolute = h.absolutePos(pos);
        var mature = ModBlocks.WEED_CROP.get().getStateForAge(7);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.setBlock(pos.below(), Blocks.FARMLAND);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute), net.minecraft.core.Direction.UP, absolute, false);
        for (var current : new net.minecraft.world.level.block.state.BlockState[]{Blocks.AIR.defaultBlockState(), ModBlocks.WEED_CROP.get().getStateForAge(0)}) {
            h.setBlock(pos, current);
            mature.useWithoutItem(h.getLevel(), player, hit);
            h.assertTrue(h.getBlockState(pos).equals(current), "Pre-event crop snapshot cannot rewrite a harvested position");
            h.assertItemEntityNotPresent(ModItems.ROUGH_BUD.get(), pos, 3);
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "crop_no_drops")
    public static void disabledBlockDropsRemainDisabled(GameTestHelper h) {
        var rule = h.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
        boolean previous = rule.get();
        try {
            rule.set(false, h.getLevel().getServer());
            var pos = new BlockPos(2, 2, 2);
            h.setBlock(pos.below(), Blocks.FARMLAND);
            h.setBlock(pos, ModBlocks.WEED_CROP.get().getStateForAge(7));
            h.useBlock(pos, h.makeMockPlayer(GameType.SURVIVAL));
            h.assertItemEntityNotPresent(ModItems.ROUGH_BUD.get(), pos, 3);
            h.assertItemEntityNotPresent(ModItems.WEED_SEEDS.get(), pos, 3);
        } finally {
            rule.set(previous, h.getLevel().getServer());
        }
        h.succeed();
    }
}
