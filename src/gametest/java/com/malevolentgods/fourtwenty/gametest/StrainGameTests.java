package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.infuser.Infusion;
import com.malevolentgods.fourtwenty.item.WeedPipeItem;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class StrainGameTests {
    private static ItemStack of(net.minecraft.world.level.ItemLike item, int n, Strain s) {
        return ModDataComponents.withStrain(new ItemStack(item, n), s);
    }

    /** A planted strain survives growth (random ticks and bone meal) and comes back in the harvest. */
    @GameTest(template = "empty")
    public static void aCropKeepsItsStrainFromSeedToHarvest(GameTestHelper h) {
        WeedCropBlock crop = ModBlocks.WEED_CROP.get();
        BlockPos pos = new BlockPos(1, 2, 1);
        h.setBlock(pos.below(), Blocks.FARMLAND);
        h.setBlock(pos, crop.defaultBlockState().setValue(WeedCropBlock.STRAIN, Strain.BLUE_DREAM));
        BlockPos abs = h.absolutePos(pos);
        for (int i = 0; i < 12; i++) crop.growCrops(h.getLevel(), abs, h.getLevel().getBlockState(abs));
        BlockState grown = h.getLevel().getBlockState(abs);
        h.assertTrue(crop.isMaxAge(grown), "bone meal grew it to full age");
        h.assertTrue(grown.getValue(WeedCropBlock.STRAIN) == Strain.BLUE_DREAM, "growth kept the strain, got " + grown.getValue(WeedCropBlock.STRAIN));
        List<ItemStack> drops = Block.getDrops(grown, h.getLevel(), abs, null);
        h.assertTrue(drops.stream().anyMatch(s -> s.is(ModItems.ROUGH_BUD.get())), "a ripe crop drops rough buds");
        for (ItemStack s : drops) h.assertTrue(ModDataComponents.strain(s) == Strain.BLUE_DREAM, "every drop is Blue Dream: " + s);
        h.assertTrue(crop.getCloneItemStack(h.getLevel(), abs, grown).get(ModDataComponents.STRAIN.get()) == Strain.BLUE_DREAM, "pick-block gives Blue Dream seeds");
        h.succeed();
    }

    /** Landrace is stored as no component at all, so new Landrace buds stack with buds from before strains. */
    @GameTest(template = "empty")
    public static void landraceBudsStackWithPlainBuds(GameTestHelper h) {
        ItemStack plain = new ItemStack(ModItems.WEED_BUD.get(), 3);
        ItemStack landrace = of(ModItems.WEED_BUD.get(), 3, Strain.LANDRACE);
        h.assertTrue(ItemStack.isSameItemSameComponents(plain, landrace), "Landrace buds are plain buds");
        h.assertFalse(ItemStack.isSameItemSameComponents(plain, of(ModItems.WEED_BUD.get(), 3, Strain.PURPLE_HAZE)), "a bred strain doesn't stack with them");
        h.succeed();
    }

    /** Paper, bud, paper makes a joint of the bud's strain. */
    @GameTest(template = "empty")
    public static void aJointIsRolledFromItsBudsStrain(GameTestHelper h) {
        var grid = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(Items.PAPER), of(ModItems.WEED_BUD.get(), 1, Strain.TANGIE_TANGO), new ItemStack(Items.PAPER)));
        var level = h.getLevel();
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        h.assertTrue(recipe.isPresent(), "the joint recipe matches");
        ItemStack joint = recipe.get().value().assemble(grid, level.registryAccess());
        h.assertTrue(joint.is(ModItems.WEED_JOINT.get()), "it makes a joint");
        h.assertTrue(ModDataComponents.strain(joint) == Strain.TANGIE_TANGO, "of Tangie Tango, got " + ModDataComponents.strain(joint));
        h.succeed();
    }

    /** A pipe takes buds of one strain, up to 16, and refuses another strain until it's empty. */
    @GameTest(template = "empty")
    public static void aPipeLoadsOneStrainAtATime(GameTestHelper h) {
        ItemStack pipe = new ItemStack(ModItems.WEED_PIPE.get());
        ItemStack buds = of(ModItems.WEED_BUD.get(), 10, Strain.MERMAID_HOTBOX);
        h.assertTrue(WeedPipeItem.fill(pipe, buds) == 10 && buds.isEmpty(), "10 buds went in");
        ItemStack more = of(ModItems.WEED_BUD.get(), 10, Strain.MERMAID_HOTBOX);
        h.assertTrue(WeedPipeItem.fill(pipe, more) == 6 && more.getCount() == 4, "only 6 more fit (16 in all)");
        ItemStack other = of(ModItems.WEED_BUD.get(), 5, Strain.CHERRY_PIE);
        h.assertTrue(WeedPipeItem.fill(pipe, other) == 0 && other.getCount() == 5, "another strain is refused");
        var load = WeedPipeItem.load(pipe);
        h.assertTrue(load.strain() == Strain.MERMAID_HOTBOX && load.buds() == 16, "loaded with 16 Mermaid Hotbox, got " + load);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyStrainCanBeInfusedFromItsColours(GameTestHelper h) {
        var bred = java.util.EnumSet.noneOf(Strain.class);
        Infusion.RECIPES.forEach(r -> bred.add(r.strain()));
        h.assertTrue(bred.size() == Strain.values().length - 1 && !bred.contains(Strain.LANDRACE), "every bred strain has a recipe, got " + bred);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var olive = Infusion.RECIPES.stream().filter(r -> r.strain() == Strain.MOSSY_PHILOSOPHER).findFirst().orElseThrow();
        player.getInventory().add(new ItemStack(ModItems.WEED_SEEDS.get(), 3));
        player.getInventory().add(new ItemStack(olive.items().get(0), 2));
        h.assertTrue(Infusion.available(player, olive) == 0, "olive needs brown as well as green");
        player.getInventory().add(new ItemStack(olive.items().get(1), 5));
        h.assertTrue(Infusion.available(player, olive) == 2, "two of each: two infusions");
        player.getInventory().add(ModDataComponents.withStrain(new ItemStack(ModItems.WEED_SEEDS.get(), 9), Strain.BLUE_DREAM));
        h.assertTrue(Infusion.available(player, olive) == 2, "only Landrace seeds take an infusion");
        h.succeed();
    }
}
