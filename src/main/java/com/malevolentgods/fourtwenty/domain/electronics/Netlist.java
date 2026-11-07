package com.malevolentgods.fourtwenty.domain.electronics;

import java.util.ArrayList;
import java.util.List;

/**
 * A circuit of components, solved like SPICE (docs/electronics/audit-and-plan.md): a DC operating point by Newton–Raphson with companion
 * models, voltage limiting and SPICE's convergence test, with GMIN stepping and source stepping when it won't converge; then time steps
 * (transient analysis) by the trapezoidal rule, starting each run of steps with backward Euler. Each separate loop gets one reference node,
 * a gauge rather than a ground; {@link Stamp#GROUND} is there for circuits that want one.
 */
public final class Netlist {
    private static final int ITERATIONS = 150;
    private static final double GMIN = 1e-12;

    private int nodes;
    private final List<Part> parts = new ArrayList<>();
    private Analysis analysis;
    private boolean started;

    public int node() { return nodes++; }

    public int nodes() { return nodes; }

    private <P extends Part> P add(P part) {
        parts.add(part);
        analysis = null;
        return part;
    }

    public Parts.Resistor resistor(int a, int b, double ohms) { return add(new Parts.Resistor(a, b, ohms)); }

    public Parts.Source source(int plus, int minus, double emf, double ohms) { return add(new Parts.Source(plus, minus, emf, ohms)); }

    public Parts.CurrentSource currentSource(int a, int b, double amps) { return add(new Parts.CurrentSource(a, b, amps)); }

    public Parts.LimitedSource limitedSource(int plus, int minus, double emf, double ohms, double maxOut, double maxIn) {
        return add(new Parts.LimitedSource(plus, minus, emf, ohms, maxOut, maxIn));
    }

    public Parts.PowerLoad powerLoad(int plus, int minus, double watts, double minVolts) { return add(new Parts.PowerLoad(plus, minus, watts, minVolts)); }

    public Parts.DriverLed driverLed(int plus, int minus, double setAmps, double forward, double rated) {
        return add(new Parts.DriverLed(plus, minus, setAmps, forward, rated));
    }

    private double reltol = 1e-6;

    /** How tightly a solve must settle, relative (SPICE: 1e-3). The absolute floors stay 1 µKb and 1 pIg. */
    public void tolerance(double relative) { reltol = Math.max(relative, 1e-15); }

    private double[] guess;
    private double stepLimit = Double.POSITIVE_INFINITY;

    /** Where the next operating point starts from (a good guess picks the right branch of a circuit with more than one answer). */
    public void guess(double[] volts) { guess = volts == null ? null : volts.clone(); }

    /** The most any node may move in one Newton step (damping, for circuits of relaxed parts). */
    public void stepLimit(double volts) { stepLimit = volts > 0 ? volts : Double.POSITIVE_INFINITY; }

    public Parts.Switch switchPart(int a, int b, boolean closed) { return add(new Parts.Switch(a, b, closed)); }

    /** Flips a switch: the next time step starts with backward Euler. */
    public void set(Parts.Switch s, boolean closed) {
        if (s.closed == closed) return;
        s.closed = closed;
        breakpoint();
    }

    public Parts.Capacitor capacitor(int a, int b, double farads) { return add(new Parts.Capacitor(a, b, farads, 0)); }

    public Parts.Inductor inductor(int a, int b, double henries) { return add(new Parts.Inductor(a, b, henries, 0)); }

    public Parts.Diode diode(int anode, int cathode, Parts.DiodeModel model) { return add(new Parts.Diode(anode, cathode, model)); }

    public Parts.Transformer transformer(int a1, int b1, int a2, int b2, double ratio, double ohms) {
        return add(new Parts.Transformer(a1, b1, a2, b2, ratio, ohms));
    }

    /** The present solution, for reading parts. */
    public Analysis analysis() { return analysis; }

    public double volts(int node) { return analysis == null ? 0 : analysis.volts(node); }

    public double current(Part part) { return analysis == null ? 0 : part.current(analysis); }

    /** The DC operating point: capacitors open, inductors shorted. True if it converged. */
    public boolean operatingPoint() {
        analysis = new Analysis(nodes);
        analysis.reltol = reltol;
        if (guess != null && guess.length == nodes) analysis.v = guess.clone();
        analysis.mode = Analysis.Mode.DC;
        boolean ok = solveRobustly();
        if (ok) for (Part p : parts) p.accept(analysis);
        started = ok;
        return ok;
    }

