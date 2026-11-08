package com.malevolentgods.fourtwenty.domain.electronics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bus logic, the adder, the comparator and the register, against Java's own arithmetic, through real gate delays. */
class ByteChipsTest {
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

        void set(int[] word, int value) { for (int i = 0; i < word.length; i++) set(word[i], ((value >> i) & 1) == 1); }

        void run() { assertTrue(n.run(0.01), "converged"); }

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

    private static int expected(Chips.Op op, int a, int b) {
        int mask = 0x1FF;
        return switch (op) {
            case AND -> a & b;
            case OR -> a | b;
            case XOR -> a ^ b;
            case NAND -> ~(a & b) & mask;
            case NOR -> ~(a | b) & mask;
            case XNOR -> ~(a ^ b) & mask;
            case NOT_A -> ~a & mask;
            case A -> a;
        };
    }

    /** Every operation, on the nine wires of two buses, for many pairs. */
    @Test void busLogicDoesEveryOperation() {
        Random r = new Random(420);
        for (Chips.Op op : Chips.Op.values()) {
            Board b = new Board();
            int[] a = b.inputs(9), c = b.inputs(9), out = b.nodes(9);
            b.n.busLogic(op, b.vdd, b.gnd, a, c, out);
            for (int k = 0; k < 24; k++) {
                int x = r.nextInt(512), y = r.nextInt(512);
                b.set(a, x);
                b.set(c, y);
                b.run();
                assertEquals(expected(op, x, y), b.word(out), op + " " + x + ", " + y);
            }
        }
    }

    /** Sums with their carry, and differences with their borrow: the edges and many random pairs. */
    @Test void theAdderAddsAndSubtracts() {
        Random r = new Random(4200);
        for (boolean subtract : new boolean[]{false, true}) {
            Board b = new Board();
            int[] a = b.inputs(8), c = b.inputs(8), sum = b.nodes(8);
            int cin = b.input(), cout = b.n.node();
            b.n.adder(subtract, b.vdd, b.gnd, a, c, cin, sum, cout);
            int[][] cases = {{0, 0, 0}, {255, 1, 0}, {255, 255, 1}, {128, 128, 0}, {0, 1, 0}, {0, 0, 1}, {100, 55, 1}, {1, 2, 0}};
            List<int[]> all = new ArrayList<>(List.of(cases));
            for (int k = 0; k < 60; k++) all.add(new int[]{r.nextInt(256), r.nextInt(256), r.nextInt(2)});
            for (int[] t : all) {
                b.set(a, t[0]);
                b.set(c, t[1]);
                b.set(cin, t[2] == 1);
                b.run();
                int want = subtract ? t[0] - t[1] - t[2] : t[0] + t[1] + t[2];
                String what = t[0] + (subtract ? " - " : " + ") + t[1] + (subtract ? " - " : " + ") + t[2];
                assertEquals(want & 0xFF, b.word(sum), what);
                assertEquals(subtract ? (want < 0 ? 1 : 0) : want >> 8, b.bit(cout), what + ": carry");
            }
        }
    }

    /** Two adders, the low one's carry into the high one's: 16-bit sums, the carry rippling across. */
    @Test void twoAddersMakeSixteenBits() {
        Random r = new Random(7);
        Board b = new Board();
        int[] a = b.inputs(16), c = b.inputs(16), sum = b.nodes(16);
        int mid = b.n.node(), cout = b.n.node(), zero = b.input();
        b.n.adder(false, b.vdd, b.gnd, java.util.Arrays.copyOfRange(a, 0, 8), java.util.Arrays.copyOfRange(c, 0, 8), zero,
                java.util.Arrays.copyOfRange(sum, 0, 8), mid);
        b.n.adder(false, b.vdd, b.gnd, java.util.Arrays.copyOfRange(a, 8, 16), java.util.Arrays.copyOfRange(c, 8, 16), mid,
                java.util.Arrays.copyOfRange(sum, 8, 16), cout);
        List<int[]> cases = new ArrayList<>(List.of(new int[]{0x00FF, 0x0001}, new int[]{0xFFFF, 0x0001}, new int[]{0x7FFF, 0x7FFF}));
        for (int k = 0; k < 30; k++) cases.add(new int[]{r.nextInt(65536), r.nextInt(65536)});
        for (int[] t : cases) {
            b.set(a, t[0]);
            b.set(c, t[1]);
            b.run();
            int want = t[0] + t[1];
            assertEquals(want & 0xFFFF, b.word(sum), t[0] + " + " + t[1]);
            assertEquals(want >> 16, b.bit(cout), t[0] + " + " + t[1] + ": carry");
        }
    }

