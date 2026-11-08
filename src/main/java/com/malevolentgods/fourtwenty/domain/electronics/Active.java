package com.malevolentgods.fourtwenty.domain.electronics;

/**
 * Active parts (docs/electronics/audit-and-plan.md, phase 3): parts with gain, whose current at one pair of pins is set by the voltage at
 * another. They stamp transconductances, so they need the general solver ({@link Stamp#transconductance}).
 */
public final class Active {
    private Active() {}

    /**
     * A MOSFET (the Sativa FET is N-channel, the Indica FET P-channel), the Shichman–Hodges square law, SPICE's level 1: off below its
     * threshold, a voltage-controlled resistor at low drain voltage, a current source in saturation, with channel-length modulation. Its
     * drain and source are the same silicon, so they swap when the current would run the other way. The gate is insulated.
     */
    public static final class Mosfet implements Part {
        /** Threshold, Kb; transconductance K, Ig per Kb²; channel-length modulation λ, per Kb. */
        public static final double VT = 1.0, K = 0.02, LAMBDA = 0.01;
        final int drain, gate, source;
        final boolean pChannel;
        final double vt, k, lambda;
        private double iLast, gmLast, gdsLast, vgsLast, vdsLast;
        private boolean swappedLast;

        Mosfet(int drain, int gate, int source, boolean pChannel, double vt, double k, double lambda) {
            this.drain = drain;
            this.gate = gate;
            this.source = source;
            this.pChannel = pChannel;
            this.vt = vt;
            this.k = k;
            this.lambda = lambda;
        }

        @Override public int[] nodes() { return new int[]{drain, source}; }

        @Override public boolean nonlinear() { return true; }

        /** The channel current for {@code vgs} and {@code vds} ≥ 0 (in the device's own sense), with its two slopes: {id, gm, gds}. */
        double[] channel(double vgs, double vds) {
            double vov = vgs - vt;
            if (vov <= 0) return new double[]{0, 0, 0};
            double m = 1 + lambda * vds;
            if (vds < vov) {
                double core = vov * vds - vds * vds / 2;
                return new double[]{k * core * m, k * vds * m, k * (vov - vds) * m + k * core * lambda};
            }
            return new double[]{k / 2 * vov * vov * m, k * vov * m, k / 2 * vov * vov * lambda};
        }

        /** The device's own voltages: N-channel measures from its source, P-channel from its source downward (vsg, vsd). */
        private double sign() { return pChannel ? -1 : 1; }

        @Override public void stamp(Stamp s, Analysis an) {
            double sg = sign();
            double vds = sg * an.across(drain, source);
            boolean swapped = vds < 0;
            // the pin acting as source is whichever end the current leaves from (N) or enters by (P)
            int d = swapped ? source : drain, src = swapped ? drain : source;
            double vgs = sg * an.across(gate, src), vdsEff = Math.abs(vds);
            double[] c = channel(vgs, vdsEff);
            double i = c[0], gm = c[1], gds = c[2] + 1e-12;
            iLast = i;
            gmLast = gm;
            gdsLast = gds;
            vgsLast = vgs;
            vdsLast = vdsEff;
            swappedLast = swapped;
            // the channel current runs d → src (N) or src → d (P): i ≈ i0 + gm·Δvgs + gds·Δvds, in the device's own sense
            int from = pChannel ? src : d, to = pChannel ? d : src;
            s.conductance(from, to, gds);
            if (pChannel) s.transconductance(from, to, src, gate, gm);
            else s.transconductance(from, to, gate, src, gm);
            s.current(from, to, i - gm * vgs - gds * vdsEff);
        }

