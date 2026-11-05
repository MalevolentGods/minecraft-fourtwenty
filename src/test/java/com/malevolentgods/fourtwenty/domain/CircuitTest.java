package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Each case is worked by hand first; the numbers in the comments are that working. */
class CircuitTest {
    private static final double BIG = 1e9;

    @Test
    void aDivider() {
        // 10 Kb behind 1 Φ into 4 Φ: 2 Ig, 8 Kb across the load
        var c = new Circuit();
        int n = c.node();
        int src = c.source(n, 10, 1, BIG, BIG);
        int r = c.conductor(n, Circuit.GROUND, 4);
        var s = c.solve();
        assertEquals(8, s.volts()[n], 1e-9);
        assertEquals(2, s.conductorAmps()[r], 1e-9);
        assertEquals(2, s.sourceAmps()[src], 1e-9);
        assertEquals(20, s.sourceWatts()[src], 1e-9, "the EMF gives 20 W: 16 to the load, 4 lost inside");
        assertEquals(4 + 16, s.losses() + 0, 1e-9 + 16, "losses count the inside");
        assertEquals(1, s.share(), 0);
    }

    @Test
    void twoSourcesShare() {
        // equal droop: each gives half of a 1 W load
        var c = new Circuit();
        int n = c.node();
        int a = c.source(n, 1, 0.005, 1.05, 0), b = c.source(n, 1, 0.005, 1.05, 0);
        c.load(n, 1, 0.5);
        var s = c.solve();
        assertEquals(s.sourceAmps()[a], s.sourceAmps()[b], 1e-12);
        assertEquals(1, s.volts()[n] * (s.sourceAmps()[a] + s.sourceAmps()[b]), 1e-9);
    }

    @Test
    void fourMachinesOnAHookahGetFullPower() {
        // the standard base, in pu: E 1, r 0.005, limit 1.05, four 0.25 W loads. V(1 - V) = 0.005, so V = 0.994975 and I = 1.005051
        var c = new Circuit();
        int n = c.node();
        int src = c.source(n, 1, 0.005, 1.05, 0);
        int[] loads = new int[4];
        for (int i = 0; i < 4; i++) loads[i] = c.load(n, 0.25, 0.5);
        var s = c.solve();
        assertEquals(1, s.share(), 0, "no brownout");
        assertEquals(0.994975, s.volts()[n], 1e-6);
        for (int l : loads) assertEquals(0.25, s.loadWatts()[l], 1e-12);
        assertEquals(1.005051, s.sourceAmps()[src], 1e-6);
    }

    @Test
    void eightMachinesBrownOutEvenly() {
        // twice what a 1.05-limited source can give: it holds its limit, the line sags under the machines' rated voltage, and each one
        // becomes its rated resistance (4 pu): eight in parallel are 0.5, so 0.525 pu and 0.069 W each, all alike
        var c = new Circuit();
        int n = c.node();
        int src = c.source(n, 1, 0.005, 1.05, 0);
        int[] loads = new int[8];
        for (int i = 0; i < 8; i++) loads[i] = c.load(n, 0.25, 1.0);
        var s = c.solve();
        for (int l : loads) assertEquals(s.loadWatts()[loads[0]], s.loadWatts()[l], 1e-12);
        assertEquals(0.525, s.volts()[n], 1e-3);
        assertEquals(0.0689, s.loadWatts()[loads[0]], 1e-3);
        assertEquals(1.05, s.sourceAmps()[src], 0.01);
    }

    @Test
    void aBatteryChargesFromWhatTheSourceCanSpare() {
        // a Hookah (E 1, limit 1.05) and an empty Stash (E 0.96, r 0.01, limit 1 each way): the Stash would take 2.67, so it takes its 1
        var c = new Circuit();
        int n = c.node();
        int hookah = c.source(n, 1, 0.005, 1.05, 0);
        int stash = c.source(n, 0.96, 0.01, 1, 1);
        var s = c.solve();
        assertEquals(-1, s.sourceAmps()[stash], 1e-4);
        assertEquals(1, s.sourceAmps()[hookah], 1e-4);
        assertEquals(0.995, s.volts()[n], 1e-6);
        assertTrue(s.sourceWatts()[stash] < 0, "energy goes into the Stash");
    }

