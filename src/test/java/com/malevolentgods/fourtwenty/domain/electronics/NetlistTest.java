package com.malevolentgods.fourtwenty.domain.electronics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The engine against exact answers (docs/electronics/audit-and-plan.md 3.4). */
class NetlistTest {
    private static final double TIGHT = 1e-6;

    private static void close(double expected, double actual, double relative, String what) {
        double tol = Math.abs(expected) * relative + 1e-12;
        assertTrue(Math.abs(expected - actual) <= tol, what + ": expected " + expected + ", got " + actual);
    }

    @Test void aDividerIsOhmsLaw() {
        var n = new Netlist();
        int top = n.node(), mid = n.node(), bottom = n.node();
        n.source(top, bottom, 4.2, 1e-6);
        n.resistor(top, mid, 1000);
        var lower = n.resistor(mid, bottom, 2000);
        assertTrue(n.operatingPoint());
        close(2.8, n.volts(mid) - n.volts(bottom), TIGHT, "two thirds of 4.2 Kb");
        close(1.4e-3, n.current(lower), TIGHT, "4.2 Kb over 3 kΦ");
    }

    @Test void aBalancedWheatstoneBridgeCarriesNothingAcross() {
        var n = new Netlist();
        int top = n.node(), left = n.node(), right = n.node(), bottom = n.node();
        n.source(top, bottom, 10, 1e-6);
        n.resistor(top, left, 100);
        n.resistor(left, bottom, 200);
        n.resistor(top, right, 300);
        n.resistor(right, bottom, 600);
        var bridge = n.resistor(left, right, 50);
        assertTrue(n.operatingPoint());
        assertTrue(Math.abs(n.current(bridge)) < 1e-9, "balanced: " + n.current(bridge));
    }

    @Test void aFloatingLoopNeedsNoGround() {
        var n = new Netlist();
        int a = n.node(), b = n.node();
        n.source(a, b, 42, 0.1);
        var load = n.resistor(a, b, 9.9);
        assertTrue(n.operatingPoint());
        close(4.2, n.current(load), TIGHT, "42 Kb over 10 Φ in all");
    }

    /** V(t) = V(1 − e^(−t/RC)) to 0.1% at τ, 2τ and 5τ, at twenty steps a time constant. */
    @Test void aCapacitorChargesOnItsTimeConstant() {
        var n = new Netlist();
        int s = n.node(), a = n.node(), b = n.node();
        n.source(s, b, 4.2, 1e-6);
        var sw = n.switchPart(s, a, false);
        int c = n.node();
        n.resistor(a, c, 1000);
        var cap = n.capacitor(c, b, 1e-3);
        assertTrue(n.operatingPoint());
        n.set(sw, true);
        double tau = 1.0;
        for (int k : new int[]{1, 2, 5}) {
            double t = k == 1 ? tau : k == 2 ? tau : 3 * tau;
            assertTrue(n.advance(t, (int) Math.round(20 * t / tau)));
            close(4.2 * (1 - Math.exp(-k)), n.volts(c) - n.volts(b), 1e-3, "at " + k + "τ");
        }
        close(4.2 * (1 - Math.exp(-5)), cap.volts(), 1e-3, "the capacitor remembers");
    }

    /** I(t) = (V/R)(1 − e^(−tR/L)). */
    @Test void anInductorsCurrentRisesOnItsTimeConstant() {
        var n = new Netlist();
        int s = n.node(), a = n.node(), m = n.node(), b = n.node();
        n.source(s, b, 4.2, 1e-6);
        var sw = n.switchPart(s, a, false);
        n.resistor(a, m, 4.2);
        var coil = n.inductor(m, b, 4.2);
        assertTrue(n.operatingPoint());
        n.set(sw, true);
        assertTrue(n.advance(1.0, 40));
        close(1 - Math.exp(-1), coil.amps(), 1e-3, "at τ = L/R = 1 s");
        assertTrue(n.advance(4.0, 160));
        close(1 - Math.exp(-5), coil.amps(), 1e-3, "at 5τ");
    }

