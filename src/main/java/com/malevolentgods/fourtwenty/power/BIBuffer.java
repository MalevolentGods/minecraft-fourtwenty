package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;

/** A plain store of BI. */
public class BIBuffer implements BINode {
    private static final int OVERLOAD_TICKS = 60;
    private final BIRules.Tier tier;
    private final long capacity;
    private final boolean in, out;
    private final Runnable changed;
    private long stored, overloadedUntil;

    public BIBuffer(BIRules.Tier tier, long capacity, boolean in, boolean out, Runnable changed) {
        this.tier = tier;
        this.capacity = capacity;
        this.in = in;
        this.out = out;
        this.changed = changed;
    }

    @Override public BIRules.Tier tier() { return tier; }

    @Override public long stored() { return stored; }

    @Override public long capacity() { return capacity; }

    @Override public long receive(long amount) {
        long taken = Math.max(0, Math.min(amount, capacity - stored));
        if (taken > 0) {
            stored += taken;
            changed.run();
        }
        return taken;
    }

    @Override public long extract(long amount) {
        long given = Math.max(0, Math.min(amount, stored));
        if (given > 0) {
            stored -= given;
            changed.run();
        }
        return given;
    }

    @Override public boolean canReceive() { return in; }

    @Override public boolean canExtract() { return out; }

    @Override public void overload(long now) { overloadedUntil = now + OVERLOAD_TICKS; }

    public boolean overloaded(long now) { return now < overloadedUntil; }

    /** On load: what was saved, trimmed to fit. */
    public void set(long value) {
        stored = Math.max(0, Math.min(value, capacity));
    }
}
