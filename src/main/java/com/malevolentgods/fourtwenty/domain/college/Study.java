package com.malevolentgods.fourtwenty.domain.college;

import java.util.List;
import java.util.Locale;

/**
 * One study on the Weed College's board (specs/017-weed-college data-model.md). Pure: ids are plain strings ("fourtwenty:bot_soil_science"),
 * items are registry ids. Loaded from data/fourtwenty/college/studies, written by tools/generate_college_studies.py.
 *
 * @param needs a later feature this study waits for ("tie_dye", "weedscout_uniform"), or null when it is available now
 */
public record Study(String id, Branch branch, int tier, List<String> parents, Kind kind, List<Supply> supplies, Gives gives, String icon,
                    String needs) {
    public Study {
        if (tier < 1 || tier > StudyRules.MAX_TIER) throw new IllegalArgumentException(id + ": tier " + tier);
        parents = List.copyOf(parents);
        supplies = List.copyOf(supplies);
    }

    public enum Branch {
        BOTANY, CHEMISTRY, GLASSWORKS, TEXTILES, ENGINEERING, ARMORY;

        public String key() { return name().toLowerCase(Locale.ROOT); }

        public static Branch of(String key) { return valueOf(key.toUpperCase(Locale.ROOT)); }
    }

    public enum Kind {
        COLOURS, STRAINS, EFFECT, UNLOCK;

        public static Kind of(String key) { return valueOf(key.toUpperCase(Locale.ROOT)); }
    }

    /**
     * Something a study uses up. {@code strain} narrows weed seeds or buds to one strain (null = any). A {@code perColour} line is counted
     * once per colour the study researches, so a colour the colony's members already know can cost less (research R9).
     */
    public record Supply(String item, int count, String strain, boolean perColour) {
        public Supply {
            if (count < 1) throw new IllegalArgumentException(item + ": count " + count);
        }
    }

    /**
     * What a finished study gives: colours the colony now knows, a seed batch of strains, a colony effect at a strength, or a
     * MineColonies research effect that unlocks crafter recipes.
     */
    public record Gives(List<String> colours, List<String> strains, int seedsEach, String effect, double strength, String researchEffect) {
        public static final Gives NOTHING = new Gives(List.of(), List.of(), 0, null, 0, null);

        public Gives {
            colours = List.copyOf(colours);
            strains = List.copyOf(strains);
        }
    }

    /** The study's length in work units, the University's: 72 at tier 1, doubling each tier. */
    public int units() { return StudyRules.units(tier); }
}
