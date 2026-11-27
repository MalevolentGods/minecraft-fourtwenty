package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.malevolentgods.fourtwenty.domain.college.ColonyResearch;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** What the Weed College is doing, for its status tab (contracts/board-actions.md "Status view"). Read-only. */
public final class CollegeStatusModule extends AbstractBuildingModule {
    /** One line per missing supply: the item id and how many. */
    public record Missing(String item, int count) {}

    public record Status(int level, String workers, CollegeBuilding.Activity activity, String reason, String reasonArg, String study, int units,
                         int total, int minutesLeft, long finished, int badBatches, List<Missing> missing) {
        public static final Status EMPTY = new Status(0, "", CollegeBuilding.Activity.IDLE, "", "", "", 0, 0, -1, 0, 0, List.of());

        public void write(RegistryFriendlyByteBuf b) {
            b.writeVarInt(level); b.writeUtf(workers); b.writeVarInt(activity.ordinal()); b.writeUtf(reason); b.writeUtf(reasonArg);
            b.writeUtf(study); b.writeVarInt(units); b.writeVarInt(total); b.writeVarInt(minutesLeft + 1); b.writeVarLong(finished);
            b.writeVarInt(badBatches); b.writeVarInt(missing.size());
            for (Missing m : missing) { b.writeUtf(m.item()); b.writeVarInt(m.count()); }
        }

        public static Status read(RegistryFriendlyByteBuf b) {
            int level = b.readVarInt();
            String workers = b.readUtf();
            var activity = CollegeBuilding.Activity.values()[b.readVarInt()];
            String reason = b.readUtf(), arg = b.readUtf(), study = b.readUtf();
            int units = b.readVarInt(), total = b.readVarInt(), minutes = b.readVarInt() - 1;
            long finished = b.readVarLong();
            int bad = b.readVarInt();
            int n = b.readVarInt();
            List<Missing> missing = new ArrayList<>();
            for (int i = 0; i < n; i++) missing.add(new Missing(b.readUtf(), b.readVarInt()));
            return new Status(level, workers, activity, reason, arg, study, units, total, minutes, finished, bad, missing);
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
        var hut = (CollegeBuilding) building;
        var assigned = hut.getAllAssignedCitizen().stream().sorted(Comparator.comparingInt(c -> c.getId())).toList();
        String names = String.join(", ", assigned.stream().map(c -> c.getName()).toList());
        ColonyResearch r = CollegeResearch.record(hut.getColony());
        String head = r.head() == null ? "" : r.head();
        Study study = head.isEmpty() ? null : CollegeStudies.board().get(head);
        int units = head.isEmpty() ? 0 : r.progress(head), total = study == null ? 0 : study.units();
        int workers = Math.max(1, assigned.size());
        int minutes = study == null || assigned.isEmpty() ? -1
                : (int) Math.ceil((total - units) * (CollegeConfig.unitTicks() / 1200.0) / workers);
        List<Missing> missing = r.missing().stream().map(s -> new Missing(s.item(), s.count())).limit(6).toList();
        var activity = assigned.isEmpty() ? CollegeBuilding.Activity.PAUSED : hut.activity();
        String reason = assigned.isEmpty() ? "college.fourtwenty.status.no_worker" : hut.reason();
        return new Status(hut.getBuildingLevel(), names, activity, reason, hut.reasonArg(), head, units, total, minutes,
                hut.studiesFinished(), r.badBatches(), missing);
    }
}
