package com.malevolentgods.fourtwenty.domain.electronics;

import java.util.Arrays;

/**
 * Logic chips with more than one output: the Puff-Pass Nug (a shift register, serial in, latched parallel out, like a 74HC595), the Pack
 * Nug (parallel in, serial out, like a 74HC165), the Sampler Nug (8-to-1 multiplexer) and the Dealer Nug (1-to-8 demultiplexer).
 * <p>
 * A chip is a {@link Chip}, which reads its inputs (1 MΦ to its ground, with the Nugs' thresholds and hysteresis), draws the quiescent
 * current and keeps the registers, and one {@link Pin} per output: an ordinary logic output stage, with the Nugs' output resistance and
 * propagation delay, scheduled like any other. The chip works out every output at once, from each solution the scheduler shows it,
 * so a clock edge is counted once however many outputs ask.
 */
public final class Chips {
    private Chips() {}

    public abstract static class Chip implements Part {
        final int vdd, gnd;
        final int[] inputs;
        private final int[] reading;
        final Pin[] pins;
        private double[] seen;
        private Logic.Out[] wanted;

        Chip(int vdd, int gnd, int[] inputs, int outputs) {
            this.vdd = vdd;
            this.gnd = gnd;
            this.inputs = inputs;
            this.reading = new int[inputs.length];
            this.pins = new Pin[outputs];
        }

        @Override public int[] nodes() { return new int[]{vdd, gnd}; }

        @Override public int[] joined() {
            int[] all = Arrays.copyOf(inputs, inputs.length + 2);
            all[inputs.length] = vdd;
            all[inputs.length + 1] = gnd;
            return all;
        }

        @Override public void stamp(Stamp s, Analysis an) {
            for (int in : inputs) s.conductance(in, gnd, 1 / Logic.R_IN);
            s.conductance(vdd, gnd, 1 / Logic.R_QUIESCENT);
        }

        /** Its supply current: quiescent, and whatever its outputs source. */
        @Override public double current(Analysis an) {
            double i = an.across(vdd, gnd) / Logic.R_QUIESCENT;
            for (Pin p : pins) if (p != null) i += p.current(an);
            return i;
        }

        public Logic.Out output(int k) { return pins[k].output(); }

        public int outputs() { return pins.length; }

        /** True if any output drives high (the block's light). */
        public boolean anyHigh() {
            for (Pin p : pins) if (p != null && p.output() == Logic.Out.HIGH) return true;
            return false;
        }

        boolean powered(Analysis an) { return an.across(vdd, gnd) >= Logic.MIN_SUPPLY; }

        int bit(Analysis an, int i) {
            double v = an.across(inputs[i], gnd), supply = an.across(vdd, gnd);
            if (v >= Logic.HIGH * supply) reading[i] = 1;
            else if (v <= Logic.LOW * supply) reading[i] = 0;
            return reading[i];
        }

        /** Inputs {@code from} to {@code from + count}, the first the lowest bit. */
        int bits(Analysis an, int from, int count) {
            int x = 0;
            for (int i = 0; i < count; i++) x |= bit(an, from + i) << i;
            return x;
        }

        Logic.Out want(int k, Analysis an, double time) {
            if (an.v != seen || wanted == null) {
                seen = an.v;
                if (!powered(an)) {
                    unpowered();
                    wanted = new Logic.Out[pins.length];
                    Arrays.fill(wanted, Logic.Out.FLOAT);
                } else {
                    for (int i = 0; i < inputs.length; i++) bit(an, i);
                    wanted = evaluate(an);
                }
            }
            return wanted[k];
        }

        /** Every output, from the inputs as now read (their readings are fresh). */
        abstract Logic.Out[] evaluate(Analysis an);

        /** Power lost: forget the clocks it saw, so the first edge after power returns is a real one. */
        void unpowered() {}

        abstract double[] registers();

        abstract void registers(double[] r);

        static Logic.Out level(int bit) { return bit != 0 ? Logic.Out.HIGH : Logic.Out.LOW; }

