package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.BudBarnRegistry;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.BudBarnBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.TrimDaddyJob;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.HiringMode;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class BudBarnLogisticsGameTests {
    @GameTest(template="empty", batch="budbarn_logistics")
    public static void tailOutputTriggersDumpAndImmediatePickupRequest(GameTestHelper h) throws Exception {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "Bud Barn logistics", "Sensi Heights");
        EntityCitizen actor = null;
        try {
            var pos = h.absolutePos(new BlockPos(1, 1, 1));
            h.getLevel().setBlockAndUpdate(pos, BudBarnRegistry.HUT.get().defaultBlockState());
            var building = (BudBarnBuilding) colony.getServerBuildingManager().addNewBuilding(
                    (TileEntityColonyBuilding) h.getLevel().getBlockEntity(pos), h.getLevel());
            building.setBuildingLevel(1);
            building.onUpgradeComplete(null, 1);
            building.getModule(BudBarnRegistry.WORK).setHiringMode(HiringMode.MANUAL);
            actor = DispensaryProductionGameTests.actor(h, colony, pos.offset(1, 0, 0));
            h.assertTrue(building.getModule(BudBarnRegistry.WORK).assignCitizen(actor.getCitizenData()), "Trim Daddy hired");
            var ai = ((TrimDaddyJob) actor.getCitizenData().getJob()).getWorkerAI();
            var wantsDump = AbstractEntityAIBasic.class.getDeclaredMethod("wantInventoryDumped");
            wantsDump.setAccessible(true);
            var inventory = actor.getInventoryCitizen();

            ItemHandlerHelper.insertItemStacked(inventory, new ItemStack(ModItems.ROUGH_BUD.get()), false);
            ItemHandlerHelper.insertItemStacked(inventory, new ItemStack(ModItems.TRIMMED_BUD.get()), false);
            h.assertTrue(!(boolean) wantsDump.invoke(ai), "Inputs alone do not force a dump");
            ItemHandlerHelper.insertItemStacked(inventory, new ItemStack(ModItems.WEED_BUD.get()), false);
            h.assertTrue((boolean) wantsDump.invoke(ai), "One tail-end cured bud forces a dump before the action quota");

            ItemHandlerHelper.insertItemStacked(building.getItemHandlerCap(), new ItemStack(ModItems.WEED_BUD.get()), false);
            ItemHandlerHelper.insertItemStacked(building.getItemHandlerCap(), new ItemStack(ModItems.WEED_SEEDS.get()), false);
            building.pickUpDay = colony.getDay() + 5;
            ai.afterDump();
            h.assertTrue(pickupRequests(building) == 1, "A small output dump creates an immediate native pickup request");
            var pickup = building.getOpenRequestsByRequestableType().get(TypeConstants.PICKUP).stream()
                    .map(colony.getRequestManager()::getRequestForToken).findFirst().orElse(null);
            h.assertTrue(pickup != null && pickup.getRequest() instanceof com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Pickup p
                    && p.getPriority() == building.getPickUpPriority(), "Immediate pickup preserves the hut's configured priority");
            h.succeed();
        } finally {
            if (actor != null) actor.discard();
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    private static int pickupRequests(BudBarnBuilding building) {
        return building.getOpenRequestsByRequestableType().getOrDefault(TypeConstants.PICKUP, java.util.List.of()).size();
    }
}