    /** Less, equal, greater: exactly one is high, the right one. */
    @Test void theComparatorWeighs() {
        Random r = new Random(11);
        Board b = new Board();
        int[] a = b.inputs(8), c = b.inputs(8);
        int lt = b.n.node(), eq = b.n.node(), gt = b.n.node();
        b.n.comparator(b.vdd, b.gnd, a, c, lt, eq, gt);
        List<int[]> cases = new ArrayList<>(List.of(new int[]{0, 0}, new int[]{255, 255}, new int[]{0, 255}, new int[]{255, 0}, new int[]{127, 128}));
        for (int k = 0; k < 40; k++) {
            int x = r.nextInt(256);
            cases.add(new int[]{x, k % 4 == 0 ? x : r.nextInt(256)});
        }
        for (int[] t : cases) {
            b.set(a, t[0]);
            b.set(c, t[1]);
            b.run();
            assertEquals(t[0] < t[1] ? 1 : 0, b.bit(lt), t[0] + " < " + t[1]);
            assertEquals(t[0] == t[1] ? 1 : 0, b.bit(eq), t[0] + " = " + t[1]);
            assertEquals(t[0] > t[1] ? 1 : 0, b.bit(gt), t[0] + " > " + t[1]);
        }
    }

    /** It takes its inputs on the clock's rising edge and holds them while they change; released, it lets go of its outputs. */
    @Test void theRegisterHoldsAndLetsGo() {
        Board b = new Board();
        int[] d = b.inputs(9);
        int clock = b.input(), release = b.input();
        int[] q = b.nodes(9);
        for (int node : q) b.n.resistor(node, b.gnd, 100_000);
        var reg = b.n.register(b.vdd, b.gnd, d, clock, release, q);
        b.run();
        b.set(d, 0x15A);
        b.run();
        assertEquals(0, b.word(q), "nothing until the clock");
        b.pulse(clock);
        assertEquals(0x15A, b.word(q));
        b.set(d, 0x0F0);
        b.run();
        assertEquals(0x15A, b.word(q), "holds while its inputs change");
        b.set(release, true);
        b.run();
        for (int k = 0; k < 9; k++) assertEquals(Logic.Out.FLOAT, reg.output(k), "released");
        assertEquals(0, b.word(q), "the pull-downs have the bus");
        b.set(release, false);
        b.run();
        assertEquals(0x15A, b.word(q), "and it still remembers");
    }

    /** Two registers on one shared bus, taking turns: whichever isn't released drives it, and each reads back its own byte. */
    @Test void twoRegistersShareABus() {
        Board b = new Board();
        int[] d = b.inputs(9), bus = b.nodes(9);
        int clock1 = b.input(), clock2 = b.input(), release1 = b.input(), release2 = b.input();
        for (int node : bus) b.n.resistor(node, b.gnd, 100_000);
        b.n.register(b.vdd, b.gnd, d, clock1, release1, bus);
        b.n.register(b.vdd, b.gnd, d, clock2, release2, bus);
        b.set(release1, true);
        b.set(release2, true);
        b.run();
        b.set(d, 0x05A);
        b.pulse(clock1);
        b.set(d, 0x1A5);
        b.pulse(clock2);
        b.set(release1, false);
        b.run();
        assertEquals(0x05A, b.word(bus), "the first has the bus");
        b.set(release1, true);
        b.set(release2, false);
        b.run();
        assertEquals(0x1A5, b.word(bus), "then the second");
    }

    /** A counter from parts: a register's outputs into an adder, plus 1, back into the register. Each clock counts one, and 255 wraps to 0. */
    @Test void aRegisterAndAnAdderCount() {
        Board b = new Board();
        int[] one = b.inputs(8), sum = b.nodes(9), q = b.nodes(9);
        int clock = b.input(), release = b.input(), cin = b.input();
        b.set(one, 1);
        b.n.adder(false, b.vdd, b.gnd, java.util.Arrays.copyOfRange(q, 0, 8), one, cin, java.util.Arrays.copyOfRange(sum, 0, 8), sum[8]);
        var reg = b.n.register(b.vdd, b.gnd, sum, clock, release, q);
        b.run();
        for (int k = 1; k <= 20; k++) {
            b.pulse(clock);
            assertEquals(k, b.word(java.util.Arrays.copyOfRange(q, 0, 8)), "count " + k);
        }
        Board w = new Board();
        int[] d = w.inputs(9), q2 = w.nodes(9);
        int clk = w.input(), rel = w.input();
        var held = w.n.register(w.vdd, w.gnd, d, clk, rel, q2);
        w.run();
        w.set(d, 255);
        w.pulse(clk);
        assertEquals(255, held.value());
        assertEquals(20, reg.value() & 0xFF);
    }

    /** A Numb Nug reads all nine wires, the byte and the carry: 0 to 511. */
    @Test void aNumbNugReadsItsByte() {
        Board b = new Board();
        int[] in = b.inputs(9);
        var display = b.n.display(b.vdd, b.gnd, in);
        for (int value : new int[]{0, 1, 42, 128, 255, 256, 510, 511, 0b1010_0101}) {
            b.set(in, value);
            b.run();
            assertEquals(value, display.read(b.n.analysis()), "reads " + value);
        }
    }
}
