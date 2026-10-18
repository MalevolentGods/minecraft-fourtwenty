package com.malevolentgods.fourtwenty.compat.minecolonies.service;

import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.BongTendingRules;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A budtender keeping the dispensary's bongs set up and, while customers are around, lit: walks
 * to a bong that is missing something and tops it up from its own inventory and the hut's storage
 * (the same direct transfer the joint service uses), with free water. Missing fuel and buds are
 * requested from the colony. One per budtender AI.
 */
public final class BongTending {
    private static final int SCAN_EVERY = 200;
    private static final int GIVE_UP_AFTER = 600;
    private List<BlockPos> bongs = List.of();
    private long scanAt;
    private BlockPos target;
    private boolean targetServing;
    private long giveUpAt;

    /** How the budtender asks the colony for supplies (backed by the native request system). */
    public interface Supplies {
        /** Ask for at least one of {@code stack}, up to {@code count}, unless already stocked or requested. */
        void request(ItemStack stack, int count);
        /** Ask for {@code count} of any bong fuel (item tag fourtwenty:bong_fuel). */
        void requestFuel(int count);
    }

    /**
     * Advance tending. Returns true while the budtender is busy with a bong this tick, so the
     * caller skips the native work AI (otherwise both would steer the worker).
     *
     * <p>Without customers the budtender only sets bongs up (water and fuel), so they do not burn
     * to an empty room; with customers around it adds buds and the bong lights itself. Queued
     * customers are always served first.
     */
    public boolean tick(EntityCitizen worker, DispensaryBuilding building, long now, Supplies supplies) {
        if (target == null) {
            if (now % 20 != 0 || building.waitingCustomers(now) > 0) return false;
            boolean serving = building.hasCustomers(now) || visitorsInside(worker, building);
            if (now >= scanAt) {
                bongs = scan(worker, building);
                scanAt = now + SCAN_EVERY;
                requestSupplies(worker, building, serving, supplies);
            }
            target = pick(worker, building, serving);
            if (target == null) return false;
            targetServing = serving;
            giveUpAt = now + GIVE_UP_AFTER;
        }
        if (now > giveUpAt || !(worker.level().getBlockEntity(target) instanceof WeedBongBlockEntity bong)) {
            target = null;
            return false;
        }
        if (!EntityNavigationUtils.walkToPos(worker, target, 2, true)) return true;
        refill(worker, building, bong, targetServing);
        target = null;
        return true;
    }

    private BlockPos pick(EntityCitizen worker, DispensaryBuilding building, boolean serving) {
        var sources = sources(worker, building);
        for (var pos : bongs) {
            if (!(worker.level().getBlockEntity(pos) instanceof WeedBongBlockEntity bong)) continue;
            if (BongTendingRules.canFix(need(bong, serving), count(sources, s -> s.is(ModItems.WEED_BUD.get())),
                    BongTendingRules.bestFuel(fuels(sources)))) return pos;
        }
        return null;
    }

    /**
     * Raise native requests for what the bongs are missing and nobody has stocked (water is free).
     * Buds are stocked ahead of customers (needs are checked as if serving), so the bong can be
     * lit the moment someone walks in; they are only put in the bong while serving.
     */
    private void requestSupplies(EntityCitizen worker, DispensaryBuilding building, boolean serving, Supplies supplies) {
        int needFuel = 0, needBuds = 0;
        for (var pos : bongs) {
            if (!(worker.level().getBlockEntity(pos) instanceof WeedBongBlockEntity bong)) continue;
            var need = need(bong, true);
            if (need.fuel()) needFuel++;
            if (need.buds()) needBuds++;
        }
        var sources = sources(worker, building);
        if (needFuel > 0 && BongTendingRules.bestFuel(fuels(sources)).isEmpty()) supplies.requestFuel(needFuel);
        if (needBuds > 0 && count(sources, s -> s.is(ModItems.WEED_BUD.get())) == 0) {
            supplies.request(new ItemStack(ModItems.WEED_BUD.get()), BongTendingRules.MAX_BUDS);
        }
    }

    /**
     * Any colonist other than the budtender inside the hut. Customers smoke standing wherever they
     * got their joint, so the queue and seat leases alone almost never show anyone around.
     */
    private static boolean visitorsInside(EntityCitizen worker, DispensaryBuilding building) {
        var corners = building.getCorners();
        var box = net.minecraft.world.phys.AABB.encapsulatingFullBlocks(corners.getA(), corners.getB());
        return !worker.level().getEntitiesOfClass(EntityCitizen.class, box, c -> c != worker && c.isAlive()).isEmpty();
    }

    /** Only the front bong: the one nearest the hut block (the counter). Lounge bongs are self-serve. */
    private static List<BlockPos> scan(EntityCitizen worker, DispensaryBuilding building) {
        var corners = building.getCorners();
        var counter = building.getPosition();
        BlockPos front = null;
        for (var pos : BlockPos.betweenClosed(corners.getA(), corners.getB())) {
            if (worker.level().getBlockEntity(pos) instanceof WeedBongBlockEntity
                    && (front == null || pos.distSqr(counter) < front.distSqr(counter))) front = pos.immutable();
        }
        return front == null ? List.of() : List.of(front);
    }