    @Test
    void machinesComeBeforeCharging() {
        // the same, with four machines: the Hookah is at its limit, the line falls, and the Stash takes only what's left
        var c = new Circuit();
        int n = c.node();
        int hookah = c.source(n, 1, 0.005, 1.05, 0);
        int stash = c.source(n, 0.96, 0.01, 1, 1);
        int[] loads = new int[4];
        for (int i = 0; i < 4; i++) loads[i] = c.load(n, 0.25, 0.5);
        var s = c.solve();
        assertEquals(1, s.share(), 0);
        for (int l : loads) assertEquals(0.25, s.loadWatts()[l], 1e-12);
        assertEquals(1.05, s.sourceAmps()[hookah], 1e-4);
        assertTrue(s.sourceAmps()[stash] < 0 && s.sourceAmps()[stash] > -0.1, "a trickle: " + s.sourceAmps()[stash]);
    }

    @Test
    void aBatteryRunsTheLoadOnItsOwn() {
        // E 0.97 behind 0.01 into 0.5 W: V(0.97 - V) = 0.005, so V = 0.964818
        var c = new Circuit();
        int n = c.node();
        int stash = c.source(n, 0.97, 0.01, 1, 1);
        int load = c.load(n, 0.5, 0.5);
        var s = c.solve();
        assertEquals(0.964818, s.volts()[n], 1e-6);
        assertEquals(0.5, s.loadWatts()[load], 1e-12);
        assertEquals(0.5 / 0.964818, s.sourceAmps()[stash], 1e-6);
    }

    @Test
    void twoBatteriesSettleByTheirDifference() {
        // 0.98 and 0.96 behind 0.01 each: 1 Ig from the fuller, and the line between at 0.97
        var c = new Circuit();
        int a = c.node(), b = c.node();
        int full = c.source(a, 0.98, 0.01, 5, 5), low = c.source(b, 0.96, 0.01, 5, 5);
        c.conductor(a, b, 1e-9);
        var s = c.solve();
        assertEquals(1, s.sourceAmps()[full], 1e-6);
        assertEquals(-1, s.sourceAmps()[low], 1e-6);
        assertEquals(0.97, s.volts()[a], 1e-6);
    }

    @Test
    void aRingSplitsTheCurrent() {
        // 10 Kb into 10 Φ through two paths, 1 Φ and 1 + 1 Φ: 2/3 Φ in parallel, 0.9375 Ig, split 2 : 1
        var c = new Circuit();
        int s0 = c.node(), n1 = c.node(), n2 = c.node();
        c.source(s0, 10, 1e-12, BIG, BIG);
        int direct = c.conductor(s0, n1, 1);
        int around = c.conductor(s0, n2, 1);
        c.conductor(n2, n1, 1);
        c.conductor(n1, Circuit.GROUND, 10);
        var s = c.solve();
        assertEquals(0.625, s.conductorAmps()[direct], 1e-9);
        assertEquals(0.3125, s.conductorAmps()[around], 1e-9);
    }

    @Test
    void aHundredBlockPuffLineLosesThreePercent() {
        // 840 W (its rated 200 Ig) through 100 Hemp Wicks of 6.3 µΦ: about 200² × 6.3e-4 = 25 W lost, 3%
        var rules = CircuitRules.Numbers.DEFAULT;
        var c = new Circuit();
        int start = c.node();
        c.source(start, 4.2, 1e-9, BIG, 0);
        int at = start;
        for (int i = 0; i < 100; i++) {
            int next = c.node();
            c.conductor(at, next, rules.wickOhms(BIRules.Tier.PUFF));
            at = next;
        }
        int load = c.load(at, 840, 2.1);
        var s = c.solve();
        double lost = s.losses() / (s.loadWatts()[load] + s.losses());
        assertTrue(lost > 0.02 && lost < 0.05, "lost " + lost);
        assertEquals(840, s.loadWatts()[load], 1e-9);
    }

