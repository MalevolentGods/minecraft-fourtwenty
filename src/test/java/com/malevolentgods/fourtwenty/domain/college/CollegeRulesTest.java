package com.malevolentgods.fourtwenty.domain.college;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Weed College's pure rules: units, tier caps, cost, bad batches, the queue, the board's locks and the colony effects. */
class CollegeRulesTest {
    private static Study study(String id, Study.Branch branch, int tier, List<String> parents) {
        return new Study(id, branch, tier, parents, Study.Kind.EFFECT, List.of(new Study.Supply("fourtwenty:weed_bud", 8, null, false)),
                Study.Gives.NOTHING, "fourtwenty:weed_bud", null);
    }

    private static Study colours(String id, List<String> colours) {
        return new Study(id, Study.Branch.CHEMISTRY, 1, List.of(), Study.Kind.COLOURS,
                List.of(new Study.Supply("fourtwenty:weed_bud", 8, null, true), new Study.Supply("fourtwenty:weed_seeds", 4, null, true),
                        new Study.Supply("minecraft:glass_bottle", 1, null, true)),
                new Study.Gives(colours, List.of(), 0, null, 0, null), "fourtwenty:weed_bud", null);
    }

    private static Study effect(String id, int tier, String effect, double strength) {
        return new Study(id, Study.Branch.ENGINEERING, tier, List.of(), Study.Kind.EFFECT, List.of(new Study.Supply("minecraft:charcoal", 1, null, false)),
                new Study.Gives(List.of(), List.of(), 0, effect, strength, null), "minecraft:charcoal", null);
    }

    @Test
    void unitsFollowTheUniversity() {
        assertEquals(List.of(72, 144, 288, 576, 1152, 2304), List.of(StudyRules.units(1), StudyRules.units(2), StudyRules.units(3),
                StudyRules.units(4), StudyRules.units(5), StudyRules.units(6)));
    }

    @Test
    void levelCapsTheTierAndTierSixNeedsTheBranchTop() {
        assertTrue(StudyRules.allows(1, 1, false));
        assertFalse(StudyRules.allows(1, 2, false));
        assertTrue(StudyRules.allows(5, 5, false));
        assertFalse(StudyRules.allows(5, 6, false));
        assertTrue(StudyRules.allows(5, 6, true));
        assertFalse(StudyRules.allows(4, 6, true));
    }

    @Test
    void workersByLevel() {
        assertEquals(List.of(0, 1, 1, 2, 2, 2), List.of(StudyRules.workers(0), StudyRules.workers(1), StudyRules.workers(2),
                StudyRules.workers(3), StudyRules.workers(4), StudyRules.workers(5)));
    }

    @Test
    void aCaughtColourCostsHalfItsShare() {
        Study warms = colours("w", List.of("mc_red", "mc_orange", "mc_yellow"));
        var full = StudyRules.cost(warms, Set.of(), 0.5);
        assertEquals(List.of(24, 12, 3), full.stream().map(Study.Supply::count).toList());
        var cheaper = StudyRules.cost(warms, Set.of("mc_red"), 0.5);
        assertEquals(List.of(20, 10, 3), cheaper.stream().map(Study.Supply::count).toList());   // red's bottle: ceil(0.5) = 1
    }

    @Test
    void badBatchesFallWithIntelligenceButNotBelowTheFloor() {
        assertTrue(StudyRules.badBatch(0.09, 0, 0.1, 0.03, 0.001));
        assertFalse(StudyRules.badBatch(0.09, 20, 0.1, 0.03, 0.001));   // 0.08
        assertTrue(StudyRules.badBatch(0.029, 1000, 0.1, 0.03, 0.001));
        assertFalse(StudyRules.badBatch(0.031, 1000, 0.1, 0.03, 0.001));
    }

