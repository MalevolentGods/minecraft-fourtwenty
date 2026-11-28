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
}
