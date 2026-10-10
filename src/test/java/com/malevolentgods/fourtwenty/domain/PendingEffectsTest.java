package com.malevolentgods.fourtwenty.domain;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PendingEffectsTest {
    private final UUID player = UUID.randomUUID();

    @Test void doseAppliesAtDeadlineExactlyOnce() {
        var queue = new PendingEffects();
        assertTrue(queue.schedule(player, 200, 3600, 0, false));
        for (int tick = 1; tick < 200; tick++) assertTrue(queue.tick(id -> true).isEmpty());
        var effects = queue.tick(id -> true);
        assertEquals(1, effects.size());
        assertEquals(new PendingEffects.Effect(player, 3600, 0, false), effects.getFirst());
        assertFalse(queue.contains(player));
        assertTrue(queue.tick(id -> true).isEmpty());
    }
    @Test void secondDoseCannotReplaceOrExtendFirst() {
        var queue = new PendingEffects();
        queue.schedule(player, 2, 3600, 0, false);
        queue.tick(id -> true);
        assertFalse(queue.schedule(player, 300, 6000, 1, true));
        assertEquals(3600, queue.tick(id -> true).getFirst().duration());
    }
    @Test void sessionsAndPlayersAreIndependent() {
        var first = new PendingEffects();
        var second = new PendingEffects();
        var other = UUID.randomUUID();
        first.schedule(player, 1, 1, 0, false);
        first.schedule(other, 2, 2, 1, true);
        assertFalse(second.contains(player));
        assertEquals(player, first.tick(id -> true).getFirst().player());
        assertEquals(other, first.tick(id -> true).getFirst().player());
    }
    @Test void logoutOrFinalDeathCancelsWithoutReplay() {
        var queue = new PendingEffects();
        queue.schedule(player, 2, 3600, 0, false);
        assertTrue(queue.tick(id -> false).isEmpty());
        assertFalse(queue.contains(player));
        assertTrue(queue.tick(id -> true).isEmpty());
    }
    @Test void explicitCancellationDoesNotAffectOtherPlayers() {
        var queue = new PendingEffects();
        var other = UUID.randomUUID();
        queue.schedule(player, 1, 1, 0, false);
        queue.schedule(other, 1, 1, 0, false);
        queue.cancel(player);
        assertEquals(other, queue.tick(id -> true).getFirst().player());
    }
    @Test void invalidDurationsNeverEnterQueue() {
        var queue = new PendingEffects();
        assertThrows(IllegalArgumentException.class, () -> queue.schedule(player, 0, 1, 0, false));
        assertFalse(queue.contains(player));
    }
}
