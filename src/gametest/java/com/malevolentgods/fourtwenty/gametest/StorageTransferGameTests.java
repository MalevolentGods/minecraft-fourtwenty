package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.StorageTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Storage a Builder breaks goes to her hut (spec 013 T038a): everything moves, overflow drops at the hut, nothing at the block. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class StorageTransferGameTests {
    @GameTest(template = "empty")
    public static void aBrokenChestsContentsGoToTheHut(GameTestHelper h) {
        var level = h.getLevel();
        var chestPos = h.absolutePos(new BlockPos(1, 1, 1));
        var hutPos = h.absolutePos(new BlockPos(3, 1, 3));
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = level.getCapability(Capabilities.ItemHandler.BLOCK, chestPos, null);
        h.assertTrue(chest != null, "a chest to empty");
        chest.insertItem(0, new ItemStack(Items.OAK_LOG, 64), false);
        chest.insertItem(1, new ItemStack(Items.COBBLESTONE, 64), false);
        chest.insertItem(2, new ItemStack(Items.DIAMOND, 5), false);

        var hut = new ItemStackHandler(1);   // room for one stack: the rest has to drop at the hut
        int moved = StorageTransfer.move(chest, hut, level, hutPos);
        h.assertTrue(moved == 133, "every item moved out: " + moved);
        for (int i = 0; i < chest.getSlots(); i++) h.assertTrue(chest.getStackInSlot(i).isEmpty(), "the chest is empty");
        h.assertTrue(hut.getStackInSlot(0).is(Items.OAK_LOG) && hut.getStackInSlot(0).getCount() == 64, "the hut took what fit");
        var atHut = level.getEntities(EntityType.ITEM, new AABB(hutPos).inflate(1.5), e -> true);
        var atChest = level.getEntities(EntityType.ITEM, new AABB(chestPos).inflate(0.9), e -> true);
        int dropped = atHut.stream().mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(dropped == 69, "the rest dropped at the hut: " + dropped);
        h.assertTrue(atChest.isEmpty(), "nothing spilled where the chest stood");
        h.succeed();
    }
}
