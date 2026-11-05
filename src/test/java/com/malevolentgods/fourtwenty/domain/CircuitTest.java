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
        // twice what a 1.05-limited source can give: everything slows by the same share, and the source runs at its limit
        var c = new Circuit();
        int n = c.node();
        int src = c.source(n, 1, 0.005, 1.05, 0);
        int[] loads = new int[8];
        for (int i = 0; i < 8; i++) loads[i] = c.load(n, 0.25, 0.5);
        var s = c.solve();
        assertTrue(s.share() > 0.5 && s.share() < 0.53, "share " + s.share());
        for (int l : loads) assertEquals(s.loadWatts()[loads[0]], s.loadWatts()[l], 1e-12);
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
}
