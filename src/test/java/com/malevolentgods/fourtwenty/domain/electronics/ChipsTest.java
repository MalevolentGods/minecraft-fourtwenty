package com.malevolentgods.fourtwenty.domain.electronics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shift registers and multiplexers on the engine: every bit where it should be, through real gate delays. */
class ChipsTest {
    /** A 4.2 Kb supply, inputs as switches to it with pull-downs. */
    private static final class Board {
        final Netlist n = new Netlist();
        final int vdd = n.node(), gnd = n.node();
        final List<Parts.Switch> switches = new ArrayList<>();
        final List<Integer> pins = new ArrayList<>();

        Board() { n.source(vdd, gnd, 4.2, 1e-3); }

        int input() {
            int pin = n.node();
            switches.add(n.switchPart(vdd, pin, false));
            pins.add(pin);
            n.resistor(pin, gnd, 10_000);
            return pin;
        }

        int[] inputs(int count) {
            int[] out = new int[count];
            for (int i = 0; i < count; i++) out[i] = input();
            return out;
        }

        int[] nodes(int count) {
            int[] out = new int[count];
            for (int i = 0; i < count; i++) out[i] = n.node();
            return out;
        }

        void set(int pin, boolean high) { n.set(switches.get(pins.indexOf(pin)), high); }

        void run() { assertTrue(n.run(0.01), "converged"); }

        /** A whole clock pulse: up, wait, down, wait. */
        void pulse(int pin) {
            set(pin, true);
            run();
            set(pin, false);
            run();
        }

        int bit(int node) {
            double v = n.volts(node) - n.volts(gnd);
            assertTrue(v >= 0.9 * 4.2 || v <= 0.1 * 4.2, "a clean level: " + v);
            return v > 2.1 ? 1 : 0;
        }

        int word(int[] nodes) {
            int x = 0;
            for (int i = 0; i < nodes.length; i++) x |= bit(nodes[i]) << i;
            return x;
        }
    }

    /** Shift 8 bits into a Puff-Pass Nug: nothing shows until the latch, then all eight at once; the carry is stage 7 all along. */
    @Test void aPuffPassNugShiftsAndLatches() {
        Board b = new Board();
        int data = b.input(), clock = b.input(), latch = b.input(), carry = b.n.node();
        int[] q = b.nodes(8);
        b.n.puffPass(b.vdd, b.gnd, data, clock, latch, q, carry);
        b.run();
        assertEquals(0, b.word(q));
        int value = 0b1011_0010;
        for (int i = 7; i >= 0; i--) {
            b.set(data, ((value >> i) & 1) == 1);
            b.pulse(clock);
            assertEquals(0, b.word(q), "outputs hold while shifting");
        }
        assertEquals(1, b.bit(carry), "stage 7 is the first bit in");
        b.pulse(latch);
        assertEquals(value, b.word(q), "all eight on the latch");
        b.set(data, false);
        b.pulse(clock);
        assertEquals(value, b.word(q), "and they hold through the next shift");
        assertEquals(0, b.bit(carry), "stage 7 is now bit 6");
        b.pulse(latch);
        assertEquals((value << 1) & 0xFF, b.word(q));
    }

    /** Two Puff-Pass Nugs, the first's carry into the second's data, one clock and one latch: a 16-bit register. */
    @Test void twoPuffPassNugsChainToSixteenBits() {
        Board b = new Board();
        int data = b.input(), clock = b.input(), latch = b.input(), carry1 = b.n.node(), carry2 = b.n.node();
        int[] low = b.nodes(8), high = b.nodes(8);
        b.n.puffPass(b.vdd, b.gnd, data, clock, latch, low, carry1);
        b.n.puffPass(b.vdd, b.gnd, carry1, clock, latch, high, carry2);
        b.run(); // powered up first: a clock already high at power-up is no edge, as on a real chip
        int value = 0xC35A;
        for (int i = 15; i >= 0; i--) {
            b.set(data, ((value >> i) & 1) == 1);
            b.pulse(clock);
        }
        b.pulse(latch);
        assertEquals(value, b.word(high) << 8 | b.word(low));
    }

    /** A Pack Nug reading eight switches into a Puff-Pass Nug over one wire, both on one clock: every input arrives on its own output. */
    @Test void aPackNugSendsEightSwitchesOverOneWire() {
        for (int value : new int[]{0, 0xFF, 0b1001_0110, 0b0110_1001, 1, 0x80}) {
            Board b = new Board();
            int[] in = b.inputs(8);
            int load = b.input(), clock = b.input(), latch = b.input(), serial = b.n.node(), carry = b.n.node();
            int[] q = b.nodes(8);
            b.n.pack(b.vdd, b.gnd, load, clock, in, serial);
            b.n.puffPass(b.vdd, b.gnd, serial, clock, latch, q, carry);
            b.run();
            for (int i = 0; i < 8; i++) b.set(in[i], ((value >> i) & 1) == 1);
            b.pulse(load);
            assertEquals((value >> 7) & 1, b.bit(serial), "input 7 comes out first");
            for (int i = 0; i < 8; i++) b.pulse(clock);
            b.pulse(latch);
            assertEquals(value, b.word(q), "sent " + Integer.toBinaryString(value));
        }
    }

