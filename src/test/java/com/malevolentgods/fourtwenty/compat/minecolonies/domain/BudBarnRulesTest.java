package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.BudBarnRules.Snapshot;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.BudBarnRules.Status;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.BudBarnRules.Step;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudBarnRulesTest {
    private static final int B = BudBarnRules.BUFFER;

    /** One tray, two racks (16 spots), by default empty. */
    private static Snapshot hut(int hanging, int cured, int trimmedHeld, int trimmedInHut, int roughHeld, int roughInHut) {
        return new Snapshot(1, 2, hanging, cured, 16 - hanging, trimmedHeld, trimmedInHut, roughHeld, roughInHut);
    }

    @Test
    void curedBudsComeDownFirst() {
        assertEquals(Step.COLLECT, BudBarnRules.next(hut(16, 1, 5, 0, 5, 5), B));
    }

    @Test
    void trimmedBudsAreHungBeforeMoreAreTrimmed() {
        assertEquals(Step.HANG, BudBarnRules.next(hut(0, 0, 1, 0, 5, 0), B));
        assertEquals(Step.HANG, BudBarnRules.next(hut(0, 0, 0, 3, 0, 0), B), "trimmed buds in the hut get fetched and hung");
    }

    @Test
    void roughBudsAreTrimmedThenFetchedThenRequested() {
        assertEquals(Step.TRIM, BudBarnRules.next(hut(16, 0, 0, 0, 4, 0), B));
        assertEquals(Step.FETCH, BudBarnRules.next(hut(16, 0, 0, 0, 0, 4), B));
        assertEquals(Step.REQUEST, BudBarnRules.next(hut(16, 0, 0, 0, 0, 0), B));
    }

    @Test
    void trimmingStopsAtFreeSpotsPlusTheBuffer() {
        // Racks full and nothing cured: trimmed buds pile up to the buffer and no further.
        assertEquals(Step.TRIM, BudBarnRules.next(hut(16, 0, B - 1, 0, 5, 0), B));
        assertEquals(Step.IDLE, BudBarnRules.next(hut(16, 0, B, 0, 5, 0), B));
        assertEquals(24, BudBarnRules.trimmedCap(16, B));
    }

    @Test
    void noTraysOrRacksMeansNoWork() {
        assertEquals(Step.IDLE, BudBarnRules.next(new Snapshot(0, 2, 0, 0, 16, 0, 0, 5, 0), B));
        assertEquals(Step.TRIM, BudBarnRules.next(new Snapshot(1, 0, 0, 0, 0, 0, 0, 5, 0), B), "trimming still works with no racks, up to the buffer");
        assertEquals(Step.IDLE, BudBarnRules.next(new Snapshot(1, 0, 0, 0, 0, B, 0, 5, 0), B));
    }

    @Test
    void theStatusSaysWhy() {
        assertEquals(Status.NO_WORKER, BudBarnRules.status(hut(0, 0, 0, 0, 0, 0), false, B));
        assertEquals(Status.NO_TRAYS, BudBarnRules.status(new Snapshot(0, 2, 0, 0, 16, 0, 0, 0, 0), true, B));
        assertEquals(Status.NO_RACKS, BudBarnRules.status(new Snapshot(1, 0, 0, 0, 0, 0, 0, 0, 0), true, B));
        assertEquals(Status.WAITING_FOR_ROUGH, BudBarnRules.status(hut(0, 0, 0, 0, 0, 0), true, B));
        assertEquals(Status.TRIMMING, BudBarnRules.status(hut(4, 0, 0, 0, 3, 0), true, B));
        assertEquals(Status.RACKS_FULL, BudBarnRules.status(hut(16, 0, B, 0, 2, 0), true, B));
        assertEquals(Status.CURING, BudBarnRules.status(hut(6, 0, 0, 0, 0, 0), true, B), "curing with room to spare and no rough buds left");
    }

    @Test
    void theWorkerSpeedsUpEachLevel() {
        for (int level = 2; level <= 5; level++)
            assertTrue(BudBarnRules.trimTicks(level, BudBarnRules.TRIM_TICKS) < BudBarnRules.trimTicks(level - 1, BudBarnRules.TRIM_TICKS), "level " + level);
        assertEquals(40, BudBarnRules.trimTicks(0, BudBarnRules.TRIM_TICKS), "levels clamp");
        assertEquals(10, BudBarnRules.trimTicks(9, BudBarnRules.TRIM_TICKS));
    }
}
