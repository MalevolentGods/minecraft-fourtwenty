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
 * A budtender keeping the dispensary's bongs going while customers are around: walks to a bong
 * that would not relight by itself and tops it up from its own inventory and the hut's storage
 * (the same direct transfer the joint service uses). One per budtender AI.
 */
public final class BongTending {
    private static final int SCAN_EVERY = 200;
    private static final int GIVE_UP_AFTER = 600;
    private List<BlockPos> bongs = List.of();
    private long scanAt;
    private BlockPos target;
    private long giveUpAt;

    /**
     * Advance tending. Returns true while the budtender is busy with a bong this tick, so the
     * caller skips the native work AI (otherwise both would steer the worker).
     */
    public boolean tick(EntityCitizen worker, DispensaryBuilding building, long now) {
        if (target == null) {
            if (now % 20 != 0 || building.waitingCustomers(now) > 0 || !building.hasCustomers(now)) return false;
            target = pick(worker, building, now);
            if (target == null) return false;
            giveUpAt = now + GIVE_UP_AFTER;
        }
        if (now > giveUpAt || !(worker.level().getBlockEntity(target) instanceof WeedBongBlockEntity bong)) {
            target = null;
            return false;
        }
        if (!EntityNavigationUtils.walkToPos(worker, target, 2, true)) return true;
        refill(worker, building, bong);
        target = null;
        return true;
    }

    private BlockPos pick(EntityCitizen worker, DispensaryBuilding building, long now) {
        if (now >= scanAt) { bongs = scan(worker, building); scanAt = now + SCAN_EVERY; }
        var sources = sources(worker, building);
        for (var pos : bongs) {
            if (!(worker.level().getBlockEntity(pos) instanceof WeedBongBlockEntity bong)) continue;
            var need = need(bong);
            if (BongTendingRules.canFix(need, count(sources, s -> s.is(ModItems.WEED_BUD.get())),
                    count(sources, s -> s.is(Items.WATER_BUCKET)) > 0, BongTendingRules.bestFuel(fuels(sources)))) return pos;
        }
        return null;
    }

    private static List<BlockPos> scan(EntityCitizen worker, DispensaryBuilding building) {
        var corners = building.getCorners();
        var found = new ArrayList<BlockPos>();
        for (var pos : BlockPos.betweenClosed(corners.getA(), corners.getB())) {
            if (worker.level().getBlockEntity(pos) instanceof WeedBongBlockEntity) found.add(pos.immutable());
        }
        return List.copyOf(found);
    }

    private static BongTendingRules.Need need(WeedBongBlockEntity bong) {
        var buds = bong.getItem(WeedBongBlockEntity.WEED_SLOT);
        return BongTendingRules.need(buds.is(ModItems.WEED_BUD.get()) ? buds.getCount() : 0,
                bong.getItem(WeedBongBlockEntity.WATER_SLOT).is(Items.WATER_BUCKET), id(bong.getItem(WeedBongBlockEntity.FUEL_SLOT)));
    }

    private void refill(EntityCitizen worker, DispensaryBuilding building, WeedBongBlockEntity bong) {
        var sources = sources(worker, building);
        var into = bong.itemHandler();
        var need = need(bong);
        if (need.clearBucket()) {
            var bucket = into.extractItem(WeedBongBlockEntity.FUEL_SLOT, 1, true);
            if (!bucket.isEmpty() && store(sources, bucket)) into.extractItem(WeedBongBlockEntity.FUEL_SLOT, 1, false);
        }
        int buds = into.getStackInSlot(WeedBongBlockEntity.WEED_SLOT).getCount();
        move(sources, s -> s.is(ModItems.WEED_BUD.get()), BongTendingRules.MAX_BUDS - buds, into, WeedBongBlockEntity.WEED_SLOT);
        if (need.water()) move(sources, s -> s.is(Items.WATER_BUCKET), 1, into, WeedBongBlockEntity.WATER_SLOT);
        if (into.getStackInSlot(WeedBongBlockEntity.FUEL_SLOT).isEmpty()) {
            BongTendingRules.bestFuel(fuels(sources)).ifPresent(fuelId -> {
                Item fuel = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(fuelId));
                move(sources, s -> s.is(fuel), BongTendingRules.fuelAmount(fuelId), into, WeedBongBlockEntity.FUEL_SLOT);
            });
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
