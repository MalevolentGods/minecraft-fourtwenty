package com.malevolentgods.fourtwenty.domain.tiedye;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TieDyeRulesTest {
    private static final TieDyeRules.Colourway ASHTRAY = new TieDyeRules.Colourway("ashtray", List.of("mc_black", "mc_gray", "mc_white"));

    @Test
    void slotsRepeatAColourwaysColours() {
        assertEquals("mc_black", ASHTRAY.colour(0));
        assertEquals("mc_white", ASHTRAY.colour(2));
        assertEquals("mc_black", ASHTRAY.colour(3), "a three-colour colourway repeats its first in slot 4");
        assertThrows(IllegalArgumentException.class, () -> new TieDyeRules.Colourway("one", List.of("mc_red")));
    }

    @Test
    void aColourwayNeedsEveryColourResearched() {
        assertFalse(ASHTRAY.orderable(Set.of("mc_black", "mc_gray")));
        assertTrue(ASHTRAY.orderable(Set.of("mc_black", "mc_gray", "mc_white", "mc_brown")));
    }

    @Test
    void theShackGrowsByLevel() {
        assertArrayEquals(new int[] {0, 1, 1, 2, 2, 2}, java.util.stream.IntStream.rangeClosed(0, 5).map(TieDyeRules::workers).toArray());
        assertArrayEquals(new int[] {0, 4, 8, 8, 16, 16}, java.util.stream.IntStream.rangeClosed(0, 5).map(l -> TieDyeRules.batch(l, 1)).toArray());
        assertEquals(1, TieDyeRules.batch(1, 0.01), "a tiny scale still dyes one");
        assertEquals("fourtwenty:tex_crumple", TieDyeRules.studyFor(0));
        assertEquals("fourtwenty:tex_mandala", TieDyeRules.studyFor(7));
    }

    @Test
    void aBatchUsesPlainItemsABudPerColourAndABottle() {
        var supplies = TieDyeRules.batchSupplies(ASHTRAY, "fourtwenty:natural_hemp", 8, 8);
        assertEquals(List.of(new TieDyeRules.Supply("fourtwenty:natural_hemp", 8), new TieDyeRules.Supply("fourtwenty:weed_bud", 3),
                new TieDyeRules.Supply("minecraft:glass_bottle", 1)), supplies);
        assertEquals(6, TieDyeRules.batchSupplies(ASHTRAY, "x:y", 16, 8).get(1).count(), "a bud per colour per 8 items");
    }

    @Test
    void ordersRunHeadFirstAndTheHeadStaysOnceSuppliesAreIn() {
        var q = new OrderQueue();
        var by = UUID.randomUUID();
        var a = q.add(0, "ashtray", TieDyeRules.Kind.HEMP, null, 12, by);
        var b = q.add(3, "campfire", TieDyeRules.Kind.RUG, null, 4, by);
        q.startBatch(8, TieDyeRules.batchSupplies(ASHTRAY, "fourtwenty:natural_hemp", 8, 8));
        assertTrue(q.cancel(a.id()), "nothing delivered yet: it can go");
        assertEquals(b.id(), q.head().id());
        assertFalse(q.batchStarted());
        var c = q.add(0, "ashtray", TieDyeRules.Kind.HEMP, null, 12, by);
        assertTrue(q.cancel(b.id()));
        q.startBatch(8, TieDyeRules.batchSupplies(ASHTRAY, "fourtwenty:natural_hemp", 8, 8));
        q.deliver(0, 2);
        assertFalse(q.cancel(c.id()), "supplies in: the head stays");
        assertEquals(6, q.missing().getFirst().count());
        q.deliver(0, 6); q.deliver(1, 3); q.deliver(2, 1);
        assertTrue(q.missing().isEmpty());
        var after = q.finishBatch();
        assertEquals(8, after.made());
        assertEquals(c.id(), q.head().id(), "12 ordered, 8 made: still the head");
        q.startBatch(4, List.of());
        q.finishBatch();
        assertNull(q.head(), "done and gone");
        assertEquals(12, q.totalMade());
    }

    private static SquareFit.Fit fitAt(java.util.Set<String> cells, int u, int v) {
        return SquareFit.fit((du, dv) -> cells.contains((u + du) + "," + (v + dv)));
    }

    private static java.util.Set<String> square(int u0, int v0, int n) {
        var out = new java.util.HashSet<String>();
        for (int a = 0; a < n; a++) for (int b = 0; b < n; b++) out.add((u0 + a) + "," + (v0 + b));
        return out;
    }

    @Test
    void anIsolatedSquareSharesOnePattern() {
        var three = square(10, 20, 3);
        assertEquals(new SquareFit.Fit(3, 0, 0), fitAt(three, 10, 20));
        assertEquals(new SquareFit.Fit(3, 2, 1), fitAt(three, 12, 21));
        var five = square(0, 0, 5);
        assertEquals(new SquareFit.Fit(5, 4, 4), fitAt(five, 4, 4));
        assertEquals(SquareFit.Fit.ONE, fitAt(java.util.Set.of("0,0"), 0, 0), "a lone block is 1x1");
    }

    @Test
    void anythingButAnIsolatedSquareIsOneByOne() {
        var row = new java.util.HashSet<>(java.util.Set.of("0,0", "1,0", "2,0"));
        assertEquals(SquareFit.Fit.ONE, fitAt(row, 1, 0), "a row isn't a square");
        var bumped = square(0, 0, 3);
        bumped.add("3,1");
        assertEquals(SquareFit.Fit.ONE, fitAt(bumped, 1, 1), "a square with a block of the look touching its side isn't");
        var six = square(0, 0, 6);
        assertEquals(SquareFit.Fit.ONE, fitAt(six, 2, 2), "6x6 is past the biggest square");
        var corner = square(0, 0, 2);
        corner.add("2,2");
        assertEquals(new SquareFit.Fit(2, 1, 1), fitAt(corner, 1, 1), "a block touching only a corner doesn't spoil it");
    }
}
