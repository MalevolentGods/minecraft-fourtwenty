package com.malevolentgods.fourtwenty.domain.handbook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Ranks, Stoner Badges and a player's progress through them (specs/016-weedcraft-handbook US6, contracts/scouting-data.md). Pure:
 * the game fires {@link Deed}s, this ticks requirements off, and works out what's earned.
 */
public final class Scouting {
    private Scouting() {}

    public enum Kind { TRACKED, HONOUR }

    /** One numbered requirement: {@code n} like "2a"; a tracked one watches for {@code deed} (matching {@code subject}) {@code count} times. */
    public record Req(String n, Kind kind, String deed, Map<String, String> subject, int count) {
        public Req {
            subject = subject == null ? Map.of() : Map.copyOf(subject);
            count = Math.max(1, count);
        }
    }

    /** A rank (earned in order) or a Stoner Badge (any order). {@code needs} is a mod id, or null. */
    public record Def(String id, boolean rank, int order, String needs, String entry, List<Req> reqs) {
        public String key(Req r) { return key(id, rank, r.n()); }
        public static String key(String id, boolean rank, String n) { return (rank ? "rank:" : "badge:") + id + "/" + n; }
        public String earnKey() { return (rank ? "rank:" : "badge:") + id; }
    }

    /** Something the player did: its name, what it was about, and how many. */
    public record Deed(String name, Map<String, String> subject, int amount) {
        public Deed {
            subject = subject == null ? Map.of() : Map.copyOf(subject);
            amount = Math.max(1, amount);
        }
        public static Deed of(String name, String... kv) {
            Map<String, String> m = new LinkedHashMap<>();
            for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
            return new Deed(name, m, 1);
        }
    }

    /** What a deed changed: requirement keys newly done, and the ranks and badges newly earned (def earn keys). */
    public record Result(List<String> ticked, List<String> earned) {
        public boolean any() { return !ticked.isEmpty() || !earned.isEmpty(); }
    }

    /** Whether a deed's subject value matches a requirement's: exact, {@code *} wildcards, and {@code a|b} alternatives. */
    public static boolean matches(String pattern, String value) {
        if (value == null) return false;
        for (String alt : pattern.split("\\|")) {
            if (alt.equals(value)) return true;
            if (alt.contains("*")) {
                StringBuilder re = new StringBuilder();
                for (String part : alt.split("\\*", -1)) {
                    if (re.length() > 0) re.append(".*");
                    re.append(Pattern.quote(part));
                }
                if (Pattern.matches(re.toString(), value)) return true;
            }
        }
        return false;
    }

    public static final int DONE = 1, HONOURED = 2;

    /** A player's progress: requirement keys done or signed, counts toward counted ones, what's earned and when. */
    public static final class Progress {
        private final Map<String, Integer> state = new LinkedHashMap<>();
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private final Map<String, Long> earned = new LinkedHashMap<>();

        public int state(String key) { return state.getOrDefault(key, 0); }

        public boolean satisfied(String key) { return state(key) != 0; }

        public int count(String key) { return counts.getOrDefault(key, 0); }

        public boolean earned(String earnKey) { return earned.containsKey(earnKey); }

        public Map<String, Long> earnedAll() { return Map.copyOf(earned); }

        public Map<String, Integer> states() { return Map.copyOf(state); }

        public Map<String, Integer> countsAll() { return Map.copyOf(counts); }

        public void restore(Map<String, Integer> s, Map<String, Integer> c, Map<String, Long> e) {
            state.clear(); state.putAll(s);
            counts.clear(); counts.putAll(c);
            earned.clear(); earned.putAll(e);
        }

        /** The highest rank earned (its order), or 0. */
        public int rank(List<Def> defs) {
            int best = 0;
            for (Def d : defs) if (d.rank() && earned(d.earnKey())) best = Math.max(best, d.order());
            return best;
        }

        /** Applies a deed to every requirement it matches, then earns whatever that completes (cascading: an earning is itself a deed). */
        public Result fire(List<Def> defs, Deed deed, long now) {
            List<String> ticked = new ArrayList<>(), earnedNow = new ArrayList<>();
            apply(defs, deed, now, ticked, earnedNow);
            return new Result(ticked, earnedNow);
        }