    /**
     * Advances the circuit by {@code seconds} in {@code steps} equal steps (from the operating point, if it hasn't been found). The first
     * step of a run is backward Euler, the rest trapezoidal, as SPICE does after a breakpoint. True if every step converged.
     */
    public boolean advance(double seconds, int steps) {
        if (!started && !operatingPoint()) return false;
        double h = seconds / Math.max(1, steps);
        boolean first = true;
        for (int i = 0; i < Math.max(1, steps); i++) {
            double[] keep = analysis.v.clone();
            analysis.mode = Analysis.Mode.TRANSIENT;
            analysis.h = h;
            analysis.method = first && breakpoint ? Analysis.Method.EULER : Analysis.Method.TRAPEZOIDAL;
            analysis.v = keep;
            if (!solveRobustly()) return false;
            for (Part p : parts) p.accept(analysis);
            first = false;
            breakpoint = false;
        }
        return true;
    }

    private boolean breakpoint = true;

    /** Something switched (a button, a gate's output): the next step starts with backward Euler, so the edge doesn't ring numerically. */
    public void breakpoint() { breakpoint = true; }

    private boolean solveRobustly() {
        double[] start = analysis.v.clone();
        if (newton(0)) return true;
        // GMIN stepping: a big leak on every node, shrunk a decade at a time, each solve starting from the last
        analysis.v = start.clone();
        boolean ok = true;
        for (double g = 1e-2; g >= GMIN * 0.99; g /= 10) {
            if (!newton(g)) {
                ok = false;
                break;
            }
        }
        if (ok && newton(0)) return true;
        // source stepping: every source raised from a tenth to full
        analysis.v = start.clone();
        for (double scale = 0.1; scale <= 1.0001; scale += 0.1) {
            analysis.sourceScale = Math.min(1, scale);
            if (!newton(0)) {
                analysis.sourceScale = 1;
                return false;
            }
        }
        analysis.sourceScale = 1;
        return true;
    }

    /** Newton–Raphson from the present voltages, with an extra leak {@code gExtra} on every node. */
    private boolean newton(double gExtra) {
        boolean nonlinear = parts.stream().anyMatch(Part::nonlinear);
        boolean[] fixed = new boolean[nodes];
        for (int r : references()) fixed[r] = true;
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Stamp stamp = new Stamp(nodes, fixed);
            for (int i = 0; i < nodes; i++) stamp.conductance(i, Stamp.GROUND, GMIN + gExtra);
            for (Part p : parts) p.stamp(stamp, analysis);
            double[] x = stamp.solve();
            if (x == null) return false;
            boolean small = true;
            for (int i = 0; i < nodes; i++) {
                double d = x[i] - analysis.v[i];
                if (Math.abs(d) > stepLimit) {
                    x[i] = analysis.v[i] + Math.signum(d) * stepLimit;
                    small = false;
                }
                if (Math.abs(x[i] - analysis.v[i]) > analysis.reltol * Math.max(Math.abs(x[i]), Math.abs(analysis.v[i])) + Parts.VNTOL * Math.min(1, analysis.reltol / Parts.RELTOL)) small = false;
            }
            analysis.v = x;
            if (!nonlinear) return true;
            if (small && iteration > 0) {
                boolean all = true;
                for (Part p : parts) if (p.nonlinear() && !p.converged(analysis)) all = false;
                if (all) return true;
            }
        }
        return false;
    }

    /** One node of each separate loop to hold at zero; a loop that touches {@link Stamp#GROUND} has one already. */
    private List<Integer> references() {
        int[] parent = new int[nodes];
        boolean[] grounded = new boolean[nodes];
        for (int i = 0; i < nodes; i++) parent[i] = i;
        java.util.function.IntUnaryOperator find = x -> {
            while (parent[x] != x) x = parent[x] = parent[parent[x]];
            return x;
        };
        for (Part p : parts) {
            if (!p.conducts(analysis)) continue;
            int[] n = p.nodes();
            if (p instanceof Parts.Transformer t) {
                join(parent, grounded, find, t.a1(), t.b1());
                join(parent, grounded, find, t.a2(), t.b2());
                continue;
            }
            join(parent, grounded, find, n[0], n[1]);
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < nodes; i++) if (find.applyAsInt(i) == i && !grounded[i]) out.add(i);
        return out;
    }

    private static void join(int[] parent, boolean[] grounded, java.util.function.IntUnaryOperator find, int a, int b) {
        if (a == Stamp.GROUND && b == Stamp.GROUND) return;
        if (a == Stamp.GROUND || b == Stamp.GROUND) {
            grounded[find.applyAsInt(a == Stamp.GROUND ? b : a)] = true;
            return;
        }
        int ra = find.applyAsInt(a), rb = find.applyAsInt(b);
        if (ra == rb) return;
        parent[ra] = rb;
        grounded[rb] |= grounded[ra];
    }
}
