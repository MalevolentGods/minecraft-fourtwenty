package com.malevolentgods.fourtwenty.domain;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Boundless Inspiration, shown as BI. Everything is sized from a Hookah: one charcoal and one cured bud burn for a minute on every tier, and
 * run four machines of that tier. A battery gives what a Hookah gives. A wire carries two Hookahs' worth; more than that burns it out. A block
 * fed from a higher tier than its own never breaks: it overloads and stops until the higher tier is gone.
 */
public final class BIRules {
    private BIRules() {}

    public static final int BURN_TICKS = 1200, MACHINES_PER_HOOKAH = 4, HOOKAHS_PER_WIRE = 2, TICKS_PER_SECOND = 20;

    public enum Tier implements StringRepresentable {
        PUFF(4_200), TOKE(8_400), RIP(16_800);

        private final int total;

        Tier(int total) { this.total = total; }

        /** What one charcoal and one bud make. */
        public int total() { return total; }

        /** BI per tick from a Hookah, or a battery, of this tier. */
        public double rate() { return (double) total / BURN_TICKS; }

        /** BI per tick a wire of this tier can carry. */
        public double wire() { return rate() * HOOKAHS_PER_WIRE; }

        /** BI per tick a machine of this tier draws. */
        public double machine() { return rate() / MACHINES_PER_HOOKAH; }

        /** The tier below; Puff is the lowest. */
        public Tier below() { return this == PUFF ? PUFF : values()[ordinal() - 1]; }

        @Override public @Nonnull String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    /** Whether power of the source's tier overloads a block of this tier. */
    public static boolean overloads(Tier part, Tier source) {
        return source.ordinal() > part.ordinal();
    }

    /** Whether {@code sent} BI in one second is more than a wire of this tier can carry. */
    public static boolean wireBurns(Tier wire, long sent) {
        return sent > wire.wire() * TICKS_PER_SECOND;
    }

    /** What each of {@code sinks} sinks gets of {@code amount}, rounded up so nothing is left over by rounding. */
    public static long share(long amount, int sinks) {
        return sinks <= 0 ? 0 : (amount + sinks - 1) / sinks;
    }

    /** A Stash only gives to a Stash that's less full, so two of them settle instead of passing the same power back and forth. */
    public static boolean flows(double sourceFill, double sinkFill) {
        return sinkFill < sourceFill;
    }
}
