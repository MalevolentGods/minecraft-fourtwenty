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

    /** Loading native stock/staff must never restore a queue or seat from the previous live instance. */
    @Override public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.nbt.CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        customers.clear();
        seats.clear();
    }

    public boolean hasServiceStock() {
        // Native getHandlers() is empty without staff and includes remote worker inventories with staff.
        var storage = getItemHandlerCap();
        if (storage != null && hasMenuItem(storage)) return true;
        for (var data : getAllAssignedCitizen()) {
            var entity = data.getEntity().orElse(null);
            if (entity instanceof EntityCitizen worker && JointService.inShop(worker, this)
                    && hasMenuItem(data.getInventory())) return true;
        }
        return false;
    }
    private boolean hasMenuItem(net.neoforged.neoforge.items.IItemHandler inventory) {
        for (var item : com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem.menu(getBuildingLevel())) {
            if (com.minecolonies.api.util.InventoryUtils.findFirstSlotInItemHandlerWith(inventory, JointService.item(item)) >= 0) return true;
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
    /** Active queue length; stale reservations are never shown as waiting customers. */
    public int waitingCustomers(long now) { prune(now); return customers.size(); }
    /** Anyone queuing at the counter or holding a seat (smoking) right now. */
    public boolean hasCustomers(long now) { prune(now); return !customers.isEmpty() || !seats.isEmpty(); }
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
    /** The waiting customer with this identity, if still queued inside the shop. */
    public EntityCitizen waitingCustomer(UUID id, long now) {
        prune(now);
        if (!customers.containsKey(id)) return null;
        var entity = ((net.minecraft.server.level.ServerLevel) getColony().getWorld()).getEntity(id);
        if (!(entity instanceof EntityCitizen customer)) { customers.remove(id); return null; }
        var task = ((DispensaryCustomer) customer).fourtwenty$smokingTask();
        if (task == null || !task.waitingAt(this) || !JointService.inShop(customer, this)) { customers.remove(id); return null; }
        return customer;
    }
    /** First still-valid customer in queue order, skipping {@code skip} (one the Budtender could not reach). */
    public EntityCitizen nextCustomer(long now, UUID skip) {
        for (var id : java.util.List.copyOf(customers.keySet())) {
            if (id.equals(skip)) continue;
            var customer = waitingCustomer(id, now);
            if (customer != null) return customer;
        }
        return null;
    }
    /** Serve a customer the Budtender is standing next to, from his inventory first, then the hut's storage. */
    public boolean serve(EntityCitizen worker, EntityCitizen customer) {
        if (!JointService.live(this) || worker.getCitizenData().getWorkBuilding() != this || !CitizenSmokingTask.available(worker)) return false;
        var sources = new java.util.ArrayList<net.neoforged.neoforge.items.IItemHandler>();
        sources.add(worker.getInventoryCitizen());
        if (getItemHandlerCap() != null) sources.add(getItemHandlerCap());
        boolean served = JointService.serve(customer, this, sources);
        if (served) customers.remove(customer.getUUID());
        return served;
    }

    /** Retain each menu item's service target and a few batches of ingredients; workers still dump their output normally. */
    @Override public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount() {
        var required = super.getRequiredItemsAndAmount();
        for (var item : com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem.values()) {
            retain(required, JointService.item(item), ProductionRules.target(item, getBuildingLevel()));
        }
        for (var input : ProductionRules.ingredientsKept(getBuildingLevel()).entrySet()) {
            retain(required, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(input.getKey())), input.getValue());
        }
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
