package com.malevolentgods.fourtwenty.domain.college;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One colony's Weed College research (specs/017-weed-college data-model.md): the queue (its head is the study in progress), work units
 * done, the head's fixed cost and what of it is already delivered, the colours the colony's owner and officers know, and the finished
 * studies. Shared by every College the colony has; saved per colony by CollegeSavedData. Pure: no Minecraft types.
 */
public final class ColonyResearch {
    private final List<String> queue = new ArrayList<>();
    private final Map<String, Integer> progress = new LinkedHashMap<>();
    private final Map<String, Long> done = new LinkedHashMap<>();
    private final Set<String> membersColours = new LinkedHashSet<>();
    /** The head study's supplies, fixed when it started (null until then), and how many of each line have been delivered. */
    private List<Study.Supply> cost;
    private final List<Integer> delivered = new ArrayList<>();
    private int badBatches, unitsWorked;

    public List<String> queue() { return Collections.unmodifiableList(queue); }

    public String head() { return queue.isEmpty() ? null : queue.getFirst(); }

    public boolean isDone(String study) { return done.containsKey(study); }

    public Map<String, Long> done() { return Collections.unmodifiableMap(done); }

    public int progress(String study) { return progress.getOrDefault(study, 0); }

    public Set<String> membersColours() { return Collections.unmodifiableSet(membersColours); }

    public List<Study.Supply> cost() { return cost == null ? null : List.copyOf(cost); }

    public List<Integer> delivered() { return List.copyOf(delivered); }

    public int badBatches() { return badBatches; }

    public int unitsWorked() { return unitsWorked; }

    /** Whether the head has begun: its cost is fixed (it can still be removed until {@link #underWay()}). */
    public boolean started() { return cost != null; }

    /** Adds a study to the end of the queue; false if it's already queued or done. Availability is the board's to check. */
    public boolean enqueue(String study) {
        if (queue.contains(study) || done.containsKey(study)) return false;
        queue.add(study);
        return true;
    }

    /**
     * Removes a queued study. The head comes off only while it's untouched: no units done and nothing delivered, even if its cost has
     * been fixed (contracts/board-actions.md), so a study queued by mistake can be taken back. Returns whether it was removed.
     */
    public boolean remove(String study) {
        int at = queue.indexOf(study);
        if (at < 0 || (at == 0 && underWay())) return false;
        queue.remove(at);
        if (at == 0) {
            cost = null;
            delivered.clear();
        }
        return true;
    }

    /** Whether the head has had work or supplies put into it, so it can no longer be taken off the queue. */
    public boolean underWay() {
        String head = head();
        if (head == null) return false;
        if (progress(head) > 0) return true;
        for (int d : delivered) if (d > 0) return true;
        return false;
    }

    /** Starts the head: fixes its cost (after the caught-colour discount) with nothing delivered yet. */
    public void start(List<Study.Supply> fixedCost) {
        if (head() == null || started()) return;
        cost = new ArrayList<>(fixedCost);
        delivered.clear();
        for (int i = 0; i < cost.size(); i++) delivered.add(0);
    }

    /** What of the head's cost is still to come, line by line (empty when everything has been delivered). */
    public List<Study.Supply> missing() {
        if (cost == null) return List.of();
        List<Study.Supply> out = new ArrayList<>();
        for (int i = 0; i < cost.size(); i++) {
            Study.Supply s = cost.get(i);
            int left = s.count() - delivered.get(i);
            if (left > 0) out.add(new Study.Supply(s.item(), left, s.strain(), false));
        }
        return out;
    }

    /** Records {@code count} of cost line {@code line} delivered (capped at the line's count). */
    public void deliver(int line, int count) {
        if (cost == null || line < 0 || line >= cost.size()) return;
        delivered.set(line, Math.min(cost.get(line).count(), delivered.get(line) + Math.max(0, count)));
    }

    /** Records one cycle of work on the head: a unit, unless it was a bad batch. Returns the head's units done. */
    public int work(boolean bad) {
        String head = head();
        if (head == null) return 0;
        unitsWorked++;
        if (bad) {
            badBatches++;
            return progress(head);
        }
        return progress.merge(head, 1, Integer::sum);
    }

    /** Writes the head up as finished on {@code day} and moves the queue on. Returns the finished study, or null. */
    public String finishHead(long day) {
        String head = head();
        if (head == null) return null;
        queue.removeFirst();
        progress.remove(head);
        done.put(head, day);
        cost = null;
        delivered.clear();
        return head;
    }

    /** Marks a study done outright (the op-only test command); removes it from the queue. */
    public void finish(String study, long day) {
        if (study.equals(head())) {
            finishHead(day);
            return;
        }
        queue.remove(study);
        progress.remove(study);
        done.put(study, day);
    }

    public boolean learnColours(Iterable<String> colours) {
        boolean any = false;
        for (String c : colours) any |= membersColours.add(c);
        return any;
    }

    // ---- restoring from a save (CollegeSavedData); values are trusted as written

    public void restore(List<String> queue, Map<String, Integer> progress, Map<String, Long> done, Set<String> colours,
                        List<Study.Supply> cost, List<Integer> delivered, int badBatches, int unitsWorked) {
        this.queue.clear();
        this.queue.addAll(queue);
        this.progress.clear();
        this.progress.putAll(progress);
        this.done.clear();
        this.done.putAll(done);
        this.membersColours.clear();
        this.membersColours.addAll(colours);
        this.cost = cost == null ? null : new ArrayList<>(cost);
        this.delivered.clear();
        if (cost != null) {
            for (int i = 0; i < cost.size(); i++) this.delivered.add(i < delivered.size() ? delivered.get(i) : 0);
        }
        this.badBatches = badBatches;
        this.unitsWorked = unitsWorked;
    }
}
