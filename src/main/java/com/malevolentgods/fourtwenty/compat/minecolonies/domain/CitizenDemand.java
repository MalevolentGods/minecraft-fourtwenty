package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import java.util.Optional;

/** Immutable durable state. Revision tokens invalidate repeated and interrupted completion callbacks. */
public record CitizenDemand(boolean eligible, long dueAt, long graceUntil, long satisfiedUntil,
                            long healthUntil, long lastConsumedAt, long revision) {
    /** UNEXPOSED and TOLERANCE_BREAK come from affinity, never from {@link #status}. */
    public enum Status { INELIGIBLE, GRACE, SATISFIED, UNMET, UNEXPOSED, TOLERANCE_BREAK }
    public CitizenDemand {
        if (revision < 0 || dueAt < -1 || graceUntil < -1 || satisfiedUntil < -1 || healthUntil < -1
                || lastConsumedAt < -1 || (eligible && (dueAt < 0 || graceUntil < dueAt))
                || (lastConsumedAt < 0 && (satisfiedUntil != -1 || healthUntil != -1))
                || (lastConsumedAt >= 0 && (satisfiedUntil <= lastConsumedAt || healthUntil <= lastConsumedAt))) {
            throw new IllegalArgumentException("Invalid citizen demand record");
        }
    }
    public static CitizenDemand empty() { return new CitizenDemand(false, -1, -1, -1, -1, -1, 0); }
    private static void checkTime(long now) {
        if (now < 0) throw new IllegalArgumentException("Negative simulation time");
    }
    /** Moving between qualifying homes preserves deadlines; re-entry gives normal grace, never extra credit. */
    public CitizenDemand observe(boolean qualifies, long now, DemandRules rules) {
        checkTime(now);
        if (eligible == qualifies) return this;
        long nextRevision = Math.incrementExact(revision);
        if (!qualifies) return new CitizenDemand(false, dueAt, graceUntil, satisfiedUntil, healthUntil, lastConsumedAt, nextRevision);
        long due = satisfiedUntil > now ? satisfiedUntil : now;
        long grace = satisfiedUntil > now ? graceUntil : Math.addExact(due, rules.graceTicks());
        return new CitizenDemand(true, due, grace, satisfiedUntil, healthUntil, lastConsumedAt, nextRevision);
    }
    public boolean due(long now) { checkTime(now); return eligible && now >= dueAt; }
    public boolean healthActive(long now) { checkTime(now); return now < healthUntil; }
    public Status status(long now) {
        checkTime(now);
        if (!eligible) return Status.INELIGIBLE;
        if (now < satisfiedUntil) return Status.SATISFIED;
        return now < graceUntil ? Status.GRACE : Status.UNMET;
    }
    /** Caller must consume exactly one joint in the same server action; collection alone must never call this. */
    public Optional<CitizenDemand> consume(long expectedRevision, long now, DemandRules rules) {
        if (expectedRevision != revision || !due(now)) return Optional.empty();
        long nextDue = Math.addExact(now, rules.intervalTicks());
        return Optional.of(new CitizenDemand(true, nextDue, Math.addExact(nextDue, rules.graceTicks()), nextDue,
                Math.addExact(now, rules.healthTicks()), now, Math.incrementExact(revision)));
    }
}
