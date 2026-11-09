package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.compat.minecolonies.StarterSeeds;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.TrimRules;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** The progression balance (specs/012-progression-balance) in the world: harvests, trimming seeds and the starting seeds. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class ProgressionGameTests {
    private ProgressionGameTests() {}

    static BlockState mature(Strain strain) {
        return ModBlocks.WEED_CROP.get().getStateForAge(7).setValue(WeedCropBlock.STRAIN, strain);
    }

    static int count(List<ItemStack> drops, net.minecraft.world.item.Item item) {
        return drops.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** A mature Landrace crop gives its seed back and one rough bud, every time. */
    @GameTest(template = "empty")
    public static void landraceGivesOneBudAndItsSeed(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < 50; i++) {
            var drops = Block.getDrops(mature(Strain.LANDRACE), h.getLevel(), pos, null, player, ItemStack.EMPTY);
            h.assertTrue(count(drops, ModItems.WEED_SEEDS.get()) == 1, "one seed back");
            h.assertTrue(count(drops, ModItems.ROUGH_BUD.get()) == 1, "one rough bud: " + count(drops, ModItems.ROUGH_BUD.get()));
            h.assertTrue(drops.stream().filter(s -> s.is(ModItems.ROUGH_BUD.get())).allMatch(s -> ModDataComponents.strain(s) == Strain.LANDRACE), "buds keep the strain");
        }
        h.succeed();
    }

    /** About one Landrace bud in eight gives a seed when trimmed (12%, between 9% and 15% over a thousand). */
    @GameTest(template = "empty")
    public static void landraceTrimsToASeedAboutOneInEight(GameTestHelper h) {
        var rng = h.getLevel().getRandom();
        int seeds = 0;
        for (int i = 0; i < 1000; i++) if (TrimRules.seeded(ProcessingConfig.chances(), Strain.LANDRACE, rng.nextDouble())) seeds++;
        h.assertTrue(seeds >= 90 && seeds <= 150, "Landrace trimming seeds per thousand: " + seeds);
        h.succeed();
    }

    /** A player's starting Landrace seeds come once: the first time four, then never again. */
    @GameTest(template = "empty")
    public static void starterSeedsComeOnce(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(StarterSeeds.give(player, 4), "the first time gives them");
        int seeds = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ModItems.WEED_SEEDS.get())) {
                h.assertTrue(ModDataComponents.strain(s) == Strain.LANDRACE, "Landrace seeds");
                seeds += s.getCount();
            }
        }
        h.assertTrue(seeds == 4, "four seeds: " + seeds);
        h.assertTrue(!StarterSeeds.give(player, 4), "never twice");
        h.succeed();
    }

    /** Up the ladder, more buds: Golden God gives 4 or 5 every time, a Cross 2 or 3 (about 2.5), a poor yield trait never less than 1. */
    @GameTest(template = "empty")
    public static void familiesYieldTheirRange(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        var abs = h.absolutePos(pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < 50; i++) {
            int golden = count(Block.getDrops(mature(Strain.GOLDEN_GOD), h.getLevel(), abs, null, player, ItemStack.EMPTY), ModItems.ROUGH_BUD.get());
            h.assertTrue(golden >= 4 && golden <= 5, "Golden God buds: " + golden);
        }
        int total = 0;
        for (int i = 0; i < 200; i++) total += count(Block.getDrops(mature(Strain.TROPICAL_THUNDER), h.getLevel(), abs, null, player, ItemStack.EMPTY), ModItems.ROUGH_BUD.get());
        h.assertTrue(total >= 440 && total <= 560, "a Cross averages about 2.5 buds: " + total / 200.0);
        h.setBlock(pos, mature(Strain.LANDRACE));
        var crop = (com.malevolentgods.fourtwenty.block.WeedCropBlockEntity) h.getBlockEntity(pos);
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Potency", 2);
        tag.putInt("Yield", 1);
        tag.putInt("Length", 2);
        crop.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(crop.traits().yield() == 1, "the crop's yield trait is 1");
        for (int i = 0; i < 20; i++) {
            int buds = count(Block.getDrops(h.getBlockState(pos), h.getLevel(), abs, crop, player, ItemStack.EMPTY), ModItems.ROUGH_BUD.get());
            h.assertTrue(buds == 1, "a poor yield trait on Landrace still gives one: " + buds);
        }
        h.succeed();
    }
}