        /** Input readings, registers, then each output's own state. */
        @Override public double[] save(double now) {
            double[] reg = registers();
            double[] out = new double[reading.length + reg.length + 3 * pins.length];
            for (int i = 0; i < reading.length; i++) out[i] = reading[i];
            System.arraycopy(reg, 0, out, reading.length, reg.length);
            for (int k = 0; k < pins.length; k++) System.arraycopy(pins[k].save(now), 0, out, reading.length + reg.length + 3 * k, 3);
            return out;
        }

        @Override public void load(double[] s, double now) {
            int regs = registers().length;
            if (s.length != reading.length + regs + 3 * pins.length) return;
            for (int i = 0; i < reading.length; i++) reading[i] = (int) s[i];
            registers(Arrays.copyOfRange(s, reading.length, reading.length + regs));
            for (int k = 0; k < pins.length; k++) {
                int at = reading.length + regs + 3 * k;
                pins[k].load(Arrays.copyOfRange(s, at, at + 3), now);
            }
            wanted = null;
        }
    }

    /** One output of a chip: a logic output stage that asks its chip what to drive. The chip stamps the supply and inputs. */
    public static final class Pin extends Logic {
        private final Chip chip;
        private final int index;

        Pin(Chip chip, int index, int out) {
            super(chip.vdd, chip.gnd, out);
            this.chip = chip;
            this.index = index;
        }

        @Override public int[] joined() { return state == Out.FLOAT ? new int[]{vdd, gnd} : new int[]{vdd, gnd, out}; }

        @Override public void stamp(Stamp s, Analysis an) {
            s.conductance(out, vdd, 1 / (state == Out.HIGH ? R_OUT : R_OFF));
            s.conductance(out, gnd, 1 / (state == Out.LOW ? R_OUT : R_OFF));
        }

        @Override public double current(Analysis an) { return state == Out.HIGH ? an.across(vdd, out) / R_OUT : 0; }

        @Override Out want(Analysis an, double time) { return chip.want(index, an, time); }
    }

    /**
     * The Puff-Pass Nug: on each rising clock edge the bits move up one stage and data comes in at stage 0; on each rising latch edge the
     * eight stages are copied to the outputs, so they change all at once and never flicker while shifting. Outputs 0 to 7 show the latch;
     * output 8, the carry, is stage 7 itself, for the next chip's data. Inputs: data, clock, latch. When the latch and the clock rise
     * together, the latch takes the stages as they were (as a 74HC595's do).
     */
    public static final class PuffPass extends Chip {
        private int shift, stored, lastClock = -1, lastLatch = -1;

        PuffPass(int vdd, int gnd, int data, int clock, int latch) { super(vdd, gnd, new int[]{data, clock, latch}, 9); }

        @Override Logic.Out[] evaluate(Analysis an) {
            int d = bit(an, 0), clk = bit(an, 1), lat = bit(an, 2);
            if (lastLatch == 0 && lat == 1) stored = shift;
            if (lastClock == 0 && clk == 1) shift = ((shift << 1) | d) & 0xFF;
            lastClock = clk;
            lastLatch = lat;
            Logic.Out[] out = new Logic.Out[9];
            for (int i = 0; i < 8; i++) out[i] = level((stored >> i) & 1);
            out[8] = level((shift >> 7) & 1);
            return out;
        }

        @Override void unpowered() { lastClock = lastLatch = -1; }

        @Override double[] registers() { return new double[]{shift, stored, lastClock, lastLatch}; }

        @Override void registers(double[] r) {
            shift = (int) r[0];
            stored = (int) r[1];
            lastClock = (int) r[2];
            lastLatch = (int) r[3];
        }

        public int stages() { return shift; }

        public int latched() { return stored; }
    }

    /**
     * The Pack Nug: while load is 1 it takes the eight inputs into its stages; with load 0, each rising clock edge moves them up one,
     * a 0 coming in at stage 0. Its output is stage 7, so input 7 comes out first. Inputs: load, clock, then the eight.
     */
    public static final class Pack extends Chip {
        private int shift, lastClock = -1;

        Pack(int vdd, int gnd, int load, int clock, int[] in) { super(vdd, gnd, concat(new int[]{load, clock}, in), 1); }

        @Override Logic.Out[] evaluate(Analysis an) {
            int load = bit(an, 0), clk = bit(an, 1);
            if (load == 1) shift = bits(an, 2, 8);
            else if (lastClock == 0 && clk == 1) shift = (shift << 1) & 0xFF;
            lastClock = clk;
            return new Logic.Out[]{level((shift >> 7) & 1)};
        }

