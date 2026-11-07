package com.malevolentgods.fourtwenty.domain.electronics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Transistors and op-amps against exact answers. */
class ActiveTest {
    private static final double VT = Active.Mosfet.VT, K = Active.Mosfet.K, L = Active.Mosfet.LAMBDA;

    private static Netlist supplied(double volts, int[] rails) {
        var n = new Netlist();
        rails[0] = n.node();
        rails[1] = n.node();
        n.source(rails[0], rails[1], volts, 1e-6);
        return n;
    }

    /** A Sativa FET, gate at 2.2 Kb, drain at 4.2: saturated, K/2·(Vgs − Vt)²·(1 + λ·Vds). */
    @Test void aSativaFetSaturatesOnTheSquareLaw() {
        int[] r = new int[2];
        var n = supplied(4.2, r);
        int g = n.node();
        n.source(g, r[1], 2.2, 1e-6);
        var fet = n.mosfet(r[0], g, r[1], false);
        assertTrue(n.operatingPoint());
        double want = K / 2 * Math.pow(2.2 - VT, 2) * (1 + L * 4.2);
        assertEquals(want, n.current(fet), want * 1e-6);
    }

    /** Below its threshold it is off; with a little drain voltage it is a resistor of 1 / (K·(Vgs − Vt)). */
    @Test void aSativaFetIsOffBelowThresholdAndAResistorAtLowVoltage() {
        int[] r = new int[2];
        var n = supplied(0.01, r);
        int g = n.node();
        var gate = n.source(g, r[1], 0.5, 1e-6);
        var fet = n.mosfet(r[0], g, r[1], false);
        assertTrue(n.operatingPoint());
        assertTrue(Math.abs(n.current(fet)) < 1e-9, "off below threshold");
        var m = new Netlist();
        int p = m.node(), q = m.node(), gg = m.node();
        m.source(p, q, 0.01, 1e-6);
        m.source(gg, q, 4.2, 1e-6);
        var on = m.mosfet(p, gg, q, false);
        assertTrue(m.operatingPoint());
        double want = K * ((4.2 - VT) * 0.01 - 0.01 * 0.01 / 2) * (1 + L * 0.01);
        assertEquals(want, m.current(on), want * 1e-6, "a resistor of about 1/(K·Vov)");
    }

    /** A current mirror: the second Sativa FET copies the first's current (scaled by λ for their different drain voltages). */
    @Test void twoSativaFetsMirrorACurrent() {
        int[] r = new int[2];
        var n = supplied(4.2, r);
        int g = n.node(), d2 = n.node();
        n.resistor(r[0], g, 1000);
        n.mosfet(g, g, r[1], false);
        var copy = n.mosfet(d2, g, r[1], false);
        n.resistor(r[0], d2, 500);
        assertTrue(n.operatingPoint());
        double vgs = n.volts(g) - n.volts(r[1]), i1 = (4.2 - vgs) / 1000;
        double vds2 = n.volts(d2) - n.volts(r[1]);
        assertEquals(i1 * (1 + L * vds2) / (1 + L * vgs), n.current(copy), i1 * 1e-4, "mirrored");
    }

    private static double inverter(double input) {
        int[] r = new int[2];
        var n = supplied(4.2, r);
        int in = n.node(), out = n.node();
        n.source(in, r[1], input, 1e-6);
        n.mosfet(out, in, r[0], true);   // Indica: source at VDD, drain at the output
        n.mosfet(out, in, r[1], false);  // Sativa: source at GND, drain at the output
        n.resistor(out, r[1], 1e9);
        assertTrue(n.operatingPoint(), "converges at " + input);
        return n.volts(out) - n.volts(r[1]);
    }

    /** A CMOS inverter of an Indica FET over a Sativa FET: rail to rail, switching at half the supply as a matched pair does. */
    @Test void aCmosInverterSwingsRailToRail() {
        assertTrue(inverter(0) > 4.19, "0 in: high");
        assertTrue(inverter(4.2) < 0.01, "1 in: low");
        assertEquals(2.1, inverter(2.1), 0.05, "a matched pair switches at half the supply");
        double prev = 5;
        for (double v = 0; v <= 4.2; v += 0.3) {
            double o = inverter(v);
            assertTrue(o <= prev + 1e-6, "falls as the input rises");
            prev = o;
        }
    }

