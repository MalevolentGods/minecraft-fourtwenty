package com.malevolentgods.fourtwenty.domain;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;
import java.util.Locale;

/** Boundless Inspiration, shown as "Inspo": power tiers and overload. Too much for a part burns it out in smoke; it never explodes. */
public final class BIRules {
    private BIRules() {}

    public enum Tier implements StringRepresentable {
        PUFF(32), TOKE(128), RIP(512);

        private final int rate;

        Tier(int rate) { this.rate = rate; }

        /** The most BI per tick this tier moves. */
        public int rate() { return rate; }

        /** The tier below; Puff is the lowest. */
        public Tier below() { return this == PUFF ? PUFF : values()[ordinal() - 1]; }

        @Override public @Nonnull String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    /** Whether power of the source's tier burns out a part of this tier. */
    public static boolean overloads(Tier part, Tier source) {
        return source.ordinal() > part.ordinal();
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