    @Test
    void steppingUpATierHalvesTheLoss() {
        // the same 840 W at Toke on Toke Wicks: 20 Ig of a 40 Ig rating, so half the share lost
        var rules = CircuitRules.Numbers.DEFAULT;
        double puff = loss(rules, BIRules.Tier.PUFF), toke = loss(rules, BIRules.Tier.TOKE);
        assertEquals(puff / 2, toke, puff * 0.02);
    }

    private static double loss(CircuitRules.Numbers rules, BIRules.Tier tier) {
        var c = new Circuit();
        int start = c.node();
        c.source(start, rules.volts(tier), 1e-9, BIG, 0);
        int at = start;
        for (int i = 0; i < 100; i++) {
            int next = c.node();
            c.conductor(at, next, rules.wickOhms(tier));
            at = next;
        }
        int load = c.load(at, 840, rules.minVolts(tier));
        var s = c.solve();
        return s.losses() / (s.loadWatts()[load] + s.losses());
    }

    @Test
    void noSourceMeansNothingFlows() {
        var c = new Circuit();
        int n = c.node();
        int load = c.load(n, 100, 1);
        var s = c.solve();
        assertEquals(0, s.loadWatts()[load], 0);
        assertArrayEquals(new double[]{0}, s.volts(), 0);
    }

    @Test
    void currentIsConservedAtEveryNode() {
        // a small mesh: two sources, a battery, three loads, five conductors
        var c = new Circuit();
        int a = c.node(), b = c.node(), d = c.node(), e = c.node();
        c.source(a, 1, 0.005, 1.05, 0);
        c.source(d, 1, 0.005, 1.05, 0);
        c.source(e, 0.97, 0.01, 1, 1);
        c.load(b, 0.3, 0.5);
        c.load(b, 0.2, 0.5);
        c.load(e, 0.4, 0.5);
        c.conductor(a, b, 0.002);
        c.conductor(b, d, 0.003);
        c.conductor(d, e, 0.001);
        c.conductor(e, a, 0.004);
        c.conductor(a, d, 0.002);
        var s = c.solve();
        assertTrue(s.converged());
        double[] net = new double[4];
        int[][] ends = {{a, b}, {b, d}, {d, e}, {e, a}, {a, d}};
        for (int i = 0; i < ends.length; i++) {
            net[ends[i][0]] -= s.conductorAmps()[i];
            net[ends[i][1]] += s.conductorAmps()[i];
        }
        int[] sourceAt = {a, d, e};
        for (int i = 0; i < 3; i++) net[sourceAt[i]] += s.sourceAmps()[i];
        int[] loadAt = {b, b, e};
        for (int i = 0; i < 3; i++) net[loadAt[i]] -= s.loadAmps()[i];
        for (double x : net) assertEquals(0, x, 1e-9);
    }

    @Test
    void aBigNetworkSolvesToo() {
        // 600 nodes in a line takes the sparse path; the answer matches the series sum
        var c = new Circuit();
        int start = c.node();
        c.source(start, 4.2, 1e-9, BIG, 0);
        int at = start;
        for (int i = 0; i < 600; i++) {
            int next = c.node();
            c.conductor(at, next, 1e-4);
            at = next;
        }
        int r = c.conductor(at, Circuit.GROUND, 1.0);
        var s = c.solve();
        assertEquals(4.2 / 1.06, s.conductorAmps()[r], 1e-6);
    }

