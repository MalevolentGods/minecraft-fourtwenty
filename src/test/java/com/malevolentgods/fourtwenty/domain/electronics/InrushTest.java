package com.malevolentgods.fourtwenty.domain.electronics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An empty capacitor straight across a stiff supply, stepped far slower than it charges: no numerical ringing. */
class InrushTest {
    @Test void anEmptyCapAcrossAStiffSupplyChargesWithoutRinging() {
        var n = new Netlist();
        int plus = n.node(), minus = n.node(), a = n.node(), b = n.node();
        n.limitedSource(plus, minus, 4.2, 2.1e-5, 1050, 0);
        n.resistor(plus, a, 1.5e-4);
        n.resistor(minus, b, 1.5e-4);
        var cap = n.capacitor(a, b, 1e-3);
        n.tolerance(1e-9);
        n.stepLimit(0.25 * 4.2);
        n.maxStep(1.0 / 20 / 10);
        double most = 0;
        for (int t = 0; t < 20; t++) {
            assertTrue(n.run(1.0 / 20), "converged at tick " + t);
            double v = n.volts(a) - n.volts(b);
            most = Math.max(most, Math.abs(v));
            assertTrue(Math.abs(v) <= 4.2 * 1.001, "never past the supply: " + v + " at tick " + t);
        }
        assertEquals(4.2, n.volts(a) - n.volts(b), 1e-3);
    }
}
