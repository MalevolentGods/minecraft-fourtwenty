package com.malevolentgods.fourtwenty.domain.electronics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Logic on the engine (docs/electronics/audit-and-plan.md 2.5 and 3.4): truth tables, adders, memory, timing, loading. */
class LogicTest {
    /** A 4.2 Kb supply, inputs as switches to it with pull-downs, outputs read as voltages. */
    private static final class Board {
        final Netlist n = new Netlist();
        final int vdd = n.node(), gnd = n.node();

        Board() { n.source(vdd, gnd, 4.2, 1e-3); }

        /** An input pin: a switch up to the supply and a 10 kΦ pull-down. */
        final List<Parts.Switch> switches = new ArrayList<>();

        int input() {
            int pin = n.node();
            switches.add(n.switchPart(vdd, pin, false));
            n.resistor(pin, gnd, 10_000);
            return pin;
        }

        void set(int input, boolean high) { n.set(switches.get(input), high); }

        boolean high(int node) { return n.volts(node) - n.volts(gnd) >= Logic.HIGH * 4.2; }

        boolean low(int node) { return n.volts(node) - n.volts(gnd) <= Logic.LOW * 4.2; }

        void run(double seconds) { assertTrue(n.run(seconds), "converged"); }
    }

    private static boolean expected(Logic.Kind k, boolean a, boolean b) {
        return switch (k) {
            case AND -> a && b;
            case OR -> a || b;
            case NAND -> !(a && b);
            case NOR -> !(a || b);
            case XOR -> a ^ b;
            case XNOR -> a == b;
            case NOT -> !a;
            case BUFFER -> a;
        };
    }

    @Test void everyGateKeepsItsTruthTable() {
        for (Logic.Kind kind : Logic.Kind.values()) {
            Board b = new Board();
            int a = b.input(), c = b.input(), out = b.n.node();
            boolean one = kind == Logic.Kind.NOT || kind == Logic.Kind.BUFFER;
            if (one) b.n.gate(kind, b.vdd, b.gnd, out, a);
            else b.n.gate(kind, b.vdd, b.gnd, out, a, c);
            for (int x = 0; x < (one ? 2 : 4); x++) {
                boolean va = (x & 1) != 0, vb = (x & 2) != 0;
                b.set(0, va);
                b.set(1, vb);
                b.run(0.01);
                boolean want = expected(kind, va, vb);
                assertTrue(want ? b.high(out) : b.low(out), kind + " " + va + "," + vb + " should be " + want + ": " + (b.n.volts(out) - b.n.volts(b.gnd)));
            }
        }
    }

    /** A full adder from Nugs: sum = a ⊕ b ⊕ cin, carry = ab + cin(a ⊕ b). Returns {sum, carry} nodes. */
    private static int[] fullAdder(Board b, int a, int c, int cin) {
        int x1 = b.n.node(), sum = b.n.node(), a1 = b.n.node(), a2 = b.n.node(), carry = b.n.node();
        b.n.gate(Logic.Kind.XOR, b.vdd, b.gnd, x1, a, c);
        b.n.gate(Logic.Kind.XOR, b.vdd, b.gnd, sum, x1, cin);
        b.n.gate(Logic.Kind.AND, b.vdd, b.gnd, a1, a, c);
        b.n.gate(Logic.Kind.AND, b.vdd, b.gnd, a2, x1, cin);
        b.n.gate(Logic.Kind.OR, b.vdd, b.gnd, carry, a1, a2);
        return new int[]{sum, carry};
    }

    @Test void aHalfAdderAdds() {
        Board b = new Board();
        int a = b.input(), c = b.input(), sum = b.n.node(), carry = b.n.node();
        b.n.gate(Logic.Kind.XOR, b.vdd, b.gnd, sum, a, c);
        b.n.gate(Logic.Kind.AND, b.vdd, b.gnd, carry, a, c);
        for (int x = 0; x < 4; x++) {
            b.set(0, (x & 1) != 0);
            b.set(1, (x & 2) != 0);
            b.run(0.01);
            int total = (x & 1) + (x >> 1);
            assertEquals(total & 1, b.high(sum) ? 1 : 0, "sum of " + x);
            assertEquals(total >> 1, b.high(carry) ? 1 : 0, "carry of " + x);
        }
    }