        @Override void unpowered() { lastClock = -1; }

        @Override double[] registers() { return new double[]{shift, lastClock}; }

        @Override void registers(double[] r) {
            shift = (int) r[0];
            lastClock = (int) r[1];
        }
    }

    /** The Sampler Nug: its output follows input number {@code select} (select bit 0 the lowest). Inputs: the eight, then three select bits. */
    public static final class Sampler extends Chip {
        Sampler(int vdd, int gnd, int[] in, int[] select) { super(vdd, gnd, concat(in, select), 1); }

        @Override Logic.Out[] evaluate(Analysis an) { return new Logic.Out[]{level(bit(an, bits(an, 8, 3)))}; }

        @Override double[] registers() { return new double[0]; }

        @Override void registers(double[] r) {}
    }

    /** The Dealer Nug: output number {@code select} follows its input; the other seven are 0. Inputs: the one, then three select bits. */
    public static final class Dealer extends Chip {
        Dealer(int vdd, int gnd, int in, int[] select) { super(vdd, gnd, concat(new int[]{in}, select), 8); }

        @Override Logic.Out[] evaluate(Analysis an) {
            int sel = bits(an, 1, 3), x = bit(an, 0);
            Logic.Out[] out = new Logic.Out[8];
            for (int k = 0; k < 8; k++) out[k] = level(k == sel ? x : 0);
            return out;
        }

        @Override double[] registers() { return new double[0]; }

        @Override void registers(double[] r) {}
    }

    /** What bus logic does to each pair of bits. */
    public enum Op { AND, OR, XOR, NAND, NOR, XNOR, NOT_A, A }

    /**
     * Bus logic: one operation on two 9-bit buses, wire for wire (channels 0 to 7 and the carry), into a 9-bit bus. NOT A and A ignore
     * B. Inputs: A's nine, then B's nine.
     */
    public static final class BusLogic extends Chip {
        final Op op;

        BusLogic(int vdd, int gnd, Op op, int[] a, int[] b) {
            super(vdd, gnd, concat(a, b), a.length);
            this.op = op;
        }

        public Op op() { return op; }

        static int apply(Op op, int a, int b) {
            return switch (op) {
                case AND -> a & b;
                case OR -> a | b;
                case XOR -> a ^ b;
                case NAND -> ~(a & b) & 1;
                case NOR -> ~(a | b) & 1;
                case XNOR -> ~(a ^ b) & 1;
                case NOT_A -> ~a & 1;
                case A -> a;
            };
        }

        @Override Logic.Out[] evaluate(Analysis an) {
            int n = pins.length;
            Logic.Out[] out = new Logic.Out[n];
            for (int i = 0; i < n; i++) out[i] = level(apply(op, bit(an, i), bit(an, n + i)));
            return out;
        }

        @Override double[] registers() { return new double[0]; }

        @Override void registers(double[] r) {}
    }

    /**
     * An 8-bit adder: A + B + carry in, the sum on 8 outputs and the carry out on the ninth. Subtracting, A − B − borrow in, the
     * difference wrapped to 8 bits (two's complement) and the ninth output 1 when it borrowed. Either way, adders chain: the low one's
     * ninth output into the high one's carry in. Inputs: A's eight, B's eight, carry in.
     */
    public static final class Adder extends Chip {
        final boolean subtract;

        Adder(int vdd, int gnd, int[] a, int[] b, int carryIn, boolean subtract) {
            super(vdd, gnd, concat(concat(a, b), new int[]{carryIn}), 9);
            this.subtract = subtract;
        }

        @Override Logic.Out[] evaluate(Analysis an) {
            int a = bits(an, 0, 8), b = bits(an, 8, 8), c = bit(an, 16);
            int r = subtract ? a - b - c : a + b + c;
            Logic.Out[] out = new Logic.Out[9];
            for (int i = 0; i < 8; i++) out[i] = level((r >> i) & 1);
            out[8] = level(subtract ? (r < 0 ? 1 : 0) : (r >> 8) & 1);
            return out;
        }

