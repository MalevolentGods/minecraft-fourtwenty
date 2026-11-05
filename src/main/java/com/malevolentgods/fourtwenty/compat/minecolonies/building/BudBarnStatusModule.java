package com.malevolentgods.fourtwenty.compat.minecolonies.building;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryConfig;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.BudBarnRules;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.Comparator;

/** What the Bud Barn is doing, for its status window. Read-only: never requests or moves anything. */
public final class BudBarnStatusModule extends AbstractBuildingModule {
    /** One line of status: what's happening, and the numbers behind it. */
    public record Status(int level, String worker, BudBarnRules.Status state, int trays, int racks, int hanging, int free,
                         int minutesToNext, int rough, int trimmed, long curedMade, long seedsMade) {
        public static final Status EMPTY = new Status(0, "", BudBarnRules.Status.NO_WORKER, 0, 0, 0, 0, -1, 0, 0, 0, 0);

        public void write(RegistryFriendlyByteBuf b) {
            b.writeVarInt(level); b.writeUtf(worker); b.writeVarInt(state.ordinal()); b.writeVarInt(trays); b.writeVarInt(racks);
            b.writeVarInt(hanging); b.writeVarInt(free); b.writeVarInt(minutesToNext + 1); b.writeVarInt(rough); b.writeVarInt(trimmed);
            b.writeVarLong(curedMade); b.writeVarLong(seedsMade);
        }

        public static Status read(RegistryFriendlyByteBuf b) {
            return new Status(b.readVarInt(), b.readUtf(), BudBarnRules.Status.values()[b.readVarInt()], b.readVarInt(), b.readVarInt(),
                    b.readVarInt(), b.readVarInt(), b.readVarInt() - 1, b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readVarLong());
        }
    }

    private Status last;

    /** Called by the colony clock now and then; marks the view dirty only when something changed. */
    public void refresh() {
        if (!building.getColony().getWorld().hasChunkAt(building.getPosition())) return;
        var next = snapshot();
        if (!next.equals(last)) {
            last = next;
            markDirty();
        }
    }

    @Override public void serializeToView(RegistryFriendlyByteBuf buffer) { snapshot().write(buffer); }

    public Status snapshot() {
        var hut = (BudBarnBuilding) building;
        var assigned = hut.getAllAssignedCitizen().stream().sorted(Comparator.comparingInt(c -> c.getId())).toList();
        var held = assigned.isEmpty() ? null : assigned.getFirst().getInventory();
        var s = hut.snapshot(held);
        int next = -1;
        for (var pos : hut.racks()) {
            var rack = hut.rack(pos);
            if (rack == null) continue;
            int left = rack.minutesLeft();
            if (left >= 0 && (next < 0 || left < next)) next = left;
        }
        var state = BudBarnRules.status(s, !assigned.isEmpty(), DispensaryConfig.budBarnBuffer());
        return new Status(hut.getBuildingLevel(), assigned.isEmpty() ? "" : assigned.getFirst().getName(), state, s.trays(), s.racks(),
                s.hanging(), s.free(), next, s.roughHeld() + s.roughInHut(), s.trimmed(), hut.curedMade(), hut.seedsMade());
    }
}
