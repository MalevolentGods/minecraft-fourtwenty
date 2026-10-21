package com.malevolentgods.fourtwenty.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The terpene system, like codons: four base terpenes are the alphabet; two in order make one of
 * 16 accords; an accord and a third base make one of 64 final terpenes, each a hemp colour.
 * Port 1 sets the family, port 2 the hue, port 3 the tone. Pure: names, colours and items live
 * elsewhere (tools/terpenes.json is the source; the codes here are the ids).
 */
public final class Terpenes {
    private Terpenes() {}

    /** The alphabet, in codex order. Each is refined from resin by one method. */
    public enum Base {
        MYR("heat"), LIM("cold"), PIN("pressure"), LAV("time");

        private final String method;

        Base(String method) { this.method = method; }

        public String code() { return name().toLowerCase(Locale.ROOT); }

        /** How resin is refined into this base (the Terpinator's method dial). */
        public String method() { return method; }

        public static Optional<Base> byCode(String code) {
            for (Base b : values()) if (b.code().equals(code)) return Optional.of(b);
            return Optional.empty();
        }
    }

    /** The tones port 3 sets, in codex column order. */
    public static final List<Base> TONE_ORDER = List.of(Base.LIM, Base.PIN, Base.LAV, Base.MYR);

    /** A terpene: one base, an accord (two) or a final terpene (three), in port order. */
    public record Code(List<Base> letters) {
        public Code {
            if (letters.isEmpty() || letters.size() > 3) throw new IllegalArgumentException("A terpene has 1 to 3 letters");
            letters = List.copyOf(letters);
        }

        public static Code of(Base... letters) { return new Code(List.of(letters)); }

        /** "myr", "myr_pin", "myr_pin_lim". */
        public String id() {
            StringBuilder out = new StringBuilder();
            for (Base b : letters) out.append(out.isEmpty() ? "" : "_").append(b.code());
            return out.toString();
        }

        public static Optional<Code> parse(String id) {
            List<Base> out = new ArrayList<>();
            for (String part : id.split("_")) {
                Optional<Base> b = Base.byCode(part);
                if (b.isEmpty()) return Optional.empty();
                out.add(b.get());
            }
            return out.isEmpty() || out.size() > 3 ? Optional.empty() : Optional.of(new Code(out));
        }

        public int tier() { return letters.size(); }

        public boolean isBase() { return tier() == 1; }

        public boolean isAccord() { return tier() == 2; }

        public boolean isFinal() { return tier() == 3; }

        /** Base terpenes' worth: 1, 2 or 3 (amounts are conserved through every blend). */
        public int units() { return tier(); }

        /** The accord a final terpene is finished from (its first two letters). */
        public Code accord() {
            if (!isFinal()) throw new IllegalStateException("Only a final terpene has an accord");
            return new Code(letters.subList(0, 2));
        }

        /** Index in {@link #all()} order within its tier: 0..3, 0..15 or 0..63. */
        public int index() {
            int i = 0;
            for (int p = 0; p < letters.size(); p++) {
                Base b = letters.get(p);
                i = i * 4 + (p == 2 ? TONE_ORDER.indexOf(b) : b.ordinal());   // port 3 counts in tone order
            }
            return i;
        }
    }

    /** Two bases in ports 1 and 2 make this accord. */
    public static Code blend(Base first, Base second) { return Code.of(first, second); }

    /** An accord in port 1 and a base in port 3 make this final terpene. */
    public static Code finish(Code accord, Base third) {
        if (!accord.isAccord()) throw new IllegalArgumentException("Finish an accord, not " + accord.id());
        return Code.of(accord.letters().get(0), accord.letters().get(1), third);
    }

    /**
     * What the Terpinator's three ports make, or empty: two bases (port 3 empty) make an accord; an
     * accord in port 1 (port 2 empty) and a base in port 3 make a final terpene.
     */
    public static Optional<Code> result(Code port1, Code port2, Code port3) {
        if (port1 == null) return Optional.empty();
        if (port1.isBase() && port2 != null && port2.isBase() && port3 == null) {
            return Optional.of(blend(port1.letters().get(0), port2.letters().get(0)));
        }
        if (port1.isAccord() && port2 == null && port3 != null && port3.isBase()) {
            return Optional.of(finish(port1, port3.letters().get(0)));
        }
        return Optional.empty();
    }

    private static final List<Code> BASES, ACCORDS, FINALS, ALL;

    static {
        List<Code> bases = new ArrayList<>(), accords = new ArrayList<>(), finals = new ArrayList<>();
        for (Base a : Base.values()) {
            bases.add(Code.of(a));
            for (Base b : Base.values()) {
                accords.add(Code.of(a, b));
                for (Base c : TONE_ORDER) finals.add(Code.of(a, b, c));
            }
        }
        BASES = Collections.unmodifiableList(bases);
        ACCORDS = Collections.unmodifiableList(accords);
        FINALS = Collections.unmodifiableList(finals);
        List<Code> all = new ArrayList<>(bases);
        all.addAll(accords);
        all.addAll(finals);
        ALL = Collections.unmodifiableList(all);
    }

    public static List<Code> bases() { return BASES; }

    public static List<Code> accords() { return ACCORDS; }

    /** In codex order: family, then hue, then tone (pale, bright, dusky, deep). */
    public static List<Code> finals() { return FINALS; }

    /** All 84 terpenes: 4 bases, 16 accords, 64 finals. */
    public static List<Code> all() { return ALL; }
}
