package com.malevolentgods.fourtwenty.compat.minecolonies.service;

import com.malevolentgods.fourtwenty.compat.minecolonies.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Preference;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.ServiceRules;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import java.util.EnumMap;
import java.util.List;

/** Server-thread item transactions for every menu item. No food handler, player item-use effects, or benefit on collection. */
public final class JointService {
    public static int jointSlot(IItemHandler inventory) {
        return InventoryUtils.findFirstSlotInItemHandlerWith(inventory, ModItems.WEED_JOINT.get());
    }

    /** Doses are used in this order: what was meant for the shop first, the reusable pipe last. */
    private static final List<MenuItem> DOSE_ORDER = List.of(MenuItem.JOINT, MenuItem.COOKIE, MenuItem.BROWNIE, MenuItem.PIPE);

    public static Item item(MenuItem item) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(item.itemId)); }

    public static MenuItem menuItem(ItemStack stack) {
        for (var item : MenuItem.values()) if (!stack.isEmpty() && stack.is(item(item))) return item;
        return null;
    }

    /** The next slot to use a dose from (joint, edible or pipe), or -1 when the citizen carries none. */
    public static int doseSlot(IItemHandler inventory) {
        for (var item : DOSE_ORDER) {
            int slot = InventoryUtils.findFirstSlotInItemHandlerWith(inventory, item(item));
            if (slot >= 0) return slot;
        }
        return -1;
    }

    /** Housing tier for service; no home means no service. */
    public static int homeLevel(EntityCitizen citizen) {
        var home = citizen.getCitizenData() == null ? null : citizen.getCitizenData().getHomeBuilding();
        return home == null ? 0 : home.getBuildingLevel();
    }

    /** Until affinity is rolled for a citizen, everyone takes whatever is in stock, best first. */
    public static Preference preference(EntityCitizen citizen) { return Preference.BOTH; }

    /**
     * Serve one visit by the customer's home level (see {@link ServiceRules}): something to consume
     * here, a supply to carry away, or both. Revalidated at every handoff, for the Budtender and for
     * self-service. Complains, like at a poor restaurant, when the take-away had to be skipped.
     */
    public static boolean serve(EntityCitizen citizen, DispensaryBuilding building, List<IItemHandler> sources) {
        if (!live(building) || !inShop(citizen, building) || !due(citizen) || !CitizenSmokingTask.available(citizen)
                || doseSlot(citizen.getInventoryCitizen()) >= 0) return false;
        var stock = new EnumMap<MenuItem, Integer>(MenuItem.class);
        for (var item : MenuItem.menu(building.getBuildingLevel())) stock.put(item, count(sources, item(item)));
        var plan = ServiceRules.plan(homeLevel(citizen), preference(citizen), stock);
        if (!plan.any()) return false;
        int moved = 0;
        if (plan.onSpot() != null) moved += move(sources, item(plan.onSpot()), 1, citizen.getInventoryCitizen());
        if (plan.takeAway() != null) moved += move(sources, item(plan.takeAway()), plan.takeAwayCount(), citizen.getInventoryCitizen());
        if (moved > 0 && plan.complain()) {
            citizen.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatableWithFallback(
                    "fourtwenty.dispensary.poor", "The dispensary had nothing for me to take home, so I only had one at the shop."), ChatPriority.CHITCHAT));
        }
        return moved > 0;
    }

    public static int count(List<IItemHandler> sources, Item item) {
        int total = 0;
        for (var source : sources) total += InventoryUtils.getItemCountInItemHandler(source, stack -> stack.is(item));
        return total;
    }

    /** Move up to {@code amount} of {@code item}, never losing an item the destination refuses. */
    private static int move(List<IItemHandler> sources, Item item, int amount, IItemHandler into) {
        int moved = 0;
        for (var source : sources) {
            for (int slot = 0; slot < source.getSlots() && moved < amount; slot++) {
                if (!source.getStackInSlot(slot).is(item)) continue;
                var taken = source.extractItem(slot, amount - moved, false);
                if (taken.isEmpty()) continue;
                var rest = ItemHandlerHelper.insertItemStacked(into, taken, false);
                moved += taken.getCount() - rest.getCount();
                if (!rest.isEmpty()) {
                    var back = source.insertItem(slot, rest, false);
                    if (!back.isEmpty()) ItemHandlerHelper.insertItemStacked(source, back, false);
                    return moved;
                }
            }
        }
        return moved;
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
    /** Anywhere inside the hut (as the restaurant serves), or within 5 blocks of the hut block for odd footprints. */
    public static boolean inShop(EntityCitizen citizen, DispensaryBuilding building) {
        return citizen.getCitizenData() != null && citizen.getCitizenData().getColony() == building.getColony()
                && citizen.level() == building.getColony().getWorld()
                && (building.isInBuilding(citizen.blockPosition()) || citizen.blockPosition().distSqr(building.getPosition()) <= 25);
    }
    /** Used by both the worker and the bounded self-service fallback; revalidate every handoff. */
    public static boolean collect(EntityCitizen citizen, DispensaryBuilding building, IItemHandler source) {
        if (!live(building) || !inShop(citizen, building) || !due(citizen)
                || !CitizenSmokingTask.available(citizen) || doseSlot(citizen.getInventoryCitizen()) >= 0 || source == null) return false;
        int slot = jointSlot(source);
        if (slot < 0 || source.extractItem(slot, 1, true).isEmpty()) return false;
        // The native helper simulates capacity before extraction, then transfers exactly the requested quantity.
        return InventoryUtils.transferXOfItemStackIntoNextFreeSlotInItemHandler(source, slot, 1, citizen.getInventoryCitizen());
    }
    /**
     * Credit and use are one synchronous action. Revision prevents a repeated completion from charging
     * twice. A joint or edible is used up; a pipe loses one use and breaks after its last. Edibles
     * never touch saturation or food history.
     */
    public static boolean consume(EntityCitizen citizen, long expectedRevision) {
        if (!due(citizen) || !CitizenSmokingTask.available(citizen)) return false;
        var saved = DispensarySavedData.get((ServerLevel) citizen.level());
        var data = citizen.getCitizenData();
        var state = saved.citizen(data.getColony().getID(), data.getUUID());
        if (state.revision() != expectedRevision) return false;
        int slot = doseSlot(citizen.getInventoryCitizen());
        if (slot < 0) return false;
        ItemStack removed = citizen.getInventoryCitizen().extractItem(slot, 1, false);
        if (removed.isEmpty()) return false;
        if (saved.recordConsumption(data.getColony().getID(), data.getUUID(), expectedRevision, DispensaryConfig.rules())) {
            if (removed.isDamageableItem()) {
                removed.setDamageValue(removed.getDamageValue() + 1);
                if (removed.getDamageValue() < removed.getMaxDamage()) {
                    var kept = citizen.getInventoryCitizen().insertItem(slot, removed, false);
                    if (!kept.isEmpty()) citizen.spawnAtLocation(kept);
                } else {
                    citizen.playSound(SoundEvents.ITEM_BREAK, 0.8f, 1f);
                }
            }
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