    private static BongTendingRules.Need need(WeedBongBlockEntity bong, boolean serving) {
        var buds = bong.getItem(WeedBongBlockEntity.WEED_SLOT);
        return BongTendingRules.need(buds.is(ModItems.WEED_BUD.get()) ? buds.getCount() : 0,
                bong.getItem(WeedBongBlockEntity.WATER_SLOT).is(Items.WATER_BUCKET), id(bong.getItem(WeedBongBlockEntity.FUEL_SLOT)), serving);
    }

    private void refill(EntityCitizen worker, DispensaryBuilding building, WeedBongBlockEntity bong, boolean serving) {
        var sources = sources(worker, building);
        var into = bong.itemHandler();
        var need = need(bong, serving);
        if (need.clearBucket()) {
            var bucket = into.extractItem(WeedBongBlockEntity.FUEL_SLOT, 1, true);
            if (!bucket.isEmpty() && store(sources, bucket)) into.extractItem(WeedBongBlockEntity.FUEL_SLOT, 1, false);
        }
        // Water is free: the budtender fills it from the tap, no bucket needed from storage.
        if (need.water()) into.insertItem(WeedBongBlockEntity.WATER_SLOT, new ItemStack(Items.WATER_BUCKET), false);
        if (into.getStackInSlot(WeedBongBlockEntity.FUEL_SLOT).isEmpty()) {
            BongTendingRules.bestFuel(fuels(sources)).ifPresent(fuelId -> {
                Item fuel = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(fuelId));
                move(sources, s -> s.is(fuel), BongTendingRules.fuelAmount(fuelId), into, WeedBongBlockEntity.FUEL_SLOT);
            });
        }
        // Buds last, and only with customers around: the bong lights as soon as it has all three.
        if (serving) {
            int buds = into.getStackInSlot(WeedBongBlockEntity.WEED_SLOT).getCount();
            move(sources, s -> s.is(ModItems.WEED_BUD.get()), BongTendingRules.MAX_BUDS - buds, into, WeedBongBlockEntity.WEED_SLOT);
        }
        worker.swing(InteractionHand.MAIN_HAND);
    }

    /** The worker's own inventory first, then the hut's storage. */
    private static List<IItemHandler> sources(EntityCitizen worker, DispensaryBuilding building) {
        var list = new ArrayList<IItemHandler>();
        list.add(worker.getInventoryCitizen());
        var storage = building.getItemHandlerCap();
        if (storage != null) list.add(storage);
        return list;
    }

    private static int count(List<IItemHandler> sources, Predicate<ItemStack> match) {
        int total = 0;
        for (var source : sources) {
            for (int slot = 0; slot < source.getSlots(); slot++) {
                var stack = source.getStackInSlot(slot);
                if (match.test(stack)) total += stack.getCount();
            }
        }
        return total;
    }

    private static java.util.Map<String, Integer> fuels(List<IItemHandler> sources) {
        var counts = new HashMap<String, Integer>();
        for (String id : BongTendingRules.FUEL_PREFERENCE) {
            Item item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id));
            counts.put(id, count(sources, s -> s.is(item)));
        }
        return counts;
    }

    /** Move up to {@code amount} matching items into {@code slot}, taking only what the bong will accept. */
    private static void move(List<IItemHandler> sources, Predicate<ItemStack> match, int amount, IItemHandler into, int slot) {
        for (var source : sources) {
            for (int i = 0; i < source.getSlots() && amount > 0; i++) {
                var stack = source.getStackInSlot(i);
                if (!match.test(stack)) continue;
                int want = Math.min(amount, stack.getCount());
                int accepted = want - into.insertItem(slot, stack.copyWithCount(want), true).getCount();
                if (accepted <= 0) return;   // the bong slot is full (or holds something else)
                var taken = source.extractItem(i, accepted, false);
                var rest = into.insertItem(slot, taken, false);
                amount -= taken.getCount() - rest.getCount();
                if (!rest.isEmpty()) ItemHandlerHelper.insertItemStacked(source, rest, false);
            }
        }
    }

    /** Put {@code stack} into the first source with room; false (and nothing moved) if none has room. */
    private static boolean store(List<IItemHandler> sources, ItemStack stack) {
        for (var source : sources) {
            if (ItemHandlerHelper.insertItemStacked(source, stack.copy(), true).isEmpty()) {
                ItemHandlerHelper.insertItemStacked(source, stack.copy(), false);
                return true;
            }
        }
        return false;
    }

    private static String id(ItemStack stack) {
        return stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** Debug/test visibility: the bong currently being walked to, if any. */
    public Optional<BlockPos> target() { return Optional.ofNullable(target); }
}
