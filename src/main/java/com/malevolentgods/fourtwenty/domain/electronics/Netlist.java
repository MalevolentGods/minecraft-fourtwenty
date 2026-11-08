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

    private int logicParts;

    private <P extends Part> P add(P part) {
        // no two real parts are quite alike: each logic part's delay is off by a fixed fraction of a percent (by the order it was made),
        // so a symmetric circuit such as a ring of inverters can't lock into a mode no real one could hold
        if (part instanceof Logic l) l.delay *= 1 + 1e-3 * ((++logicParts * 0.6180339887) % 1.0);
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

    /** A Lit Bit: an RGB LED of {@code duty} channels' worth (0 to 3), each sized to draw {@code channelAmps} at {@code rated} volts. */
    public Parts.RgbLed rgbLed(int plus, int minus, double duty, double forward, double rated, double channelAmps) {
        return add(new Parts.RgbLed(plus, minus, duty, forward, rated, channelAmps));
    }

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

    /** A Sativa FET (N-channel) or Indica FET (P-channel) MOSFET: drain, gate, source. */
    public Active.Mosfet mosfet(int drain, int gate, int source, boolean pChannel) {
        return add(new Active.Mosfet(drain, gate, source, pChannel, Active.Mosfet.VT, Active.Mosfet.K, Active.Mosfet.LAMBDA));
    }

    /** A Hype Amp op-amp: supply, ground, output, non-inverting and inverting inputs. */
    /** A Slow Burner (a current regulator): {@code amps} from {@code a} to {@code b} when the voltage drives it, whatever that voltage past its knee. */
    public Active.CurrentRegulator currentRegulator(int a, int b, double amps) { return add(new Active.CurrentRegulator(a, b, amps)); }

    public Active.OpAmp opAmp(int vdd, int gnd, int out, int plus, int minus) { return add(new Active.OpAmp(vdd, gnd, out, plus, minus)); }

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

    /**
     * Picks up a circuit that was running before it was rebuilt: each part takes back its memory, and the next {@link #run} starts from
     * the operating point with that memory in place rather than from rest. A part with nothing to remember (one just placed) starts
     * where the circuit's DC operating point puts it: a new capacitor charged, not empty. An empty one across a stiff supply asks
     * thousands of amps of the first microsecond's step, past any source's limit.
     */
    public boolean resume(java.util.Map<Part, double[]> memory) {
        if (!operatingPoint() && memory.isEmpty()) {
            breakpoint();
            return false;
        }
        for (var e : memory.entrySet()) e.getKey().load(e.getValue(), time);
        boolean ok = operatingPointKeeping();
        breakpoint();
        return ok;
    }

    /** A DC solve that doesn't overwrite what capacitors and inductors remember (they were just restored). */
    private boolean operatingPointKeeping() {
        analysis = new Analysis(nodes);
        analysis.reltol = reltol;
        if (guess != null && guess.length == nodes) analysis.v = guess.clone();
        // a short transient step, so restored charges and currents stand rather than being reset to their DC values
        analysis.mode = Analysis.Mode.TRANSIENT;
        analysis.method = Analysis.Method.EULER;
        analysis.h = SETTLE;
        boolean ok = solveRobustly();
        if (ok) for (Part p : parts) p.accept(analysis);
        started = ok;
        return ok;
    }

    /** Every part's memory, to keep between ticks. */
    public java.util.Map<Part, double[]> memory() {
        java.util.Map<Part, double[]> out = new java.util.IdentityHashMap<>();
        for (Part p : parts) {
            double[] s = p.save(time);
            if (s.length > 0) out.put(p, s);
        }
        return out;
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
        time += seconds;
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

    // ---- logic: events inside the tick (docs/electronics/audit-and-plan.md 4.1, option 3) --------------------------------------

    /** After a logic output changes, the circuit is re-solved a microsecond on, so capacitors barely move while the edge settles. */
    static final double SETTLE = 1e-6;
    private double time;
    private int budget = 20_000;
    private double maxStep = Double.POSITIVE_INFINITY;

    /** Seconds simulated so far. */
    public double time() { return time; }

    /** The most logic events one {@link #run} may process; past it, the rest wait for the next run (a slow circuit, never a slow server). */
    public void eventBudget(int events) { budget = Math.max(1, events); }

    /** The longest analog step between events (for circuits with capacitors or inductors to integrate). */
    public void maxStep(double seconds) { maxStep = seconds > 0 ? seconds : Double.POSITIVE_INFINITY; }

    public Logic.Gate gate(Logic.Kind kind, int vdd, int gnd, int out, int... inputs) { return add(new Logic.Gate(kind, vdd, gnd, out, inputs)); }

    public Logic.TriState triState(int vdd, int gnd, int out, int a, int enable) { return add(new Logic.TriState(vdd, gnd, out, a, enable)); }

    public Logic.Latch latch(int vdd, int gnd, int out, int set, int reset) { return add(new Logic.Latch(vdd, gnd, out, set, reset)); }

    public Logic.FlipFlop flipFlop(int vdd, int gnd, int out, int d, int clock) { return add(new Logic.FlipFlop(vdd, gnd, out, d, clock)); }

    public Logic.Ticker ticker(int vdd, int gnd, int out, double period) { return add(new Logic.Ticker(vdd, gnd, out, period)); }

    /** A chip and an output stage on each of {@code outs}, in order. */
    private <C extends Chips.Chip> C chip(C chip, int... outs) {
        add(chip);
        for (int k = 0; k < outs.length; k++) chip.pins[k] = add(new Chips.Pin(chip, k, outs[k]));
        return chip;
    }

    /** A Puff-Pass Nug: {@code outs} the eight latched outputs, {@code carry} stage 7. */
    public Chips.PuffPass puffPass(int vdd, int gnd, int data, int clock, int latch, int[] outs, int carry) {
        return chip(new Chips.PuffPass(vdd, gnd, data, clock, latch), Chips.concat(outs, new int[]{carry}));
    }

    /** A Pack Nug: {@code in} its eight inputs, {@code out} the serial output. */
    public Chips.Pack pack(int vdd, int gnd, int load, int clock, int[] in, int out) { return chip(new Chips.Pack(vdd, gnd, load, clock, in), out); }

    /** A Sampler Nug: eight inputs, three select bits (the first the lowest), one output. */
    public Chips.Sampler sampler(int vdd, int gnd, int[] in, int[] select, int out) { return chip(new Chips.Sampler(vdd, gnd, in, select), out); }

    /** Bus logic: {@code op} on buses {@code a} and {@code b}, wire for wire, into {@code out} (all the same width). */
    public Chips.BusLogic busLogic(Chips.Op op, int vdd, int gnd, int[] a, int[] b, int[] out) { return chip(new Chips.BusLogic(vdd, gnd, op, a, b), out); }

    /** An 8-bit adder (or subtractor): eight sum outputs, then the carry (or borrow) out. */
    public Chips.Adder adder(boolean subtract, int vdd, int gnd, int[] a, int[] b, int carryIn, int[] sum, int carryOut) {
        return chip(new Chips.Adder(vdd, gnd, a, b, carryIn, subtract), Chips.concat(sum, new int[]{carryOut}));
    }

    /** An 8-bit comparator: A < B, A = B, A > B. */
    public Chips.Comparator comparator(int vdd, int gnd, int[] a, int[] b, int less, int equal, int greater) {
        return chip(new Chips.Comparator(vdd, gnd, a, b), less, equal, greater);
    }

    /** A register as wide as {@code d}: takes it on the clock's rising edge, drives {@code q} unless released. */
    public Chips.Register register(int vdd, int gnd, int[] d, int clock, int release, int[] q) { return chip(new Chips.Register(vdd, gnd, d, clock, release), q); }

    /** A Numb Nug: reads a byte on {@code in}. */
    public Chips.Display display(int vdd, int gnd, int[] in) { return add(new Chips.Display(vdd, gnd, in)); }

    /** A Dankoder: its keys, each standing for {@code codes[i]}; {@code out} the number on 0 to 4, 5 to 7 low, the carry key-down. */
    public Chips.Encoder dankoder(int vdd, int gnd, int[] keys, int[] codes, int[] out) { return chip(new Chips.Encoder(vdd, gnd, keys, codes), out); }

    /**
     * A Manyfier ({@code divide} false) or a Fewinator: A and B each 18 pins (low bus, high bus), {@code out} 36 (front low, front high,
     * right low, right high).
     */
    public Chips.Arith16 arith16(boolean divide, int vdd, int gnd, int[] a, int[] b, int[] out) {
        return chip(new Chips.Arith16(vdd, gnd, a, b, divide), out);
    }

    /** A Dealer Nug: one input, three select bits, eight outputs. */
    public Chips.Dealer dealer(int vdd, int gnd, int in, int[] select, int[] outs) { return chip(new Chips.Dealer(vdd, gnd, in, select), outs); }

    /**
     * Runs the circuit for {@code seconds}, logic and all: solve, let every logic part look at its inputs and queue an output change
     * after its delay (a change its inputs take back before it's due never happens), step the analog circuit to the next change, make it,
     * re-solve, and again, until the time is up or the event budget is spent. True if every solve converged.
     */
    /** This circuit's elimination plans (Stamp.Plans): worked out once per pattern, reused every solve. */
    private final Stamp.Plans plans = new Stamp.Plans();

    /** What the last run took: logic events, and linear solves (Newton iterations). */
    private int lastEvents, solves;

    public int lastEvents() { return lastEvents; }

    public int lastSolves() { return solves; }

    public boolean run(double seconds) {
        solves = 0;
        if (!started && !operatingPoint()) return false;
        List<Logic> logic = new ArrayList<>();
        for (Part p : parts) if (p instanceof Logic l) logic.add(l);
        double end = time + seconds;
        int events = 0;
        // see the circuit as it stands now (a switch may have flipped since the last run)
        if (!stepTo(time + SETTLE)) return false;
        schedule(logic);
        while (true) {
            double next = Double.POSITIVE_INFINITY;
            for (Logic l : logic) if (l.pending != null) next = Math.min(next, l.due);
            lastEvents = events;
            if (next > end || events >= budget) return stepTo(end) && settle(logic);
            if (next > time && !stepTo(next)) return false;
            for (Logic l : logic) {
                if (l.pending != null && l.due <= time + 1e-12) {
                    l.state = l.pending;
                    l.pending = null;
                    events++;
                }
            }
            breakpoint();
            if (!stepTo(time + SETTLE)) return false;
            schedule(logic);
        }
    }

    private boolean settle(List<Logic> logic) {
        schedule(logic);
        return true;
    }

    /** Each logic part, from the present solution: queue its output change, or take back one its inputs no longer want. */
    private void schedule(List<Logic> logic) {
        for (Logic l : logic) {
            Logic.Out want = l.want(analysis, time);
            if (l instanceof Logic.Ticker t && want != Logic.Out.FLOAT && want == l.state) {
                // a running Ticker: its next edge, on the beat
                if (l.pending == null) {
                    l.pending = want == Logic.Out.HIGH ? Logic.Out.LOW : Logic.Out.HIGH;
                    l.due = t.nextEdge(time);
                }
                continue;
            }
            if (want == l.state) {
                l.pending = null;
            } else if (want != l.pending) {
                l.pending = want;
                l.due = time + l.delay;
            }
        }
    }

    /** Integrates the analog circuit from now to {@code target}, in steps no longer than {@link #maxStep}. */
    private boolean stepTo(double target) {
        double span = target - time;
        if (span <= 0) return true;
        int steps = Double.isInfinite(maxStep) ? 1 : (int) Math.ceil(span / maxStep - 1e-9);
        return advance(span, Math.max(1, steps));
    }

    /** Something switched (a button, a gate's output): the next step starts with backward Euler, so the edge doesn't ring numerically. */
    public void breakpoint() { breakpoint = true; }

    /** The tolerance a solve settles for when the one asked for can't be reached: SPICE's default, a thousandth, made a thousand times stricter. */
    static final double LOOSE_RELTOL = 1e-6;

    private boolean solveRobustly() {
        double[] start = analysis.v.clone();
        if (newton(0)) return true;
        // a node held only by gigaohm leakage (a logic output not yet driven, a diode's far side) can't be pinned to a part in a
        // billion: it wobbles in its last digits for ever. Settle for a part in a million before the costly stepping below, which
        // can't do better and, failing, would run again every tick.
        if (analysis.reltol < LOOSE_RELTOL) {
            double tight = analysis.reltol;
            analysis.reltol = LOOSE_RELTOL;
            boolean settled = newton(0);
            analysis.reltol = tight;
            if (settled) return true;
            analysis.v = start.clone();
        }
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
            solves++;
            Stamp stamp = new Stamp(nodes, fixed, plans);
            for (int i = 0; i < nodes; i++) stamp.conductance(i, Stamp.GROUND, GMIN + gExtra);
            for (Part p : parts) p.stamp(stamp, analysis);
            double[] x = stamp.solve();
            if (x == null) return false;
            // breakpoint stepping: a part with a corner on the way stops the whole step just past it
            double alpha = 1;
            for (Part p : parts) if (p.nonlinear()) alpha = Math.min(alpha, p.allowStep(analysis, x));
            if (alpha < 1) for (int i = 0; i < nodes; i++) x[i] = analysis.v[i] + alpha * (x[i] - analysis.v[i]);
            boolean small = alpha >= 1;
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
            int[] n = p.joined();
            if (n.length > 2) {
                for (int k = 1; k < n.length; k++) join(parent, grounded, find, n[0], n[k]);
                continue;
            }
            if (p instanceof Parts.Transformer t) {
                join(parent, grounded, find, t.a1(), t.b1());
                join(parent, grounded, find, t.a2(), t.b2());
                continue;
            }
            join(parent, grounded, find, n[0], n[1]);
        }
        // each loop's reference: the minus pin of its stiffest source, so the rails are measured from the node the solve can hold
        // best; a node hanging off one transistor makes every stiff stamp ride on a weak tie
        int[] best = new int[nodes];
        double[] bestOhms = new double[nodes];
        java.util.Arrays.fill(best, -1);
        java.util.Arrays.fill(bestOhms, Double.POSITIVE_INFINITY);
        for (Part p : parts) {
            int minus;
            double ohms;
            if (p instanceof Parts.Source s) {
                minus = s.minus();
                ohms = s.ohms();
            } else if (p instanceof Parts.LimitedSource s) {
                minus = s.minus;
                ohms = s.ohms;
            } else continue;
            if (minus == Stamp.GROUND) continue;
            int root = find.applyAsInt(minus);
            if (ohms < bestOhms[root]) {
                bestOhms[root] = ohms;
                best[root] = minus;
            }
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < nodes; i++) if (find.applyAsInt(i) == i && !grounded[i]) out.add(best[i] >= 0 ? best[i] : i);
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
