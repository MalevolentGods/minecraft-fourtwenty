package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.malevolentgods.fourtwenty.domain.college.ColonyEffects;
import com.malevolentgods.fourtwenty.domain.college.ColonyResearch;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.malevolentgods.fourtwenty.domain.college.StudyBoard;
import com.malevolentgods.fourtwenty.domain.college.StudyRules;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.util.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The Weed College's research, server side (contracts/colony-research-api.md): what a colony has studied and what it does, and the
 * queue operations the board, the command and the Weedologist use. Everything is keyed by colony; outside a colony the answers are the
 * neutral ones.
 */
public final class CollegeResearch {
    private CollegeResearch() {}

    /** The colony at {@code pos}, or null. */
    public static IColony colonyAt(Level level, BlockPos pos) {
        return level.isClientSide() ? null : IColonyManager.getInstance().getColonyByPosFromWorld(level, pos);
    }

    public static ColonyResearch record(IColony colony) {
        return CollegeSavedData.get((ServerLevel) colony.getWorld()).colony(colony.getID());
    }

    /** Saves the colony's research and refreshes every one of its Colleges' Board and Status tabs (each re-sends only if it changed). */
    public static void changed(IColony colony) {
        CollegeSavedData.get((ServerLevel) colony.getWorld()).setDirty();
        for (var b : colony.getServerBuildingManager().getBuildings().values()) {
            if (b instanceof CollegeBuilding college) {
                college.getModule(CollegeRegistry.BOARD).refresh();
                college.getModule(CollegeRegistry.STATUS).refresh();
            }
        }
    }

    /** The highest level among the colony's Weed Colleges (0 if it has none built). */
    public static int collegeLevel(IColony colony) {
        int best = 0;
        for (var b : colony.getServerBuildingManager().getBuildings().values()) {
            if (b instanceof CollegeBuilding college) best = Math.max(best, college.getBuildingLevel());
        }
        return best;
    }

    // ---- the contract

    public static boolean knowsColour(Level level, BlockPos pos, String code) {
        IColony colony = colonyAt(level, pos);
        return colony != null && knownColours(colony).contains(code);
    }

    public static Set<String> knownColours(Level level, BlockPos pos) {
        IColony colony = colonyAt(level, pos);
        return colony == null ? Set.of() : knownColours(colony);
    }

    public static Set<String> knownColours(IColony colony) {
        Set<String> out = new java.util.LinkedHashSet<>();
        for (Study s : doneStudies(colony)) if (s.kind() == Study.Kind.COLOURS) out.addAll(s.gives().colours());
        return out;
    }

    public static boolean done(Level level, BlockPos pos, String study) {
        IColony colony = colonyAt(level, pos);
        return colony != null && record(colony).isDone(study);
    }

    public static double factor(Level level, BlockPos pos, ColonyEffects.Effect effect) {
        IColony colony = colonyAt(level, pos);
        return colony == null ? effect.neutral : ColonyEffects.factor(effect, doneStudies(colony));
    }

    public static Collection<Study> doneStudies(IColony colony) {
        StudyBoard board = CollegeStudies.board();
        List<Study> out = new ArrayList<>();
        for (String id : record(colony).done().keySet()) {
            Study s = board.get(id);
            if (s != null) out.add(s);
        }
        return out;
    }

    // ---- operations

    /** Why a queue request was refused, or null on success. */
    public static String queue(IColony colony, String study) {
        ColonyResearch r = record(colony);
        StudyBoard board = CollegeStudies.board();
        if (board.get(study) == null) return "college.fourtwenty.refused.unknown";
        if (!board.canQueue(study, r, collegeLevel(colony), CollegeFeatures::present)) return "college.fourtwenty.refused.locked";
        r.enqueue(study);
        changed(colony);
        return null;
    }

    /** Why a removal was refused, or null on success. */
    public static String remove(IColony colony, String study) {
        ColonyResearch r = record(colony);
        if (!r.queue().contains(study)) return "college.fourtwenty.refused.not_queued";
        boolean headStarted = study.equals(r.head()) && r.started();
        if (!r.remove(study)) return "college.fourtwenty.refused.started";
        if (headStarted) {
            // its supplies were asked for; nothing has come yet, so the asks are dropped (the next study makes its own)
            for (var b : colony.getServerBuildingManager().getBuildings().values()) {
                if (b instanceof CollegeBuilding college) {
                    for (var citizen : college.getAllAssignedCitizen()) college.cancelAllRequestsOfCitizenOrBuilding(citizen);
                }
            }
        }
        changed(colony);
        return null;
    }

    /** Starts the head study if it hasn't started: fixes its cost with the members' known colours (research R9). */
    public static void startHead(IColony colony) {
        ColonyResearch r = record(colony);
        if (r.head() == null || r.started()) return;
        Study s = CollegeStudies.board().get(r.head());
        if (s == null) return;
        r.start(StudyRules.cost(s, r.membersColours(), CollegeConfig.knownFactor()));
        changed(colony);
    }

    /** Writes the head up: done, announced to the colony, its effects applied. Returns the study, or null. */
    public static Study finishHead(IColony colony) {
        ColonyResearch r = record(colony);
        String id = r.finishHead(colony.getDay());
        if (id == null) return null;
        changed(colony);
        Study s = CollegeStudies.board().get(id);
        announce(colony, id);
        if (s != null) CollegeEffects.finished(colony, s);
        return s;
    }

    /** Marks a study done outright (the op-only test command). */
    public static void finish(IColony colony, String study) {
        record(colony).finish(study, colony.getDay());
        changed(colony);
        announce(colony, study);
        Study s = CollegeStudies.board().get(study);
        if (s != null) CollegeEffects.finished(colony, s);
    }

    /**
     * Tells the colony a study is done, as MineColonies announces a University study. An accord study is named for each player as they may
     * see it (research R10): by its accord to those who've discovered it, as an undiscovered accord to the rest.
     */
    private static void announce(IColony colony, String id) {
        Study s = CollegeStudies.board().get(id);
        String accord = s == null ? null : s.accord();
        if (accord == null) {
            MessageUtils.format(Component.translatable("college.fourtwenty.finished", Component.translatable(nameKey(id)))).sendTo(colony).forAllPlayers();
            return;
        }
        for (var player : colony.getMessagePlayerEntities()) {
            Component name = com.malevolentgods.fourtwenty.terpene.TerpeneDiscovery.known(player).contains(accord)
                    ? Component.translatable("college.fourtwenty.board.accord", Component.translatable("item.fourtwenty.terpene_" + accord))
                    : Component.translatable("college.fourtwenty.board.accord_unknown");
            MessageUtils.format(Component.translatable("college.fourtwenty.finished", name)).sendTo(player);
        }
    }

    public static String nameKey(String id) {
        return "college.fourtwenty.study." + id.substring(id.indexOf(':') + 1);
    }
}
