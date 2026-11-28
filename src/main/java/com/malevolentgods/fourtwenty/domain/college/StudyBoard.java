package com.malevolentgods.fourtwenty.domain.college;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The Weed College's tree of studies (specs/017-weed-college FR-006 to FR-009, FR-015, FR-016): lookup, validation, and what a colony
 * may start now, with the reason for everything it may not. Pure and tested.
 */
public final class StudyBoard {
    /** Where a study stands for one colony. */
    public enum State { LOCKED, AVAILABLE, QUEUED, IN_PROGRESS, DONE }

    /** Why a study is locked, in the order they're reported: a later feature, a parent, the College's level, the branch's tier 5. */
    public enum Lock { NONE, FEATURE, PARENT, LEVEL, BRANCH }

    /** A study's state for one colony; {@code arg} is the lock's subject (a feature id, a parent study, a needed level). */
    public record Standing(State state, Lock lock, String arg) {
        static final Standing AVAILABLE = new Standing(State.AVAILABLE, Lock.NONE, null);
    }

    private final Map<String, Study> studies = new LinkedHashMap<>();

    public StudyBoard(Collection<Study> all) {
        for (Study s : all) studies.put(s.id(), s);
    }

    public Collection<Study> all() { return studies.values(); }

    public Study get(String id) { return studies.get(id); }

    public int size() { return studies.size(); }

    /**
     * Problems with the tree, empty when it's sound: duplicate ids are impossible here (the map keeps the last), so it checks unknown
     * parents, parents of a higher tier, cycles, unreachable studies, and a tier-6 study whose branch has no tier-5 study.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        for (Study s : studies.values()) {
            for (String p : s.parents()) {
                Study parent = studies.get(p);
                if (parent == null) errors.add(s.id() + ": unknown parent " + p);
                else if (parent.tier() > s.tier()) errors.add(s.id() + ": parent " + p + " is of a higher tier");
            }
            if (s.tier() == StudyRules.MAX_TIER && branchTop(s.branch()).isEmpty()) errors.add(s.id() + ": tier 6 with no tier-5 study in its branch");
        }
        // cycles and reachability: every study must resolve to roots through known parents
        Map<String, Integer> mark = new HashMap<>();
        for (String id : studies.keySet()) {
            if (cycle(id, mark)) errors.add(id + ": part of a cycle");
        }
        return errors;
    }

    private boolean cycle(String id, Map<String, Integer> mark) {
        Integer m = mark.get(id);
        if (m != null) return m == 1;
        mark.put(id, 1);
        Study s = studies.get(id);
        if (s != null) {
            for (String p : s.parents()) {
                if (studies.containsKey(p) && cycle(p, mark)) return true;
            }
        }
        mark.put(id, 2);
        return false;
    }

    /** The branch's tier-5 studies. */
    public List<Study> branchTop(Study.Branch branch) {
        List<Study> out = new ArrayList<>();
        for (Study s : studies.values()) if (s.branch() == branch && s.tier() == StudyRules.MAX_LEVEL) out.add(s);
        return out;
    }

    /**
     * Every study's standing for a colony with {@code record}, a College of {@code level}, and {@code features} present. A study may be
     * queued once each parent is done or queued ahead of it, so a chain can be queued at once; the level must already allow it.
     */
    public Map<String, Standing> standings(ColonyResearch record, int level, Predicate<String> features) {
        Map<String, Standing> out = new LinkedHashMap<>();
        Set<String> queued = new HashSet<>(record.queue());
        for (Study s : studies.values()) {
            if (record.isDone(s.id())) out.put(s.id(), new Standing(State.DONE, Lock.NONE, null));
            else if (s.id().equals(record.head())) out.put(s.id(), new Standing(State.IN_PROGRESS, lockFor(s, record, level, features, queued).lock(), null));
            else if (queued.contains(s.id())) out.put(s.id(), new Standing(State.QUEUED, Lock.NONE, null));
            else out.put(s.id(), lockFor(s, record, level, features, queued));
        }
        return out;
    }

    /** Whether {@code study} may be added to the queue now. */
    public boolean canQueue(String study, ColonyResearch record, int level, Predicate<String> features) {
        Study s = studies.get(study);
        if (s == null || record.isDone(study) || record.queue().contains(study)) return false;
        return lockFor(s, record, level, features, new HashSet<>(record.queue())).state() == State.AVAILABLE;
    }

    /**
     * Whether the head study can be worked now. A queued study's parents are done by the time it reaches the head; its level may have
     * dropped since (a College downgraded or demolished), which pauses it with the reason.
     */
    public Standing headStanding(ColonyResearch record, int level, Predicate<String> features) {
        Study s = studies.get(record.head());
        if (s == null) return new Standing(State.LOCKED, Lock.FEATURE, record.head());
        Standing lock = lockFor(s, record, level, features, Set.of());
        return lock.state() == State.AVAILABLE ? new Standing(State.IN_PROGRESS, Lock.NONE, null) : lock;
    }

    private Standing lockFor(Study s, ColonyResearch record, int level, Predicate<String> features, Set<String> queuedAhead) {
        if (s.needs() != null && !features.test(s.needs())) return new Standing(State.LOCKED, Lock.FEATURE, s.needs());
        for (String p : s.parents()) {
            if (!record.isDone(p) && !queuedAhead.contains(p)) return new Standing(State.LOCKED, Lock.PARENT, p);
        }
        boolean branchTopDone = true;
        if (s.tier() == StudyRules.MAX_TIER) {
            for (Study top : branchTop(s.branch())) {
                if (!record.isDone(top.id()) && !queuedAhead.contains(top.id())) {
                    branchTopDone = false;
                    break;
                }
            }
        }
        if (!StudyRules.allows(level, s.tier(), true)) {
            return new Standing(State.LOCKED, Lock.LEVEL, String.valueOf(Math.min(s.tier(), StudyRules.MAX_LEVEL)));
        }
        if (!branchTopDone) return new Standing(State.LOCKED, Lock.BRANCH, s.branch().key());
        return Standing.AVAILABLE;
    }
}
