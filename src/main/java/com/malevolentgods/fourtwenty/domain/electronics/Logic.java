package com.malevolentgods.fourtwenty.domain.electronics;

import java.util.Arrays;

/**
 * A logic part (docs/electronics/audit-and-plan.md 2.5): a mixed-signal device. Its inputs are high-impedance pins read against
 * thresholds set by its own supply; its output is a switched pull-up or pull-down through an output resistance; with too little supply it
 * lets its output float. Between events it is a linear part, so the engine solves it exactly; at events (an output changing, after the
 * part's propagation delay) the {@link Netlist} re-solves. Kinds: the Nugs (gates), the Latch Nug, the Flip-Flop Nug and the Ticker.
 */
public abstract class Logic implements Part {
    /** What the output does: pull low, pull high, or let go (high impedance). */
    public enum Out { LOW, HIGH, FLOAT }

    /** Input thresholds as shares of the supply, as CMOS logic: below LOW reads 0, above HIGH reads 1, between keeps its last reading. */
    public static final double LOW = 0.3, HIGH = 0.7;
    /** Below this supply a part does nothing and its output floats. */
    public static final double MIN_SUPPLY = 1.5;
    public static final double R_OUT = 100, R_IN = 1e6, R_OFF = 1e10, R_QUIESCENT = 1e5, DELAY = 1e-3;

    final int vdd, gnd, out;
    final int[] inputs;
    double delay = DELAY;
    Out state = Out.FLOAT;
    /** An output change waiting out the delay, and when it's due (absolute seconds); null when none. */
    Out pending;
    double due;
    private final int[] reading;

    Logic(int vdd, int gnd, int out, int... inputs) {
        this.vdd = vdd;
        this.gnd = gnd;
        this.out = out;
        this.inputs = inputs;
        this.reading = new int[inputs.length];
        Arrays.fill(reading, 0);
    }

    public Out output() { return state; }

    public void delay(double seconds) { delay = Math.max(0, seconds); }

    public double delay() { return delay; }

    @Override public int[] nodes() { return new int[]{vdd, gnd}; }

    /**
     * The pins it ties into one loop, for finding references: its inputs (1 MΦ to its ground) and supply, and its output only while it
     * drives it. A floating output is held to the part by nothing but its stage's 10 GΦ leakage, too little to be the reference a whole
     * powered loop hangs from: it gets a reference of its own instead.
     */
    @Override public int[] joined() {
        boolean driving = state != Out.FLOAT;
        int[] all = Arrays.copyOf(inputs, inputs.length + (driving ? 3 : 2));
        all[inputs.length] = vdd;
        all[inputs.length + 1] = gnd;
        if (driving) all[inputs.length + 2] = out;
        return all;
    }

    @Override public void stamp(Stamp s, Analysis an) {
        for (int in : inputs) s.conductance(in, gnd, 1 / R_IN);
        s.conductance(vdd, gnd, 1 / R_QUIESCENT);
        s.conductance(out, vdd, 1 / (state == Out.HIGH ? R_OUT : R_OFF));
        s.conductance(out, gnd, 1 / (state == Out.LOW ? R_OUT : R_OFF));
    }

    /** Current drawn from its supply (quiescent, and what its output sources), from vdd to gnd through the part. */
    @Override public double current(Analysis an) {
        double i = an.across(vdd, gnd) / R_QUIESCENT;
        if (state == Out.HIGH) i += an.across(vdd, out) / R_OUT;
        return i;
    }

    double supply(Analysis an) { return an.across(vdd, gnd); }

    boolean powered(Analysis an) { return supply(an) >= MIN_SUPPLY; }

    /** Input {@code i} as a bit: 1, 0, or its last reading while the voltage is between the thresholds. */
    int bit(Analysis an, int i) {
        double v = an.across(inputs[i], gnd), supply = supply(an);
        if (v >= HIGH * supply) reading[i] = 1;
        else if (v <= LOW * supply) reading[i] = 0;
        return reading[i];
    }

    /** Extra memory a kind keeps (a latch's or flip-flop's bit, the clock it last saw). */
    double[] memory() { return new double[0]; }

    void remember(double[] memory) {}

    /** Output, pending change and when it's due (from now), what each input last read, then the kind's own memory. */
    @Override public double[] save(double now) {
        double[] mem = memory();
        double[] out = new double[3 + reading.length + mem.length];
        out[0] = state.ordinal();
        out[1] = pending == null ? -1 : pending.ordinal();
        out[2] = pending == null ? 0 : Math.max(0, due - now);
        for (int i = 0; i < reading.length; i++) out[3 + i] = reading[i];
        System.arraycopy(mem, 0, out, 3 + reading.length, mem.length);
        return out;
    }