        @Override double[] registers() { return new double[0]; }

        @Override void registers(double[] r) {}
    }

    /** An 8-bit comparator: three outputs, A < B, A = B, A > B, unsigned. Inputs: A's eight, B's eight. */
    public static final class Comparator extends Chip {
        Comparator(int vdd, int gnd, int[] a, int[] b) { super(vdd, gnd, concat(a, b), 3); }

        @Override Logic.Out[] evaluate(Analysis an) {
            int a = bits(an, 0, 8), b = bits(an, 8, 8);
            return new Logic.Out[]{level(a < b ? 1 : 0), level(a == b ? 1 : 0), level(a > b ? 1 : 0)};
        }

        @Override double[] registers() { return new double[0]; }

        @Override void registers(double[] r) {}
    }

    /**
     * A 9-bit register (a 74HC574's kind): on each rising clock edge it takes its inputs; it drives them out until release is 1, when
     * it lets go of every output, so several can share one bus and take turns. Inputs: the nine, clock, release.
     */
    public static final class Register extends Chip {
        private int q, lastClock = -1;

        Register(int vdd, int gnd, int[] d, int clock, int release) { super(vdd, gnd, concat(d, new int[]{clock, release}), d.length); }

        @Override Logic.Out[] evaluate(Analysis an) {
            int n = pins.length, clk = bit(an, n), release = bit(an, n + 1);
            if (lastClock == 0 && clk == 1) q = bits(an, 0, n);
            lastClock = clk;
            Logic.Out[] out = new Logic.Out[n];
            for (int i = 0; i < n; i++) out[i] = release == 1 ? Logic.Out.FLOAT : level((q >> i) & 1);
            return out;
        }

        @Override void unpowered() { lastClock = -1; }

        @Override double[] registers() { return new double[]{q, lastClock}; }

        @Override void registers(double[] r) {
            q = (int) r[0];
            lastClock = (int) r[1];
        }

        public int value() { return q; }
    }

    /**
     * The Numb Nug (a 7-segment display's driver): it reads the nine wires of a bus (a byte and the carry, 0 to 511) against its own
     * supply, with the Nugs' thresholds, and drives nothing; what it shows is drawn from what it reads.
     */
    public static final class Display extends Chip {
        Display(int vdd, int gnd, int[] in) { super(vdd, gnd, in, 0); }

        /** The byte it reads now (0 if unpowered). */
        public int read(Analysis an) { return powered(an) ? bits(an, 0, inputs.length) : 0; }

        @Override Logic.Out[] evaluate(Analysis an) { return new Logic.Out[0]; }

        @Override double[] registers() { return new double[0]; }

        @Override void registers(double[] r) {}
    }

    /**
     * The Dankoder (a keypad encoder, a 74C922's kind): sixteen key inputs, each with the number it stands for. While a key is down it
     * drives that number on outputs 0 to 3 (the lowest number if several are down) and the carry, output 8, high; let go, the carry
     * falls and the number stays, the last key pressed. Outputs 4 to 7 are always 0.
     */
    public static final class Encoder extends Chip {
        private final int[] codes;
        private int code;

        Encoder(int vdd, int gnd, int[] keys, int[] codes) {
            super(vdd, gnd, keys, ComponentBus.WIDTH);
            this.codes = codes.clone();
        }

        @Override Logic.Out[] evaluate(Analysis an) {
            int down = -1;
            for (int i = 0; i < inputs.length; i++) if (bit(an, i) == 1 && (down < 0 || codes[i] < down)) down = codes[i];
            if (down >= 0) code = down;
            Logic.Out[] out = new Logic.Out[pins.length];
            for (int k = 0; k < 8; k++) out[k] = level(k < 4 ? (code >> k) & 1 : 0);
            out[8] = level(down >= 0 ? 1 : 0);
            return out;
        }

        @Override double[] registers() { return new double[]{code}; }

        @Override void registers(double[] r) { code = (int) r[0]; }

        /** The number it holds: the key down, or the last one pressed. */
        public int code() { return code; }
    }

    /** A bus's width: eight wires and the carry. */
    static final class ComponentBus {
        static final int WIDTH = 9;
    }

    static int[] concat(int[] a, int[] b) {
        int[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