    /** A 4-bit ripple-carry adder of 20 Nugs, every one of its 512 cases. */
    @Test void aFourBitAdderGetsAll512SumsRight() {
        Board b = new Board();
        int[] a = new int[4], c = new int[4];
        for (int i = 0; i < 4; i++) a[i] = b.input();
        for (int i = 0; i < 4; i++) c[i] = b.input();
        int cin = b.input();
        int carry = cin;
        int[] sum = new int[4];
        for (int i = 0; i < 4; i++) {
            int[] fa = fullAdder(b, a[i], c[i], carry);
            sum[i] = fa[0];
            carry = fa[1];
        }
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 2; z++) {
                    for (int i = 0; i < 4; i++) {
                        b.set(i, (x >> i & 1) == 1);
                        b.set(4 + i, (y >> i & 1) == 1);
                    }
                    b.set(8, z == 1);
                    // the longest path is about 9 Nugs deep: 9 ms, well inside a 50 ms tick
                    b.run(0.05);
                    int got = 0;
                    for (int i = 0; i < 4; i++) {
                        assertTrue(b.high(sum[i]) || b.low(sum[i]), "a clean level on bit " + i);
                        got |= (b.high(sum[i]) ? 1 : 0) << i;
                    }
                    got |= (b.high(carry) ? 1 : 0) << 4;
                    assertEquals(x + y + z, got, x + " + " + y + " + " + z);
                }
            }
        }
    }

    @Test void aLatchNugRemembers() {
        Board b = new Board();
        int s = b.input(), r = b.input(), q = b.n.node();
        b.n.latch(b.vdd, b.gnd, q, s, r);
        b.run(0.01);
        assertTrue(b.low(q), "starts reset");
        b.set(0, true);
        b.run(0.01);
        assertTrue(b.high(q), "set");
        b.set(0, false);
        b.run(0.01);
        assertTrue(b.high(q), "holds");
        b.set(1, true);
        b.run(0.01);
        assertTrue(b.low(q), "reset");
        b.set(1, false);
        b.run(0.01);
        assertTrue(b.low(q), "holds reset");
    }

    @Test void aFlipFlopNugTakesDOnTheRisingEdge() {
        Board b = new Board();
        int d = b.input(), clk = b.input(), q = b.n.node();
        b.n.flipFlop(b.vdd, b.gnd, q, d, clk);
        b.set(0, true);
        b.run(0.01);
        assertTrue(b.low(q), "no edge yet");
        b.set(1, true);
        b.run(0.01);
        assertTrue(b.high(q), "took D on the edge");
        b.set(0, false);
        b.run(0.01);
        assertTrue(b.high(q), "D changing while the clock is high does nothing");
        b.set(1, false);
        b.run(0.01);
        assertTrue(b.high(q), "nor on the falling edge");
        b.set(1, true);
        b.run(0.01);
        assertTrue(b.low(q), "the next rising edge takes the new D");
    }

    /** Three NOT Nugs in a ring oscillate at 1 / (2 · 3 · delay): 6 ms at 1 ms a Nug. */
    @Test void aRingOfThreeInvertersOscillatesAtItsDelay() {
        Board b = new Board();
        int x = b.n.node(), y = b.n.node(), z = b.n.node();
        var g = b.n.gate(Logic.Kind.NOT, b.vdd, b.gnd, y, x);
        b.n.gate(Logic.Kind.NOT, b.vdd, b.gnd, z, y);
        b.n.gate(Logic.Kind.NOT, b.vdd, b.gnd, x, z);
        List<Double> rises = new ArrayList<>();
        Logic.Out last = g.output();
        for (int i = 0; i < 400; i++) {
            b.run(0.00025);
            if (last != Logic.Out.HIGH && g.output() == Logic.Out.HIGH) rises.add(b.n.time());
            last = g.output();
        }
        assertTrue(rises.size() >= 10, "it oscillates: " + rises.size());
        double period = (rises.get(rises.size() - 1) - rises.get(1)) / (rises.size() - 2);
        assertEquals(0.006, period, 0.006 * 0.05, "period");
    }

    @Test void aTickerTicksOnTheBeat() {
        Board b = new Board();
        int out = b.n.node();
        var t = b.n.ticker(b.vdd, b.gnd, out, 0.01);
        int edges = 0;
        Logic.Out last = t.output();
        for (int i = 0; i < 200; i++) {
            b.run(0.0005);
            if (t.output() != last) edges++;
            last = t.output();
        }
        assertEquals(20, edges, 1, "twenty edges in 0.1 s at 100 Hz");
    }

    /** A Nug driving a 10 Φ load can't hold its output high: the next Nug reads a 0, as a real overloaded gate fails. */
    @Test void anOverloadedOutputFailsLikeARealGate() {
        Board b = new Board();
        int a = b.input(), mid = b.n.node(), out = b.n.node();
        b.n.gate(Logic.Kind.BUFFER, b.vdd, b.gnd, mid, a);
        b.n.resistor(mid, b.gnd, 10);
        b.n.gate(Logic.Kind.BUFFER, b.vdd, b.gnd, out, mid);
        b.set(0, true);
        b.run(0.02);
        assertTrue(b.low(mid), "dragged down: " + (b.n.volts(mid) - b.n.volts(b.gnd)));
        assertTrue(b.low(out), "and the next Nug reads 0");
    }

    @Test void anUnpoweredNugLetsItsOutputFloat() {
        var n = new Netlist();
        int vdd = n.node(), gnd = n.node(), in = n.node(), out = n.node();
        n.resistor(in, gnd, 1000);
        var g = n.gate(Logic.Kind.NOT, vdd, gnd, out, in);
        n.resistor(vdd, gnd, 1000);
        assertTrue(n.run(0.01));
        assertEquals(Logic.Out.FLOAT, g.output());
    }

    /** A pulse shorter than a Nug's delay never gets through (inertial delay), as a real gate filters glitches. */
    @Test void aGlitchShorterThanTheDelayIsFiltered() {
        Board b = new Board();
        int a = b.input(), out = b.n.node();
        var g = b.n.gate(Logic.Kind.BUFFER, b.vdd, b.gnd, out, a);
        b.run(0.01);
        assertEquals(Logic.Out.LOW, g.output());
        b.set(0, true);
        b.run(0.0004);
        b.set(0, false);
        b.run(0.01);
        assertEquals(Logic.Out.LOW, g.output(), "the 0.4 ms pulse never reached the output");
    }

    /** An RC delay into a Nug: its input crosses 70% of the supply at t = −RC·ln 0.3, and the output follows one delay later. */
    @Test void anRcDelayTriggersANugOnTime() {
        Board b = new Board();
        b.n.maxStep(0.002);
        int s = b.n.node(), in = b.n.node(), out = b.n.node();
        var sw = b.n.switchPart(b.vdd, s, false);
        b.n.resistor(s, in, 1000);
        b.n.capacitor(in, b.gnd, 1e-4);
        var g = b.n.gate(Logic.Kind.BUFFER, b.vdd, b.gnd, out, in);
        b.run(0.01);
        b.n.set(sw, true);
        double start = b.n.time(), seen = -1;
        for (int i = 0; i < 400 && seen < 0; i++) {
            b.run(0.001);
            if (g.output() == Logic.Out.HIGH) seen = b.n.time() - start;
        }
        double tau = 1000 * 1e-4, expected = -tau * Math.log(1 - Logic.HIGH) + Logic.DELAY;
        assertTrue(seen > 0, "it switched");
        assertEquals(expected, seen, 0.004, "on time");
    }
}
