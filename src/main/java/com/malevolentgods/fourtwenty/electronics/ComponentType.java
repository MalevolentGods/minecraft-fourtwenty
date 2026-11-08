package com.malevolentgods.fourtwenty.electronics;

import com.malevolentgods.fourtwenty.domain.electronics.Logic;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * Every electronic component block (docs/electronics/audit-and-plan.md part 3): what part it is, its pins and the faces they're on
 * (relative to the way the block faces), and the values it can be set to. Pin faces: two-pin parts run back to front; Nugs take inputs on
 * the back or the sides, give their output in front, and are powered top (VDD) and bottom (GND).
 */
public enum ComponentType {
    RESINATOR("resinator", Part.RESISTOR, Layout.TWO_PIN, "Φ", series(1, 1e6), 1000),
    KIEF_CAP("kief_cap", Part.CAPACITOR, Layout.TWO_PIN, "Ga", series(1e-6, 1), 1e-3),
    SPLIFF_COIL("spliff_coil", Part.INDUCTOR, Layout.TWO_PIN, "Ma", series(1e-3, 10), 1),
    ONE_HITTER("one_hitter", Part.DIODE, Layout.TWO_PIN, "", List.of(), 0),
    ZEN_HITTER("zen_hitter", Part.ZENER, Layout.TWO_PIN, "Kb", List.of(2.4, 3.3, 4.2, 5.1, 6.8, 9.1, 12.0, 24.0, 42.0), 4.2),
    TOGGLE_JOINT("toggle_joint", Part.TOGGLE, Layout.TWO_PIN, "", List.of(), 0),
    KILL_SWITCH("kill_switch", Part.REDSTONE_SWITCH, Layout.TWO_PIN, "", List.of(), 0),
    PUFF_BUTTON("puff_button", Part.BUTTON, Layout.TWO_PIN, "", List.of(), 0),
    SLOW_BURNER("slow_burner", Part.REGULATOR, Layout.TWO_PIN, "Ig",
            List.of(1e-4, 2.2e-4, 4.7e-4, 1e-3, 2.2e-3, 4.7e-3, 0.01, 0.022, 0.047, 0.1, 0.22, 0.47, 1.0), 1e-3),
    NOT_NUG("not_nug", Part.GATE, Layout.ONE_INPUT, "", List.of(), 0, Logic.Kind.NOT),
    BUFFER_NUG("buffer_nug", Part.GATE, Layout.ONE_INPUT, "", List.of(), 0, Logic.Kind.BUFFER),
    AND_NUG("and_nug", Part.GATE, Layout.TWO_INPUT, "", List.of(), 0, Logic.Kind.AND),
    OR_NUG("or_nug", Part.GATE, Layout.TWO_INPUT, "", List.of(), 0, Logic.Kind.OR),
    NAND_NUG("nand_nug", Part.GATE, Layout.TWO_INPUT, "", List.of(), 0, Logic.Kind.NAND),
    NOR_NUG("nor_nug", Part.GATE, Layout.TWO_INPUT, "", List.of(), 0, Logic.Kind.NOR),
    XOR_NUG("xor_nug", Part.GATE, Layout.TWO_INPUT, "", List.of(), 0, Logic.Kind.XOR),
    XNOR_NUG("xnor_nug", Part.GATE, Layout.TWO_INPUT, "", List.of(), 0, Logic.Kind.XNOR),
    TRI_STATE_NUG("tri_state_nug", Part.TRI_STATE, Layout.TWO_INPUT, "", List.of(), 0),
    LATCH_NUG("latch_nug", Part.LATCH, Layout.TWO_INPUT, "", List.of(), 0),
    FLIP_FLOP_NUG("flip_flop_nug", Part.FLIP_FLOP, Layout.TWO_INPUT, "", List.of(), 0),
    TICKER("ticker", Part.TICKER, Layout.NO_INPUT, "s", List.of(0.002, 0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0), 1.0),
    SATIVA_FET("sativa_fet", Part.NMOS, Layout.THREE_PIN, "", List.of(), 0),
    INDICA_FET("indica_fet", Part.PMOS, Layout.THREE_PIN, "", List.of(), 0),
    HYPE_AMP("hype_amp", Part.OPAMP, Layout.TWO_INPUT, "", List.of(), 0),
    PUFF_PASS_NUG("puff_pass_nug", Part.SHIFT_OUT, Layout.PUFF_PASS, "", List.of(), 0),
    PACK_NUG("pack_nug", Part.SHIFT_IN, Layout.PACK, "", List.of(), 0),
    SAMPLER_NUG("sampler_nug", Part.MUX, Layout.SAMPLER, "", List.of(), 0),
    DEALER_NUG("dealer_nug", Part.DEMUX, Layout.DEALER, "", List.of(), 0),
    BLEND_NUG("blend_nug", Part.BITWISE, Layout.BLEND, "", List.of(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0), 0),
    STACKER_NUG("stacker_nug", Part.ARITH, Layout.STACKER, "", List.of(0.0, 1.0), 0),
    SCALE_NUG("scale_nug", Part.COMPARE, Layout.SCALE, "", List.of(), 0),
    JAR_NUG("jar_nug", Part.REGISTER, Layout.JAR, "", List.of(), 0),
    NUMB_NUG("numb_nug", Part.DISPLAY, Layout.DISPLAY, "", List.of(0.0, 1.0), 0);