        @Override public boolean converged(Analysis an) {
            double sg = sign(), vds = sg * an.across(drain, source);
            if ((vds < 0) != swappedLast) return false;
            int src = swappedLast ? drain : source;
            double vgs = sg * an.across(gate, src), vdsEff = Math.abs(vds);
            double predicted = iLast + gmLast * (vgs - vgsLast) + gdsLast * (vdsEff - vdsLast), actual = channel(vgs, vdsEff)[0];
            return Math.abs(actual - predicted) <= an.reltol() * Math.max(Math.abs(actual), Math.abs(predicted)) + Parts.ABSTOL;
        }

        /** Like SPICE's fetlim: a gate voltage may move at most a couple of kerberos past the threshold in one step. */
        @Override public double allowStep(Analysis an, double[] next) {
            double from = an.across(gate, source), to = at(next, gate) - at(next, source), delta = Math.abs(to - from);
            double most = Math.max(2.0, 0.5 * Math.abs(from) + 1);
            return delta > most ? most / delta : 1;
        }

        private static double at(double[] v, int node) { return node == Stamp.GROUND ? 0 : v[node]; }

        /** The current into its drain (out of it, for a P-channel device's usual direction, it is negative). */
        @Override public double current(Analysis an) {
            double sg = sign(), vds = sg * an.across(drain, source);
            boolean swapped = vds < 0;
            int src = swapped ? drain : source;
            double i = channel(sg * an.across(gate, src), Math.abs(vds))[0];
            return sg * (swapped ? -i : i);
        }
    }

    /**
     * An op-amp, the Hype Amp: a gain of 100,000 on the difference of its inputs, its output limited smoothly just inside its supply
     * (tanh), through an output resistance; high-impedance inputs; with too little supply its output floats. Supply and inputs are
     * referred to its GND pin.
     */
    public static final class OpAmp implements Part {
        public static final double GAIN = 1e5, R_OUT = 100, R_IN = 1e9, MARGIN = 0.02;
        final int vdd, gnd, out, plus, minus;
        private double vrelLast, slopeLast, vdLast;

        OpAmp(int vdd, int gnd, int out, int plus, int minus) {
            this.vdd = vdd;
            this.gnd = gnd;
            this.out = out;
            this.plus = plus;
            this.minus = minus;
        }

        @Override public int[] nodes() { return new int[]{vdd, gnd}; }

        @Override public int[] joined() { return new int[]{vdd, gnd, out, plus, minus}; }

        @Override public boolean nonlinear() { return true; }

        /** Where it drives its output, above its GND pin, for the input difference {@code vd}; and its slope there. */
        double[] target(double supply, double vd) {
            double half = supply / 2 * (1 - 2 * MARGIN), mid = supply / 2;
            double x = GAIN * vd / Math.max(half, 1e-9);
            double t = Math.tanh(x);
            return new double[]{mid + half * t, GAIN * (1 - t * t)};
        }

        @Override public void stamp(Stamp s, Analysis an) {
            s.conductance(plus, gnd, 1 / R_IN);
            s.conductance(minus, gnd, 1 / R_IN);
            s.conductance(vdd, gnd, 1 / Logic.R_QUIESCENT);
            double supply = an.across(vdd, gnd);
            if (supply < Logic.MIN_SUPPLY) {
                s.conductance(out, gnd, 1 / Logic.R_OFF);
                vrelLast = Double.NaN;
                return;
            }
            double vd = an.across(plus, minus);
            double[] t = target(supply, vd);
            vrelLast = t[0];
            slopeLast = t[1];
            vdLast = vd;
            double g = 1 / R_OUT;
            // current into its output: g·(vrel − (v_out − v_gnd)), with vrel ≈ vrel0 + slope·(vd − vd0)
            s.conductance(out, gnd, g);
            s.transconductance(gnd, out, plus, minus, g * t[1]);
            s.current(gnd, out, g * (t[0] - t[1] * vd));
        }

