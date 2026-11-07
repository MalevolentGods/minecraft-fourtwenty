package com.malevolentgods.fourtwenty.domain.electronics;

/**
 * The components: each a stamp of its companion model (docs/electronics/audit-and-plan.md part 2). Currents are in Ignus, voltages in
 * Kerberos, resistance in Phus, capacitance in Kb·s per Ig (our farad) and inductance in Φ·s (our henry), all SI-shaped.
 */
public final class Parts {
    private Parts() {}

    /**
     * The stiffest a part may be: 1 µΦ. A node's matrix entry holds the sum of its conductances in one double, good to 1e-16 of the
     * largest, so the error in the smallest is about 1e-16 × (largest / smallest). Keeping "ideal" parts (sources, closed switches, an
     * inductor in DC) to 1e6 against a megaphu input bounds that at 1e-4 of the input's current and 1e-7 for anything a kilophu or
     * less, far inside SPICE's own tolerance (reltol 1e-3). SPICE gives ideal voltage sources rows of their own for the same reason.
     */
    static final double MIN_OHMS = 1e-6, SHORT = 1 / MIN_OHMS;
    /** SPICE's tolerances: relative, absolute voltage, absolute current. */
    static final double RELTOL = 1e-3, VNTOL = 1e-6, ABSTOL = 1e-12;

    /** A resistance R between a and b. */
    public record Resistor(int a, int b, double ohms) implements Part {
        public Resistor { ohms = Math.max(ohms, MIN_OHMS); }

        @Override public int[] nodes() { return new int[]{a, b}; }

        @Override public void stamp(Stamp s, Analysis an) { s.conductance(a, b, 1 / ohms); }

        @Override public double current(Analysis an) { return an.across(a, b) / ohms; }
    }

    /** A voltage source: an EMF from minus to plus behind a small internal resistance (its Norton equivalent). */
    public record Source(int plus, int minus, double emf, double ohms) implements Part {
        public Source { ohms = Math.max(ohms, MIN_OHMS); }

        @Override public int[] nodes() { return new int[]{plus, minus}; }

        @Override public void stamp(Stamp s, Analysis an) {
            s.conductance(plus, minus, 1 / ohms);
            s.current(minus, plus, an.sourceScale() * emf / ohms);
        }

        /** Current out of its + terminal. */
        @Override public double current(Analysis an) { return (an.sourceScale() * emf - an.across(plus, minus)) / ohms; }
    }

    /** A current source, pushing {@code amps} from a through itself to b. */
    public record CurrentSource(int a, int b, double amps) implements Part {
        @Override public int[] nodes() { return new int[]{a, b}; }

        @Override public void stamp(Stamp s, Analysis an) { s.current(a, b, an.sourceScale() * amps); }

        @Override public double current(Analysis an) { return an.sourceScale() * amps; }
    }

    /** A switch, lever or button: a tiny resistance closed, a leak of a few teraphus open. */
    public static final class Switch implements Part {
        static final double CLOSED = MIN_OHMS, OPEN = 1e12;
        final int a, b;
        boolean closed;

        Switch(int a, int b, boolean closed) {
            this.a = a;
            this.b = b;
            this.closed = closed;
        }

        @Override public int[] nodes() { return new int[]{a, b}; }

        @Override public boolean conducts(Analysis an) { return closed; }

        @Override public void stamp(Stamp s, Analysis an) { s.conductance(a, b, 1 / (closed ? CLOSED : OPEN)); }

        @Override public double current(Analysis an) { return an.across(a, b) / (closed ? CLOSED : OPEN); }

        public boolean closed() { return closed; }
    }

    /** A capacitor: open in DC; in time, i = C·dv/dt by backward Euler or the trapezoidal rule. */
    public static final class Capacitor implements Part {
        final int a, b;
        final double farads;
        double vPrev, iPrev;

        Capacitor(int a, int b, double farads, double v0) {
            this.a = a;
            this.b = b;
            this.farads = Math.max(farads, 0);
            this.vPrev = v0;
        }

