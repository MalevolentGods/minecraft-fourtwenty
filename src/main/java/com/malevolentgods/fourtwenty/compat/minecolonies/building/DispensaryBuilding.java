package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.ProductionRules;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Map;
import java.util.function.Predicate;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.UUID;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.*;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.other.SittingEntity;

public final class DispensaryBuilding extends AbstractBuilding {
    private record Seat(UUID citizen, long expires) {}
    // Leases contain identities only, expire when an entity stops ticking, and are deliberately not saved.
    private final Map<UUID, Long> customers = new LinkedHashMap<>();
    private final Map<BlockPos, Seat> seats = new HashMap<>();
    public DispensaryBuilding(IColony colony, BlockPos pos) { super(colony, pos); }
    @Override public String getSchematicName() { return "dispensary"; }
    @Override public int getMaxBuildingLevel() { return 5; }
    public int stockTarget() { return ProductionRules.stockTarget(getBuildingLevel()); }

    public boolean hasServiceStock() {
        // Native getHandlers() is empty without staff and includes remote worker inventories with staff.
        var storage = getItemHandlerCap();
        if (storage != null && JointService.jointSlot(storage) >= 0) return true;
        for (var data : getAllAssignedCitizen()) {
            var entity = data.getEntity().orElse(null);
            if (entity instanceof EntityCitizen worker && JointService.atCounter(worker, this)
                    && JointService.jointSlot(data.getInventory()) >= 0) return true;
        }
        return false;
    }
    public void refreshCustomer(EntityCitizen citizen, long now) {
        prune(now);
        var task = ((DispensaryCustomer) citizen).fourtwenty$smokingTask();
        if (task != null && task.waitingAt(this)) customers.put(citizen.getUUID(), now + 40);
        seats.replaceAll((pos, lease) -> lease.citizen().equals(citizen.getUUID()) ? new Seat(lease.citizen(), now + 40) : lease);
    }
    public void releaseCustomer(UUID citizen) { customers.remove(citizen); }
    public void releaseSeat(UUID citizen) { seats.values().removeIf(lease -> lease.citizen().equals(citizen)); }
    private void prune(long now) {
        customers.values().removeIf(expiry -> now >= expiry);
        seats.values().removeIf(lease -> now >= lease.expires());
    }
    public boolean ownsSeat(UUID citizen, BlockPos pos, long now) {
        prune(now);
        var lease = seats.get(pos);
        return lease != null && lease.citizen().equals(citizen);
    }
    public BlockPos reserveSeat(EntityCitizen citizen, long now) {
        prune(now);
        for (String tag : getColony().getWorld().isRaining() ? new String[]{"sit_in", "sit"} : new String[]{"sit_in", "sit", "sit_out"}) {
            for (var pos : getLocationsFromTag(tag)) {
                var world = getColony().getWorld();
                if (world.hasChunkAt(pos) && !seats.containsKey(pos) && !SittingEntity.isSittingPosOccupied(pos, world)
                        && !world.getBlockState(pos).isAir() && world.getFluidState(pos).isEmpty()
                        && world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).isEmpty()) {
                    seats.put(pos, new Seat(citizen.getUUID(), now + 40)); return pos;
                }
            }
        }
        return null;
    }
    /** Serve one waiting customer at the counter without disturbing an in-flight crafting recipe. */
    public boolean serveCustomer(EntityCitizen worker, long now) {
        prune(now);
        if (!JointService.live(this) || !JointService.atCounter(worker, this)
                || worker.getCitizenData().getWorkBuilding() != this || !CitizenSmokingTask.available(worker)) return false;
        for (var id : java.util.List.copyOf(customers.keySet())) {
            var entity = ((net.minecraft.server.level.ServerLevel) getColony().getWorld()).getEntity(id);
            if (!(entity instanceof EntityCitizen customer)) { customers.remove(id); continue; }
            var task = ((DispensaryCustomer) customer).fourtwenty$smokingTask();
            if (task == null || !task.waitingAt(this) || !JointService.atCounter(customer, this)) { customers.remove(id); continue; }
            boolean served = JointService.collect(customer, this, worker.getInventoryCitizen());
            if (!served) served = JointService.collect(customer, this, getItemHandlerCap());
            if (served) { customers.remove(id); return true; }
        }
        return false;
    }

    /** Retain the service target and one production batch; workers still dump their output normally. */
    @Override public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount() {
        var required = super.getRequiredItemsAndAmount();
        retain(required, ModItems.WEED_JOINT.get(), stockTarget());
        retain(required, ModItems.WEED_BUD.get(), getBuildingLevel() > 0 ? 16 : 0);
        retain(required, Items.PAPER, getBuildingLevel() > 0 ? 32 : 0);
        return required;
    }
    private static void retain(Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> required, Item item, int minimum) {
        // Merge native request reservations and user minimum stock, avoiding competing predicate order.
        int amount = minimum;
        boolean workerInventory = false;
        var sample = new ItemStack(item);
        var iterator = required.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().test(sample)) {
                amount = Math.max(amount, entry.getValue().getA());
                workerInventory |= entry.getValue().getB();
                iterator.remove();
            }
        }
        required.put(stack -> stack.is(item), new Tuple<>(amount, workerInventory));
    }
}