    @Test
    void theQueueKeepsProgressAndRefusesToRemoveAHeadUnderWay() {
        var r = new ColonyResearch();
        assertTrue(r.enqueue("a"));
        assertTrue(r.enqueue("b"));
        assertFalse(r.enqueue("a"));
        assertTrue(r.remove("b"));
        assertTrue(r.enqueue("b"));
        r.start(List.of(new Study.Supply("fourtwenty:weed_bud", 8, null, false)));
        assertFalse(r.underWay());
        assertTrue(r.remove("a"), "a head with a fixed cost but nothing in it can come off");
        assertFalse(r.started(), "and its cost goes with it");
        assertTrue(r.enqueue("a"));
        assertEquals("b", r.head());
        assertTrue(r.remove("b"));
        assertTrue(r.enqueue("b"));
        assertEquals("a", r.head());
        r.start(List.of(new Study.Supply("fourtwenty:weed_bud", 8, null, false)));
        r.deliver(0, 1);
        assertTrue(r.underWay());
        assertFalse(r.remove("a"), "once supplies are in, the head stays");
        r.deliver(0, 4);
        assertEquals(3, r.missing().getFirst().count());
        r.deliver(0, 50);
        assertTrue(r.missing().isEmpty());
        assertEquals(1, r.work(false));
        assertEquals(1, r.work(true));
        assertEquals(1, r.badBatches());
        assertEquals("a", r.finishHead(7));
        assertTrue(r.isDone("a"));
        assertEquals("b", r.head());
        assertFalse(r.started());
        assertFalse(r.enqueue("a"));
    }

    @Test
    void theBoardLocksByFeatureThenParentThenLevelThenBranch() {
        var root = study("r", Study.Branch.BOTANY, 1, List.of());
        var child = study("c", Study.Branch.BOTANY, 2, List.of("r"));
        var top = study("t5", Study.Branch.BOTANY, 5, List.of("c"));
        var cap = study("t6", Study.Branch.BOTANY, 6, List.of("t5"));
        var waiting = new Study("w", Study.Branch.TEXTILES, 1, List.of(), Study.Kind.EFFECT, List.of(new Study.Supply("minecraft:string", 1, null, false)),
                Study.Gives.NOTHING, "minecraft:string", "tie_dye");
        var board = new StudyBoard(List.of(root, child, top, cap, waiting));
        assertTrue(board.validate().isEmpty());
        var r = new ColonyResearch();
        Map<String, StudyBoard.Standing> s = board.standings(r, 1, f -> false);
        assertEquals(StudyBoard.State.AVAILABLE, s.get("r").state());
        assertEquals(StudyBoard.Lock.PARENT, s.get("c").lock());
        assertEquals(StudyBoard.Lock.FEATURE, s.get("w").lock());
        r.enqueue("r");
        assertEquals(StudyBoard.Lock.LEVEL, board.standings(r, 1, f -> false).get("c").lock());   // parent queued ahead; the level stops it
        assertTrue(board.canQueue("c", r, 2, f -> false));
        r.finish("r", 1);
        r.finish("c", 2);
        r.finish("t5", 3);
        assertEquals(StudyBoard.Lock.LEVEL, board.standings(r, 4, f -> true).get("t6").lock());
        assertEquals(StudyBoard.State.AVAILABLE, board.standings(r, 5, f -> true).get("t6").state());
    }

    @Test
    void validationFindsUnknownParentsCyclesAndALonelyTierSix() {
        var a = study("a", Study.Branch.ARMORY, 1, List.of("b"));
        var b = study("b", Study.Branch.ARMORY, 1, List.of("a"));
        var ghost = study("g", Study.Branch.ARMORY, 2, List.of("nobody"));
        var lonely = study("l", Study.Branch.GLASSWORKS, 6, List.of());
        var errors = new StudyBoard(List.of(a, b, ghost, lonely)).validate();
        assertTrue(errors.stream().anyMatch(e -> e.contains("cycle")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("unknown parent nobody")));
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("l:")));
    }

    @Test
    void aLaterStudyReplacesAnEarlierOneInTheSameEffect() {
        var bench = effect("bench", 1, "trim_cure_speed", 1.25);
        var line = effect("line", 5, "trim_cure_speed", 1.5);
        assertEquals(1.0, ColonyEffects.factor(ColonyEffects.Effect.TRIM_CURE_SPEED, List.of()));
        assertEquals(1.25, ColonyEffects.factor(ColonyEffects.Effect.TRIM_CURE_SPEED, List.of(bench)));
        assertEquals(1.5, ColonyEffects.factor(ColonyEffects.Effect.TRIM_CURE_SPEED, List.of(line, bench)));
        assertEquals(0.0, ColonyEffects.factor(ColonyEffects.Effect.FARM_BONUS_BUD, List.of(bench)));
    }
}
