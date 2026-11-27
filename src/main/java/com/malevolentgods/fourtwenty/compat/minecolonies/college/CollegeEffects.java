package com.malevolentgods.fourtwenty.compat.minecolonies.college;

import com.malevolentgods.fourtwenty.domain.college.Study;
import com.minecolonies.api.colony.IColony;

/**
 * What happens when a study is written up, beyond being marked done (specs/017-weed-college FR-022, FR-024, FR-025). Colours and colony
 * effects are read straight from the record ({@link CollegeResearch#knownColours}, {@link CollegeResearch#factor}); this applies the rest:
 * Botany's seed batches and the MineColonies research effects of unlock studies. Filled in by milestone M3 (tasks T029, T030).
 */
public final class CollegeEffects {
    private CollegeEffects() {}

    public static void finished(IColony colony, Study study) {
        // M3: seed batches (strains) to a College for pickup; unlock studies' MineColonies research effects.
    }
}
