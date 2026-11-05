package com.malevolentgods.fourtwenty.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A DC circuit solved by modified nodal analysis: node voltages {@code v} from {@code G·v = i}, ground implicit. Parts:
 * <ul>
 *   <li><b>conductors</b>, between two nodes (Wicks);</li>
 *   <li><b>sources</b>, an EMF behind an internal resistance with a limit on the current out and in (Hookahs, Stashes, a Hit Box's
 *   output): past a limit the source holds that current instead, like a bench supply;</li>
 *   <li><b>constant-power loads</b> (machines), each the resistance V²/P at the last voltage until the line settles.</li>
 * </ul>
 * When the sources can't carry the loads without the line falling under a load's minimum voltage, every load is scaled by the same
 * share (a brownout), the furthest a continuation from no load gets. Pure maths, no game types; see specs/006-circuit-solver/research.md R8.
 */
public final class Circuit {
    /** Past a current limit a source still gives a little with the voltage, at its internal conductance over STIFF, so a node never floats. */
    private static final double GMIN = 1e-12, TOLERANCE = 1e-9, STEP = 0.25, STIFF = 1e5;
    private static final int ITERATIONS = 60;
    private static final double MIN_STEP = 1.0 / 4096;
    private static final int CV = 0, CC_OUT = 1, CC_IN = 2;

    private record Conductor(int a, int b, double ohms) {}
    private record Source(int node, double emf, double ohms, double maxOut, double maxIn) {}
    private record Load(int node, double watts, double minVolts) {}

    /** The node a conductor can run to for a resistance to ground. */
    public static final int GROUND = -1;

    private int nodes;
    private final List<Conductor> conductors = new ArrayList<>();
    private final List<Source> sources = new ArrayList<>();
    private final List<Load> loads = new ArrayList<>();

    public int node() { return nodes++; }

    public int nodes() { return nodes; }

    /** A resistance between two nodes, or from a node to {@link #GROUND}; returns its index. */
    public int conductor(int a, int b, double ohms) {
        conductors.add(new Conductor(a, b, Math.max(ohms, 1e-15)));
        return conductors.size() - 1;
    }

    /** An EMF behind {@code ohms} at {@code node}, giving at most {@code maxOut} and taking at most {@code maxIn}; returns its index. */
    public int source(int node, double emf, double ohms, double maxOut, double maxIn) {
        sources.add(new Source(node, emf, Math.max(ohms, 1e-15), Math.max(0, maxOut), Math.max(0, maxIn)));
        return sources.size() - 1;
    }

    /** A load drawing {@code watts} at any voltage from {@code minVolts} up; below that it acts as the resistance it has there. */
    public int load(int node, double watts, double minVolts) {
        loads.add(new Load(node, Math.max(0, watts), Math.max(minVolts, 1e-9)));
        return loads.size() - 1;
    }

    /**
     * What the circuit settles at. Source watts are what leaves or (negative) enters the source's own store: its EMF's share, which
     * includes its internal loss. Load watts are what reaches each load. {@code share} is the brownout factor, 1 when healthy.
     */
    public record Solution(double[] volts, double[] conductorAmps, double[] sourceAmps, double[] sourceWatts, double[] loadWatts,
                           double[] loadAmps, double losses, double share, boolean converged) {}

