package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.PaveRules;
import com.malevolentgods.fourtwenty.paving.PaveNetwork;
import com.malevolentgods.fourtwenty.paving.PavePenItem;
import com.malevolentgods.fourtwenty.paving.Paving;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class PaveGameTests {
    private static final BlockPos A = new BlockPos(1, 2, 1), B = new BlockPos(3, 2, 1);

    private static ServerPlayer paver(GameTestHelper h, ItemStack pen) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, pen);
        return player;
    }

    /** Works the pen on {@code pos} until it paves; the ticks it took, or -1 if it never did. */
    private static int pave(GameTestHelper h, ServerPlayer player, BlockPos pos) {
        for (int t = 1; t <= 200; t++) if (Paving.work(player, player.getMainHandItem(), h.absolutePos(pos))) return t;
        return -1;
    }

    @GameTest(template = "empty")
    public static void dirtPavesQuickerThanStoneAndBudsChargeThePen(GameTestHelper h) {
        h.setBlock(A, Blocks.DIRT);
        h.setBlock(B, Blocks.STONE);
        ServerPlayer player = paver(h, new ItemStack(ModItems.PAVE_PEN.get()));
        player.getInventory().add(new ItemStack(ModItems.ROUGH_BUD.get(), 1));
        int dirt = pave(h, player, A), stone = pave(h, player, B);
        h.assertBlockPresent(ModTerpenes.PATHS.get(0).get(), A);
        h.assertBlockPresent(ModTerpenes.PATHS.get(0).get(), B);
        h.assertTrue(dirt > 0 && dirt < stone, "dirt took " + dirt + " ticks, stone " + stone);
        h.assertTrue(player.getInventory().countItem(ModItems.ROUGH_BUD.get()) == 0, "the rough bud went into the pen");
        var n = Paving.numbers();
        int left = PaveRules.ROUGH - PaveRules.cost(PaveRules.Action.LAY, 0.5, n) - PaveRules.cost(PaveRules.Action.LAY, 1.5, n);
        h.assertTrue(PavePenItem.state(player.getMainHandItem()).charge() == left, "charge " + PavePenItem.state(player.getMainHandItem()).charge() + ", wanted " + left);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void noWeedNoRoad(GameTestHelper h) {
        h.setBlock(A, Blocks.DIRT);
        ServerPlayer player = paver(h, new ItemStack(ModItems.PAVE_PEN.get()));
        h.assertTrue(pave(h, player, A) == -1, "an empty pen with no buds paves nothing");
        h.assertBlockPresent(Blocks.DIRT, A);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pavedRoadTakesSmoothStoneOneForEight(GameTestHelper h) {
        h.setBlock(A, Blocks.DIRT);
        h.setBlock(B, ModTerpenes.PATHS.get(0).get());
        ServerPlayer player = paver(h, new ItemStack(ModItems.PAVE_PEN.get()));
        player.getInventory().add(new ItemStack(ModItems.WEED_BUD.get(), 1));
        PaveNetwork.apply(player, new PaveNetwork.SetMode(2, 0));
        h.assertTrue(pave(h, player, A) == -1, "no smooth stone, no Paved Road");
        player.getInventory().add(new ItemStack(Items.SMOOTH_STONE, 1));
        h.assertTrue(pave(h, player, A) > 0 && pave(h, player, B) > 0, "laid one and upgraded one");
        h.assertBlockPresent(ModTerpenes.PATHS.get(1).get(), A);
        h.assertBlockPresent(ModTerpenes.PATHS.get(1).get(), B);
        h.assertTrue(player.getInventory().countItem(Items.SMOOTH_STONE) == 0, "one smooth stone taken");
        h.assertTrue(PavePenItem.state(player.getMainHandItem()).stone() == PaveRules.ROADS_PER_MATERIAL - 2, "and six roads left on it");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyTheProLaysSlabs(GameTestHelper h) {
        h.setBlock(A, Blocks.DIRT);
        h.setBlock(B, Blocks.DIRT);
        ServerPlayer player = paver(h, new ItemStack(ModItems.PAVE_PEN.get()));
        player.getInventory().add(new ItemStack(ModItems.WEED_BUD.get(), 2));
        PaveNetwork.apply(player, new PaveNetwork.SetMode(1, 1));
        h.assertTrue(PavePenItem.state(player.getMainHandItem()).shape() == 0, "a plain pen stays on whole blocks");
        pave(h, player, A);
        h.assertBlockPresent(ModTerpenes.PATHS.get(0).get(), A);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PAVE_PEN_PRO.get()));
        PaveNetwork.apply(player, new PaveNetwork.SetMode(1, 1));
        pave(h, player, B);
        h.assertBlockPresent(ModTerpenes.PATH_SLABS.get(0).get(), B);
        pave(h, player, A);
        h.assertBlockPresent(ModTerpenes.PATH_SLABS.get(0).get(), A);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theProKeepsThePensCharge(GameTestHelper h) {
        var recipe = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("fourtwenty", "pave_pen_pro")).orElseThrow().value();
        ItemStack pen = new ItemStack(ModItems.PAVE_PEN.get());
        PavePenItem.set(pen, PavePenItem.state(pen).withCharge(1234).withCredit(5, 3));
        var input = CraftingInput.of(2, 2, List.of(pen, new ItemStack(Items.EMERALD), new ItemStack(Items.EMERALD), new ItemStack(ModTerpenes.PATHS.get(1).get())));
        h.assertTrue(((net.minecraft.world.item.crafting.CraftingRecipe) recipe).matches(input, h.getLevel()), "the Pro recipe matches");
        ItemStack pro = ((net.minecraft.world.item.crafting.CraftingRecipe) recipe).assemble(input, h.getLevel().registryAccess());
        h.assertTrue(pro.is(ModItems.PAVE_PEN_PRO.get()), "it makes a Pro");
        var s = PavePenItem.state(pro);
        h.assertTrue(s.charge() == 1234 && s.stone() == 5 && s.gold() == 3, "and keeps what the pen carried: " + s);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theMachinesNeedNoNether(GameTestHelper h) {
        Set<net.minecraft.world.item.Item> nether = Set.of(Items.BREWING_STAND, Items.BLAZE_ROD, Items.POLISHED_BLACKSTONE, Items.QUARTZ, Items.GLOWSTONE_DUST);
        for (String id : List.of("terpinator", "terpinator_lab", "inspiration_infuser", "inspiration_infuser_lab")) {
            var recipe = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("fourtwenty", id)).orElseThrow().value();
            for (var ingredient : recipe.getIngredients())
                for (ItemStack option : ingredient.getItems())
                    h.assertFalse(nether.contains(option.getItem()), id + " still takes " + BuiltInRegistries.ITEM.getKey(option.getItem()));
        }
        h.succeed();
    }
}
