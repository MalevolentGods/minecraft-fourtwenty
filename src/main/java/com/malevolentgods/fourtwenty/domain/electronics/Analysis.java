package com.malevolentgods.fourtwenty.domain.electronics;

/** What a part needs to stamp itself: the voltages now, and whether this is a DC operating point or a time step (and how long). */
public final class Analysis {
    public enum Mode { DC, TRANSIENT }

    /** Backward Euler (first step after a change: damps nothing it shouldn't, rings never) or trapezoidal (accurate, no numerical damping). */
    public enum Method { EULER, TRAPEZOIDAL }

    Mode mode = Mode.DC;
    Method method = Method.EULER;
    double h;
    /** Every source is scaled by this (source stepping); 1 normally. */
    double sourceScale = 1;
    double[] v;

    Analysis(int nodes) { v = new double[nodes]; }

    public Mode mode() { return mode; }

    public Method method() { return method; }

    /** The time step, in seconds. */
    public double h() { return h; }

    public double sourceScale() { return sourceScale; }

    /** A node's voltage (0 for {@link Stamp#GROUND}). */
    public double volts(int node) { return node == Stamp.GROUND ? 0 : v[node]; }

    /** The voltage of {@code a} above {@code b}. */
    public double across(int a, int b) { return volts(a) - volts(b); }
}
