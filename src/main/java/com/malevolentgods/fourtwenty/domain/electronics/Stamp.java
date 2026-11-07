package com.malevolentgods.fourtwenty.domain.electronics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * One linear system G·v = i to be solved: every part stamps a conductance between two nodes and a current into them. Every stamp is a
 * positive conductance (docs/electronics/audit-and-plan.md 2.6), so the matrix is symmetric positive definite once each loop's reference
 * node is fixed at zero, and it is solved by elimination with no pivoting, fewest neighbours first: Wick networks are mostly trees,
 * solved in linear time.
 *
 * <p>Stiff parts (an ideal source's tiny resistance beside a kilophu resistor) cost digits in elimination, so the answer is refined: the
 * current left over at each node, against the original matrix, is solved for again with the same factors, as SPICE does. {@link #GROUND}
 * is the node −1, held at zero.
 */
public final class Stamp {
    public static final int GROUND = -1;
    private static final int REFINEMENTS = 3;

    private final List<Map<Integer, Double>> rows = new ArrayList<>();
    private final double[] rhs;
    private final boolean[] fixed;

    Stamp(int nodes, boolean[] fixed) {
        for (int i = 0; i < nodes; i++) rows.add(new HashMap<>());
        rhs = new double[nodes];
        this.fixed = fixed;
    }

    int size() { return rhs.length; }

    private boolean free(int node) { return node != GROUND && !fixed[node]; }

    private void add(int r, int c, double value) {
        // a fixed node is held at zero: its own row is dropped, and in other rows it contributes nothing
        if (!free(r) || !free(c) || value == 0) return;
        rows.get(r).merge(c, value, Double::sum);
    }

    /** A conductance {@code g} between {@code a} and {@code b} (either may be {@link #GROUND}). */
    public void conductance(int a, int b, double g) {
        add(a, a, g);
        add(b, b, g);
        add(a, b, -g);
        add(b, a, -g);
    }

    /** A current {@code amps} driven from {@code from} through the part to {@code to}, i.e. drawn out of {@code from} and into {@code to}. */
    public void current(int from, int to, double amps) {
        if (free(from)) rhs[from] -= amps;
        if (free(to)) rhs[to] += amps;
    }

    /**
     * A symmetric two-port coupling (a transformer referred through its winding resistance): the 2×2 block [[g11, g12], [g12, g22]] on
     * the port voltages v1 = a1 − b1 and v2 = a2 − b2.
     */
    public void coupling(int a1, int b1, int a2, int b2, double g11, double g12, double g22) {
        conductance(a1, b1, g11);
        conductance(a2, b2, g22);
        add(a1, a2, g12);
        add(a1, b2, -g12);
        add(b1, a2, -g12);
        add(b1, b2, g12);
        add(a2, a1, g12);
        add(b2, a1, -g12);
        add(a2, b1, -g12);
        add(b2, b1, g12);
    }

    /** The voltages, or null if the system is singular. */
    double[] solve() {
        int n = rhs.length;
        List<Map<Integer, Double>> original = new ArrayList<>(n);
        for (Map<Integer, Double> r : rows) original.add(new HashMap<>(r));
        Factor f = factor(n);
        if (f == null) return null;
        double[] x = f.apply(rhs);
        if (x == null) return null;
        for (int round = 0; round < REFINEMENTS; round++) {
            double[] residual = rhs.clone();
            // every round, whatever the size of the leftovers: a stiff source's huge current must not hide a resistor's small one. The
            // leftover is summed in double-double (exact products by fma, compensated sums), or it would be lost under the source's current
            double worst = 0;
            for (int i = 0; i < n; i++) {
                if (fixed[i]) continue;
                double hi = rhs[i], lo = 0;
                for (var e : original.get(i).entrySet()) {
                    double a = e.getValue(), xj = x[e.getKey()];
                    double p = -a * xj, pe = -Math.fma(a, xj, p);
                    double sum = hi + p, bp = sum - hi, se = (hi - (sum - bp)) + (p - bp);
                    hi = sum;
                    lo += se + pe;
                }
                residual[i] = hi + lo;
                worst = Math.max(worst, Math.abs(residual[i]));
            }
            if (worst == 0) break;
            double[] dx = f.apply(residual);
            if (dx == null) break;
            for (int i = 0; i < n; i++) x[i] += dx[i];
        }
        return x;
    }

    /** The elimination, kept so it can be applied to more than one right-hand side. */
    private record Factor(int[] order, int count, double[] pivots, int[][] targets, double[][] factors, List<Map<Integer, Double>> eliminated,
                          boolean[] fixed) {
        double[] apply(double[] rhs) {
            double[] b = rhs.clone();
            for (int s = 0; s < count; s++) {
                int k = order[s];
                for (int t = 0; t < targets[k].length; t++) b[targets[k][t]] -= factors[k][t] * b[k];
            }
            double[] x = new double[b.length];
            for (int s = count - 1; s >= 0; s--) {
                int k = order[s];
                double sum = b[k];
                for (var e : eliminated.get(k).entrySet()) sum -= e.getValue() * x[e.getKey()];
                x[k] = sum / pivots[k];
                if (!Double.isFinite(x[k])) return null;
            }
            return x;
        }
    }

    private Factor factor(int n) {
        boolean[] done = new boolean[n];
        int[] order = new int[n];
        double[] pivots = new double[n];
        int[][] targets = new int[n][];
        double[][] factors = new double[n][];
        List<Map<Integer, Double>> eliminated = new ArrayList<>(java.util.Collections.nCopies(n, null));
        PriorityQueue<long[]> queue = new PriorityQueue<>((p, q) -> Long.compare(p[0], q[0]));
        for (int i = 0; i < n; i++) if (!fixed[i]) queue.add(new long[]{rows.get(i).size(), i});
        int step = 0;
        while (!queue.isEmpty()) {
            long[] top = queue.poll();
            int k = (int) top[1];
            if (done[k] || top[0] != rows.get(k).size()) continue;
            Map<Integer, Double> row = rows.get(k);
            double pivot = row.getOrDefault(k, 0.0);
            if (!(Math.abs(pivot) > 1e-300)) return null;
            done[k] = true;
            order[step++] = k;
            pivots[k] = pivot;
            Map<Integer, Double> others = new HashMap<>(row);
            others.remove(k);
            eliminated.set(k, others);
            targets[k] = new int[others.size()];
            factors[k] = new double[others.size()];
            int t = 0;
            for (var ej : others.entrySet()) {
                int j = ej.getKey();
                Map<Integer, Double> rowJ = rows.get(j);
                double f = ej.getValue() / pivot;
                targets[k][t] = j;
                factors[k][t++] = f;
                for (var ei : others.entrySet()) rowJ.merge(ei.getKey(), -f * ei.getValue(), Double::sum);
                rowJ.remove(k);
                queue.add(new long[]{rowJ.size(), j});
            }
        }
        return new Factor(order, step, pivots, targets, factors, eliminated, fixed);
    }
}
