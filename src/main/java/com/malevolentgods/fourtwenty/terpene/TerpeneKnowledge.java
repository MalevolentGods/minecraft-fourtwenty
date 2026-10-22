package com.malevolentgods.fourtwenty.terpene;

import com.malevolentgods.fourtwenty.domain.Terpenes;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * What the local player has discovered: terpene codes plus {@link #RESIN}. Filled from the
 * server ({@link TerpeneNetwork}); plain Java, so common code (item names) can ask it. On a
 * dedicated server nothing is ever hidden: {@link #knows} is only consulted on the client.
 */
public final class TerpeneKnowledge {
    private TerpeneKnowledge() {}

    /** The resin's discovery key (it has no terpene code). */
    public static final String RESIN = "resin";

    private static final Set<String> KNOWN = Collections.synchronizedSet(new LinkedHashSet<>());

    /** Shows the discovery toast; installed by the client. */
    private static volatile Consumer<String> onLearn = code -> {};

    /** Whether item names should be hidden at all (true only on a client). */
    private static volatile boolean client;

    public static void installClient(Consumer<String> toast) {
        client = true;
        onLearn = toast;
    }

    public static boolean knows(String key) { return !client || KNOWN.contains(key); }

    public static boolean knows(Terpenes.Code code) { return knows(code.id()); }

    /** The client's own copy, for the screen and guide. */
    public static Set<String> known() {
        synchronized (KNOWN) {
            return Set.copyOf(KNOWN);
        }
    }

    public static int count(int tier) {
        synchronized (KNOWN) {
            return (int) KNOWN.stream().filter(k -> !k.equals(RESIN)).filter(k -> k.split("_").length == tier).count();
        }
    }

    /** From the server: the whole list (on login) or newly learned keys (each gets a toast). */
    static void receive(java.util.List<String> keys, boolean replace) {
        if (replace) {
            KNOWN.clear();
            KNOWN.addAll(keys);
            return;
        }
        for (String key : keys) {
            if (KNOWN.add(key)) onLearn.accept(key);
        }
    }
}
