package com.malevolentgods.fourtwenty.domain.handbook;

import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Def;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Deed;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Kind;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Progress;
import com.malevolentgods.fourtwenty.domain.handbook.Scouting.Req;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoutingTest {
    private static Req tracked(String n, String deed, Map<String, String> subject, int count) { return new Req(n, Kind.TRACKED, deed, subject, count); }

    private static Req honour(String n) { return new Req(n, Kind.HONOUR, null, null, 1); }

    private static final Def GARDEN = new Def("garden", false, 0, null, null, List.of(
            tracked("1", "plant", Map.of(), 2), tracked("2", "harvest", Map.of("grade", "0"), 1), honour("3")));
    private static final Def R1 = new Def("one", true, 1, null, null, List.of(tracked("1", "trim", Map.of(), 1)));
    private static final Def R2 = new Def("two", true, 2, null, null, List.of(tracked("1", "earn", Map.of("id", "garden"), 1)));
    private static final List<Def> ALL = List.of(R1, R2, GARDEN);

    @Test
    void wildcardsAndAlternativesMatch() {
        assertTrue(Scouting.matches("fourtwenty:hemp_*", "fourtwenty:hemp_pin_pin_pin"));
        assertTrue(Scouting.matches("fourtwenty:snip_station|fourtwenty:bud_barber", "fourtwenty:bud_barber"));
        assertFalse(Scouting.matches("fourtwenty:snip_station|fourtwenty:bud_barber", "fourtwenty:dry_box"));
        assertFalse(Scouting.matches("a", null));
    }

    @Test
    void aCountedRequirementTicksOnTheLastOne() {
        var p = new Progress();
        assertTrue(p.fire(ALL, Deed.of("plant"), 1).ticked().isEmpty());
        assertEquals(1, p.count("badge:garden/1"));
        assertEquals(List.of("badge:garden/1"), p.fire(ALL, Deed.of("plant"), 2).ticked());
        assertEquals(2, p.state("badge:garden/1") + 1, "done");
    }

    @Test
    void theSubjectMustMatch() {
        var p = new Progress();
        assertTrue(p.fire(ALL, Deed.of("harvest", "grade", "1"), 1).ticked().isEmpty(), "a grade 1 harvest isn't a Landrace one");
        assertEquals(List.of("badge:garden/2"), p.fire(ALL, Deed.of("harvest", "grade", "0"), 2).ticked());
    }

    @Test
    void honourRequirementsNeedASignatureAndTrackedOnesCannotBeSigned() {
        var p = new Progress();
        assertTrue(p.sign(ALL, "badge:garden/1", 1).ticked().isEmpty(), "a tracked requirement can't be signed");
        assertEquals(List.of("badge:garden/3"), p.sign(ALL, "badge:garden/3", 2).ticked());
        assertTrue(p.sign(ALL, "badge:garden/3", 3).ticked().isEmpty(), "twice is nothing new");
    }

    @Test
    void aBadgeIsEarnedWhenEveryRequirementIsDoneAndEarningIsADeed() {
        var p = new Progress();
        p.fire(ALL, Deed.of("harvest", "grade", "0"), 1);
        p.sign(ALL, "badge:garden/3", 1);
        var r = p.fire(ALL, new Deed("plant", Map.of(), 2), 5);
        assertEquals(List.of("badge:garden"), r.earned());
        assertTrue(p.earned("badge:garden"));
        assertTrue(p.satisfied("rank:two/1"), "the badge ticked 'earn the garden badge' on the rank");
        assertFalse(p.earned("rank:two"), "but rank two waits for rank one");
    }

    @Test
    void ranksAreEarnedInOrderButTheirRequirementsTickEarly() {
        var p = new Progress();
        p.fire(ALL, Deed.of("harvest", "grade", "0"), 1);
        p.sign(ALL, "badge:garden/3", 1);
        p.fire(ALL, new Deed("plant", Map.of(), 2), 1);
        assertFalse(p.earned("rank:two"));
        assertEquals(0, p.rank(ALL));
        var r = p.fire(ALL, Deed.of("trim"), 9);
        assertEquals(List.of("rank:one", "rank:two"), r.earned(), "rank one, and rank two straight after it");
        assertEquals(2, p.rank(ALL));
    }

    @Test
    void resettingOneRankAlsoUnearnsTheLaterOnes() {
        var p = new Progress();
        p.fire(ALL, Deed.of("harvest", "grade", "0"), 1);
        p.sign(ALL, "badge:garden/3", 1);
        p.fire(ALL, new Deed("plant", Map.of(), 2), 1);
        p.fire(ALL, Deed.of("trim"), 1);
        p.reset(ALL, "rank:one");
        assertFalse(p.earned("rank:one"));
        assertFalse(p.earned("rank:two"));
        assertTrue(p.earned("badge:garden"), "badges are separate");
        p.reset(ALL, null);
        assertTrue(p.states().isEmpty() && p.earnedAll().isEmpty());
    }

    @Test
    void progressSurvivesARestore() {
        var p = new Progress();
        p.fire(ALL, new Deed("plant", Map.of(), 1), 1);
        var q = new Progress();
        q.restore(p.states(), p.countsAll(), p.earnedAll());
        assertEquals(1, q.count("badge:garden/1"));
    }
}
