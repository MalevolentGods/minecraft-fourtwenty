package com.malevolentgods.fourtwenty.domain;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Boundless Inspiration (BI), the mod's electricity. Power is in Willards (W), stored energy in Willard-seconds (Ws), and every amount here
 * is in Ws: a rate per tick times 20 is Willards. Everything is sized from a Hookah, which puts out 420 W: one charcoal and one cured bud
 * burn for a minute on every tier, and run four machines of that tier. A battery gives what a Hookah gives. A wire is rated for two Hookahs' current of its tier (CircuitRules). A block
 * fed from a higher tier than its own never breaks: it overloads and stops until the higher tier is gone.
 */
public final class BIRules {
    private BIRules() {}

    public static final int BURN_TICKS = 1200, MACHINES_PER_HOOKAH = 4, HOOKAHS_PER_WIRE = 2, TICKS_PER_SECOND = 20;

    public enum Tier implements StringRepresentable {
        PUFF(25_200), TOKE(50_400), RIP(100_800), BLAZE(201_600);

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

    /** Willards from Ws per tick. */
    public static double watts(double wsPerTick) { return wsPerTick * TICKS_PER_SECOND; }

    /** Whether power of the source's tier overloads a block of this tier. */
    public static boolean overloads(Tier part, Tier source) {
        return source.ordinal() > part.ordinal();
    }



}