        private void apply(List<Def> defs, Deed deed, long now, List<String> ticked, List<String> earnedNow) {
            for (Def d : defs) {
                for (Req r : d.reqs()) {
                    if (r.kind() != Kind.TRACKED || !r.deed().equals(deed.name())) continue;
                    String key = d.key(r);
                    if (satisfied(key) || !subjectMatches(r, deed)) continue;
                    int n = count(key) + deed.amount();
                    counts.put(key, Math.min(n, r.count()));
                    if (n >= r.count()) {
                        state.put(key, DONE);
                        ticked.add(key);
                    }
                }
            }
            checkEarned(defs, now, ticked, earnedNow);
        }

        private static boolean subjectMatches(Req r, Deed deed) {
            for (var e : r.subject().entrySet()) {
                if (!matches(e.getValue(), deed.subject().get(e.getKey()))) return false;
            }
            return true;
        }

        private void checkEarned(List<Def> defs, long now, List<String> ticked, List<String> earnedNow) {
            boolean again = true;
            while (again) {
                again = false;
                for (Def d : defs) {
                    if (earned(d.earnKey())) continue;
                    if (d.rank() && d.order() > 1 && !earnedRank(defs, d.order() - 1)) continue;
                    boolean all = true;
                    for (Req r : d.reqs()) if (!satisfied(d.key(r))) { all = false; break; }
                    if (!all) continue;
                    earned.put(d.earnKey(), now);
                    earnedNow.add(d.earnKey());
                    again = true;
                    // an earning is a deed: "earn the X badge", "have N badges"
                    Deed e = new Deed("earn", Map.of("id", d.id(), "kind", d.rank() ? "rank" : "badge"), 1);
                    for (Def other : defs) {
                        for (Req r : other.reqs()) {
                            if (r.kind() != Kind.TRACKED || !r.deed().equals("earn")) continue;
                            String key = other.key(r);
                            if (satisfied(key) || !subjectMatches(r, e)) continue;
                            int n = count(key) + 1;
                            counts.put(key, Math.min(n, r.count()));
                            if (n >= r.count()) { state.put(key, DONE); ticked.add(key); }
                        }
                    }
                }
            }
        }

        private boolean earnedRank(List<Def> defs, int order) {
            for (Def d : defs) if (d.rank() && d.order() == order) return earned(d.earnKey());
            return true;
        }

        /** The player signs an honour requirement on their honour; false if it isn't one, or is already done. */
        public Result sign(List<Def> defs, String key, long now) {
            for (Def d : defs) {
                for (Req r : d.reqs()) {
                    if (!d.key(r).equals(key)) continue;
                    if (r.kind() != Kind.HONOUR || satisfied(key)) return new Result(List.of(), List.of());
                    state.put(key, HONOURED);
                    List<String> ticked = new ArrayList<>(List.of(key)), earnedNow = new ArrayList<>();
                    checkEarned(defs, now, ticked, earnedNow);
                    return new Result(ticked, earnedNow);
                }
            }
            return new Result(List.of(), List.of());
        }

        /** For testing and tools: a requirement done, whatever it needs (an operator's command); the same earning follows. */
        public Result force(List<Def> defs, String key, long now) {
            for (Def d : defs) {
                for (Req r : d.reqs()) {
                    if (!d.key(r).equals(key) || satisfied(key)) continue;
                    state.put(key, r.kind() == Kind.HONOUR ? HONOURED : DONE);
                    counts.put(key, r.count());
                    List<String> ticked = new ArrayList<>(List.of(key)), earnedNow = new ArrayList<>();
                    checkEarned(defs, now, ticked, earnedNow);
                    return new Result(ticked, earnedNow);
                }
            }
            return new Result(List.of(), List.of());
        }

        /** Back to open: everything, or one rank or badge (its earn key, like "badge:munchies"). Later ranks lose their earning with a rank. */
        public void reset(List<Def> defs, String earnKey) {
            if (earnKey == null) { state.clear(); counts.clear(); earned.clear(); return; }
            Def target = null;
            for (Def d : defs) if (d.earnKey().equals(earnKey)) target = d;
            if (target == null) return;
            clear(target);
            if (target.rank()) for (Def d : defs) if (d.rank() && d.order() > target.order()) clear(d);
        }

        private void clear(Def d) {
            earned.remove(d.earnKey());
            for (Req r : d.reqs()) { state.remove(d.key(r)); counts.remove(d.key(r)); }
        }
    }
}
