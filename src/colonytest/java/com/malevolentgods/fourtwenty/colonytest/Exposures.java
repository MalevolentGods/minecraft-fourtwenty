package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryConfig;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Affinity;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.Preference;
import com.minecolonies.api.colony.ICitizenData;
import net.minecraft.server.level.ServerLevel;

/**
 * Fixtures predate affinity: cravings now need a first exposure. Give a fixture citizen a fixed
 * daily smoker affinity (the old one-joint-a-day demand) and pin drift off so deadlines stay exact.
 */
final class Exposures {
    static void dailySmoker(ICitizenData citizen) {
        DispensaryConfig.overrideTuning(new AffinityRules.Tuning(0, 0.75, 48_000));
        var saved = DispensarySavedData.get((ServerLevel) citizen.getColony().getWorld());
        saved.updateAffinity(citizen.getColony().getID(), citizen.getUUID(), affinity -> affinity.exposed() ? affinity
                : new Affinity(saved.time(), Preference.SMOKE, 3, 3, -1, saved.time()));
    }

    private Exposures() {}
}
