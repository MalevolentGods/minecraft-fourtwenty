package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class BongGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static WeedBongBlockEntity place(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.WEED_BONG.get());
        return (WeedBongBlockEntity) h.getBlockEntity(POS);
    }
    private static void stock(WeedBongBlockEntity b, int buds, int fuel) {
        b.setItem(0, new ItemStack(ModItems.WEED_BUD.get(), buds));
        b.setItem(1, new ItemStack(Items.WATER_BUCKET));
        b.setItem(2, new ItemStack(Items.CHARCOAL, fuel));
        b.lightBong(null);
    }
    private static void tick(GameTestHelper h, WeedBongBlockEntity b) {
        WeedBongBlockEntity.tick(h.getLevel(), b.getBlockPos(), b.getBlockState(), b);
    }

    @GameTest(template = "empty")
    public static void capabilitiesConservePartialTransfersAndSimulation(GameTestHelper h) {
        var b = place(h);
        for (Direction side : Direction.values()) {
            var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, b.getBlockPos(), side);
            h.assertTrue(handler != null, "Every side has a handler");
            var incoming = new ItemStack(ModItems.WEED_BUD.get(), 64);
            h.assertTrue(handler.insertItem(0, incoming.copyWithCount(60), true).isEmpty() && b.isEmpty(), "Simulation preserves inventory");
            b.setItem(0, new ItemStack(ModItems.WEED_BUD.get(), 60));
            h.assertTrue(handler.insertItem(0, incoming, false).getCount() == 60 && incoming.getCount() == 64 && b.getItem(0).getCount() == 64, "Partial insert returns remainder without mutating input");
            h.assertTrue(handler.extractItem(0, 64, true).getCount() == 64 && b.getItem(0).getCount() == 64, "Extraction simulation is read-only");
            h.assertTrue(handler.extractItem(0, 64, false).getCount() == 64 && b.isEmpty(), "Every bud extracted");
            for (int slot = 0; slot < 3; slot++) h.assertTrue(handler.insertItem(slot, new ItemStack(Items.DIAMOND), false).is(Items.DIAMOND), "Wrong ingredients rejected");
        }
        h.assertTrue(!b.canLight(), "Invalid insertions cannot ignite");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ignitionPaysOnceAndWaterRemovalExtinguishes(GameTestHelper h) {
        var b = place(h);
        stock(b, 4, 4);
        tick(h, b);
        h.assertTrue(b.isLit() && b.getItem(0).getCount() == 3 && b.getItem(2).getCount() == 3, "One charge paid at ignition");
        b.itemHandler().extractItem(0, 64, false);
        tick(h, b);
        h.assertTrue(b.isLit() && b.getItem(2).getCount() == 3, "Paid charge survives extraction of queued buds");
        b.itemHandler().extractItem(1, 1, false);
        h.assertTrue(!b.isLit() && b.getItem(0).isEmpty() && b.getItem(2).getCount() == 3, "Water removal extinguishes without refund");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void itWaitsToBeLitWithNothingToPower(GameTestHelper h) {
        var b = place(h);
        b.setItem(0, new ItemStack(ModItems.WEED_BUD.get(), 64));
        b.setItem(1, new ItemStack(Items.WATER_BUCKET));
        b.setItem(2, new ItemStack(Items.CHARCOAL, 4));
        tick(h, b);
        tick(h, b);
        h.assertTrue(!b.isLit() && b.getItem(0).getCount() == 64, "Nothing needs the power, so it doesn't burn");
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(b.getBlockPos().getCenter());
        var menu = b.createMenu(1, player.getInventory(), player);
        h.assertTrue(menu.clickMenuButton(player, com.malevolentgods.fourtwenty.menu.WeedBongMenu.LIGHT), "The Light button works");
        h.assertTrue(b.isLit() && b.getItem(0).getCount() == 63, "and starts a session");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void burnRunsFor1200TicksWithoutDoubleCharge(GameTestHelper h) {
        var b = place(h);
        stock(b, 1, 1);
        for (int i = 0; i < 1199; i++) tick(h, b);
        h.assertTrue(b.isLit() && b.burnTime() == 1199, "Burn remains lit before its deadline");
        tick(h, b);
        h.assertTrue(!b.isLit() && b.burnTime() == 0 && b.getItem(0).isEmpty() && b.getItem(2).isEmpty(), "Paid charge expires exactly once");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentAndLegacySavesPreservePaidCharge(GameTestHelper h) {
        var b = place(h);
        stock(b, 3, 3);
        for (int i = 0; i < 43; i++) tick(h, b);
        var saved = b.saveWithoutMetadata(h.getLevel().registryAccess());
        b.loadWithComponents(saved, h.getLevel().registryAccess());
        tick(h, b);
        h.assertTrue(b.burnTime() == 44 && b.getItem(0).getCount() == 2 && b.getItem(2).getCount() == 2, "Current save resumes without extra charge");
        saved.remove("ChargePaid");
        b.loadWithComponents(saved, h.getLevel().registryAccess());
        tick(h, b);
        h.assertTrue(b.isLit() && b.burnTime() == 44 && b.getItem(0).getCount() == 1 && b.getItem(2).getCount() == 2, "Legacy save pays only the remaining bud");
        b.loadWithComponents(b.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        tick(h, b);
        h.assertTrue(b.getItem(0).getCount() == 1 && b.getItem(2).getCount() == 2, "Migrated save never charges twice");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyInvalidItemsRemainRecoverable(GameTestHelper h) {
        var b = place(h);
        var tag = b.saveWithoutMetadata(h.getLevel().registryAccess());
        var items = net.minecraft.core.NonNullList.withSize(3, ItemStack.EMPTY);
        items.set(0, new ItemStack(Items.DIAMOND, 16));
        items.set(1, new ItemStack(Items.WATER_BUCKET));
        items.set(2, new ItemStack(Items.STICK, 32));
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items, h.getLevel().registryAccess());
        tag.putBoolean("IsLit", true);
        tag.remove("ChargePaid");
        b.loadWithComponents(tag, h.getLevel().registryAccess());
        tick(h, b);
        h.assertTrue(!b.isLit(), "Invalid legacy contents do not burn");
        h.assertTrue(b.itemHandler().extractItem(0, 1, true).getCount() == 1 && b.getItem(0).getCount() == 16, "Legacy partial recovery simulation preserves contents");
        h.assertTrue(b.itemHandler().extractItem(0, 1, false).getCount() == 1 && b.getItem(0).getCount() == 15, "Legacy contents support hopper-sized recovery");
        h.assertTrue(b.itemHandler().extractItem(0, 64, false).getCount() == 15, "Remaining invalid bud slot is recoverable");
        h.assertTrue(b.itemHandler().extractItem(2, 64, false).getCount() == 32, "Invalid fuel is recoverable");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingDropsInventoryOnceAndInvalidatesHandler(GameTestHelper h) {
        var b = place(h);
        stock(b, 4, 4);
        tick(h, b);
        var stale = b.itemHandler();
        h.getLevel().destroyBlock(b.getBlockPos(), true);
        h.assertItemEntityCountIs(ModItems.WEED_BONG.get(), POS, 3, 1);
        h.assertItemEntityCountIs(ModItems.WEED_BUD.get(), POS, 3, 3);
        h.assertItemEntityCountIs(Items.CHARCOAL, POS, 3, 3);
        h.assertItemEntityCountIs(Items.WATER_BUCKET, POS, 3, 1);
        h.assertTrue(stale.extractItem(0, 64, false).isEmpty(), "Removed handler cannot extract");
        h.assertTrue(stale.insertItem(0, new ItemStack(ModItems.WEED_BUD.get()), false).getCount() == 1, "Removed handler rejects insertion");
        h.assertTrue(h.getBlockState(POS).isAir(), "Removal does not resurrect block");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void nativeHopperFillsTheBudSlot(GameTestHelper h) {
        var b = place(h);
        h.setBlock(POS.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        var hopper = (HopperBlockEntity) h.getBlockEntity(POS.above());
        hopper.setItem(0, new ItemStack(ModItems.WEED_BUD.get(), 64));
        h.runAfterDelay(100, () -> {
            h.assertTrue(b.getItem(0).getCount() > 0, "Buds went in");
            h.assertTrue(b.getItem(0).getCount() + hopper.getItem(0).getCount() == 64, "Every bud is accounted for");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void twoMenusShareLimitsAndPreserveShiftClickRemainders(GameTestHelper h) {
        var b = place(h);
        var first = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var second = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        first.setPos(b.getBlockPos().getX(), b.getBlockPos().getY(), b.getBlockPos().getZ());
        second.setPos(first.position());
        var menu = b.createMenu(1, first.getInventory(), first);
        var viewer = b.createMenu(2, second.getInventory(), second);
        b.setItem(0, new ItemStack(ModItems.WEED_BUD.get(), 60));
        first.getInventory().setItem(9, new ItemStack(ModItems.WEED_BUD.get(), 64));
        menu.quickMoveStack(first, 3);
        h.assertTrue(first.getInventory().getItem(9).getCount() == 60 && viewer.getSlot(0).getItem().getCount() == 64,
            "Shift click inserts only capacity and both viewers share inventory");
        viewer.quickMoveStack(second, 0);
        h.assertTrue(b.getItem(0).isEmpty() && second.getInventory().countItem(ModItems.WEED_BUD.get()) == 64,
            "Other viewer extracts exactly the shared contents");
        h.succeed();
    }
}
