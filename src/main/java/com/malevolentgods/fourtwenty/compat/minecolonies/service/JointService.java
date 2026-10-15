package com.malevolentgods.fourtwenty.compat.minecolonies.service;

import com.malevolentgods.fourtwenty.compat.minecolonies.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Server-thread item transactions. No food handler, player item-use effects, or benefit on collection. */
public final class JointService {
    public static int jointSlot(IItemHandler inventory) {
        return InventoryUtils.findFirstSlotInItemHandlerWith(inventory, ModItems.WEED_JOINT.get());
    }
    public static boolean due(EntityCitizen citizen) {
        if (!(citizen.level() instanceof ServerLevel level) || citizen.getCitizenData() == null || !citizen.isAlive()) return false;
        var data = citizen.getCitizenData();
        if (data.getColony().getWorld() != level || data.getColony().getCitizenManager().getCivilian(data.getId()) != data) return false;
        var saved = DispensarySavedData.get(level);
        return saved.observe(data.getColony().getID(), data.getUUID(), ColonyIntegration.eligible(data), DispensaryConfig.rules()).due(saved.time());
    }
    public static boolean live(DispensaryBuilding building) {
        var level = building.getColony().getWorld();
        return building.getBuildingLevel() > 0 && level != null && level.hasChunkAt(building.getPosition())
                && level.getBlockState(building.getPosition()).is(DispensaryRegistry.HUT.get())
                && building.getColony().getServerBuildingManager().getBuilding(building.getPosition()) == building;
    }
    public static boolean atCounter(EntityCitizen citizen, DispensaryBuilding building) {
        return citizen.getCitizenData() != null && citizen.getCitizenData().getColony() == building.getColony()
                && citizen.level() == building.getColony().getWorld()
                && citizen.blockPosition().distSqr(building.getPosition()) <= 25;
    }
    /** Used by both the worker and the bounded self-service fallback; revalidate every handoff. */
    public static boolean collect(EntityCitizen citizen, DispensaryBuilding building, IItemHandler source) {
        if (!live(building) || !atCounter(citizen, building) || !due(citizen)
                || !CitizenSmokingTask.available(citizen) || jointSlot(citizen.getInventoryCitizen()) >= 0 || source == null) return false;
        int slot = jointSlot(source);
        if (slot < 0 || source.extractItem(slot, 1, true).isEmpty()) return false;
        // The native helper simulates capacity before extraction, then transfers exactly the requested quantity.
        return InventoryUtils.transferXOfItemStackIntoNextFreeSlotInItemHandler(source, slot, 1, citizen.getInventoryCitizen());
    }
    /** Credit and extraction are one synchronous action. Revision prevents a repeated completion from charging twice. */
    public static boolean consume(EntityCitizen citizen, long expectedRevision) {
        if (!due(citizen) || !CitizenSmokingTask.available(citizen)) return false;
        var saved = DispensarySavedData.get((ServerLevel) citizen.level());
        var data = citizen.getCitizenData();
        var state = saved.citizen(data.getColony().getID(), data.getUUID());
        if (state.revision() != expectedRevision) return false;
        int slot = jointSlot(citizen.getInventoryCitizen());
        if (slot < 0) return false;
        ItemStack removed = citizen.getInventoryCitizen().extractItem(slot, 1, false);
        if (removed.isEmpty()) return false;
        if (saved.recordConsumption(data.getColony().getID(), data.getUUID(), expectedRevision, DispensaryConfig.rules())) {
            com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CitizenBenefits.refresh(data);
            return true;
        }
        // Defensive rollback; no intervening callback can normally invalidate the server-thread revision.
        var remainder = citizen.getInventoryCitizen().insertItem(slot, removed, false);
        if (!remainder.isEmpty()) citizen.spawnAtLocation(remainder);
        return false;
    }
    private JointService() {}
}
