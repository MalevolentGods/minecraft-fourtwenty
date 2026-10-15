package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.JointService;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;

/** Read-only inventory and service diagnostics; never creates requests or advances demand. */
public final class DispensaryStatusModule extends AbstractBuildingModule {
    private DispensaryStatus last;
    /** Called by the simulation clock, independently of MineColonies' staggered slow-colony ticks. */
    public void refresh() {
        if (!building.getColony().getWorld().hasChunkAt(building.getPosition())) return;
        var next = snapshot();
        if (!next.equals(last)) { last = next; markDirty(); }
    }
    @Override public void serializeToView(RegistryFriendlyByteBuf buffer) { snapshot().write(buffer); }

    public DispensaryStatus snapshot() {
        var hut = (DispensaryBuilding) building;
        // Native hut capability already combines its inventory and registered racks. Do not add racks again.
        int[] totals = count(hut.getItemHandlerCap());
        int available = totals[2];
        var assigned = hut.getAllAssignedCitizen().stream().sorted(Comparator.comparingInt(c -> c.getId())).toList();
        boolean atCounter = false;
        for (var citizen : assigned) {
            var held = count(citizen.getInventory());
            for (int n = 0; n < totals.length; n++) totals[n] += held[n];
            if (citizen.getEntity().orElse(null) instanceof EntityCitizen worker && JointService.atCounter(worker, hut)) {
                atCounter = true; available += held[2];
            }
        }
        var conditions = new ArrayList<String>();
        if (hut.getBuildingLevel() < 1) conditions.add("unfinished");
        if (assigned.isEmpty()) conditions.add("unstaffed");
        else if (!atCounter) conditions.add("away");
        if (totals[0] < 1) conditions.add("buds");
        if (totals[1] < 2) conditions.add("paper");
        if (available == 0) conditions.add("empty");
        boolean request = hut.getOpenRequestsByRequestableType().values().stream().flatMap(java.util.Collection::stream).distinct()
                .map(hut.getColony().getRequestManager()::getRequestForToken)
                .anyMatch(r -> r != null && r.getRequest() instanceof Stack stack && stack.matches(new ItemStack(ModItems.WEED_JOINT.get())));
        if (request) conditions.add("request");
        long now = DispensarySavedData.get((ServerLevel) hut.getColony().getWorld()).time();
        return new DispensaryStatus(hut.getBuildingLevel(), assigned.isEmpty() ? "" : assigned.getFirst().getName(),
                totals[0], totals[1], totals[2], available, hut.stockTarget(), hut.waitingCustomers(now), conditions);
    }
    private static int[] count(IItemHandler inventory) {
        int[] counts = new int[3];
        if (inventory != null) for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.is(ModItems.WEED_BUD.get())) counts[0] += stack.getCount();
            else if (stack.is(Items.PAPER)) counts[1] += stack.getCount();
            else if (stack.is(ModItems.WEED_JOINT.get())) counts[2] += stack.getCount();
        }
        return counts;
    }
}
