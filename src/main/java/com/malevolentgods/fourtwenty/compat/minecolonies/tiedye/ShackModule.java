package com.malevolentgods.fourtwenty.compat.minecolonies.tiedye;

import com.malevolentgods.fourtwenty.domain.tiedye.OrderQueue;
import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.malevolentgods.fourtwenty.tiedye.Colourways;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The Tie Dye Shack's Orders tab, sent to the client (contracts/orders.md): what the Shack is doing, which patterns and colourways the
 * colony can order, and the queue. Colour codes travel; colour names never do (the client names only what its viewer has caught).
 */
public final class ShackModule extends AbstractBuildingModule {
    public record ColourwayView(String id, List<String> colours, boolean orderable) {}

    public record OrderView(int id, int pattern, String colourway, TieDyeRules.Kind kind, String wood, int count, int made) {}

    public record View(int level, String workers, ShackBuilding.Activity activity, String reason, List<TieDyeRules.Supply> missing, long made,
                       List<Boolean> patterns, List<ColourwayView> colourways, List<OrderView> queue, boolean headUnderWay, List<TieDyeRules.Kind> kinds) {
        public static final View EMPTY = new View(0, "", ShackBuilding.Activity.IDLE, "", List.of(), 0, List.of(), List.of(), List.of(), false, List.of());

        public void write(RegistryFriendlyByteBuf b) {
            b.writeVarInt(level); b.writeUtf(workers); b.writeEnum(activity); b.writeUtf(reason);
            b.writeCollection(missing, (x, s) -> { x.writeUtf(s.item(), 128); x.writeVarInt(s.count()); });
            b.writeVarLong(made);
            b.writeCollection(patterns, (x, p) -> x.writeBoolean(p));
            b.writeCollection(colourways, (x, c) -> { x.writeUtf(c.id(), 64); x.writeCollection(c.colours(), (y, s) -> y.writeUtf(s, 64)); x.writeBoolean(c.orderable()); });
            b.writeCollection(queue, (x, o) -> {
                x.writeVarInt(o.id()); x.writeVarInt(o.pattern()); x.writeUtf(o.colourway(), 64); x.writeEnum(o.kind()); x.writeUtf(o.wood(), 64);
                x.writeVarInt(o.count()); x.writeVarInt(o.made());
            });
            b.writeBoolean(headUnderWay);
            b.writeCollection(kinds, (x, k) -> x.writeEnum(k));
        }

        public static View read(RegistryFriendlyByteBuf b) {
            int level = b.readVarInt();
            String workers = b.readUtf();
            var activity = b.readEnum(ShackBuilding.Activity.class);
            String reason = b.readUtf();
            List<TieDyeRules.Supply> missing = b.readList(x -> new TieDyeRules.Supply(x.readUtf(128), x.readVarInt()));
            long made = b.readVarLong();
            List<Boolean> patterns = b.readList(x -> x.readBoolean());
            List<ColourwayView> colourways = b.readList(x -> new ColourwayView(x.readUtf(64), x.readList(y -> y.readUtf(64)), x.readBoolean()));
            List<OrderView> queue = b.readList(x -> new OrderView(x.readVarInt(), x.readVarInt(), x.readUtf(64), x.readEnum(TieDyeRules.Kind.class),
                    x.readUtf(64), x.readVarInt(), x.readVarInt()));
            boolean underWay = b.readBoolean();
            List<TieDyeRules.Kind> kinds = b.readList(x -> x.readEnum(TieDyeRules.Kind.class));
            return new View(level, workers, activity, reason, missing, made, patterns, colourways, queue, underWay, kinds);
        }
    }

    private View last;

    /** Marks the view dirty only when it changed (the colony clock, and every change to the colony's orders). */
    public void refresh() {
        if (!building.getColony().getWorld().hasChunkAt(building.getPosition())) return;
        var next = snapshot();
        if (!next.equals(last)) {
            last = next;
            markDirty();
        }
    }

    @Override public void serializeToView(RegistryFriendlyByteBuf buffer) { snapshot().write(buffer); }

    public View snapshot() {
        var shack = (ShackBuilding) building;
        var colony = shack.getColony();
        var assigned = shack.getAllAssignedCitizen().stream().sorted(Comparator.comparingInt(c -> c.getId())).toList();
        String names = String.join(", ", assigned.stream().map(c -> c.getName()).toList());
        OrderQueue q = TieDyeOrders.record(colony);
        List<Boolean> patterns = new ArrayList<>();
        for (int p = 0; p < TieDyeRules.PATTERNS.size(); p++) patterns.add(TieDyeOrders.patternUnlocked(colony, p));
        Set<String> known = TieDyeOrders.knownColours(colony);
        List<ColourwayView> colourways = Colourways.list().stream().map(c -> new ColourwayView(c.id(), c.colours(), c.orderable(known))).toList();
        List<OrderView> queue = q.orders().stream().map(o -> new OrderView(o.id(), o.pattern(), o.colourway(), o.kind(), o.wood(), o.count(), o.made())).toList();
        List<TieDyeRules.Kind> kinds = java.util.Arrays.stream(TieDyeRules.Kind.values()).filter(TieDyeOrders::available).toList();
        var activity = assigned.isEmpty() ? ShackBuilding.Activity.PAUSED : shack.activity();
        String reason = assigned.isEmpty() ? "tiedye.fourtwenty.status.no_worker" : shack.reason();
        return new View(shack.getBuildingLevel(), names, activity, reason, List.copyOf(q.missing()), shack.made(), patterns, colourways, queue,
                q.batchUnderWay(), kinds);
    }
}