    /** Wires on a bus face, and in an Eighth Ribbon: channels 0 to 7, then the carry. */
    public static final int BUS = 9;

    /** What the block is in the circuit. */
    public enum Part { RESISTOR, CAPACITOR, INDUCTOR, DIODE, ZENER, TOGGLE, REDSTONE_SWITCH, BUTTON, REGULATOR, GATE, TRI_STATE, LATCH, FLIP_FLOP, TICKER, NMOS, PMOS, OPAMP, SHIFT_OUT, SHIFT_IN, MUX, DEMUX, BITWISE, ARITH, COMPARE, REGISTER, DISPLAY }

    /** A pin's face, relative to the block's front. */
    public enum Face { FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM }

    /**
     * Which pins a part has, in the order the engine wants them. Two-pin parts: {back, front} (a One-Hitter's anode is its back, its
     * cathode, the banded end, its front). Logic: {vdd, gnd, out, inputs...}; a Hype Amp's inputs are + on the left, − on the right.
     * FETs: {source, drain, gate}, the source at the back, the drain in front, the gate on the left. A bus face (an Eighth Ribbon's
     * nine wires) is nine pins in a row, channel 0 first, the carry last: Puff-Pass {vdd, gnd, data, clock, latch, out 0-7, carry}; Pack
     * {vdd, gnd, load, clock, in 0-7, (carry), out}; Sampler {vdd, gnd, in 0-7, (carry), select 0-2 and six unused, out}; Dealer {vdd,
     * gnd, in, select 0-2 and six unused, out 0-7, (carry)}. Blend {vdd, gnd, A 0-8, B 0-8, out 0-8}; Stacker {vdd, gnd, A 0-7 (carry),
     * B 0-7 (carry), carry in, sum 0-7, carry out}; Scale {vdd, gnd, A 0-7 (carry), B 0-7 (carry), A&lt;B, A=B, A&gt;B and six unused};
     * Jar {vdd, gnd, in 0-8, clock, release, out 0-8}.
     */
    public enum Layout {
        TWO_PIN(Face.BACK, Face.FRONT),
        NO_INPUT(Face.TOP, Face.BOTTOM, Face.FRONT),
        ONE_INPUT(Face.TOP, Face.BOTTOM, Face.FRONT, Face.BACK),
        TWO_INPUT(Face.TOP, Face.BOTTOM, Face.FRONT, Face.LEFT, Face.RIGHT),
        THREE_PIN(Face.BACK, Face.FRONT, Face.LEFT),
        PUFF_PASS(cat(new Face[]{Face.TOP, Face.BOTTOM, Face.BACK, Face.LEFT, Face.RIGHT}, bus(Face.FRONT))),
        PACK(cat(new Face[]{Face.TOP, Face.BOTTOM, Face.LEFT, Face.RIGHT}, bus(Face.BACK), new Face[]{Face.FRONT})),
        SAMPLER(cat(new Face[]{Face.TOP, Face.BOTTOM}, bus(Face.BACK), bus(Face.LEFT), new Face[]{Face.FRONT})),
        DEALER(cat(new Face[]{Face.TOP, Face.BOTTOM, Face.BACK}, bus(Face.LEFT), bus(Face.FRONT))),
        BLEND(cat(new Face[]{Face.TOP, Face.BOTTOM}, bus(Face.BACK), bus(Face.LEFT), bus(Face.FRONT))),
        STACKER(cat(new Face[]{Face.TOP, Face.BOTTOM}, bus(Face.BACK), bus(Face.LEFT), new Face[]{Face.RIGHT}, bus(Face.FRONT))),
        SCALE(cat(new Face[]{Face.TOP, Face.BOTTOM}, bus(Face.BACK), bus(Face.LEFT), bus(Face.FRONT))),
        JAR(cat(new Face[]{Face.TOP, Face.BOTTOM}, bus(Face.BACK), new Face[]{Face.LEFT, Face.RIGHT}, bus(Face.FRONT))),
        DISPLAY(cat(new Face[]{Face.TOP, Face.BOTTOM}, bus(Face.BACK)));

        public final List<Face> pins;

        Layout(Face... pins) { this.pins = List.of(pins); }

        private static Face[] bus(Face face) {
            Face[] out = new Face[BUS];
            java.util.Arrays.fill(out, face);
            return out;
        }

        private static Face[] cat(Face[]... parts) {
            java.util.List<Face> out = new java.util.ArrayList<>();
            for (Face[] p : parts) out.addAll(java.util.Arrays.asList(p));
            return out.toArray(new Face[0]);
        }
    }

    public final String id;
    public final Part part;
    public final Layout layout;
    public final String unit;
    /** The values it can be set to, in order; empty for a part with none. */
    public final List<Double> values;
    public final double defaultValue;
    public final Logic.Kind gate;