        @Override public boolean converged(Analysis an) {
            double supply = an.across(vdd, gnd);
            if (supply < Logic.MIN_SUPPLY) return Double.isNaN(vrelLast);
            if (Double.isNaN(vrelLast)) return false;
            double vd = an.across(plus, minus), actual = target(supply, vd)[0], predicted = vrelLast + slopeLast * (vd - vdLast);
            return Math.abs(actual - predicted) <= an.reltol() * Math.max(Math.abs(actual), supply) + Parts.VNTOL * 1e-3;
        }

        /**
         * Saturated, its slope is nearly zero, so a plain Newton step throws the output from rail to rail and back for ever. Like
         * pnjlim: the input difference may leave saturation only as far as the edge of the linear region, where the next step sees
         * the full gain.
         */
        @Override public double allowStep(Analysis an, double[] next) {
            double supply = an.across(vdd, gnd);
            if (supply < Logic.MIN_SUPPLY) return 1;
            double edge = supply / 2 * (1 - 2 * MARGIN) / GAIN;
            double from = an.across(plus, minus), to = (at(next, plus) - at(next, minus));
            if (Math.abs(to) <= edge || Math.abs(from) <= edge && Math.abs(to - from) <= 4 * edge) return 1;
            if (Math.signum(from) == Math.signum(to) && Math.abs(to) <= Math.abs(from)) return 1;
            double target = Math.signum(to) * edge;
            return Math.max(1e-6, Math.abs((target - from) / (to - from)));
        }

        private static double at(double[] v, int node) { return node == Stamp.GROUND ? 0 : v[node]; }

        /** The current it sources from its output (into the circuit). */
        @Override public double current(Analysis an) {
            double supply = an.across(vdd, gnd);
            if (supply < Logic.MIN_SUPPLY) return 0;
            return (target(supply, an.across(plus, minus))[0] - an.across(out, gnd)) / R_OUT;
        }
    }

    /**
     * A current regulator, the Slow Burner, as a current-regulator diode: it lets through its set current, whatever the voltage across
     * it once there's enough (the current eases in as tanh(v / {@link #KNEE}), 96% of it at 1 Kb); either way round, so a capacitor fills
     * at a steady rate through it one way and drains steadily the other. Not a source: its current always runs the way the voltage across
     * it pushes, so it only ever takes power, never gives it.
     */
    public static final class CurrentRegulator implements Part {
        public static final double KNEE = 0.5;
        final int a, b;
        final double amps;
        private double vLast, iLast, gLast;

        CurrentRegulator(int a, int b, double amps) {
            this.a = a;
            this.b = b;
            this.amps = Math.abs(amps);
        }

        @Override public int[] nodes() { return new int[]{a, b}; }

        @Override public boolean nonlinear() { return true; }

        double[] at(double v) {
            double t = Math.tanh(v / KNEE);
            return new double[]{amps * t, amps / KNEE * (1 - t * t) + 1e-12};
        }

        @Override public void stamp(Stamp s, Analysis an) {
            double v = an.across(a, b);
            double[] c = at(v);
            vLast = v;
            iLast = c[0];
            gLast = c[1];
            s.conductance(a, b, c[1]);
            s.current(a, b, c[0] - c[1] * v);
        }

        @Override public boolean converged(Analysis an) {
            double v = an.across(a, b), actual = at(v)[0], predicted = iLast + gLast * (v - vLast);
            return Math.abs(actual - predicted) <= an.reltol() * Math.max(Math.abs(actual), amps * 1e-3) + Parts.ABSTOL;
        }

        /** Like pnjlim: the voltage across it may move at most a couple of knees past where it was in one step, so tanh's flat ends don't throw Newton. */
        @Override public double allowStep(Analysis an, double[] next) {
            double from = an.across(a, b), to = (a == Stamp.GROUND ? 0 : next[a]) - (b == Stamp.GROUND ? 0 : next[b]);
            double most = 4 * KNEE + Math.abs(from);
            return Math.abs(to - from) > most ? most / Math.abs(to - from) : 1;
        }

        @Override public double current(Analysis an) { return at(an.across(a, b))[0]; }
    }
}