    /** A CMOS NAND of four transistors: two Indica in parallel up, two Sativa in series down. Its truth table. */
    @Test void aCmosNandOfFourTransistors() {
        for (int x = 0; x < 4; x++) {
            int[] r = new int[2];
            var n = supplied(4.2, r);
            int a = n.node(), b = n.node(), out = n.node(), mid = n.node();
            n.source(a, r[1], (x & 1) * 4.2, 1e-6);
            n.source(b, r[1], (x >> 1) * 4.2, 1e-6);
            n.mosfet(out, a, r[0], true);
            n.mosfet(out, b, r[0], true);
            n.mosfet(out, a, mid, false);
            n.mosfet(mid, b, r[1], false);
            n.resistor(out, r[1], 1e9);
            assertTrue(n.operatingPoint(), "converges for " + x);
            double v = n.volts(out) - n.volts(r[1]);
            boolean high = x != 3;
            assertTrue(high ? v > 4.1 : v < 0.1, "NAND " + (x & 1) + "," + (x >> 1) + ": " + v);
        }
    }

    private static Netlist amp(int[] p) {
        var n = new Netlist();
        int vdd = n.node(), gnd = n.node();
        n.source(vdd, gnd, 4.2, 1e-6);
        p[0] = vdd;
        p[1] = gnd;
        return n;
    }

    /** A Hype Amp as a follower: out = in, to within what a gain of 100,000 allows. */
    @Test void aHypeAmpFollows() {
        int[] p = new int[2];
        var n = amp(p);
        int in = n.node(), out = n.node();
        n.source(in, p[1], 1.3, 1e-6);
        n.opAmp(p[0], p[1], out, in, out);
        assertTrue(n.operatingPoint());
        assertEquals(1.3, n.volts(out) - n.volts(p[1]), 1e-3);
    }

    /** Non-inverting, gain 1 + Rf/Rg = 2: 1.0 Kb in, 2.0 out. */
    @Test void aHypeAmpAmplifies() {
        int[] p = new int[2];
        var n = amp(p);
        int in = n.node(), out = n.node(), fb = n.node();
        n.source(in, p[1], 1.0, 1e-6);
        n.opAmp(p[0], p[1], out, in, fb);
        n.resistor(out, fb, 10_000);
        n.resistor(fb, p[1], 10_000);
        assertTrue(n.operatingPoint());
        assertEquals(2.0, n.volts(out) - n.volts(p[1]), 2e-3);
    }

    /** Inverting about a 2.1 Kb reference, gain −Rf/Rin = −2: 1.6 in (0.5 below) gives 3.1 out (1.0 above). */
    @Test void aHypeAmpInverts() {
        int[] p = new int[2];
        var n = amp(p);
        int in = n.node(), out = n.node(), sum = n.node(), ref = n.node();
        n.source(in, p[1], 1.6, 1e-6);
        n.source(ref, p[1], 2.1, 1e-6);
        n.opAmp(p[0], p[1], out, ref, sum);
        n.resistor(in, sum, 10_000);
        n.resistor(sum, out, 20_000);
        assertTrue(n.operatingPoint());
        assertEquals(3.1, n.volts(out) - n.volts(p[1]), 2e-3);
    }

    /** With no feedback it compares: out near the top rail when + is above −, near the bottom when below; unpowered, it floats. */
    @Test void aHypeAmpComparesAndFloatsUnpowered() {
        for (double[] c : new double[][]{{2.0, 1.0, 1}, {1.0, 2.0, 0}}) {
            int[] p = new int[2];
            var n = amp(p);
            int a = n.node(), b = n.node(), out = n.node();
            n.source(a, p[1], c[0], 1e-6);
            n.source(b, p[1], c[1], 1e-6);
            n.opAmp(p[0], p[1], out, a, b);
            n.resistor(out, p[1], 1e6);
            assertTrue(n.operatingPoint());
            double v = n.volts(out) - n.volts(p[1]);
            assertTrue(c[2] == 1 ? v > 4.0 && v < 4.2 : v < 0.2 && v >= 0, "rails: " + v);
        }
        var n = new Netlist();
        int vdd = n.node(), gnd = n.node(), a = n.node(), out = n.node();
        n.resistor(vdd, gnd, 1000);
        n.source(a, gnd, 1.0, 1e-6);
        var amp = n.opAmp(vdd, gnd, out, a, out);
        assertTrue(n.operatingPoint());
        assertEquals(0, n.current(amp), 1e-12, "unpowered: its output floats");
    }
}
