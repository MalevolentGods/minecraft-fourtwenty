package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeResearch;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.ShackBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.ShackModule;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.TieDyeNetwork;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.TieDyeOrders;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.TieDyeRegistry;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.TieDyeSavedData;
import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.tiedye.ModTieDye;
import com.malevolentgods.fourtwenty.tiedye.TieDye;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Tie Dye Shack in a real colony (specs/018-tie-dye-shack T014): who can order what, a batch fetched, dyed and delivered to the
 * warehouse in its look, and the orders surviving a save.
 */
@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class TieDyeShackGameTests {
    private static ShackBuilding shack(GameTestHelper h, IColony colony, BlockPos pos, int level) {
        h.getLevel().setBlockAndUpdate(pos, TieDyeRegistry.HUT.get().defaultBlockState());
        var building = (ShackBuilding) colony.getServerBuildingManager().addNewBuilding(
                (TileEntityColonyBuilding) h.getLevel().getBlockEntity(pos), h.getLevel());
        building.setBuildingLevel(level);
        building.onUpgradeComplete(null, level);
        return building;
    }

    private static TieDyeNetwork.OrderAction add(IColony colony, BlockPos pos, int pattern, String colourway, TieDyeRules.Kind kind, int count) {
        return new TieDyeNetwork.OrderAction(colony.getID(), pos, TieDyeNetwork.Action.ADD, pattern, colourway, kind, "", count, 0);
    }

    @GameTest(template = "empty", batch = "tiedye")
    public static void ordersCheckWhoAndWhat(GameTestHelper h) {
        var owner = FakePlayerFactory.getMinecraft(h.getLevel());
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), owner, "Shack orders", "Sensi Heights");
        try {
            var pos = h.absolutePos(new BlockPos(1, 1, 1));
            var building = shack(h, colony, pos, 1);
            var stranger = FakePlayerFactory.get(h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("shack-stranger".getBytes()), "[ShackStranger]"));
            var hemp = TieDyeRules.Kind.HEMP;
            h.assertTrue("tiedye.fourtwenty.refused.permission".equals(TieDyeNetwork.act(stranger, add(colony, pos, 0, "ashtray", hemp, 8))), "strangers can't order");
            h.assertTrue("tiedye.fourtwenty.refused.pattern".equals(TieDyeNetwork.act(owner, add(colony, pos, 0, "ashtray", hemp, 8))), "Crumple needs its study");
            CollegeResearch.finish(colony, "fourtwenty:tex_crumple");
            h.assertTrue("tiedye.fourtwenty.refused.colourway".equals(TieDyeNetwork.act(owner, add(colony, pos, 0, "ashtray", hemp, 8))), "Ashtray needs the neutrals");
            CollegeResearch.finish(colony, "fourtwenty:chem_neutrals");
            h.assertTrue("tiedye.fourtwenty.refused.colourway".equals(TieDyeNetwork.act(owner, add(colony, pos, 0, "campfire", hemp, 8))), "Campfire needs the warms");
            h.assertTrue("tiedye.fourtwenty.refused.pattern".equals(TieDyeNetwork.act(owner, add(colony, pos, 3, "ashtray", hemp, 8))), "Spiral needs its study");
            h.assertTrue(TieDyeNetwork.act(owner, add(colony, pos, 0, "ashtray", hemp, 8)) == null, "the owner orders Crumple hemp in Ashtray");
            h.assertTrue(TieDyeNetwork.act(owner, add(colony, pos, 0, "ashtray", hemp, 999)) != null, "not 999 at once");
            var view = building.getModule(TieDyeRegistry.ORDERS).snapshot();
            h.assertTrue(view.patterns().get(0) && !view.patterns().get(3), "Crumple open, Spiral not: " + view.patterns());
            h.assertTrue(view.colourways().stream().anyMatch(c -> c.id().equals("ashtray") && c.orderable())
                    && view.colourways().stream().anyMatch(c -> c.id().equals("campfire") && !c.orderable()), "Ashtray can be made, Campfire can't");
            h.assertTrue(view.queue().size() == 1 && view.queue().getFirst().count() == 8, "one order queued: " + view.queue());
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            view.write(buffer);
            h.assertTrue(ShackModule.View.read(buffer).equals(view), "the view survives the trip to the client");
            int id = view.queue().getFirst().id();
            h.assertTrue(TieDyeNetwork.act(owner, new TieDyeNetwork.OrderAction(colony.getID(), pos, TieDyeNetwork.Action.CANCEL, 0, "", hemp, "", 0, id)) == null,
                    "and cancels it");
            h.assertTrue(TieDyeOrders.record(colony).orders().isEmpty(), "gone");
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    @GameTest(template = "empty", batch = "tiedye")
    public static void aBatchIsDyedAndDelivered(GameTestHelper h) {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "Shack batch", "Sensi Heights");
        try {
            var building = shack(h, colony, h.absolutePos(new BlockPos(1, 1, 1)), 1);
            var housePos = h.absolutePos(new BlockPos(5, 1, 1));
            h.getLevel().setBlockAndUpdate(housePos, com.minecolonies.api.blocks.ModBlocks.blockHutWareHouse.defaultBlockState());
            var warehouse = colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding) h.getLevel().getBlockEntity(housePos), h.getLevel());
            warehouse.setBuildingLevel(1);
            CollegeResearch.finish(colony, "fourtwenty:tex_crumple");
            CollegeResearch.finish(colony, "fourtwenty:chem_neutrals");
            h.assertTrue(TieDyeOrders.add(colony, null, 0, "ashtray", TieDyeRules.Kind.HEMP, "", 6) == null, "ordered 6");
            h.assertTrue(TieDyeOrders.startBatch(colony, 1), "a batch starts");
            var q = TieDyeOrders.record(colony);
            h.assertTrue(q.batchItems() == 4, "a level-1 batch is 4, got " + q.batchItems());
            h.assertTrue(q.missing().equals(java.util.List.of(new TieDyeRules.Supply("fourtwenty:natural_hemp", 4),
                    new TieDyeRules.Supply("fourtwenty:weed_bud", 3), new TieDyeRules.Supply("minecraft:glass_bottle", 1))), "its supplies: " + q.missing());
            // save and reload mid-batch
            var provider = h.getLevel().registryAccess();
            var saved = TieDyeSavedData.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(), provider);
            var back = TieDyeSavedData.read(saved, provider).orders(colony.getID());
            h.assertTrue(back.batchStarted() && back.batchItems() == 4 && back.head().count() == 6, "the order and its batch survive a save");
            for (int line = 0; line < q.batch().size(); line++) q.deliver(line, q.batch().get(line).count());
            int made = TieDyeOrders.finishBatch(colony, building);
            h.assertTrue(made == 4 && q.head().made() == 4, "4 made, 2 to go");
            int found = 0;
            var handler = warehouse.getItemHandlerCap();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                var stack = handler.getStackInSlot(slot);
                if (stack.is(ModTieDye.HEMP_ITEM.get()) && new TieDye(0, "ashtray").equals(stack.get(ModDataComponents.TIE_DYE.get()))) found += stack.getCount();
            }
            h.assertTrue(found == 4, "4 Crumple hemp in Ashtray in the warehouse, got " + found);
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }
}