    /** A Sampler Nug picks each of its eight inputs in turn, whatever the others are doing. */
    @Test void aSamplerNugPicksEveryInput() {
        Board b = new Board();
        int[] in = b.inputs(8), sel = b.inputs(3);
        int out = b.n.node();
        b.n.sampler(b.vdd, b.gnd, in, sel, out);
        for (int pattern : new int[]{0b1010_0101, 0b0101_1010, 0xFF, 0}) {
            for (int i = 0; i < 8; i++) b.set(in[i], ((pattern >> i) & 1) == 1);
            for (int s = 0; s < 8; s++) {
                for (int k = 0; k < 3; k++) b.set(sel[k], ((s >> k) & 1) == 1);
                b.run();
                assertEquals((pattern >> s) & 1, b.bit(out), "pattern " + Integer.toBinaryString(pattern) + " select " + s);
            }
        }
    }

    /** A Dealer Nug sends its input to the selected output only; the other seven stay at 0. */
    @Test void aDealerNugDealsToOneOutput() {
        Board b = new Board();
        int in = b.input();
        int[] sel = b.inputs(3), out = b.nodes(8);
        b.n.dealer(b.vdd, b.gnd, in, sel, out);
        for (int x = 0; x < 2; x++) {
            b.set(in, x == 1);
            for (int s = 0; s < 8; s++) {
                for (int k = 0; k < 3; k++) b.set(sel[k], ((s >> k) & 1) == 1);
                b.run();
                assertEquals(x << s, b.word(out), "input " + x + " select " + s);
            }
        }
    }

    /** A Sampler Nug and a Dealer Nug on the same select: eight lines carried over one wire and back, each to its own place. */
    @Test void aSamplerAndDealerCarryEightLinesOverOne() {
        Board b = new Board();
        int[] in = b.inputs(8), sel = b.inputs(3), out = b.nodes(8);
        int wire = b.n.node();
        b.n.sampler(b.vdd, b.gnd, in, sel, wire);
        b.n.dealer(b.vdd, b.gnd, wire, sel, out);
        int pattern = 0b1100_1010;
        for (int i = 0; i < 8; i++) b.set(in[i], ((pattern >> i) & 1) == 1);
        for (int s = 0; s < 8; s++) {
            for (int k = 0; k < 3; k++) b.set(sel[k], ((s >> k) & 1) == 1);
            b.run();
            assertEquals(pattern & (1 << s), b.word(out), "select " + s);
        }
    }

    /** Saved half-way through shifting (a chunk unloading) and loaded into a fresh circuit: the bits carry on where they were. */
    @Test void aPuffPassNugRemembersAcrossASave() {
        int value = 0b0110_1101;
        Board b = new Board();
        int data = b.input(), clock = b.input(), latch = b.input(), carry = b.n.node();
        int[] q = b.nodes(8);
        var chip = b.n.puffPass(b.vdd, b.gnd, data, clock, latch, q, carry);
        b.run();
        for (int i = 7; i >= 4; i--) {
            b.set(data, ((value >> i) & 1) == 1);
            b.pulse(clock);
        }
        double[] saved = chip.save(b.n.time());
        Board c = new Board();
        int data2 = c.input(), clock2 = c.input(), latch2 = c.input(), carry2 = c.n.node();
        int[] q2 = c.nodes(8);
        var again = c.n.puffPass(c.vdd, c.gnd, data2, clock2, latch2, q2, carry2);
        c.n.resume(java.util.Map.of(again, saved));
        for (int i = 3; i >= 0; i--) {
            c.set(data2, ((value >> i) & 1) == 1);
            c.pulse(clock2);
        }
        c.pulse(latch2);
        assertEquals(value, c.word(q2));
    }

    /** With too little supply every output lets go, and the chip draws only its quiescent current. */
    @Test void anUnpoweredChipFloats() {
        var n = new Netlist();
        int vdd = n.node(), gnd = n.node(), in = n.node(), out = n.node();
        int[] q = new int[8];
        for (int i = 0; i < 8; i++) q[i] = n.node();
        n.source(vdd, gnd, 1.0, 1e-3);
        n.source(in, gnd, 1.0, 1e-3);
        int[] sel = {n.node(), n.node(), n.node()};
        for (int s : sel) n.resistor(s, gnd, 1000);
        var dealer = n.dealer(vdd, gnd, in, sel, q);
        for (int node : q) n.resistor(node, gnd, 1e6);
        assertTrue(n.run(0.01));
        for (int k = 0; k < 8; k++) assertEquals(Logic.Out.FLOAT, dealer.output(k));
        assertEquals(1.0 / Logic.R_QUIESCENT, n.current(dealer), 1e-9);
    }
}
