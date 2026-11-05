package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.domain.BIRules;

/** A plain store of Boundless Inspiration, counted in Willard-seconds (Ws). */
public class BIBuffer implements BINode {
    /** Saves before Willards counted plain BI, a sixth of a Ws: a Hookah made 70 BI a second where it now makes 420 W. */
    public static final int WS_PER_OLD_BI = 6;
    private static final int OVERLOAD_TICKS = 60;
    private final BIRules.Tier tier;
    private final long capacity;
    private final boolean in, out;
    private final Runnable changed;
    private final double demand;
    private long stored, overloadedUntil;
    private boolean asking;

    public BIBuffer(BIRules.Tier tier, long capacity, boolean in, boolean out, Runnable changed) {
        this(tier, capacity, in, out, com.malevolentgods.fourtwenty.domain.CircuitRules.ratedWatts(tier.machine()), changed);
    }

    /** With {@code demand}, the Willards it asks for as a load while it has room. */
    public BIBuffer(BIRules.Tier tier, long capacity, boolean in, boolean out, double demand, Runnable changed) {
        this.tier = tier;
        this.capacity = capacity;
        this.in = in;
        this.out = out;
        this.demand = demand;
        this.changed = changed;
    }

    /** Asks once it's used a share of its room (the config's hysteresis), and until it's full, so it doesn't flicker on and off. */
    @Override public double demandWatts() {
        asking = com.malevolentgods.fourtwenty.domain.CircuitRules.asks(asking, fill(), com.malevolentgods.fourtwenty.processing.ProcessingConfig.circuit().loadHysteresis());
        return asking ? demand : 0;
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

    @Override public boolean overloaded(long now) { return now < overloadedUntil; }

    /** What a block saved under {@code key}, or under {@code oldKey} in BI from before Willards. */
    public static long loadWs(net.minecraft.nbt.CompoundTag tag, String key, String oldKey) {
        return tag.contains(key) ? tag.getLong(key) : tag.getLong(oldKey) * WS_PER_OLD_BI;
    }

    /** On load: what was saved, trimmed to fit. */
    public void set(long value) {
        stored = Math.max(0, Math.min(value, capacity));
    }
}
