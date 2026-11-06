package com.malevolentgods.fourtwenty.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A DC circuit solved by modified nodal analysis: node voltages {@code v} from {@code G·v = i}. Every part sits between a + node and a −
 * node, so current flows only around closed loops; there is no ground (each node's tiny leak only fixes the voltage of a loop that floats).
 * {@link #GROUND} remains for tests that want a part to one side of a reference. Parts:
 * <ul>
 *   <li><b>conductors</b>, between two nodes (Wicks);</li>
 *   <li><b>sources</b>, an EMF behind an internal resistance with a limit on the current out and in (Hookahs, Stashes, a Hit Box's
 *   output): past a limit the source holds that current instead, like a bench supply;</li>
 *   <li><b>loads</b> (machines): from their rated voltage up they hold a constant power, like a regulated supply, each the resistance
 *   V²/P at the last voltage until the line settles; below it they're a plain resistance sized for it, R = V²/P at the rated voltage, so
 *   a series string or a sagging line gives them less power by Ohm's law (and series strings are stable). Wired backwards, a load draws
 *   nothing.</li>
 * </ul>
 * A first probe finds the loads with no loop through them, or wired backwards, and leaves them out. Each separate loop gets one reference
 * node, which no current can flow through, so the voltages are well defined however stiff the Wicks are. If the solve can't settle at full
 * load, the loads are raised from nothing in steps (continuation) and the furthest it gets is the share they run at. Pure maths, no game types; see specs/006-circuit-solver/research.md R8 and
 * specs/007-polarity-circuits/research.md R1.
 */
public final class Circuit {
    /** Past a current limit a source still gives a little with the voltage, at its internal conductance over STIFF, so a node never floats. */
    private static final double GMIN = 1e-12, TOLERANCE = 1e-9, STEP = 0.25, STIFF = 1e5, PROBE = 1e-6, OPEN = 1e-4;
    private static final int ITERATIONS = 60;
    private static final double MIN_STEP = 1.0 / 4096;
    private static final int CV = 0, CC_OUT = 1, CC_IN = 2;

    /** What became of a load: running, in a brownout, with no closed loop through it, or wired backwards. */
    public static final int OK = 0, BROWNOUT = 1, NO_LOOP = 2, REVERSED = 3;

    /** The reference a part can run to when one side of it isn't a node, for tests. */
    public static final int GROUND = -1;

    private record Conductor(int a, int b, double ohms) {}
    private record Source(int plus, int minus, double emf, double ohms, double maxOut, double maxIn) {}
    private record Load(int plus, int minus, double watts, double minVolts) {}
    /** An LED behind a constant-current driver: dark below {@code forward}, rising to {@code setAmps} at {@code rated}, held above. */
    private record Led(int plus, int minus, double setAmps, double forward, double rated) {
        double amps(double volts) {
            if (volts <= forward) return 0;
            return setAmps * (rated <= forward ? 1 : Math.min(1, (volts - forward) / (rated - forward)));
        }
    }

    private int nodes;
    private final List<Conductor> conductors = new ArrayList<>();
    private final List<Source> sources = new ArrayList<>();
    private final List<Load> loads = new ArrayList<>();
    private final List<Led> leds = new ArrayList<>();
    /** Each LED's status from the probe: OK, NO_LOOP or REVERSED. */
    private int[] ledStatus = new int[0];
    /** Each load's status from the probe: OK, NO_LOOP or REVERSED. */
    private int[] status = new int[0];

    public int node() { return nodes++; }

    public int nodes() { return nodes; }

    /** A resistance between two nodes, or from a node to {@link #GROUND}; returns its index. */
    public int conductor(int a, int b, double ohms) {
        conductors.add(new Conductor(a, b, Math.max(ohms, 1e-15)));
        return conductors.size() - 1;
    }

    /**
     * An EMF from {@code minus} to {@code plus} behind {@code ohms}, giving at most {@code maxOut} out of its + terminal and taking at most
     * {@code maxIn} into it; returns its index.
     */
    public int source(int plus, int minus, double emf, double ohms, double maxOut, double maxIn) {
        sources.add(new Source(plus, minus, emf, Math.max(ohms, 1e-15), Math.max(0, maxOut), Math.max(0, maxIn)));
        return sources.size() - 1;
    }

    /** A source from {@code node} to {@link #GROUND}. */
    public int source(int node, double emf, double ohms, double maxOut, double maxIn) { return source(node, GROUND, emf, ohms, maxOut, maxIn); }

    /**
     * A load across {@code plus} and {@code minus} holding {@code watts} at any voltage from {@code minVolts} (its rated voltage) up; below
     * that it is the resistance it has there. Wired backwards, it draws nothing.
     */
    public int load(int plus, int minus, double watts, double minVolts) {
        loads.add(new Load(plus, minus, Math.max(0, watts), Math.max(minVolts, 1e-9)));
        return loads.size() - 1;
    }

    /** A load from {@code node} to {@link #GROUND}. */
    public int load(int node, double watts, double minVolts) { return load(node, GROUND, watts, minVolts); }

    /** An LED across {@code plus} and {@code minus} (Lit Bits: domain/LedRules); returns its index. */
    public int led(int plus, int minus, double setAmps, double forward, double rated) {
        leds.add(new Led(plus, minus, Math.max(0, setAmps), Math.max(0, forward), Math.max(forward, rated)));
        return leds.size() - 1;
    }

    /**
     * What the circuit settles at. Source watts are what leaves or (negative) enters the source's own store: its EMF's share, which
     * includes its internal loss. Load watts are what reaches each load. {@code share} is the brownout factor, 1 when healthy.
     * {@code loadStatus} says, for each load, OK, BROWNOUT, NO_LOOP or REVERSED.
     */
    public record Solution(double[] volts, double[] conductorAmps, double[] sourceAmps, double[] sourceWatts, double[] loadWatts,
                           double[] loadAmps, double losses, double share, boolean converged, int[] loadStatus, double[] ledAmps, int[] ledStatus) {
        /** The voltage across a load, + minus −. */
        public double loadVolts(double[] v, int plus, int minus) { return at(v, plus) - at(v, minus); }
    }

    public Solution solve() {
        double scale = sources.stream().mapToDouble(Source::emf).max().orElse(0);
        if (nodes == 0 || scale <= 0) return dead();
        double[] v = probe(scale);
        if (v == null) return dead();
        int[] mode = new int[sources.size()];
        double[] direct = v.clone();
        int[] directMode = mode.clone();
        if (newton(1, direct, directMode, scale) && new Attempt(1, direct, directMode, true).healthy()) return new Attempt(1, direct, directMode, true).solution();
        if (!newton(0, v, mode, scale)) return new Attempt(0, v, mode, false).solution();
        // continuation: raise the loads from nothing, each step starting from the last answer, so the solve follows the high-voltage
        // branch; the furthest it gets with every load above its minimum is the brownout share
        double share = 0, step = 0.25;
        while (share < 1 && step >= MIN_STEP) {
            double next = Math.min(1, share + step);
            double[] trial = v.clone();
            int[] trialMode = mode.clone();
            if (newton(next, trial, trialMode, scale) && new Attempt(next, trial, trialMode, true).healthy()) {
                share = next;
                v = trial;
                mode = trialMode;
                step = Math.min(step * 2, 0.5);
            } else {
                step /= 2;
            }
        }
        return new Attempt(share, v, mode, true).solution();
    }

    private Solution dead() {
        int[] out = new int[loads.size()];
        Arrays.fill(out, NO_LOOP);
        int[] ledOut = new int[leds.size()];
        Arrays.fill(ledOut, NO_LOOP);
        return new Solution(new double[nodes], new double[conductors.size()], new double[sources.size()], new double[sources.size()],
                new double[loads.size()], new double[loads.size()], 0, 0, true, out, new double[leds.size()], ledOut);
    }

    /**
     * One linear solve with every source holding its voltage and every load a whisper of a conductance: a load with no loop through it
     * sees no voltage, and one wired backwards sees it the wrong way round. Those take no part in the real solve. Returns the voltages as
     * a starting point.
     */
    private double[] probe(double scale) {
        status = new int[loads.size()];
        Sparse sparse = new Sparse(nodes);
        double[] b = new double[nodes];
        for (int i = 0; i < nodes; i++) add(sparse, i, i, GMIN);
        reference(sparse, true);
        for (Conductor c : conductors) between(sparse, c.a(), c.b(), 1 / c.ohms());
        for (Source s : sources) {
            between(sparse, s.plus(), s.minus(), 1 / s.ohms());
            inject(b, s.plus(), s.minus(), s.emf() / s.ohms());
        }
        for (Load l : loads) if (l.watts() > 0) between(sparse, l.plus(), l.minus(), PROBE * l.watts() / (scale * scale));
        for (Led l : leds) between(sparse, l.plus(), l.minus(), PROBE * Math.max(l.setAmps(), 1e-9) / scale);
        double[] v = sparse.solve(b);
        if (v == null) return null;
        for (int i = 0; i < loads.size(); i++) {
            double across = across(v, loads.get(i));
            status[i] = across < -OPEN * scale ? REVERSED : across <= OPEN * scale ? NO_LOOP : OK;
        }
        ledStatus = new int[leds.size()];
        for (int i = 0; i < leds.size(); i++) {
            Led l = leds.get(i);
            double across = at(v, l.plus()) - at(v, l.minus());
            ledStatus[i] = across < -OPEN * scale ? REVERSED : across <= OPEN * scale ? NO_LOOP : OK;
        }
        return v;
    }

    private boolean active(int load) { return status[load] == OK; }

    /**
     * One node of each separate loop, to hold at zero: the gauge, not a ground. A loop touching {@link #GROUND} already has one. With
     * {@code probing}, every load joins the loops it touches; otherwise only the loads still in the solve do.
     */
    private List<Integer> references(boolean probing) {
        int[] parent = new int[nodes];
        boolean[] grounded = new boolean[nodes];
        for (int i = 0; i < nodes; i++) parent[i] = i;
        java.util.function.IntUnaryOperator find = new java.util.function.IntUnaryOperator() {
            @Override public int applyAsInt(int x) {
                while (parent[x] != x) x = parent[x] = parent[parent[x]];
                return x;
            }
        };
        java.util.function.BiConsumer<Integer, Integer> join = (a, b) -> {
            if (a == GROUND && b == GROUND) return;
            if (a == GROUND || b == GROUND) {
                grounded[find.applyAsInt(a == GROUND ? b : a)] = true;
                return;
            }
            int ra = find.applyAsInt(a), rb = find.applyAsInt(b);
            if (ra == rb) return;
            parent[ra] = rb;
            grounded[rb] |= grounded[ra];
        };
        for (Conductor c : conductors) join.accept(c.a(), c.b());
        for (Source src : sources) join.accept(src.plus(), src.minus());
        for (int i = 0; i < loads.size(); i++) {
            if (probing ? loads.get(i).watts() > 0 : active(i)) join.accept(loads.get(i).plus(), loads.get(i).minus());
        }
        for (int i = 0; i < leds.size(); i++) {
            if (probing || ledStatus[i] == OK) join.accept(leds.get(i).plus(), leds.get(i).minus());
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < nodes; i++) if (find.applyAsInt(i) == i && !grounded[i]) out.add(i);
        return out;
    }

    private void reference(Sparse sparse, boolean probing) {
        for (int r : references(probing)) add(sparse, r, r, 1);
    }

    private final class Attempt {
        final double share;
        final double[] v;
        final int[] mode;
        final boolean converged;

        Attempt(double share, double[] v, int[] mode, boolean converged) {
            this.share = share;
            this.v = v;
            this.mode = mode;
            this.converged = converged;
        }

        boolean healthy() { return converged; }

        Solution solution() {
            double[] cAmps = new double[conductors.size()];
            double losses = 0;
            for (int i = 0; i < cAmps.length; i++) {
                Conductor c = conductors.get(i);
                cAmps[i] = (at(v, c.a()) - at(v, c.b())) / c.ohms();
                losses += cAmps[i] * cAmps[i] * c.ohms();
            }
            double[] sAmps = new double[sources.size()], sWatts = new double[sources.size()];
            for (int i = 0; i < sAmps.length; i++) {
                Source s = sources.get(i);
                double across = across(v, s);
                sAmps[i] = sourceAmps(s, mode[i], across);
                sWatts[i] = across * sAmps[i] + sAmps[i] * sAmps[i] * s.ohms();
                losses += sAmps[i] * sAmps[i] * s.ohms();
            }
            double[] lWatts = new double[loads.size()], lAmps = new double[loads.size()];
            int[] out = status.clone();
            for (int i = 0; i < lWatts.length; i++) {
                if (!active(i)) continue;
                Load l = loads.get(i);
                double volts = across(v, l), want = l.watts() * share;
                lWatts[i] = volts >= l.minVolts() ? want : volts * volts * want / (l.minVolts() * l.minVolts());
                lAmps[i] = volts > 0 ? lWatts[i] / volts : 0;
                if (share < 1) out[i] = BROWNOUT;
            }
            double[] ledAmps = new double[leds.size()];
            for (int i = 0; i < leds.size(); i++) {
                if (ledStatus[i] != OK) continue;
                Led l = leds.get(i);
                ledAmps[i] = l.amps(at(v, l.plus()) - at(v, l.minus()));
            }
            return new Solution(v, cAmps, sAmps, sWatts, lWatts, lAmps, losses, share, converged, out, ledAmps, ledStatus.clone());
        }
    }

    private static double at(double[] v, int node) { return node == GROUND ? 0 : v[node]; }

    private static double across(double[] v, Load l) { return at(v, l.plus()) - at(v, l.minus()); }

    private static double across(double[] v, Source s) { return at(v, s.plus()) - at(v, s.minus()); }

    /** Which part of its line a source is on at this voltage across it: holding its voltage, or at a current limit. */
    private static int modeAt(Source s, double volts) {
        return volts < outEdge(s) ? CC_OUT : volts > inEdge(s) ? CC_IN : CV;
    }

    private static double outEdge(Source s) { return s.emf() - s.maxOut() * s.ohms(); }

    private static double inEdge(Source s) { return s.emf() + s.maxIn() * s.ohms(); }

    private static double sourceAmps(Source s, int mode, double volts) {
        double soft = 1 / (s.ohms() * STIFF);
        return switch (mode) {
            case CC_OUT -> s.maxOut() + soft * (outEdge(s) - volts);
            case CC_IN -> -s.maxIn() + soft * (inEdge(s) - volts);
            default -> (s.emf() - volts) / s.ohms();
        };
    }

    /** Relaxation steps at this load share from {@code v}, in place; true when it settles. */
    private boolean newton(double share, double[] v, int[] mode, double scale) {
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            for (int i = 0; i < mode.length; i++) mode[i] = modeAt(sources.get(i), across(v, sources.get(i)));
            Sparse sparse = new Sparse(nodes);
            double[] b = new double[nodes];
            for (int i = 0; i < nodes; i++) add(sparse, i, i, GMIN);
            reference(sparse, false);
            for (Conductor c : conductors) between(sparse, c.a(), c.b(), 1 / c.ohms());
            for (int i = 0; i < mode.length; i++) {
                Source s = sources.get(i);
                double soft = 1 / (s.ohms() * STIFF);
                switch (mode[i]) {
                    case CC_OUT -> {
                        between(sparse, s.plus(), s.minus(), soft);
                        inject(b, s.plus(), s.minus(), s.maxOut() + soft * outEdge(s));
                    }
                    case CC_IN -> {
                        between(sparse, s.plus(), s.minus(), soft);
                        inject(b, s.plus(), s.minus(), -s.maxIn() + soft * inEdge(s));
                    }
                    default -> {
                        between(sparse, s.plus(), s.minus(), 1 / s.ohms());
                        inject(b, s.plus(), s.minus(), s.emf() / s.ohms());
                    }
                }
            }
            for (int li = 0; li < loads.size(); li++) {
                Load l = loads.get(li);
                if (!active(li)) continue;
                // with no share yet, a load is still a whisper of a conductance, so the loops through it stay joined
                double p = Math.max(l.watts() * share, PROBE * l.watts());
                // the load is the resistance it had at the last voltage (V² / P), never below its minimum: a relaxation that always moves
                // the line the way a real one settles, where Newton's method would follow a constant-power load's negative slope
                double volts = Math.max(across(v, l), l.minVolts());
                between(sparse, l.plus(), l.minus(), p / (volts * volts));
            }
            for (int li = 0; li < leds.size(); li++) {
                if (ledStatus[li] != OK) continue;
                Led l = leds.get(li);
                // the conductance that carries its current at the last voltage (I / V), and a whisper when it's dark, so the loop stays joined
                double volts = Math.max(at(v, l.plus()) - at(v, l.minus()), 1e-9);
                between(sparse, l.plus(), l.minus(), Math.max(l.amps(volts) / volts, PROBE * Math.max(l.setAmps(), 1e-9) / scale));
            }
            double[] x = sparse.solve(b);
            if (x == null) return false;
            // step toward x, but stop just past the first current limit any source crosses: its mode changes there (breakpoint stepping)
            double alpha = 1;
            for (Source src : sources) {
                double from = across(v, src), to = at(x, src.plus()) - at(x, src.minus());
                for (double edge : new double[]{outEdge(src), inEdge(src)}) {
                    if ((from - edge) * (to - edge) < 0) alpha = Math.min(alpha, (edge - from) / (to - from) + 1e-7);
                }
            }
            double delta = 0;
            for (int i = 0; i < nodes; i++) {
                double d = Math.max(-STEP * scale, Math.min(STEP * scale, alpha * (x[i] - v[i])));
                if (!Double.isFinite(d)) return false;
                v[i] += d;
                delta = Math.max(delta, Math.abs(d));
            }
            if (alpha >= 1 && delta < TOLERANCE * scale) {
                for (int i = 0; i < mode.length; i++) mode[i] = modeAt(sources.get(i), across(v, sources.get(i)));
                return true;
            }
        }
        return false;
    }

    private static void add(Sparse sparse, int r, int c, double value) {
        if (r == GROUND || c == GROUND) return;
        sparse.add(r, c, value);
    }

    /** A conductance {@code g} between two nodes (either may be {@link #GROUND}). */
    private static void between(Sparse sparse, int a, int b, double g) {
        add(sparse, a, a, g);
        add(sparse, b, b, g);
        add(sparse, a, b, -g);
        add(sparse, b, a, -g);
    }

    /** A current {@code amps} pushed into {@code plus} and drawn out of {@code minus}. */
    private static void inject(double[] b, int plus, int minus, double amps) {
        if (plus != GROUND) b[plus] += amps;
        if (minus != GROUND) b[minus] -= amps;
    }

    /**
     * The conductance matrix, symmetric and positive definite (every part is a positive conductance), solved directly by elimination,
     * fewest neighbours first: Wick networks are mostly trees, which this solves in linear time, with little fill-in on meshes.
     */
    private static final class Sparse {
        final List<java.util.Map<Integer, Double>> rows = new ArrayList<>();

        Sparse(int n) {
            for (int i = 0; i < n; i++) rows.add(new java.util.HashMap<>());
        }

        void add(int r, int c, double value) { rows.get(r).merge(c, value, Double::sum); }

        /** null if a pivot vanishes. */
        double[] solve(double[] rhs) {
            int n = rhs.length;
            double[] b = rhs.clone();
            boolean[] done = new boolean[n];
            int[] order = new int[n];
            double[] pivots = new double[n];
            List<java.util.Map<Integer, Double>> snapshots = new ArrayList<>(java.util.Collections.nCopies(n, null));
            java.util.PriorityQueue<long[]> queue = new java.util.PriorityQueue<>((p, q) -> Long.compare(p[0], q[0]));
            for (int i = 0; i < n; i++) queue.add(new long[]{rows.get(i).size(), i});
            int step = 0;
            while (!queue.isEmpty()) {
                long[] top = queue.poll();
                int k = (int) top[1];
                if (done[k] || top[0] != rows.get(k).size()) continue;
                var row = rows.get(k);
                double pivot = row.getOrDefault(k, 0.0);
                if (Math.abs(pivot) < 1e-300) return null;
                done[k] = true;
                order[step++] = k;
                pivots[k] = pivot;
                var others = new java.util.HashMap<>(row);
                others.remove(k);
                snapshots.set(k, others);
                for (var ej : others.entrySet()) {
                    int j = ej.getKey();
                    var rowJ = rows.get(j);
                    double f = ej.getValue() / pivot;
                    b[j] -= f * b[k];
                    for (var ei : others.entrySet()) rowJ.merge(ei.getKey(), -f * ei.getValue(), Double::sum);
                    rowJ.remove(k);
                    queue.add(new long[]{rowJ.size(), j});
                }
            }
            double[] x = new double[n];
            for (int s = step - 1; s >= 0; s--) {
                int k = order[s];
                double sum = b[k];
                for (var e : snapshots.get(k).entrySet()) sum -= e.getValue() * x[e.getKey()];
                x[k] = sum / pivots[k];
            }
            return x;
        }
    }
}
