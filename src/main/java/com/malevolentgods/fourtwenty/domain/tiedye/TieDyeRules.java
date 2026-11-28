package com.malevolentgods.fourtwenty.domain.tiedye;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Tie Dye Shack's rules (specs/018-tie-dye-shack): patterns and what unlocks them, which colourways a colony can order, how the
 * Shack grows by level, and what a batch costs. Pure and tested.
 */
public final class TieDyeRules {
    private TieDyeRules() {}

    /** Colour slots in every pattern; a colourway of fewer colours repeats them (slot k takes colour k mod n). */
    public static final int SLOTS = 4;

    /** The 8 patterns in order, by key; pattern {@code i} is unlocked by the College study {@code fourtwenty:tex_<key>}. */
    public static final List<String> PATTERNS = List.of("crumple", "stripes", "bullseye", "spiral", "sunburst", "shibori", "heart", "mandala");

    /** The things the Shack dyes. */
    public enum Kind { HEMP, RUG, COUCH, BED, SHIRT }

    /** A curated palette of 2 to 4 colour codes. */
    public record Colourway(String id, List<String> colours) {
        public Colourway {
            colours = List.copyOf(colours);
            if (colours.size() < 2 || colours.size() > SLOTS) throw new IllegalArgumentException(id + ": " + colours.size() + " colours");
        }

        public String colour(int slot) { return colours.get(Math.floorMod(slot, colours.size())); }

        /** Whether a colony knowing {@code known} can dye in it: every colour researched. */
        public boolean orderable(Set<String> known) { return known.containsAll(colours); }

        /** Its distinct colours, each costing one bud a batch. */
        public Set<String> distinct() { return new LinkedHashSet<>(colours); }
    }

    public static boolean validPattern(int pattern) { return pattern >= 0 && pattern < PATTERNS.size(); }

    public static String studyFor(int pattern) { return "fourtwenty:tex_" + PATTERNS.get(pattern); }

    /** Tie Dyers by level: 1, 1, 2, 2, 2 (data-model.md). */
    public static int workers(int level) { return level <= 0 ? 0 : level < 3 ? 1 : 2; }

    /** Items dyed in one batch by level: 4, 8, 8, 16, 16, times the configured scale. */
    public static int batch(int level, double scale) {
        int base = switch (Math.max(0, Math.min(5, level))) {
            case 0 -> 0;
            case 1 -> 4;
            case 2, 3 -> 8;
            default -> 16;
        };
        return Math.max(level <= 0 ? 0 : 1, (int) Math.round(base * scale));
    }

    /** One line of a batch's supplies. */
    public record Supply(String item, int count) {}

    /**
     * What dyeing {@code items} of a kind in a colourway uses: the plain items, a Cured Bud for each distinct colour per
     * {@code itemsPerBud} items (at least one each), and a Glass Bottle per batch (research R5). {@code plain} is the kind's plain item.
     */
    public static List<Supply> batchSupplies(Colourway c, String plain, int items, int itemsPerBud) {
        List<Supply> out = new ArrayList<>();
        if (items <= 0) return out;
        out.add(new Supply(plain, items));
        int buds = c.distinct().size() * Math.max(1, (int) Math.ceil(items / (double) Math.max(1, itemsPerBud)));
        out.add(new Supply("fourtwenty:weed_bud", buds));
        out.add(new Supply("minecraft:glass_bottle", 1));
        return out;
    }
}