    ComponentType(String id, Part part, Layout layout, String unit, List<Double> values, double defaultValue) {
        this(id, part, layout, unit, values, defaultValue, null);
    }

    ComponentType(String id, Part part, Layout layout, String unit, List<Double> values, double defaultValue, Logic.Kind gate) {
        this.id = id;
        this.part = part;
        this.layout = layout;
        this.unit = unit;
        this.values = values;
        this.defaultValue = defaultValue;
        this.gate = gate;
    }

    /** A powered part read as bits: the Nugs and the Ticker. */
    public boolean logic() {
        return switch (part) {
            case GATE, TRI_STATE, LATCH, FLIP_FLOP, TICKER, SHIFT_OUT, SHIFT_IN, MUX, DEMUX, BITWISE, ARITH, COMPARE, REGISTER -> true;
            default -> false;
        };
    }

    /** A chip with a bus face: a shift register or a multiplexer. */
    public boolean chip() {
        return switch (part) {
            case SHIFT_OUT, SHIFT_IN, MUX, DEMUX, BITWISE, ARITH, COMPARE, REGISTER, DISPLAY -> true;
            default -> false;
        };
    }

    /** A value as a player reads it: a Blend Nug's operation, a Stacker Nug's add or subtract, else the number in its unit. */
    public String describe(double value) {
        return switch (part) {
            case BITWISE -> switch ((int) Math.round(value)) {
                case 0 -> "AND";
                case 1 -> "OR";
                case 2 -> "XOR";
                case 3 -> "NAND";
                case 4 -> "NOR";
                case 5 -> "XNOR";
                case 6 -> "NOT A";
                default -> "A";
            };
            case ARITH -> value > 0.5 ? "subtract" : "add";
            case DISPLAY -> value > 0.5 ? "hex" : "decimal";
            default -> ComponentBlockEntity.format(value, unit);
        };
    }

    /** A part people touch or read, that mounts flush on a Rolling Tray: a Puff Button, a Toggle Joint, a Numb Nug. */
    public boolean mountable() { return part == Part.BUTTON || part == Part.TOGGLE || part == Part.DISPLAY; }

    /** Its signal lines when mounted: one for a switch, a bus for a Numb Nug. */
    public int mountSignals() { return part == Part.DISPLAY ? BUS : 1; }

    /**
     * Where pin {@code i} goes when mounted, as a Rolling Tray's wire: a switch's back pin to the board's +, its front to the tray's
     * signal; a Numb Nug's supply to the rails, its nine data pins to the tray's signal lines.
     */
    public int mountWire(int i) {
        if (part == Part.DISPLAY) return i == 0 ? RollingTrayBlock.PLUS : i == 1 ? RollingTrayBlock.MINUS : i - 2;
        return i == 0 ? RollingTrayBlock.PLUS : 0;
    }

    /** A transistor, read by its gate, drain and channel current. */
    public boolean fet() { return part == Part.NMOS || part == Part.PMOS; }

    /** A Resinator's power rating, in Willards: past it for as long as a Wick takes to burn, it burns out. 0 for parts without one. */
    public double ratedWatts() { return part == Part.RESISTOR ? 10 : 0; }

    /** The index of {@link #defaultValue} in {@link #values}. */
    public int defaultIndex() {
        for (int i = 0; i < values.size(); i++) if (Math.abs(values.get(i) - defaultValue) <= 1e-9 * Math.abs(defaultValue)) return i;
        return 0;
    }

    /** The world direction of a pin face on a part facing {@code front} with spin {@code spin} (Orientation). */
    public static Direction toWorld(Direction front, int spin, Face face) { return Orientation.toWorld(front, spin, face); }

    /** The pin on world side {@code side} of a part facing {@code front} with spin {@code spin}, or -1 if that side has none. */
    public int pinAt(Direction front, int spin, Direction side) {
        for (int i = 0; i < layout.pins.size(); i++) if (toWorld(front, spin, layout.pins.get(i)) == side) return i;
        return -1;
    }

    /** How many pins are on world side {@code side}: 0, 1, or {@link #BUS} for a bus face. */
    public int widthAt(Direction front, int spin, Direction side) {
        int n = 0;
        for (Face f : layout.pins) if (toWorld(front, spin, f) == side) n++;
        return n;
    }

    public static ComponentType byId(String id) {
        for (ComponentType t : values()) if (t.id.equals(id)) return t;
        return null;
    }

    /** The E6 series (1, 1.5, 2.2, 3.3, 4.7, 6.8) from {@code from} to {@code to}, a decade at a time. */
    private static List<Double> series(double from, double to) {
        double[] e6 = {1, 1.5, 2.2, 3.3, 4.7, 6.8};
        java.util.List<Double> out = new java.util.ArrayList<>();
        for (double decade = from; decade <= to * 1.0001; decade *= 10) {
            for (double m : e6) {
                double v = Math.round(m * decade * 1e9) / 1e9;
                if (v <= to * 1.0001) out.add(v);
            }
        }
        return List.copyOf(out);
    }
}
