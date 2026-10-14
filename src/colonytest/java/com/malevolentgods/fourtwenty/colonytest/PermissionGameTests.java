package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.gametest.EffectGameTests;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.util.ChunkDataHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_permissiontests")
@PrefixGameTestTemplate(false)
public final class PermissionGameTests {
    @GameTest(template = "empty", batch = "colony_permissions")
    public static void visitorCannotHarvestBreakOrOpenBong(GameTestHelper h) {
        var level = h.getLevel();
        var center = h.absolutePos(new BlockPos(3, 2, 3));
        var colony = IColonyManager.getInstance().createColony(level, center, FakePlayerFactory.getMinecraft(level), "Permission fixture", "Colonial");
        var visitor = EffectGameTests.player(h);
        try {
            ChunkDataHelper.tryClaim(level, center, true, (Colony) colony, true);
            h.assertTrue(IColonyManager.getInstance().getColonyByPosFromWorld(level, center) == colony, "Fixture chunk is claimed");
            var permissions = colony.getPermissions();
            for (var action : new Action[]{Action.RIGHTCLICK_BLOCK, Action.BREAK_BLOCKS, Action.OPEN_CONTAINER})
                permissions.setPermission(permissions.getRankNeutral(), action, false);
            visitor.setPos(center.getX(), center.getY(), center.getZ());
            level.setBlockAndUpdate(center.below(), Blocks.FARMLAND.defaultBlockState());
            var mature = ModBlocks.WEED_CROP.get().defaultBlockState().setValue(CropBlock.AGE, 7);
            level.setBlockAndUpdate(center, mature);
            var hit = new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false);
            visitor.gameMode.useItemOn(visitor, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(level.getBlockState(center).equals(mature), "Visitor harvest denied before mutation");
            visitor.gameMode.destroyBlock(center);
            h.assertTrue(level.getBlockState(center).equals(mature), "Visitor breaking denied");
            level.setBlockAndUpdate(center, ModBlocks.WEED_BONG.get().defaultBlockState());
            visitor.gameMode.useItemOn(visitor, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(visitor.containerMenu == visitor.inventoryMenu, "Visitor bong menu denied");
            permissions.addPlayer(visitor.getGameProfile(), permissions.getRankOfficer());
            visitor.gameMode.useItemOn(visitor, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(visitor.containerMenu != visitor.inventoryMenu, "Authorized resident can open bong");
            visitor.closeContainer();
            level.setBlockAndUpdate(center, mature);
            visitor.gameMode.useItemOn(visitor, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(level.getBlockState(center).getValue(CropBlock.AGE) == 0, "Authorized resident can harvest");
            permissions.removePlayer(visitor.getUUID());
            // GameTestServer's default operator level is zero; use an actual level-four operator.
            level.getServer().getPlayerList().getOps().add(new net.minecraft.server.players.ServerOpListEntry(visitor.getGameProfile(), 4, false));
            level.setBlockAndUpdate(center, mature);
            boolean operatorMayHarvest = permissions.hasPermission(visitor, Action.RIGHTCLICK_BLOCK);
            visitor.gameMode.useItemOn(visitor, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            h.assertTrue((level.getBlockState(center).getValue(CropBlock.AGE) == 0) == operatorMayHarvest,
                "Operator follows the colony's native permission decision");
            level.getServer().getPlayerList().deop(visitor.getGameProfile());

            // A hoe clicked just outside one claimed chunk must not harvest a protected neighbor.
            var edge = new BlockPos((center.getX() >> 4) * 16 + 15, center.getY(), center.getZ());
            var outside = edge.east();
            ChunkDataHelper.tryClaim(level, outside, false, (Colony) colony, true);
            h.assertTrue(IColonyManager.getInstance().getColonyByPosFromWorld(level, edge) == colony &&
                IColonyManager.getInstance().getColonyByPosFromWorld(level, outside) == null, "Fixture crosses a claimed chunk boundary");
            for (var crop : new BlockPos[]{edge, outside}) {
                level.setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
                level.setBlockAndUpdate(crop, mature);
            }
            visitor.setPos(outside.getX(), outside.getY(), outside.getZ());
            visitor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_HOE));
            visitor.gameMode.useItemOn(visitor, level, visitor.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(outside), Direction.UP, outside, false));
            h.assertTrue(level.getBlockState(edge).equals(mature), "Area harvesting cannot cross colony permissions");
            h.succeed();
        } finally {
            level.getServer().getPlayerList().deop(visitor.getGameProfile());
            EffectGameTests.remove(visitor);
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, level);
        }
    }
}
