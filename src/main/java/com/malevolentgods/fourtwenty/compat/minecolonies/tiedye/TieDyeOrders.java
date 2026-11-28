package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeResearch;
import com.malevolentgods.fourtwenty.domain.tiedye.OrderQueue;
import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.malevolentgods.fourtwenty.tiedye.Colourways;
import com.malevolentgods.fourtwenty.tiedye.ModTieDye;
import com.malevolentgods.fourtwenty.tiedye.TieDye;
import com.minecolonies.api.colony.IColony;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Tie Dye Shack's orders, server side (contracts/orders.md): what a colony may order, adding and cancelling, what a batch uses, and
 * delivering what's dyed. Patterns come from the College's Textiles studies, colours from its Chemistry studies (017 FR-021).
 */
public final class TieDyeOrders {
    private TieDyeOrders() {}

    public static final int MAX_COUNT = 256;

    public static OrderQueue record(IColony colony) { return TieDyeSavedData.get((ServerLevel) colony.getWorld()).orders(colony.getID()); }

    /** Saves the colony's orders and refreshes every one of its Shacks' tabs (each re-sends only if it changed). */
    public static void changed(IColony colony) {
        TieDyeSavedData.get((ServerLevel) colony.getWorld()).setDirty();
        for (var b : colony.getServerBuildingManager().getBuildings().values()) {
            if (b instanceof ShackBuilding shack) shack.getModule(TieDyeRegistry.ORDERS).refresh();
        }
    }

    public static boolean patternUnlocked(IColony colony, int pattern) {
        return TieDyeRules.validPattern(pattern) && CollegeResearch.record(colony).isDone(TieDyeRules.studyFor(pattern));
    }

    public static Set<String> knownColours(IColony colony) { return CollegeResearch.knownColours(colony); }

    public static boolean colourwayOrderable(IColony colony, String colourway) {
        return Colourways.known(colourway) && Colourways.get(colourway).orderable(knownColours(colony));
    }

    /** The kinds the Shack makes so far (the rest arrive in later milestones of spec 018). */
    public static boolean available(TieDyeRules.Kind kind) { return output(kind, "oak") != null; }

    /** The plain item a kind is dyed from. */
    public static String plain(TieDyeRules.Kind kind, String wood) {
        return switch (kind) {
            case HEMP -> "fourtwenty:natural_hemp";
            case RUG -> "fourtwenty:rug_natural";
            case COUCH -> "fourtwenty:couch_natural";
            case BED -> "fourtwenty:" + (wood == null || wood.isEmpty() ? "oak" : wood) + "_bed";
            case SHIRT -> "minecraft:white_wool";
        };
    }

    /** The tie-dyed item a kind becomes, or null while that kind isn't made yet. */
    public static Item output(TieDyeRules.Kind kind, String wood) {
        return switch (kind) {
            case HEMP -> ModTieDye.HEMP_ITEM.get();
            default -> null;
        };
    }

    /** Why an order was refused, or null when it was added. */
    public static String add(IColony colony, UUID by, int pattern, String colourway, TieDyeRules.Kind kind, String wood, int count) {
        if (!patternUnlocked(colony, pattern)) return "tiedye.fourtwenty.refused.pattern";
        if (!colourwayOrderable(colony, colourway)) return "tiedye.fourtwenty.refused.colourway";
        if (kind == null || !available(kind) || count < 1 || count > MAX_COUNT) return "tiedye.fourtwenty.refused.kind";
        record(colony).add(pattern, colourway, kind, wood, count, by);
        changed(colony);
        return null;
    }

    /** Why a cancel was refused, or null when it went. Requests for a batch that hadn't started are dropped. */
    public static String cancel(IColony colony, int id) {
        OrderQueue q = record(colony);
        boolean head = q.head() != null && q.head().id() == id;
        boolean started = head && q.batchStarted();
        if (q.orders().stream().noneMatch(o -> o.id() == id)) return "tiedye.fourtwenty.refused.no_order";
        if (!q.cancel(id)) return "tiedye.fourtwenty.refused.started";
        if (started) {
            for (var b : colony.getServerBuildingManager().getBuildings().values()) {
                if (b instanceof ShackBuilding shack) for (var c : shack.getAllAssignedCitizen()) shack.cancelAllRequestsOfCitizenOrBuilding(c);
            }
        }
        changed(colony);
        return null;
    }

    /** Fixes the head order's next batch, sized by the Shack's level. Returns false if there's nothing to start. */
    public static boolean startBatch(IColony colony, int level) {
        OrderQueue q = record(colony);
        OrderQueue.Order head = q.head();
        if (head == null || q.batchStarted()) return false;
        int items = Math.min(head.left(), TieDyeRules.batch(level, TieDyeConfig.batchScale()));
        if (items <= 0) return false;
        int plainCount = head.kind() == TieDyeRules.Kind.SHIRT ? items * TieDyeConfig.woolPerShirt() : items;
        List<TieDyeRules.Supply> supplies = TieDyeRules.batchSupplies(Colourways.get(head.colourway()), plain(head.kind(), head.wood()), plainCount,
                TieDyeConfig.itemsPerBud());
        q.startBatch(items, supplies);
        changed(colony);
        return true;
    }

    /** The batch is dyed: its items go to the warehouse; the order moves on. Returns how many were made. */
    public static int finishBatch(IColony colony, ShackBuilding shack) {
        OrderQueue q = record(colony);
        OrderQueue.Order head = q.head();
        if (head == null || !q.batchStarted()) return 0;
        int items = q.batchItems();
        Item out = output(head.kind(), head.wood());
        q.finishBatch();
        changed(colony);
        if (out != null) deliver(colony, shack, ModTieDye.stack(out, new TieDye(head.pattern(), head.colourway()), items));
        return items;
    }

    /** Into the warehouse closest to the Shack; what doesn't fit into the Shack for pickup; only if that's full, dropped at its door. */
    static void deliver(IColony colony, ShackBuilding shack, ItemStack stack) {
        BlockPos near = shack.getPosition();
        while (!stack.isEmpty()) {
            ItemStack part = stack.split(stack.getMaxStackSize());
            var warehouse = colony.getServerBuildingManager().getClosestWarehouseInColony(near);
            var handler = warehouse == null ? null : warehouse.getItemHandlerCap();
            if (handler != null) part = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(handler, part, false);
            if (part.isEmpty()) continue;
            var hut = shack.getItemHandlerCap();
            if (hut != null) part = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(hut, part, false);
            shack.createPickupRequest(com.minecolonies.api.colony.requestsystem.requestable.deliveryman.AbstractDeliverymanRequestable.getMaxBuildingPriority(true));
            if (!part.isEmpty()) {
                var level = colony.getWorld();
                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, near.getX() + 0.5, near.getY() + 1, near.getZ() + 0.5, part));
            }
        }
    }
}
