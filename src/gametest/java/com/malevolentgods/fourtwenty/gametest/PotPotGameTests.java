package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.PottedPotPotBlock;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class PotPotGameTests {
    private static final String CODE = "pin_pin_pin";

    @GameTest(template = "empty")
    public static void aSeedInAPotPotGrowsAndHarvestsLikeTheCrop(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 2, 1);
        h.setBlock(pos, ModTerpenes.POT_POTS.get(CODE).get());
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        // Planting: a seed makes it a Potted Pot Pot of the same colour, with a sprout; the seed is used.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WEED_SEEDS.get(), 2));
        h.useBlock(pos, player);
        BlockState potted = h.getBlockState(pos);
        h.assertTrue(potted.is(ModTerpenes.POTTED_POT_POTS.get(CODE).get()) && potted.getValue(PottedPotPotBlock.AGE) == 0, "not planted: " + potted);
        h.assertTrue(player.getMainHandItem().getCount() == 1, "the seed wasn't used");
        // Not mature: right-clicking does nothing.
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.useBlock(pos, player);
        h.assertTrue(h.getBlockState(pos).getValue(PottedPotPotBlock.AGE) == 0, "harvested a sprout");
        // Bone meal grows it to maturity.
        var block = (PottedPotPotBlock) potted.getBlock();
        for (int i = 0; i < 10 && h.getBlockState(pos).getValue(PottedPotPotBlock.AGE) < PottedPotPotBlock.MAX_AGE; i++) {
            block.performBonemeal(h.getLevel(), h.getLevel().getRandom(), h.absolutePos(pos), h.getBlockState(pos));
        }
        h.assertTrue(h.getBlockState(pos).getValue(PottedPotPotBlock.AGE) == PottedPotPotBlock.MAX_AGE, "bone meal didn't grow it");
        // Harvest: buds drop, and the plant starts again from a sprout.
        h.useBlock(pos, player);
        h.assertTrue(h.getBlockState(pos).getValue(PottedPotPotBlock.AGE) == 0, "not reset after the harvest");
        h.assertItemEntityPresent(ModItems.ROUGH_BUD.get(), pos, 2.0);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pottedPotPotsBreakAsThemselves(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 2, 1);
        Block potted = ModTerpenes.POTTED_POT_POTS.get(CODE).get();
        h.setBlock(pos, potted.defaultBlockState().setValue(PottedPotPotBlock.AGE, 5));
        h.getLevel().destroyBlock(h.absolutePos(pos), true);
        h.assertItemEntityPresent(potted.asItem(), pos, 2.0);
        BlockPos empty = new BlockPos(3, 2, 1);
        h.setBlock(empty, ModTerpenes.POT_POTS.get(CODE).get());
        h.getLevel().destroyBlock(h.absolutePos(empty), true);
        h.assertItemEntityPresent(ModTerpenes.POT_POTS.get(CODE).get().asItem(), empty, 2.0);
        // A placed Potted Pot Pot holds a sprout.
        h.assertTrue(potted.defaultBlockState().getValue(PottedPotPotBlock.AGE) == 0, "a placed Potted Pot Pot isn't a sprout");
        h.succeed();
    }
}
