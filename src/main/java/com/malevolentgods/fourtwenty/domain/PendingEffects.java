package com.malevolentgods.fourtwenty.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** A session-local countdown; no world-time, entity or persistence references. */
public final class PendingEffects {
    public record Effect(UUID player, int duration, int amplifier, boolean premium) {}
    private record Pending(int remaining, Effect effect) {}
    private final Map<UUID, Pending> pending = new HashMap<>();

    public boolean schedule(UUID player, int delay, int duration, int amplifier, boolean premium) {
        if (delay < 1 || duration < 1 || amplifier < 0) throw new IllegalArgumentException("Invalid effect timing");
        return pending.putIfAbsent(player, new Pending(delay, new Effect(player, duration, amplifier, premium))) == null;
    }
    public boolean contains(UUID player) { return pending.containsKey(player); }
    public void cancel(UUID player) { pending.remove(player); }

    /** Remove due entries before applying any effects, so callbacks cannot replay a dose. */
    public List<Effect> tick(Predicate<UUID> eligible) {
        List<Effect> due = new ArrayList<>();
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var value = entry.getValue();
            if (!eligible.test(entry.getKey())) iterator.remove();
            else if (value.remaining() <= 1) {
                due.add(value.effect());
                iterator.remove();
            } else entry.setValue(new Pending(value.remaining() - 1, value.effect()));
        }
        return due;
    }
}