    @Test
    void aBigMeshSolvesQuickly() {
        // the worst case for a base: a 30 x 30 mesh of Puff Wicks (900 nodes, nothing to collapse), four Hookahs, 100 machines
        var rules = CircuitRules.Numbers.DEFAULT;
        double wick = rules.wickOhms(BIRules.Tier.PUFF);
        long best = Long.MAX_VALUE;
        Circuit.Solution s = null;
        for (int round = 0; round < 3; round++) {
            var c = new Circuit();
            int[][] n = new int[30][30];
            for (int x = 0; x < 30; x++) for (int y = 0; y < 30; y++) n[x][y] = c.node();
            for (int x = 0; x < 30; x++) for (int y = 0; y < 30; y++) {
                if (x + 1 < 30) c.conductor(n[x][y], n[x + 1][y], wick);
                if (y + 1 < 30) c.conductor(n[x][y], n[x][y + 1], wick);
            }
            for (int[] at : new int[][]{{0, 0}, {29, 0}, {0, 29}, {29, 29}}) c.source(n[at[0]][at[1]], 4.2, rules.sourceOhms(BIRules.Tier.PUFF), rules.sourceAmps(BIRules.Tier.PUFF), 0);
            for (int i = 0; i < 100; i++) c.load(n[(i * 7) % 30][(i * 13) % 30], 16, 2.1);
            long start = System.nanoTime();
            s = c.solve();
            best = Math.min(best, System.nanoTime() - start);
        }
        assertTrue(s.converged() && s.share() == 1, "four Hookahs carry 1,600 W");
        System.out.println("900-node mesh, 100 loads: " + best / 1_000_000.0 + " ms");
        assertTrue(best < 250_000_000L, "solved in " + best / 1_000_000.0 + " ms");
    }

    @Test
    void aLongLineWithMachinesSolvesFast() {
        // SC-004: 1,000 Wicks and 100 devices, uncollapsed: a line with a machine every ten blocks and Hookahs at both ends
        var rules = CircuitRules.Numbers.DEFAULT;
        long best = Long.MAX_VALUE;
        for (int round = 0; round < 5; round++) {
            var c = new Circuit();
            int prev = c.node(), first = prev;
            for (int i = 1; i < 1000; i++) {
                int next = c.node();
                c.conductor(prev, next, rules.wickOhms(BIRules.Tier.PUFF));
                if (i % 10 == 0) c.load(next, 8, 2.1);
                prev = next;
            }
            for (int end : new int[]{first, prev}) c.source(end, 4.2, rules.sourceOhms(BIRules.Tier.PUFF), rules.sourceAmps(BIRules.Tier.PUFF), 0);
            long start = System.nanoTime();
            var s = c.solve();
            best = Math.min(best, System.nanoTime() - start);
            assertTrue(s.converged() && s.share() == 1);
        }
        System.out.println("1,000-Wick line, 100 loads: " + best / 1_000_000.0 + " ms");
        assertTrue(best < 20_000_000L, "solved in " + best / 1_000_000.0 + " ms");
    }

    // ---- loops: no ground, every part between its own + and - ----

    private static int[] hookah(Circuit c) {
        int plus = c.node(), minus = c.node();
        c.source(plus, minus, 1, 0.005, 1.05, 0);
        return new int[]{plus, minus};
    }

    @Test
    void parallelLoadsShareTheVoltage() {
        var c = new Circuit();
        int[] h = hookah(c);
        int[] loads = {c.load(h[0], h[1], 0.25, 0.05), c.load(h[0], h[1], 0.25, 0.05), c.load(h[0], h[1], 0.25, 0.05)};
        var s = c.solve();
        double v = s.volts()[h[0]] - s.volts()[h[1]];
        for (int l : loads) {
            assertEquals(0.25, s.loadWatts()[l], 1e-9);
            assertEquals(0.25 / v, s.loadAmps()[l], 1e-9);
        }
        assertEquals(0.75 / v, s.sourceAmps()[0], 1e-6, "the source carries the sum");
    }

