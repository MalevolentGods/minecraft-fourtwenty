package com.malevolentgods.fourtwenty.domain.electronics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * One linear system G·v = i to be solved: every part stamps a conductance between two nodes and a current into them, and an active part
 * (a transistor, an op-amp) a transconductance, a current at one pair of nodes set by the voltage across another. The pattern of the
 * matrix is kept symmetric (every entry has its mirror, zero if need be) but its values needn't be, and it is solved by Gaussian
 * elimination fewest neighbours first, without pivoting: Wick networks are mostly trees, solved in linear time, and what passive parts
 * stamp is symmetric positive definite once each loop's reference node is fixed at zero (docs/electronics/audit-and-plan.md 2.6).
 *
 * <p>Stiff parts (an ideal source's tiny resistance beside a kilophu resistor) cost digits in elimination, so the answer is refined: the
 * current left over at each node, against the original matrix, is solved for again with the same factors, as SPICE does. {@link #GROUND}
 * is the node −1, held at zero.
 *
 * <p>Where the matrix's entries sit hardly ever changes from one solve to the next (a circuit's parts stamp the same places every Newton
 * step and every event), so the elimination is worked out once per pattern, its order and its fill, as a {@link Plan}; each solve then
 * only does the arithmetic, on arrays. {@link Plans} keeps the last few, per circuit.
 */
public final class Stamp {
    public static final int GROUND = -1;
    private static final int REFINEMENTS = 3;

    private final int n;
    private final boolean[] fixed;
    private final double[] rhs;
    private final Plans plans;
    // the entries as stamped, in order: row, column, value (a mirror entry is stamped too, at zero)
    private int[] pos = new int[256];
    private double[] val = new double[128];
    private int count;

    Stamp(int nodes, boolean[] fixed) { this(nodes, fixed, null); }

    Stamp(int nodes, boolean[] fixed, Plans plans) {
        this.n = nodes;
        this.fixed = fixed;
        this.rhs = new double[nodes];
        this.plans = plans == null ? new Plans() : plans;
    }

    int size() { return n; }

    private boolean free(int node) { return node != GROUND && !fixed[node]; }

    private void entry(int r, int c, double v) {
        if (count == val.length) {
            val = Arrays.copyOf(val, count * 2);
            pos = Arrays.copyOf(pos, count * 4);
        }
        pos[2 * count] = r;
        pos[2 * count + 1] = c;
        val[count++] = v;
    }

    private void add(int r, int c, double value) {
        // a fixed node is held at zero: its own row is dropped, and in other rows it contributes nothing
        if (!free(r) || !free(c) || value == 0) return;
        entry(r, c, value);
        // keep the pattern symmetric: the mirror entry exists, even if it's zero
        if (r != c) entry(c, r, 0);
    }

    /**
     * A transconductance: a current {@code gm}·(v(ctrlPlus) − v(ctrlMinus)) driven from {@code from} through the part to {@code to}, as a
     * transistor's channel answers its gate, or an op-amp's output its inputs.
     */
    public void transconductance(int from, int to, int ctrlPlus, int ctrlMinus, double gm) {
        add(from, ctrlPlus, gm);
        add(from, ctrlMinus, -gm);
        add(to, ctrlPlus, -gm);
        add(to, ctrlMinus, gm);
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
        Plan plan = plans.find(n, fixed, pos, count);
        double[] a = new double[plan.slots];
        for (int e = 0; e < count; e++) a[plan.slotOf[e]] += val[e];
        double[] original = Arrays.copyOf(a, plan.originalSlots);
        double[] factors = new double[plan.updates];
        if (!plan.factor(a, factors)) return null;
        double[] x = plan.apply(a, factors, rhs);
        if (x == null) return null;
        double[] hi = new double[n], lo = new double[n];
        for (int round = 0; round < REFINEMENTS; round++) {
            // every round, whatever the size of the leftovers: a stiff source's huge current must not hide a resistor's small one. The
            // leftover is summed in double-double (exact products by fma, compensated sums), or it would be lost under the source's current
            System.arraycopy(rhs, 0, hi, 0, n);
            Arrays.fill(lo, 0);
            for (int s = 0; s < plan.originalSlots; s++) {
                int i = plan.slotRow[s];
                double av = original[s], xj = x[plan.slotCol[s]];
                double p = -av * xj, pe = -Math.fma(av, xj, p);
                double sum = hi[i] + p, bp = sum - hi[i], se = (hi[i] - (sum - bp)) + (p - bp);
                hi[i] = sum;
                lo[i] += se + pe;
            }
            double[] residual = new double[n];
            double worst = 0;
            for (int i = 0; i < n; i++) {
                if (fixed[i]) continue;
                residual[i] = hi[i] + lo[i];
                worst = Math.max(worst, Math.abs(residual[i]));
            }
            if (worst == 0) break;
            double[] dx = plan.apply(a, factors, residual);
            if (dx == null) break;
            // once a correction moves nothing past the last digit or so, another round can only find the same again
            double moved = 0, size = 0;
            for (int i = 0; i < n; i++) {
                x[i] += dx[i];
                moved = Math.max(moved, Math.abs(dx[i]));
                size = Math.max(size, Math.abs(x[i]));
            }
            if (moved <= 1e-15 * size) break;
        }
        return x;
    }

    /** The last few elimination plans a circuit used, tried newest first. */
    public static final class Plans {
        private static final int KEEP = 6;
        private final List<Plan> recent = new ArrayList<>();

        Plan find(int n, boolean[] fixed, int[] pos, int count) {
            for (int i = 0; i < recent.size(); i++) {
                Plan p = recent.get(i);
                if (p.matches(n, fixed, pos, count)) {
                    if (i > 0) {
                        recent.remove(i);
                        recent.add(0, p);
                    }
                    return p;
                }
            }
            Plan p = Plan.of(n, fixed, pos, count);
            if (Boolean.getBoolean("fourtwenty.profile") && n > 300) {
                long work = 0;
                int maxm = 0;
                for (int[] o : p.others) if (o != null) { work += (long) o.length * o.length; maxm = Math.max(maxm, o.length); }
                System.out.println("PROFILE plan n " + n + " entries " + count + " slots " + p.slots + " original " + p.originalSlots + " work " + work + " max degree " + maxm);
            }
            recent.add(0, p);
            if (recent.size() > KEEP) recent.remove(recent.size() - 1);
            return p;
        }
    }

    /**
     * An elimination worked out for one pattern: where each stamped entry goes (its slot), the order the nodes are eliminated in
     * (fewest neighbours first, counting the fill as it comes), and for each step which slots it reads and which it updates.
     */
    static final class Plan {
        final int n;
        final boolean[] fixed;
        final int[] pos;                // the pattern it was made for: each entry's row and column
        final int count;
        final int[] slotOf;             // entry -> slot
        final int slots, originalSlots; // slots in all; the first originalSlots are the stamped ones (the rest is fill)
        final int[] slotRow, slotCol;   // each original slot's place
        // the elimination, step by step: its node, its pivot's slot, the nodes still beside it (and the slots of its row there), and,
        // for each of those, the slot of its entry in the pivot's column and the slots of its row it updates, one per neighbour
        final int steps;
        final int[] node, pivotSlot;
        final int[][] others, rowSlots, colSlots;
        final int[][][] updateSlots;
        final int[] factorBase;         // where each step's factors start in the factors array
        final int updates;

        private Plan(int n, boolean[] fixed, int[] pos, int count, int[] slotOf, int slots, int originalSlots, int[] slotRow, int[] slotCol,
                     int steps, int[] node, int[] pivotSlot, int[][] others, int[][] rowSlots, int[][] colSlots, int[][][] updateSlots) {
            this.n = n;
            this.fixed = fixed.clone();
            this.pos = Arrays.copyOf(pos, 2 * count);
            this.count = count;
            this.slotOf = slotOf;
            this.slots = slots;
            this.originalSlots = originalSlots;
            this.slotRow = slotRow;
            this.slotCol = slotCol;
            this.steps = steps;
            this.node = node;
            this.pivotSlot = pivotSlot;
            this.others = others;
            this.rowSlots = rowSlots;
            this.colSlots = colSlots;
            this.updateSlots = updateSlots;
            this.factorBase = new int[steps + 1];
            for (int s = 0; s < steps; s++) factorBase[s + 1] = factorBase[s] + others[s].length;
            this.updates = factorBase[steps];
        }

        boolean matches(int n2, boolean[] fixed2, int[] pos2, int count2) {
            return n2 == n && count2 == count && Arrays.equals(fixed, fixed2) && Arrays.equals(pos, 0, 2 * count, pos2, 0, 2 * count);
        }

        static Plan of(int n, boolean[] fixed, int[] pos, int count) {
            // each row: column -> slot; the stamped slots first, so they can be read back as the original matrix
            List<Map<Integer, Integer>> rows = new ArrayList<>(n);
            for (int i = 0; i < n; i++) rows.add(new HashMap<>());
            int[] slotOf = new int[count];
            int[] next = {0};
            List<int[]> places = new ArrayList<>();
            for (int e = 0; e < count; e++) {
                int r = pos[2 * e], c = pos[2 * e + 1];
                slotOf[e] = rows.get(r).computeIfAbsent(c, k -> {
                    places.add(new int[]{r, c});
                    return next[0]++;
                });
            }
            int originalSlots = next[0];
            int[] slotRow = new int[originalSlots], slotCol = new int[originalSlots];
            for (int s = 0; s < originalSlots; s++) {
                slotRow[s] = places.get(s)[0];
                slotCol[s] = places.get(s)[1];
            }
            // the elimination order, as the solve did it: fewest neighbours first, the fill counted as it comes
            boolean[] done = new boolean[n];
            int[] node = new int[n], pivotSlot = new int[n];
            int[][] others = new int[n][], rowSlots = new int[n][], colSlots = new int[n][];
            int[][][] updateSlots = new int[n][][];
            PriorityQueue<long[]> queue = new PriorityQueue<>((p, q) -> Long.compare(p[0], q[0]));
            for (int i = 0; i < n; i++) if (!fixed[i]) queue.add(new long[]{rows.get(i).size(), i});
            int step = 0;
            while (!queue.isEmpty()) {
                long[] top = queue.poll();
                int k = (int) top[1];
                if (done[k] || top[0] != rows.get(k).size()) continue;
                Map<Integer, Integer> row = rows.get(k);
                Integer ps = row.get(k);
                if (ps == null) {
                    // no diagonal: singular; give it a slot that stays zero, so the solve says so
                    ps = next[0]++;
                    row.put(k, ps);
                }
                done[k] = true;
                node[step] = k;
                pivotSlot[step] = ps;
                int m = row.size() - 1;
                int[] oth = new int[m], rs = new int[m];
                int t = 0;
                for (var e : row.entrySet()) {
                    if (e.getKey() == k) continue;
                    oth[t] = e.getKey();
                    rs[t++] = e.getValue();
                }
                int[] cs = new int[m];
                int[][] us = new int[m][m];
                for (int a = 0; a < m; a++) {
                    Map<Integer, Integer> rowJ = rows.get(oth[a]);
                    Integer jk = rowJ.get(k);
                    if (jk == null) {
                        jk = next[0]++;
                        rowJ.put(k, jk);
                    }
                    cs[a] = jk;
                    for (int b = 0; b < m; b++) us[a][b] = rowJ.computeIfAbsent(oth[b], c -> next[0]++);
                    rowJ.remove(k);
                    queue.add(new long[]{rowJ.size(), oth[a]});
                }
                others[step] = oth;
                rowSlots[step] = rs;
                colSlots[step] = cs;
                updateSlots[step] = us;
                step++;
            }
            return new Plan(n, fixed, pos, count, slotOf, next[0], originalSlots, slotRow, slotCol, step, node, pivotSlot, others, rowSlots,
                    colSlots, updateSlots);
        }

        /** Eliminates in place: {@code a} becomes the factors' rows, {@code f} each step's multipliers. False if a pivot vanishes. */
        boolean factor(double[] a, double[] f) {
            for (int s = 0; s < steps; s++) {
                double pivot = a[pivotSlot[s]];
                if (!(Math.abs(pivot) > 1e-300)) return false;
                int[] rs = rowSlots[s], cs = colSlots[s];
                int base = factorBase[s];
                for (int t = 0; t < cs.length; t++) {
                    double m = a[cs[t]] / pivot;
                    f[base + t] = m;
                    if (m == 0) continue;
                    int[] us = updateSlots[s][t];
                    for (int u = 0; u < rs.length; u++) a[us[u]] -= m * a[rs[u]];
                }
            }
            return true;
        }

        double[] apply(double[] a, double[] f, double[] rhs) {
            double[] b = rhs.clone();
            for (int s = 0; s < steps; s++) {
                double bk = b[node[s]];
                if (bk == 0) continue;
                int[] oth = others[s];
                int base = factorBase[s];
                for (int t = 0; t < oth.length; t++) b[oth[t]] -= f[base + t] * bk;
            }
            double[] x = new double[n];
            for (int s = steps - 1; s >= 0; s--) {
                int k = node[s];
                double sum = b[k];
                int[] oth = others[s], rs = rowSlots[s];
                for (int u = 0; u < oth.length; u++) sum -= a[rs[u]] * x[oth[u]];
                x[k] = sum / a[pivotSlot[s]];
                if (!Double.isFinite(x[k])) return null;
            }
            return x;
        }
    }
}