    public Solution solve() {
        double scale = sources.stream().mapToDouble(Source::emf).max().orElse(0);
        if (nodes == 0 || scale <= 0) return dead();
        double[] v = new double[nodes];
        Arrays.fill(v, scale);
        int[] mode = new int[sources.size()];
        double[] direct = v.clone();
        int[] directMode = mode.clone();
        if (newton(1, direct, directMode, scale) && new Attempt(1, direct, directMode, true).healthy()) return new Attempt(1, direct, directMode, true).solution();
        if (!newton(0, v, mode, scale)) return new Attempt(0, v, mode, false).solution();
        // continuation: raise the loads from nothing, each step starting from the last answer, so Newton follows the high-voltage
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
        double[] amps = new double[loads.size()];
        return new Solution(new double[nodes], new double[conductors.size()], new double[sources.size()], new double[sources.size()],
                new double[loads.size()], amps, 0, 0, true);
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

        boolean healthy() {
            if (!converged) return false;
            for (Load l : loads) if (l.watts() * share > 0 && !(v[l.node()] >= l.minVolts() * (1 - 1e-9))) return false;
            return true;
        }

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
                sAmps[i] = sourceAmps(s, mode[i], v[s.node()]);
                sWatts[i] = v[s.node()] * sAmps[i] + sAmps[i] * sAmps[i] * s.ohms();
                losses += sAmps[i] * sAmps[i] * s.ohms();
            }
            double[] lWatts = new double[loads.size()], lAmps = new double[loads.size()];
            for (int i = 0; i < lWatts.length; i++) {
                Load l = loads.get(i);
                double volts = v[l.node()], want = l.watts() * share;
                lWatts[i] = volts >= l.minVolts() ? want : volts * volts * want / (l.minVolts() * l.minVolts());
                lAmps[i] = volts > 0 ? lWatts[i] / volts : 0;
            }
            return new Solution(v, cAmps, sAmps, sWatts, lWatts, lAmps, losses, share, converged);
        }
    }

    private static double at(double[] v, int node) { return node == GROUND ? 0 : v[node]; }

    /** Which part of its line a source is on at this voltage: holding its voltage, or at a current limit. */
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

    /** Newton steps at this load share from {@code v}, in place; true when it settles. */
    private boolean newton(double share, double[] v, int[] mode, double scale) {
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            for (int i = 0; i < mode.length; i++) mode[i] = modeAt(sources.get(i), v[sources.get(i).node()]);
            Sparse sparse = new Sparse(nodes);
            double[] b = new double[nodes];
            for (int i = 0; i < nodes; i++) add(sparse, i, i, GMIN);
            for (Conductor c : conductors) {
                double y = 1 / c.ohms();
                add(sparse, c.a(), c.a(), y);
                add(sparse, c.b(), c.b(), y);
                add(sparse, c.a(), c.b(), -y);
                add(sparse, c.b(), c.a(), -y);
            }
            for (int i = 0; i < mode.length; i++) {
                Source s = sources.get(i);
                double soft = 1 / (s.ohms() * STIFF);
                switch (mode[i]) {
                    case CC_OUT -> {
                        add(sparse, s.node(), s.node(), soft);
                        b[s.node()] += s.maxOut() + soft * outEdge(s);
                    }
                    case CC_IN -> {
                        add(sparse, s.node(), s.node(), soft);
                        b[s.node()] += -s.maxIn() + soft * inEdge(s);
                    }
                    default -> {
                        add(sparse, s.node(), s.node(), 1 / s.ohms());
                        b[s.node()] += s.emf() / s.ohms();
                    }
                }
            }
            for (Load l : loads) {
                double p = l.watts() * share;
                if (p <= 0) continue;
                // the load is the resistance it had at the last voltage (V² / P), never below its minimum: a relaxation that always moves
                // the line the way a real one settles, where Newton's method would follow a constant-power load's negative slope
                double volts = Math.max(v[l.node()], l.minVolts());
                add(sparse, l.node(), l.node(), p / (volts * volts));
            }
            double[] x = sparse.solve(b);
            if (x == null) return false;
            // step toward x, but stop just past the first current limit any source crosses: its mode changes there (breakpoint stepping)
            double alpha = 1;
            for (Source src : sources) {
                double from = v[src.node()], to = x[src.node()];
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
                for (int i = 0; i < mode.length; i++) mode[i] = modeAt(sources.get(i), v[sources.get(i).node()]);
                return true;
            }
        }
        return false;
    }

    private static void add(Sparse sparse, int r, int c, double value) {
        if (r == GROUND || c == GROUND) return;
        sparse.add(r, c, value);
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
