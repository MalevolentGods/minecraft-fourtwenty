package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CitizenDemandTest {
    private final DemandRules rules = DemandRules.defaults();
    @Test void onlyAdultAssignedCompletedLevelThreeOrHigherQualifies() {
        for (int level = 0; level <= 5; level++) {
            assertEquals(level >= 3, DemandRules.eligible(true, true, level));
            assertFalse(DemandRules.eligible(false, true, level));
            assertFalse(DemandRules.eligible(true, false, level));
        }
        // A planned level 3 upgrade still has completed level 2 as its only rule input.
        assertFalse(DemandRules.eligible(true, true, 2));
    }
    @Test void existingEligibleCitizenStartsDueWithFullGrace() {
        var state = CitizenDemand.empty().observe(true, 123, rules);
        assertTrue(state.due(123));
        assertEquals(CitizenDemand.Status.GRACE, state.status(24_122));
        assertEquals(CitizenDemand.Status.UNMET, state.status(24_123));
        assertFalse(state.healthActive(123));
    }
    @Test void exactConsumptionSatisfactionGraceAndHealthBoundaries() {
        var state = CitizenDemand.empty().observe(true, 100, rules).consume(1, 100, rules).orElseThrow();
        assertFalse(state.due(24_099));
        assertEquals(CitizenDemand.Status.SATISFIED, state.status(24_099));
        assertTrue(state.healthActive(24_099));
        assertTrue(state.due(24_100));
        assertFalse(state.healthActive(24_100));
        assertEquals(CitizenDemand.Status.GRACE, state.status(24_100));
        assertEquals(CitizenDemand.Status.GRACE, state.status(48_099));
        assertEquals(CitizenDemand.Status.UNMET, state.status(48_100));
    }
    @Test void reassignmentPreservesScheduleAndLossSuppressesDemandAndPenalty() {
        var state = CitizenDemand.empty().observe(true, 100, rules);
        assertSame(state, state.observe(true, 23_000, rules));
        var lost = state.observe(false, 24_100, rules);
        assertFalse(lost.due(90_000));
        assertEquals(CitizenDemand.Status.INELIGIBLE, lost.status(90_000));
        assertTrue(lost.consume(state.revision(), 90_000, rules).isEmpty());
        var returned = lost.observe(true, 90_000, rules);
        assertTrue(returned.due(90_000));
        assertEquals(CitizenDemand.Status.GRACE, returned.status(90_000));
        assertEquals(114_000, returned.graceUntil());
        assertTrue(returned.consume(state.revision(), 90_000, rules).isEmpty());
    }
    @Test void eligibilityCyclingCannotCreateAnExtraDoseOrRemoveEarnedHealth() {
        var paid = CitizenDemand.empty().observe(true, 0, rules).consume(1, 0, rules).orElseThrow();
        var lost = paid.observe(false, 10, rules);
        assertTrue(lost.healthActive(11));
        var returned = lost.observe(true, 20, rules);
        assertFalse(returned.due(20));
        assertEquals(paid.graceUntil(), returned.graceUntil());
        assertTrue(returned.consume(returned.revision(), 20, rules).isEmpty());
    }
    @Test void duplicateAndStaleCompletionTokensStayInvalidAfterRestore() {
        var before = CitizenDemand.empty().observe(true, 10, rules);
        var paid = before.consume(before.revision(), 10, rules).orElseThrow();
        var restored = new CitizenDemand(paid.eligible(), paid.dueAt(), paid.graceUntil(), paid.satisfiedUntil(),
                paid.healthUntil(), paid.lastConsumedAt(), paid.revision());
        assertTrue(restored.consume(before.revision(), 10, rules).isEmpty());
        assertTrue(restored.consume(before.revision(), 100_000, rules).isEmpty());
        assertTrue(restored.consume(restored.revision(), 100_000, rules).isPresent());
    }
    @Test void configurationChangesPreserveExistingDeadlines() {
        var paid = CitizenDemand.empty().observe(true, 0, rules).consume(1, 0, rules).orElseThrow();
        var changed = new DemandRules(100, 200, 300);
        assertSame(paid, paid.observe(true, 1, changed));
        var next = paid.consume(paid.revision(), 24_000, changed).orElseThrow();
        assertEquals(24_100, next.dueAt());
        assertEquals(24_300, next.graceUntil());
        assertEquals(24_300, next.healthUntil());
    }
    @Test void overdueDemandDoesNotBankMissedDoses() {
        var state = CitizenDemand.empty().observe(true, 0, rules).consume(1, 1_000_000, rules).orElseThrow();
        assertEquals(1_024_000, state.dueAt());
        assertTrue(state.consume(state.revision(), 1_000_000, rules).isEmpty());
    }
    @Test void healthRefreshHasOneDeadlineEvenWhenDurationExceedsInterval() {
        var longHealth = new DemandRules(10, 10, 100);
        var state = CitizenDemand.empty().observe(true, 0, longHealth).consume(1, 0, longHealth).orElseThrow();
        state = state.consume(state.revision(), 10, longHealth).orElseThrow();
        assertEquals(110, state.healthUntil());
        assertFalse(state.healthActive(110));
    }
    @Test void invalidDurationsAndCorruptRecordsAreRejected() {
        for (int invalid : new int[]{-1, 0, DemandRules.MAX_TICKS + 1}) {
            assertThrows(IllegalArgumentException.class, () -> new DemandRules(invalid, 1, 1));
            assertThrows(IllegalArgumentException.class, () -> new DemandRules(1, invalid, 1));
            assertThrows(IllegalArgumentException.class, () -> new DemandRules(1, 1, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> new CitizenDemand(true, -1, -1, -1, -1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> CitizenDemand.empty().observe(true, -1, rules));
    }
}
