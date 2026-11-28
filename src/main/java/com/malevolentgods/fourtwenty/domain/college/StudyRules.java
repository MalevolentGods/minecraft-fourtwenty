package com.malevolentgods.fourtwenty.domain.college;

import com.malevolentgods.fourtwenty.domain.Studies;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The Weed College's numbers (specs/017-weed-college FR-010, FR-011, FR-015, FR-016, FR-020). Pure and tested. */
public final class StudyRules {
    private StudyRules() {}

    public static final int MAX_TIER = 6, MAX_LEVEL = 5;

    /** Work units a study of {@code tier} takes: 72, 144, 288, 576, 1152, 2304 (the University's, via the Think Tank's {@link Studies}). */
    public static int units(int tier) { return Studies.units(tier); }

    /**
     * Whether a College of {@code level} may work a study of {@code tier}. Tiers 1 to 5 need that level; tier 6 needs level 5 and every
     * tier-5 study of its branch done ({@code branchTopDone}).
     */
    public static boolean allows(int level, int tier, boolean branchTopDone) {
        if (tier <= MAX_LEVEL) return level >= tier;
        return level >= MAX_LEVEL && branchTopDone;
    }

    /** Weedologists a College of {@code level} employs: one at levels 1 and 2, two from 3 (FR-004). */
    public static int workers(int level) {
        return level <= 0 ? 0 : level < 3 ? 1 : 2;
    }

    /**
     * What a study costs, fixed when it starts. A per-colour line is counted once for each colour the study researches; a colour the colony's
     * members already know costs {@code knownFactor} of its share, rounded up. Lines for the same item and strain are merged.
     */
    public static List<Study.Supply> cost(Study study, Set<String> knownColours, double knownFactor) {
        Map<String, Study.Supply> merged = new LinkedHashMap<>();
        for (Study.Supply s : study.supplies()) {
            int count = s.count();
            if (s.perColour()) {
                count = 0;
                for (String colour : study.gives().colours()) {
                    count += knownColours.contains(colour) ? (int) Math.ceil(s.count() * knownFactor) : s.count();
                }
                if (count == 0) continue;
            }
            String key = s.item() + "|" + s.strain();
            Study.Supply prior = merged.get(key);
            merged.put(key, new Study.Supply(s.item(), count + (prior == null ? 0 : prior.count()), s.strain(), false));
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * Whether the unit just worked was a bad batch (worked again, nothing lost). The chance falls with the Weedologist's Intelligence,
     * never below {@code floor}. {@code roll} is uniform in 0..1.
     */
    public static boolean badBatch(double roll, int intelligence, double base, double floor, double stepPerPoint) {
        return roll < Math.max(floor, base - Math.max(0, intelligence) * stepPerPoint);
    }
}
