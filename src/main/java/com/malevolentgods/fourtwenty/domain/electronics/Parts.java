package com.malevolentgods.fourtwenty.domain.electronics;

/**
 * The components: each a stamp of its companion model (docs/electronics/audit-and-plan.md part 2). Currents are in Ignus, voltages in
 * Kerberos, resistance in Phus, capacitance in Garcias (Ga, Ig·s per Kb) and inductance in Marleys (Ma, Kb·s per Ig), all SI-shaped.
 */
public final class Parts {
    private Parts() {}

    /**
     * The stiffest a part should be: 1 µΦ (a switch closes to this, an inductor shorts to it in DC). A node's matrix entry holds the sum of its conductances in one double, good to 1e-16 of the
     * largest, so the error in the smallest is about 1e-16 × (largest / smallest). Keeping "ideal" parts (sources, closed switches, an
     * inductor in DC) to 1e6 against a megaphu input bounds that at 1e-4 of the input's current and 1e-7 for anything a kilophu or
     * less, far inside SPICE's own tolerance (reltol 1e-3). SPICE gives ideal voltage sources rows of their own for the same reason.
     */
    static final double MIN_OHMS = 1e-6, SHORT = 1 / MIN_OHMS;
    /** SPICE's tolerances: relative, absolute voltage, absolute current. */
    static final double RELTOL = 1e-3, VNTOL = 1e-6, ABSTOL = 1e-12;

    /** A resistance R between a and b. */
    public record Resistor(int a, int b, double ohms) implements Part {
        public Resistor { ohms = Math.max(ohms, 1e-15); }

        @Override public int[] nodes() { return new int[]{a, b}; }

        @Override public void stamp(Stamp s, Analysis an) { s.conductance(a, b, 1 / ohms); }

        @Override public double current(Analysis an) { return an.across(a, b) / ohms; }
    }

    /** A voltage source: an EMF from minus to plus behind a small internal resistance (its Norton equivalent). */
    public record Source(int plus, int minus, double emf, double ohms) implements Part {
        public Source { ohms = Math.max(ohms, 1e-15); }

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

    /**
     * A source with a current limit both ways (a Hookah, a Stash, a bench supply): an EMF behind a resistance, giving at most
     * {@code maxOut} out of its + terminal and taking at most {@code maxIn} into it; past a limit it holds that current, with a soft
     * slope so no node ever floats. Piecewise linear: Newton finds the right piece, stepping only to the edge of a piece it would jump
     * past (as SPICE limits a junction).
     */
    public static final class LimitedSource implements Part {
        static final double STIFF = 1e5;
        final int plus, minus;
        final double emf, ohms, maxOut, maxIn;
        private double vLast, iLast, gLast;
        private boolean started;

        LimitedSource(int plus, int minus, double emf, double ohms, double maxOut, double maxIn) {
            this.plus = plus;
            this.minus = minus;
            this.emf = emf;
            this.ohms = Math.max(ohms, 1e-15);
            this.maxOut = Math.max(0, maxOut);
            this.maxIn = Math.max(0, maxIn);
        }

        @Override public int[] nodes() { return new int[]{plus, minus}; }

        @Override public boolean nonlinear() { return true; }

        private double outEdge(double k) { return k * emf - k * maxOut * ohms; }

        private double inEdge(double k) { return k * emf + k * maxIn * ohms; }

        /** Current out of its + terminal at {@code v} across it. */
        double amps(double v, double k) {
            double soft = 1 / (ohms * STIFF);
            if (v < outEdge(k)) return k * maxOut + soft * (outEdge(k) - v);
            if (v > inEdge(k)) return -k * maxIn + soft * (inEdge(k) - v);
            return (k * emf - v) / ohms;
        }

        private double conductance(double v, double k) { return v < outEdge(k) || v > inEdge(k) ? 1 / (ohms * STIFF) : 1 / ohms; }

