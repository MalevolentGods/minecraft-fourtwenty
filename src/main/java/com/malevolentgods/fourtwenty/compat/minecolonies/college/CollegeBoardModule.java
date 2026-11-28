package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.malevolentgods.fourtwenty.domain.college.ColonyResearch;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.malevolentgods.fourtwenty.domain.college.StudyBoard;
import com.malevolentgods.fourtwenty.domain.college.StudyRules;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Weed College's board, sent to the client for the Board tab (specs/017-weed-college US2, contracts/board-actions.md): every study's
 * definition, where it stands for this colony and what it would cost now, and the queue. Read-only; queueing goes through
 * {@link CollegeNetwork}. The definitions travel with it, so a server's data pack is what its players see.
 */
public final class CollegeBoardModule extends AbstractBuildingModule {
    /** One study as this colony sees it. {@code cost} is empty once done; for the head that has started, it's the fixed cost. */
    public record Entry(String id, Study.Branch branch, int tier, List<String> parents, Study.Kind kind, String icon, List<String> colours,
                        String needs, StudyBoard.State state, StudyBoard.Lock lock, String lockArg, List<Study.Supply> cost) {}

    /**
     * The whole board. {@code unitTicks} lets the client turn units into hours; {@code headStarted} means the head's cost is fixed, and
     * {@code headUnderWay} that work or supplies have gone into it, so it can no longer come off the queue.
     */
    public record Board(int level, int unitTicks, List<String> queue, int headUnits, boolean headStarted, boolean headUnderWay,
                        List<Entry> entries) {
        public static final Board EMPTY = new Board(0, 1200, List.of(), 0, false, false, List.of());

        public Entry entry(String id) {
            for (Entry e : entries) if (e.id().equals(id)) return e;
            return null;
        }

        public void write(RegistryFriendlyByteBuf b) {
            b.writeVarInt(level); b.writeVarInt(unitTicks);
            b.writeCollection(queue, (x, s) -> x.writeUtf(s, 128));
            b.writeVarInt(headUnits); b.writeBoolean(headStarted); b.writeBoolean(headUnderWay);
            b.writeVarInt(entries.size());
            for (Entry e : entries) {
                b.writeUtf(e.id(), 128); b.writeEnum(e.branch()); b.writeVarInt(e.tier());
                b.writeCollection(e.parents(), (x, s) -> x.writeUtf(s, 128));
                b.writeEnum(e.kind()); b.writeUtf(e.icon(), 128);
                b.writeCollection(e.colours(), (x, s) -> x.writeUtf(s, 64));
                b.writeUtf(e.needs() == null ? "" : e.needs(), 64);
                b.writeEnum(e.state()); b.writeEnum(e.lock()); b.writeUtf(e.lockArg() == null ? "" : e.lockArg(), 128);
                b.writeVarInt(e.cost().size());
                for (Study.Supply s : e.cost()) {
                    b.writeUtf(s.item(), 128); b.writeVarInt(s.count()); b.writeUtf(s.strain() == null ? "" : s.strain(), 64);
                }
            }
        }

        public static Board read(RegistryFriendlyByteBuf b) {
            int level = b.readVarInt(), unitTicks = b.readVarInt();
            List<String> queue = b.readList(x -> x.readUtf(128));
            int headUnits = b.readVarInt();
            boolean started = b.readBoolean(), underWay = b.readBoolean();
            int n = b.readVarInt();
            List<Entry> entries = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                String id = b.readUtf(128);
                Study.Branch branch = b.readEnum(Study.Branch.class);
                int tier = b.readVarInt();
                List<String> parents = b.readList(x -> x.readUtf(128));
                Study.Kind kind = b.readEnum(Study.Kind.class);
                String icon = b.readUtf(128);
                List<String> colours = b.readList(x -> x.readUtf(64));
                String needs = b.readUtf(64);
                StudyBoard.State state = b.readEnum(StudyBoard.State.class);
                StudyBoard.Lock lock = b.readEnum(StudyBoard.Lock.class);
                String arg = b.readUtf(128);
                int lines = b.readVarInt();
                List<Study.Supply> cost = new ArrayList<>(lines);
                for (int j = 0; j < lines; j++) {
                    String item = b.readUtf(128);
                    int count = b.readVarInt();
                    String strain = b.readUtf(64);
                    cost.add(new Study.Supply(item, count, strain.isEmpty() ? null : strain, false));
                }
                entries.add(new Entry(id, branch, tier, parents, kind, icon, colours, needs.isEmpty() ? null : needs, state, lock,
                        arg.isEmpty() ? null : arg, cost));
            }
            return new Board(level, unitTicks, queue, headUnits, started, underWay, entries);
        }
    }

    private Board last;

    /** Marks the view dirty only when the board changed (the colony clock, and every change to the colony's research). */
    public void refresh() {
        if (!building.getColony().getWorld().hasChunkAt(building.getPosition())) return;
        var next = snapshot();
        if (!next.equals(last)) {
            last = next;
            markDirty();
        }
    }

    @Override public void serializeToView(RegistryFriendlyByteBuf buffer) { snapshot().write(buffer); }

    public Board snapshot() {
        var colony = building.getColony();
        ColonyResearch r = CollegeResearch.record(colony);
        StudyBoard board = CollegeStudies.board();
        int level = CollegeResearch.collegeLevel(colony);
        Map<String, StudyBoard.Standing> standings = board.standings(r, level, CollegeFeatures::present);
        List<Entry> entries = new ArrayList<>(board.size());
        for (Study s : board.all()) {
            StudyBoard.Standing st = standings.get(s.id());
            List<Study.Supply> cost = st.state() == StudyBoard.State.DONE ? List.of()
                    : s.id().equals(r.head()) && r.started() ? r.cost()
                    : StudyRules.cost(s, r.membersColours(), CollegeConfig.knownFactor());
            entries.add(new Entry(s.id(), s.branch(), s.tier(), s.parents(), s.kind(), s.icon(), s.gives().colours(), s.needs(),
                    st.state(), st.lock(), st.arg(), cost));
        }
        int headUnits = r.head() == null ? 0 : r.progress(r.head());
        // a copy: the record's queue is a live view, and the last snapshot must not change with it
        return new Board(level, CollegeConfig.unitTicks(), List.copyOf(r.queue()), headUnits, r.started(), r.underWay(), entries);
    }
}
