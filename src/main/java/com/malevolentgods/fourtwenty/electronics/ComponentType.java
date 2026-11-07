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
    TICKER("ticker", Part.TICKER, Layout.NO_INPUT, "s", List.of(0.002, 0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0), 1.0);

    /** What the block is in the circuit. */
    public enum Part { RESISTOR, CAPACITOR, INDUCTOR, DIODE, ZENER, TOGGLE, REDSTONE_SWITCH, BUTTON, GATE, TRI_STATE, LATCH, FLIP_FLOP, TICKER }

    /** A pin's face, relative to the block's front. */
    public enum Face { FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM }

    /**
     * Which pins a part has, in the order the engine wants them. Two-pin parts: {back, front} (a One-Hitter's anode is its back, its
     * cathode, the banded end, its front). Logic: {vdd, gnd, out, inputs...}.
     */
    public enum Layout {
        TWO_PIN(Face.BACK, Face.FRONT),
        NO_INPUT(Face.TOP, Face.BOTTOM, Face.FRONT),
        ONE_INPUT(Face.TOP, Face.BOTTOM, Face.FRONT, Face.BACK),
        TWO_INPUT(Face.TOP, Face.BOTTOM, Face.FRONT, Face.LEFT, Face.RIGHT);

        public final List<Face> pins;

        Layout(Face... pins) { this.pins = List.of(pins); }
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

    public boolean logic() { return layout != Layout.TWO_PIN; }

    /** The index of {@link #defaultValue} in {@link #values}. */
    public int defaultIndex() {
        for (int i = 0; i < values.size(); i++) if (Math.abs(values.get(i) - defaultValue) <= 1e-9 * Math.abs(defaultValue)) return i;
        return 0;
    }

    /** The world direction of a pin face on a block facing {@code front}. Left is the clockwise side, as everywhere in the mod. */
    public static Direction toWorld(Direction front, Face face) {
        return switch (face) {
            case FRONT -> front;
            case BACK -> front.getOpposite();
            case LEFT -> front.getClockWise();
            case RIGHT -> front.getCounterClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    /** The pin on world side {@code side} of a block facing {@code front}, or -1 if that side has none. */
    public int pinAt(Direction front, Direction side) {
        for (int i = 0; i < layout.pins.size(); i++) if (toWorld(front, layout.pins.get(i)) == side) return i;
        return -1;
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