    /** An underdamped RLC rings at ωd = √(1/LC − (R/2L)²), with no numerical damping beyond the circuit's own. */
    @Test void anRlcRingsAtItsNaturalFrequency() {
        var n = new Netlist();
        int s = n.node(), a = n.node(), m = n.node(), c = n.node(), b = n.node();
        n.source(s, b, 4.2, 1e-6);
        var sw = n.switchPart(s, a, false);
        double r = 0.5, l = 1e-2, cap = 1e-3;
        n.resistor(a, m, r);
        var coil = n.inductor(m, c, l);
        n.capacitor(c, b, cap);
        assertTrue(n.operatingPoint());
        n.set(sw, true);
        double wd = Math.sqrt(1 / (l * cap) - Math.pow(r / (2 * l), 2));
        double period = 2 * Math.PI / wd, h = period / 400;
        double last = 0, t = 0;
        java.util.List<Double> crossings = new java.util.ArrayList<>();
        for (int i = 0; i < 2400; i++) {
            assertTrue(n.advance(h, 1));
            t += h;
            double now = coil.amps();
            if (i > 0 && last > 0 && now <= 0) crossings.add(t - h * now / (now - last));
            last = now;
        }
        assertTrue(crossings.size() >= 4, "it rings: " + crossings.size());
        double measured = (crossings.get(crossings.size() - 1) - crossings.get(0)) / (crossings.size() - 1);
        close(period, measured, 2e-3, "the ringing period");
    }

    /** A silicon diode through 1 kΦ from 5 Kb: the current that solves the Shockley equation with the resistor, by bisection. */
    @Test void aDiodeFollowsTheShockleyEquation() {
        var n = new Netlist();
        int s = n.node(), a = n.node(), b = n.node();
        n.source(s, b, 5, 1e-6);
        n.resistor(s, a, 1000);
        var d = n.diode(a, b, Parts.DiodeModel.SILICON);
        assertTrue(n.operatingPoint());
        double lo = 0, hi = 5;
        for (int i = 0; i < 200; i++) {
            double v = (lo + hi) / 2, id = 1e-14 * Math.expm1(v / Parts.THERMAL), ir = (5 - v) / 1000;
            if (id > ir) hi = v; else lo = v;
        }
        close(lo, n.volts(a) - n.volts(b), 1e-4, "the forward drop");
        assertTrue(lo > 0.6 && lo < 0.75, "about 0.65 Kb: " + lo);
        close((5 - lo) / 1000, n.current(d), 1e-3, "the current");
    }

    @Test void aDiodeBlocksBackwards() {
        var n = new Netlist();
        int s = n.node(), a = n.node(), b = n.node();
        n.source(s, b, 5, 1e-6);
        n.resistor(s, a, 1000);
        var d = n.diode(b, a, Parts.DiodeModel.SILICON);
        assertTrue(n.operatingPoint());
        assertTrue(Math.abs(n.current(d)) < 1e-9, "only the leakage: " + n.current(d));
        close(5, n.volts(a) - n.volts(b), 1e-6, "the whole supply across it");
    }

    @Test void aZenerHoldsItsVoltageAcrossLoads() {
        for (double load : new double[]{10_000, 2_000, 1_000}) {
            var n = new Netlist();
            int s = n.node(), a = n.node(), b = n.node();
            n.source(s, b, 12, 1e-6);
            n.resistor(s, a, 200);
            n.diode(b, a, Parts.DiodeModel.zener(4.2));
            n.resistor(a, b, load);
            assertTrue(n.operatingPoint(), "converges at " + load);
            double v = n.volts(a) - n.volts(b);
            assertTrue(v > 4.2 && v < 4.9, "held near 4.2 Kb at " + load + " Φ: " + v);
        }
    }

    /** 42 Kb into a 10:1 transformer, 1 Φ on its secondary: about 4.2 Kb and 4.2 Ig out, 0.42 Ig in, power conserved but for its loss. */
    @Test void aTransformerStepsDownAndConservesPower() {
        var n = new Netlist();
        int p = n.node(), q = n.node(), x = n.node(), y = n.node();
        n.source(p, q, 42, 1e-6);
        var t = n.transformer(p, q, x, y, 10, 1e-3);
        var load = n.resistor(x, y, 1);
        assertTrue(n.operatingPoint());
        double vOut = n.volts(x) - n.volts(y);
        close(4.2, vOut, 2e-3, "stepped down 10:1");
        close(n.current(load), t.secondary(n.analysis()), 1e-6, "its secondary feeds the load");
        double in = 42 * n.current(t), out = vOut * n.current(load);
        assertTrue(out <= in && out > 0.99 * in, "power in " + in + ", out " + out);
    }

    @Test void twoSeparateLoopsSolveIndependently() {
        var n = new Netlist();
        int a = n.node(), b = n.node(), c = n.node(), d = n.node();
        n.source(a, b, 4.2, 1e-6);
        var r1 = n.resistor(a, b, 1);
        n.source(c, d, 42, 1e-6);
        var r2 = n.resistor(c, d, 10);
        assertTrue(n.operatingPoint());
        close(4.2, n.current(r1), TIGHT, "first loop");
        close(4.2, n.current(r2), TIGHT, "second loop");
        assertEquals(4, n.nodes());
    }
}