    @Test
    void seriesLoadsShareTheCurrentAndSplitTheVoltage() {
        // three equal machines rated for the line, + to -: one current through all, each a third of the voltage and, as resistors sized
        // for the full voltage, a ninth of the power
        var c = new Circuit();
        int[] h = hookah(c);
        int a = c.node(), b = c.node();
        int l1 = c.load(h[0], a, 0.25, 1.0), l2 = c.load(a, b, 0.25, 1.0), l3 = c.load(b, h[1], 0.25, 1.0);
        var s = c.solve();
        assertEquals(1, s.share(), 0);
        double total = s.volts()[h[0]] - s.volts()[h[1]];
        assertEquals(s.loadAmps()[l1], s.loadAmps()[l2], 1e-9);
        assertEquals(s.loadAmps()[l2], s.loadAmps()[l3], 1e-9);
        double v1 = s.volts()[h[0]] - s.volts()[a], v2 = s.volts()[a] - s.volts()[b], v3 = s.volts()[b] - s.volts()[h[1]];
        assertEquals(total, v1 + v2 + v3, 1e-9, "Kirchhoff's voltage law");
        assertEquals(total / 3, v2, 1e-6);
        assertEquals(0.25 / 9, s.loadWatts()[l2], 1e-3, "a ninth of its power");
    }

    @Test
    void aMixOfSeriesAndParallel() {
        // two loads in parallel, in series with a third: KCL at the middle node, KVL around the loop
        var c = new Circuit();
        int[] h = hookah(c);
        int mid = c.node();
        int p1 = c.load(h[0], mid, 0.2, 1.0), p2 = c.load(h[0], mid, 0.2, 1.0), s1 = c.load(mid, h[1], 0.4, 1.0);
        var s = c.solve();
        assertEquals(s.loadAmps()[p1] + s.loadAmps()[p2], s.loadAmps()[s1], 1e-6);
        assertEquals(s.volts()[h[0]] - s.volts()[h[1]], (s.volts()[h[0]] - s.volts()[mid]) + (s.volts()[mid] - s.volts()[h[1]]), 1e-12);
        for (int l : new int[]{p1, p2, s1}) assertEquals(Circuit.OK, s.loadStatus()[l]);
    }

    @Test
    void aReversedLoadTakesNothing() {
        var c = new Circuit();
        int[] h = hookah(c);
        int good = c.load(h[0], h[1], 0.25, 0.05), backwards = c.load(h[1], h[0], 0.25, 0.05);
        var s = c.solve();
        assertEquals(Circuit.REVERSED, s.loadStatus()[backwards]);
        assertEquals(0, s.loadWatts()[backwards], 0);
        assertEquals(0.25, s.loadWatts()[good], 1e-9, "and doesn't stop the others");
    }

    @Test
    void noReturnPathMeansNoCurrent() {
        // a supply line only: the load's - goes nowhere
        var c = new Circuit();
        int[] h = hookah(c);
        int dangling = c.node();
        int l = c.load(h[0], dangling, 0.25, 0.05);
        int ok = c.load(h[0], h[1], 0.25, 0.05);
        var s = c.solve();
        assertEquals(Circuit.NO_LOOP, s.loadStatus()[l]);
        assertEquals(0, s.loadWatts()[l], 0);
        assertEquals(Circuit.OK, s.loadStatus()[ok]);
        assertEquals(1, s.share(), 0, "an open load doesn't brown the rest out");
    }

    @Test
    void sourcesInSeriesAddTheirVoltages() {
        var c = new Circuit();
        int a = c.node(), mid = c.node(), b = c.node();
        c.source(a, mid, 1, 0.005, 1.05, 0);
        c.source(mid, b, 1, 0.005, 1.05, 0);
        int r = c.conductor(a, b, 10);
        var s = c.solve();
        assertEquals(2 / (10 + 0.01), s.conductorAmps()[r], 1e-9);
    }

    @Test
    void aShortRunsTheSourceFlatOut() {
        var c = new Circuit();
        int[] h = hookah(c);
        c.conductor(h[0], h[1], 1e-6);
        var s = c.solve();
        assertEquals(1.05, s.sourceAmps()[0], 0.005, "at its current limit (and its tiny slope past it), no more");
    }
}