        @Override public int[] nodes() { return new int[]{a, b}; }

        @Override public boolean conducts(Analysis an) { return an.mode() != Analysis.Mode.DC && farads > 0; }

        private double geq(Analysis an) { return an.method() == Analysis.Method.TRAPEZOIDAL ? 2 * farads / an.h() : farads / an.h(); }

        private double history(Analysis an) { return an.method() == Analysis.Method.TRAPEZOIDAL ? geq(an) * vPrev + iPrev : geq(an) * vPrev; }

        @Override public void stamp(Stamp s, Analysis an) {
            if (an.mode() == Analysis.Mode.DC || farads == 0) return;
            s.conductance(a, b, geq(an));
            // i = geq·v − history: the history is a current pushed back from b to a
            s.current(b, a, history(an));
        }

        @Override public double current(Analysis an) {
            if (an.mode() == Analysis.Mode.DC || farads == 0) return 0;
            return geq(an) * an.across(a, b) - history(an);
        }

        @Override public void accept(Analysis an) {
            iPrev = current(an);
            vPrev = an.across(a, b);
        }

        public double volts() { return vPrev; }
    }

    /** An inductor: a short in DC; in time, v = L·di/dt by backward Euler or the trapezoidal rule. */
    public static final class Inductor implements Part {
        final int a, b;
        final double henries;
        double iPrev, vPrev;

        Inductor(int a, int b, double henries, double i0) {
            this.a = a;
            this.b = b;
            this.henries = Math.max(henries, 1e-15);
            this.iPrev = i0;
        }

        @Override public int[] nodes() { return new int[]{a, b}; }

        private double geq(Analysis an) { return an.method() == Analysis.Method.TRAPEZOIDAL ? an.h() / (2 * henries) : an.h() / henries; }

        private double history(Analysis an) { return an.method() == Analysis.Method.TRAPEZOIDAL ? iPrev + geq(an) * vPrev : iPrev; }

        @Override public void stamp(Stamp s, Analysis an) {
            if (an.mode() == Analysis.Mode.DC) {
                s.conductance(a, b, SHORT);
                return;
            }
            s.conductance(a, b, geq(an));
            s.current(a, b, history(an));
        }

        @Override public double current(Analysis an) {
            if (an.mode() == Analysis.Mode.DC) return SHORT * an.across(a, b);
            return geq(an) * an.across(a, b) + history(an);
        }

        @Override public void accept(Analysis an) {
            iPrev = current(an);
            vPrev = an.across(a, b);
        }

        public double amps() { return iPrev; }
    }

    /**
     * A diode's parameters: saturation current, emission coefficient, and reverse breakdown voltage (0 for none) with the current that
     * flows at it (SPICE's BV and IBV).
     */
    public record DiodeModel(double saturation, double emission, double breakdown, double breakdownCurrent) {
        /** A silicon signal diode: about 0.65 Kb at a few mIg. */
        public static final DiodeModel SILICON = new DiodeModel(1e-14, 1.0, 0, 0);

        /** A Zener of breakdown {@code volts}, 1 mIg at the knee. */
        public static DiodeModel zener(double volts) { return new DiodeModel(1e-14, 1.0, volts, 1e-3); }

        /** An LED, by its forward voltage at about 10 mIg. */
        public static DiodeModel led(double forward) { return new DiodeModel(0.01 / Math.expm1(forward / (2.0 * THERMAL)), 2.0, 0, 0); }
    }

    /** kT/q at room temperature, in Kb. */
    public static final double THERMAL = 0.025852;

    /** A junction diode, anode a to cathode k: the Shockley equation, a reverse breakdown knee, Newton limiting (pnjlim). */
    public static final class Diode implements Part {
        final int a, k;
        final DiodeModel m;
        private final double nvt, vcrit, vcritReverse;
        private double vLast, iLast, gLast;
        private boolean started;

