package com.malevolentgods.fourtwenty.domain;

/**
 * Boundless Inspiration as a real DC circuit. Voltage is in Kerberos (Kb), current in Ignus (Ig), resistance in Phus (Φ), power in
 * Willards (W). Each tier is a voltage class, and a tier's base is its voltage and its Hookah's power: one Hookah's current is the base
 * current, V²/P the base resistance. Every number here is in per unit (pu) of those bases, so one rule set covers all four tiers. See
 * specs/006-circuit-solver/research.md.
 */
public final class CircuitRules {
    private CircuitRules() {}

    /** The tunable numbers; the config builds these. */
    public record Numbers(double voltsPuff, double voltsToke, double voltsRip, double voltsBlaze,
                          double sourceDroop, double sourceLimit,
                          double stashEmfLow, double stashEmfHigh, double stashResistance, double stashLimit,
                          double loadMinVolts, double loadHysteresis,
                          double wickOhmsPerBlock, double wickAmpRating, double wickVoltRating, int burnTicks,
                          double overloadMargin, double hitBoxLoss) {
        public static final Numbers DEFAULT = new Numbers(4.2, 42, 420, 4_200,
                0.005, 1.05,
                0.96, 0.99, 0.01, 1.0,
                1.0, 0.10,
                1.5e-4, 2.0, 1.5, 20,
                1.5, 0.02);

        /** The tier's nominal voltage. */
        public double volts(BIRules.Tier tier) {
            return switch (tier) {
                case PUFF -> voltsPuff;
                case TOKE -> voltsToke;
                case RIP -> voltsRip;
                case BLAZE -> voltsBlaze;
            };
        }

        /** One Hookah's current at this tier. */
        public double baseAmps(BIRules.Tier tier) { return hookahWatts(tier) / volts(tier); }

        /** The tier's base resistance, V² / P. */
        public double baseOhms(BIRules.Tier tier) { return volts(tier) * volts(tier) / hookahWatts(tier); }

        /** A Hookah's EMF and its internal resistance, and the most current it gives. */
        public double sourceOhms(BIRules.Tier tier) { return sourceDroop * baseOhms(tier); }

        public double sourceAmps(BIRules.Tier tier) { return sourceLimit * baseAmps(tier); }

        /** A Stash's EMF, which follows how full it is. */
        public double stashEmf(BIRules.Tier tier, double fill) {
            double f = Math.max(0, Math.min(1, fill));
            return (stashEmfLow + (stashEmfHigh - stashEmfLow) * f) * volts(tier);
        }

        public double stashOhms(BIRules.Tier tier) { return stashResistance * baseOhms(tier); }

        public double stashAmps(BIRules.Tier tier) { return stashLimit * baseAmps(tier); }

        /** A Wick block's resistance, current rating and insulation rating. */
        public double wickOhms(BIRules.Tier tier) { return wickOhmsPerBlock * baseOhms(tier); }

        public double wickAmps(BIRules.Tier tier) { return wickAmpRating * baseAmps(tier); }

        public double wickVolts(BIRules.Tier tier) { return wickVoltRating * volts(tier); }

        /** Above this a block of the tier overloads. */
        public double overloadVolts(BIRules.Tier tier) { return overloadMargin * volts(tier); }

        /** A machine of this tier holds its power from this voltage up; below it, it is the resistance it has there (R = V² / P). */
        public double minVolts(BIRules.Tier tier) { return loadMinVolts * volts(tier); }
    }

    /** A Hookah's power at full current, in Willards: 420 W at Puff. */
    public static double hookahWatts(BIRules.Tier tier) { return BIRules.watts(tier.rate()); }

    /** A machine's rated draw in Willards from its cost in Ws per tick. */
    public static double ratedWatts(double wsPerTick) { return BIRules.watts(wsPerTick); }

    /** Whether a load with this much of its buffer full asks for power again: once it's used the hysteresis share, until it's full. */
    public static boolean asks(boolean asking, double fill, double hysteresis) {
        if (fill >= 1) return false;
        return asking || fill <= 1 - hysteresis;
    }

    /** Whether a Wick that has been over a rating for {@code ticks} burns out. */
    public static boolean burns(int ticks, int burnTicks) { return ticks >= burnTicks; }
}
