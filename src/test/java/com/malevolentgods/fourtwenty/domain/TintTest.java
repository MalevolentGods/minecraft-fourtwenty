package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/** The Java colour model against the design tools (tools/infuser) and the prototype. */
class TintTest {
    static Tint tint() throws Exception {
        try (InputStream in = new FileInputStream("src/main/resources/assets/fourtwenty/infuser/tint.json")) { return Tint.load(in); }
    }

    static int[] pool(Tint t, Tint.Target target, double globs) {
        int[] q = new int[t.resins().size()];
        int tot = target.parts().values().stream().mapToInt(Integer::intValue).sum();
        target.parts().forEach((n, p) -> q[t.resins().indexOf(n)] = (int) Math.round((double) p / tot * globs * 128));
        return q;
    }

    @Test
    void everyTargetsOwnRecipeMatchesIt() throws Exception {
        Tint t = tint();
        assertEquals(80, t.targets().size());
        for (var target : t.targets()) {
            double d = t.distance(Tint.specks(pool(t, target, 3)), target);
            assertTrue(d <= Tint.MATCH, target.name() + " recipe misses by " + d);
        }
    }

    @Test
    void colourDifferenceIsCiede2000() {
        // a pair from the CIEDE2000 reference data (Sharma, Wu and Dalal): 2.0425
        assertEquals(2.0425, Tint.de2000(new double[]{50, 2.6772, -79.7751}, new double[]{50, 0, -82.7485}), 1e-4);
    }

    @Test
    void anEmptyPoolLooksAsThePrototypeDrawsIt() throws Exception {
        Tint t = tint();
        assertEquals(t.emptyRgb(), t.rgb(new double[t.resins().size()]));
    }

    /** The user's path to Big Blue Thought: 3 drops of Moonmilk, then Ocean Couch, is green all the way and lands. */
    @Test
    void theRightRecipeInAnyOrderIsGreen() throws Exception {
        Tint t = tint();
        var target = t.target("lav_pin_pin");
        int w = t.resins().indexOf("white"), u = t.resins().indexOf("ultramarine");
        int[] q = new int[t.resins().size()];
        for (int k = 0; k < 3; k++) { q[w] += 4; assertTrue(t.canFinish(q, target), "Moonmilk drop " + k); }
        for (int k = 0; k < 10; k++) { q[u] += 4; assertTrue(t.canFinish(q, target), "Ocean Couch drop " + k); }
        assertTrue(t.distance(Tint.specks(q), target) <= Tint.MATCH, "and it matches");
        int[] other = new int[q.length];
        other[u] = 40; other[w] = 12;
        assertTrue(t.canFinish(other, target), "Ocean Couch first is fine too");
    }

    /** A real splash of the wrong resin goes red, even though it could be drowned; a trace is forgiven. */
    @Test
    void aDetourIsRedButATraceIsForgiven() throws Exception {
        Tint t = tint();
        var target = t.target("lav_pin_pin");
        int w = t.resins().indexOf("white"), u = t.resins().indexOf("ultramarine"), c = t.resins().indexOf("crimson");
        int[] splash = new int[t.resins().size()];
        splash[w] = 12; splash[c] = 8; splash[u] = 40;
        assertFalse(t.canFinish(splash, target));
        int[] trace = new int[t.resins().size()];
        trace[w] = 128; trace[u] = 400; trace[c] = 2;
        assertTrue(t.canFinish(trace, target));
    }

    @Test
    void twinsAreListedBothWays() throws Exception {
        Tint t = tint();
        for (var target : t.targets())
            for (String twin : target.twins()) assertTrue(t.target(twin).twins().contains(target.id()), target.id() + " <-> " + twin);
    }
}
