package com.malevolentgods.fourtwenty.domain.electronics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** A diode between two nodes that barely touch anything at power-up: a Buffer's output not yet driven, and a clock input. */
class FloatingDiodeTest {
    @Test void aDiodeBetweenTwoNearlyFloatingNodesSolves() {
        var n = new Netlist();
        int vdd = n.node(), gnd = n.node(), in = n.node(), dreset = n.node(), clk = n.node();
        n.limitedSource(vdd, gnd, 4.2, 2.1e-5, 1050, 0);
        n.resistor(in, gnd, 1e6);
        n.gate(Logic.Kind.BUFFER, vdd, gnd, dreset, in);
        n.diode(dreset, clk, Parts.DiodeModel.SILICON);
        n.switchPart(vdd, clk, false);
        // the clock input of a register: 1 MΦ to its ground, as a Nug's input
        n.gate(Logic.Kind.BUFFER, vdd, gnd, n.node(), clk);
        n.tolerance(1e-9);
        n.stepLimit(0.25 * 4.2);
        long t = System.nanoTime();
        assertTrue(n.operatingPoint(), "converges");
        assertTrue(n.run(0.05), "runs");
        System.out.println("FLOATING took " + (System.nanoTime() - t) / 1e6 + " ms");
    }
}
