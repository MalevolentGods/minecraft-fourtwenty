package com.malevolentgods.fourtwenty.colonytest;

import com.ldtteam.multipiston.TileEntityMultiPiston;
import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.util.FoodUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class ColonyGameTests {
    @GameTest(template = "empty")
    public static void colonyFoodPredicateExcludesOnlyEdibles(GameTestHelper h) {
        h.assertTrue(!FoodUtils.EDIBLE.test(new ItemStack(ModItems.WEED_COOKIE.get())), "Citizens reject cookies");
        h.assertTrue(!FoodUtils.EDIBLE.test(new ItemStack(ModItems.WEED_BROWNIE.get())), "Citizens reject brownies");
        h.assertTrue(FoodUtils.EDIBLE.test(new ItemStack(Items.BREAD)), "Existing citizen foods remain enabled");
        var storage = new net.minecraft.world.SimpleContainer(3);
        storage.addItem(new ItemStack(ModItems.WEED_COOKIE.get(), 16));
        storage.addItem(new ItemStack(ModItems.WEED_BROWNIE.get(), 16));
        h.assertTrue(storage.countItem(ModItems.WEED_COOKIE.get()) == 16 && storage.countItem(ModItems.WEED_BROWNIE.get()) == 16,
            "Food exclusion does not disable normal inventory storage");
        h.succeed();
    }

    /** Invoke the real piston's movement transaction in all directions, without redstone timing noise. */
    @GameTest(template = "empty")
    public static void multipistonPreservesContentsAndPaidBurnInEveryDirection(GameTestHelper h) {
        var center = new BlockPos(3, 3, 3);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absolutePos(center).getX(), h.absolutePos(center).getY(), h.absolutePos(center).getZ());
        for (Direction direction : Direction.values()) {
            for (int mode = 0; mode < 3; mode++) {
                var source = center.relative(direction.getOpposite());
                var destination = center.relative(direction);
                h.setBlock(source, ModBlocks.WEED_BONG.get());
                h.setBlock(destination, Blocks.AIR);
                h.setBlock(center, com.ldtteam.multipiston.ModBlocks.multipiston.get());
                var bong = (WeedBongBlockEntity) h.getBlockEntity(source);
                if (mode > 0) {
                    bong.setItem(0, new ItemStack(ModItems.WEED_BUD.get(), 4));
                    bong.setItem(1, new ItemStack(Items.WATER_BUCKET));
                    bong.setItem(2, new ItemStack(Items.COAL, 8));
                }
                if (mode == 2) for (int i = 0; i < 57; i++) WeedBongBlockEntity.tick(h.getLevel(), bong.getBlockPos(), bong.getBlockState(), bong);
                var expected = bong.saveWithoutMetadata(h.getLevel().registryAccess());
                var oldHandler = bong.itemHandler();
                var oldMenu = bong.createMenu(1, player.getInventory(), player);
                var piston = (TileEntityMultiPiston) h.getBlockEntity(center);
                piston.setRange(1);
                piston.setInput(direction);
                piston.setOutput(direction.getOpposite());
                piston.tick();
                piston.handleRedstone(true);
                piston.handleTick();
                h.assertTrue(h.getBlockState(source).isAir() && h.getLevel().getBlockEntity(h.absolutePos(source)) == null,
                    "Source removed after movement " + direction + "/" + mode);
                var moved = (WeedBongBlockEntity) h.getBlockEntity(destination);
                h.assertTrue(expected.equals(moved.saveWithoutMetadata(h.getLevel().registryAccess())), "Inventory and paid burn survive movement");
                h.assertTrue(!oldMenu.stillValid(player) && oldMenu.quickMoveStack(player, 0).isEmpty(), "Source menu invalidated");
                h.assertTrue(oldHandler.extractItem(0, 64, false).isEmpty(), "Old capability cannot extract transferred inventory");
                h.assertTrue(oldHandler.insertItem(0, new ItemStack(ModItems.WEED_BUD.get()), false).getCount() == 1, "Old capability rejects insertion");
                h.assertItemEntityCountIs(ModItems.WEED_BUD.get(), center, 4, 0);
                h.assertItemEntityCountIs(ModItems.WEED_BONG.get(), center, 4, 0);
                // Reverse the actual movement and then block a subsequent destination.
                piston.handleRedstone(false);
                piston.handleTick();
                var returned = (WeedBongBlockEntity) h.getBlockEntity(source);
                h.assertTrue(expected.equals(returned.saveWithoutMetadata(h.getLevel().registryAccess())), "Reverse move conserves state");
                h.setBlock(destination, Blocks.BEDROCK);
                piston.handleRedstone(true);
                piston.handleTick();
                h.assertTrue(h.getBlockEntity(source) == returned, "Blocked destination retains original entity");
                h.assertTrue(!PistonBaseBlock.isPushable(returned.getBlockState(), h.getLevel(), returned.getBlockPos(), direction, false, direction),
                    "Vanilla pistons still cannot move a bong entity");
                // Remove entities first: fixture cleanup must not add drops to the next iteration.
                h.getLevel().removeBlockEntity(returned.getBlockPos());
                h.setBlock(source, Blocks.AIR);
                h.setBlock(destination, Blocks.AIR);
                h.setBlock(center, Blocks.AIR);
            }
        }
        h.succeed();
    }
}
