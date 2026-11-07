package com.malevolentgods.fourtwenty.domain.electronics;

/**
 * What a component shows of its circuit (docs/electronics/style-pass.md): packed into one int for the client. Bits 0 to 8 are its outputs
 * (a Nug's one, a chip's nine), bits 9 to 12 a level from 0 to 15 (heat, charge, current, an output's height), bit 13 whether it is
 * active (powered, or carrying current: its plinth's light line), bit 14 whether it is over its rating (smoke).
 */
public final class Shown {
    private Shown() {}

    public static final int LEVELS = 15;
    /** Currents below this show nothing; each decade above it is three levels, so 10 Ig is full. */
    public static final double QUIET_AMPS = 1e-4;

    public static int pack(int bits, int level, boolean active, boolean hot) {
        return (bits & 0x1FF) | (Math.max(0, Math.min(LEVELS, level)) << 9) | (active ? 1 << 13 : 0) | (hot ? 1 << 14 : 0);
    }

    public static int bits(int shown) { return shown & 0x1FF; }

    public static boolean bit(int shown, int k) { return (shown >> k & 1) != 0; }

    public static int level(int shown) { return shown >> 9 & 0xF; }

    public static boolean active(int shown) { return (shown >> 13 & 1) != 0; }

    public static boolean hot(int shown) { return (shown >> 14 & 1) != 0; }

    /** A share (0 to 1, clamped) as a level. */
    public static int share(double x) {
        if (!(x > 0)) return 0;
        return (int) Math.round(Math.min(1, x) * LEVELS);
    }

    /** A current on a log scale: nothing below {@link #QUIET_AMPS}, three levels a decade, full at 10 Ig. */
    public static int amps(double amps) {
        double a = Math.abs(amps);
        if (!(a > QUIET_AMPS)) return 0;
        return (int) Math.max(1, Math.min(LEVELS, Math.round(3 * Math.log10(a / QUIET_AMPS))));
    }

    /** Heat: the power burned as a share of the rating, two thirds of the way at the rating and full at one and a half times it. */
    public static int heat(double watts, double rated) {
        if (!(rated > 0) || !(watts > 0)) return 0;
        return share(watts / rated / 1.5);
    }
}