        Diode(int a, int k, DiodeModel m) {
            this.a = a;
            this.k = k;
            this.m = m;
            nvt = m.emission() * THERMAL;
            vcrit = nvt * Math.log(nvt / (Math.sqrt(2) * m.saturation()));
            vcritReverse = m.breakdown() > 0 ? nvt * Math.log(nvt / (Math.sqrt(2) * m.breakdownCurrent())) : 0;
        }

        @Override public int[] nodes() { return new int[]{a, k}; }

        @Override public boolean nonlinear() { return true; }

        private double amps(double v) {
            double i = m.saturation() * Math.expm1(v / nvt);
            if (m.breakdown() > 0) i -= m.breakdownCurrent() * Math.exp((-v - m.breakdown()) / nvt);
            return i;
        }

        private double slope(double v) {
            double g = m.saturation() / nvt * Math.exp(v / nvt);
            if (m.breakdown() > 0) g += m.breakdownCurrent() / nvt * Math.exp((-v - m.breakdown()) / nvt);
            return g + 1e-12;
        }

        /** SPICE's pnjlim: past the critical voltage, a junction may move only logarithmically in one step. */
        private double limit(double vNew, double vOld) {
            if (vNew > vcrit && Math.abs(vNew - vOld) > 2 * nvt) {
                if (vOld > 0) {
                    double arg = 1 + (vNew - vOld) / nvt;
                    return arg > 0 ? vOld + nvt * Math.log(arg) : vcrit;
                }
                return nvt * Math.log(vNew / nvt);
            }
            if (m.breakdown() > 0) {
                // the breakdown knee is a junction too, mirrored: limit its voltage past the knee the same way
                double past = -vNew - m.breakdown(), before = -vOld - m.breakdown();
                if (past > vcritReverse && Math.abs(past - before) > 2 * nvt) {
                    double limited = before > 0 ? (1 + (past - before) / nvt > 0 ? before + nvt * Math.log(1 + (past - before) / nvt) : vcritReverse)
                            : nvt * Math.log(past / nvt);
                    return -m.breakdown() - limited;
                }
            }
            return vNew;
        }

        @Override public void stamp(Stamp s, Analysis an) {
            double v = an.across(a, k);
            v = started ? limit(v, vLast) : Math.min(v, vcrit);
            started = true;
            double i = amps(v), g = slope(v);
            vLast = v;
            iLast = i;
            gLast = g;
            s.conductance(a, k, g);
            // i = g·v + (i0 − g·v0): the constant part is a current from a to k
            s.current(a, k, i - g * v);
        }

        @Override public boolean converged(Analysis an) {
            double v = an.across(a, k);
            double predicted = iLast + gLast * (v - vLast), actual = amps(v);
            return Math.abs(actual - predicted) <= RELTOL * Math.max(Math.abs(actual), Math.abs(predicted)) + ABSTOL;
        }

        @Override public double current(Analysis an) { return amps(an.across(a, k)); }
    }

    /**
     * A transformer (the Hit Box): primary a1–b1, secondary a2–b2, turns ratio n = V1/V2, referred through its winding resistance R:
     * I1 = (V1 − n·V2)/R into the primary, and n times that out of the secondary. Symmetric, so the system stays positive definite.
     */
    public record Transformer(int a1, int b1, int a2, int b2, double ratio, double ohms) implements Part {
        @Override public int[] nodes() { return new int[]{a1, b1}; }

        @Override public void stamp(Stamp s, Analysis an) {
            double g = 1 / Math.max(ohms, MIN_OHMS);
            s.coupling(a1, b1, a2, b2, g, -ratio * g, ratio * ratio * g);
        }

        /** Current into the primary. */
        @Override public double current(Analysis an) { return (an.across(a1, b1) - ratio * an.across(a2, b2)) / Math.max(ohms, MIN_OHMS); }

        /** Current out of the secondary's + terminal. */
        public double secondary(Analysis an) { return ratio * current(an); }
    }
}