        @Override public void stamp(Stamp s, Analysis an) {
            double k = an.sourceScale(), v = an.across(plus, minus);
            if (started) {
                // stop just past the first edge between the last voltage and this one: the piece changes there
                for (double edge : new double[]{outEdge(k), inEdge(k)}) {
                    if ((vLast - edge) * (v - edge) < 0) v = edge + Math.signum(v - edge) * 1e-9 * Math.max(1, Math.abs(edge));
                }
            }
            started = true;
            double g = conductance(v, k), i = amps(v, k);
            vLast = v;
            iLast = i;
            gLast = g;
            s.conductance(plus, minus, g);
            // out of + : i(v0) - g(v - v0), so a current i + g v0 pushed from minus to plus
            s.current(minus, plus, i + g * v);
        }

        @Override public boolean converged(Analysis an) {
            double v = an.across(plus, minus), predicted = iLast - gLast * (v - vLast), actual = amps(v, an.sourceScale());
            return Math.abs(actual - predicted) <= an.reltol() * Math.max(Math.abs(actual), Math.abs(predicted)) + ABSTOL;
        }

        @Override public double current(Analysis an) { return amps(an.across(plus, minus), an.sourceScale()); }

        public double ohms() { return ohms; }
    }

    /**
     * A machine: from its rated voltage up it holds a constant power, like a regulated supply; below it, it is the resistance it has
     * there; reversed it draws nothing. A constant power's slope is negative, which would break the matrix's symmetry, so it is stamped as
     * the resistance it has at the last voltage (V squared over P), a relaxation that always moves the line the way a real one settles.
     */
    public static final class PowerLoad implements Part {
        final int plus, minus;
        final double watts, minVolts;
        double share = 1;
        private double gLast;

        PowerLoad(int plus, int minus, double watts, double minVolts) {
            this.plus = plus;
            this.minus = minus;
            this.watts = Math.max(0, watts);
            this.minVolts = Math.max(minVolts, 1e-9);
        }

        @Override public int[] nodes() { return new int[]{plus, minus}; }

        @Override public boolean nonlinear() { return true; }

        public void share(double share) { this.share = share; }

        /** What it draws at {@code v} across it. */
        public double wattsAt(double v) {
            if (v <= 0) return 0;
            double want = watts * share;
            return v >= minVolts ? want : v * v * want / (minVolts * minVolts);
        }

        public double ampsAt(double v) { return v <= 0 ? 0 : wattsAt(v) / v; }

        @Override public void stamp(Stamp s, Analysis an) {
            double v = Math.max(an.across(plus, minus), minVolts);
            // with no share yet it is still a whisper of a conductance, so the loops through it stay joined
            gLast = Math.max(watts * share, 1e-6 * watts) / (v * v);
            s.conductance(plus, minus, gLast);
        }

        @Override public boolean converged(Analysis an) {
            double v = an.across(plus, minus), actual = ampsAt(v), stamped = gLast * Math.max(v, 0);
            return Math.abs(actual - stamped) <= an.reltol() * Math.max(Math.abs(actual), Math.abs(stamped)) + ABSTOL;
        }

        @Override public double current(Analysis an) { return ampsAt(an.across(plus, minus)); }
    }

    /** An LED behind a constant-current driver (a Lit Bit): dark below {@code forward}, rising to {@code setAmps} at {@code rated}, held above. */
    public static final class DriverLed implements Part {
        final int plus, minus;
        final double setAmps, forward, rated;
        private double gLast;

        DriverLed(int plus, int minus, double setAmps, double forward, double rated) {
            this.plus = plus;
            this.minus = minus;
            this.setAmps = Math.max(0, setAmps);
            this.forward = Math.max(0, forward);
            this.rated = Math.max(this.forward, rated);
        }

        @Override public int[] nodes() { return new int[]{plus, minus}; }

        @Override public boolean nonlinear() { return true; }

        public double ampsAt(double v) {
            if (v <= forward) return 0;
            return setAmps * (rated <= forward ? 1 : Math.min(1, (v - forward) / (rated - forward)));
        }

        @Override public void stamp(Stamp s, Analysis an) {
            double v = Math.max(an.across(plus, minus), 1e-9), scale = Math.max(rated, 1e-9);
            gLast = Math.max(ampsAt(v) / v, 1e-6 * Math.max(setAmps, 1e-9) / scale);
            s.conductance(plus, minus, gLast);
        }