    @Override public void load(double[] s, double now) {
        if (s.length < 3 + reading.length) return;
        Out[] outs = Out.values();
        state = outs[(int) Math.max(0, Math.min(outs.length - 1, s[0]))];
        pending = s[1] < 0 ? null : outs[(int) Math.min(outs.length - 1, s[1])];
        due = now + s[2];
        for (int i = 0; i < reading.length; i++) reading[i] = (int) s[3 + i];
        remember(java.util.Arrays.copyOfRange(s, 3 + reading.length, s.length));
    }

    /** What the output should be now, from the inputs as solved (and the time, for a Ticker). */
    abstract Out want(Analysis an, double time);

    // ---- the parts --------------------------------------------------------------------------------------------------------------

    /** The gates: NOT, Buffer, AND, OR, NAND, NOR, XOR, XNOR. */
    public enum Kind { NOT, BUFFER, AND, OR, NAND, NOR, XOR, XNOR }

    /** A Nug: one of the eight gates, on any number of inputs (one for NOT and Buffer). */
    public static final class Gate extends Logic {
        final Kind kind;

        Gate(Kind kind, int vdd, int gnd, int out, int... inputs) {
            super(vdd, gnd, out, inputs);
            this.kind = kind;
        }

        @Override Out want(Analysis an, double time) {
            if (!powered(an)) return Out.FLOAT;
            int ones = 0;
            for (int i = 0; i < inputs.length; i++) ones += bit(an, i);
            boolean all = ones == inputs.length, any = ones > 0, odd = (ones & 1) == 1;
            boolean y = switch (kind) {
                case NOT -> !any;
                case BUFFER -> any;
                case AND -> all;
                case OR -> any;
                case NAND -> !all;
                case NOR -> !any;
                case XOR -> odd;
                case XNOR -> !odd;
            };
            return y ? Out.HIGH : Out.LOW;
        }
    }

    /** A Tri-State Nug: passes A when EN is 1, lets go of its output when EN is 0. Inputs: A, EN. */
    public static final class TriState extends Logic {
        TriState(int vdd, int gnd, int out, int a, int enable) { super(vdd, gnd, out, a, enable); }

        @Override Out want(Analysis an, double time) {
            if (!powered(an) || bit(an, 1) == 0) return Out.FLOAT;
            return bit(an, 0) == 1 ? Out.HIGH : Out.LOW;
        }
    }

    /** A Latch Nug (SR): S sets it, R resets it, neither holds it; both at once reset it. Inputs: S, R. */
    public static final class Latch extends Logic {
        private boolean q;

        Latch(int vdd, int gnd, int out, int set, int reset) { super(vdd, gnd, out, set, reset); }

        @Override double[] memory() { return new double[]{q ? 1 : 0}; }

        @Override void remember(double[] m) { if (m.length >= 1) q = m[0] > 0.5; }

        @Override Out want(Analysis an, double time) {
            if (!powered(an)) return Out.FLOAT;
            int s = bit(an, 0), r = bit(an, 1);
            if (r == 1) q = false;
            else if (s == 1) q = true;
            return q ? Out.HIGH : Out.LOW;
        }
    }

    /** A Flip-Flop Nug (D): on each rising edge of its clock it takes D. Inputs: D, CLK. */
    public static final class FlipFlop extends Logic {
        private boolean q;
        private int lastClock = -1;

        FlipFlop(int vdd, int gnd, int out, int d, int clock) { super(vdd, gnd, out, d, clock); }

        @Override double[] memory() { return new double[]{q ? 1 : 0, lastClock}; }

        @Override void remember(double[] m) {
            if (m.length >= 2) {
                q = m[0] > 0.5;
                lastClock = (int) m[1];
            }
        }

        @Override Out want(Analysis an, double time) {
            if (!powered(an)) {
                lastClock = -1;
                return Out.FLOAT;
            }
            int d = bit(an, 0), clk = bit(an, 1);
            if (lastClock == 0 && clk == 1) q = d == 1;
            lastClock = clk;
            return q ? Out.HIGH : Out.LOW;
        }
    }

    /** A Ticker: a square wave of {@code period} seconds, high for the first half. No inputs. */
    public static final class Ticker extends Logic {
        final double period;

        Ticker(int vdd, int gnd, int out, double period) {
            super(vdd, gnd, out);
            this.period = Math.max(period, 2e-6);
            this.delay = 0;
        }

        @Override Out want(Analysis an, double time) {
            if (!powered(an)) return Out.FLOAT;
            double phase = time % period;
            return phase < period / 2 - 1e-12 ? Out.HIGH : Out.LOW;
        }

        /** When its output next changes, after {@code time}. */
        double nextEdge(double time) {
            double half = period / 2;
            return (Math.floor(time / half + 1e-9) + 1) * half;
        }
    }
}