        @Override public boolean converged(Analysis an) {
            double v = an.across(plus, minus), actual = ampsAt(v), stamped = gLast * Math.max(v, 0);
            return Math.abs(actual - stamped) <= an.reltol() * Math.max(Math.abs(actual), setAmps) + 1e-9 * setAmps + ABSTOL;
        }

        @Override public double current(Analysis an) { return ampsAt(an.across(plus, minus)); }
    }

    /**
     * A Lit Bit as a real RGB LED: each channel an LED (the Shockley equation) behind its current-setting resistor, its colour a duty
     * cycle (PWM) on each channel, as real RGB strips are driven. The resistor is sized so one channel at full duty draws
     * {@code channelAmps} at {@code rated} volts; below the LED's forward voltage it's dark, and as the supply sags it dims along the
     * diode's knee. Its slope is positive, so it stamps as a Newton companion and the matrix stays symmetric.
     */
    public static final class RgbLed implements Part {
        final int plus, minus;
        /** The sum of the three channels' duties (0 to 3): the share of a channel's current the lamp draws in all. */
        final double duty;
        final double saturation, nvt, series;
        private double vLast, iLast, gLast;

        RgbLed(int plus, int minus, double duty, double forward, double rated, double channelAmps) {
            this.plus = plus;
            this.minus = minus;
            this.duty = Math.max(0, duty);
            DiodeModel m = DiodeModel.led(forward);
            this.saturation = m.saturation();
            this.nvt = m.emission() * THERMAL;
            double drop = nvt * Math.log(channelAmps / saturation + 1);
            this.series = Math.max((rated - drop) / Math.max(channelAmps, 1e-12), 1e-6);
        }

        @Override public int[] nodes() { return new int[]{plus, minus}; }

        @Override public boolean nonlinear() { return true; }

        /** One channel at full duty: the current that solves the diode with its resistor, i = Is(e^((v − iR)/nVt) − 1). */
        double channel(double v) {
            if (v <= 0) return saturation * Math.expm1(v / nvt);
            double lo = 0, hi = v / series;
            for (int k = 0; k < 80; k++) {
                double i = (lo + hi) / 2;
                if (i - saturation * Math.expm1((v - i * series) / nvt) > 0) hi = i; else lo = i;
            }
            return (lo + hi) / 2;
        }

        /** The channel's slope at {@code v}: the diode's conductance in series with its resistor. */
        double slope(double v) {
            double i = Math.max(channel(v), 0);
            double gd = (i + saturation) / nvt;
            return gd / (1 + gd * series) + 1e-12;
        }

        public double ampsAt(double v) { return duty * channel(v); }

        @Override public void stamp(Stamp s, Analysis an) {
            double v = an.across(plus, minus);
            double i = ampsAt(v), g = duty * slope(v) + 1e-12;
            vLast = v;
            iLast = i;
            gLast = g;
            s.conductance(plus, minus, g);
            s.current(plus, minus, i - g * v);
        }

        @Override public boolean converged(Analysis an) {
            double v = an.across(plus, minus), predicted = iLast + gLast * (v - vLast), actual = ampsAt(v);
            return Math.abs(actual - predicted) <= an.reltol() * Math.max(Math.abs(actual), Math.abs(predicted)) + ABSTOL;
        }

        @Override public double current(Analysis an) { return ampsAt(an.across(plus, minus)); }
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

        @Override public double[] save(double now) { return new double[]{vPrev, iPrev}; }

        @Override public void load(double[] state, double now) {
            if (state.length >= 2) {
                vPrev = state[0];
                iPrev = state[1];
            }
        }
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

        @Override public double[] save(double now) { return new double[]{iPrev, vPrev}; }

        @Override public void load(double[] state, double now) {
            if (state.length >= 2) {
                iPrev = state[0];
                vPrev = state[1];
            }
        }
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
            return Math.abs(actual - predicted) <= an.reltol() * Math.max(Math.abs(actual), Math.abs(predicted)) + ABSTOL;
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
