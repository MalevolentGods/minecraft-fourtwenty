package com.malevolentgods.fourtwenty.compat.minecolonies.service;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryConfig;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.AffinityRules;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Exposure to weed, which is what starts a colonist's cravings: a bong session, secondhand smoke
 * from a colonist or player smoking nearby, or a dose. The first exposure rolls the citizen's
 * affinity; bong sessions also roll drift once a day. Children are never exposed.
 */
public final class Exposure {
    /** First exposure only (secondhand smoke): rolls affinity for a newcomer, changes nothing otherwise. */
    public static void expose(ServerLevel level, ICitizenData citizen) {
        if (citizen.isChild()) return;
        var saved = DispensarySavedData.get(level);
        saved.updateAffinity(citizen.getColony().getID(), citizen.getUUID(),
                affinity -> AffinityRules.expose(affinity, saved.time(), level.random::nextDouble));
    }

    /** Inside a burning bong's area: exposes a newcomer, otherwise the day's bong drift roll. */
    public static void bongSession(ServerLevel level, ICitizenData citizen) {
        if (citizen.isChild()) return;
        var saved = DispensarySavedData.get(level);
        saved.updateAffinity(citizen.getColony().getID(), citizen.getUUID(),
                affinity -> AffinityRules.bongSession(affinity, saved.time(), DispensaryConfig.tuning(), level.random::nextDouble));
    }

    /** After a dose: one drift roll. */
    public static void dose(ServerLevel level, ICitizenData citizen) {
        var saved = DispensarySavedData.get(level);
        saved.updateAffinity(citizen.getColony().getID(), citizen.getUUID(),
                affinity -> AffinityRules.drift(affinity, saved.time(), DispensaryConfig.tuning(), level.random::nextDouble));
    }

    /** Someone smoked a joint or pipe: colonists close by (other than the smoker) are exposed. */
    public static void secondhand(ServerLevel level, LivingEntity smoker) {
        int radius = DispensaryConfig.secondhandSmokeRadius();
        if (radius <= 0) return;
        var at = BlockPos.containing(smoker.position());
        for (var colony : IColonyManager.getInstance().getColonies(level)) {
            if (!colony.isCoordInColony(level, at)) continue;
            for (var citizen : colony.getCitizenManager().getCitizens()) {
                var entity = citizen.getEntity().orElse(null);
                if (entity == null || entity == smoker || entity.distanceToSqr(smoker) > (double) radius * radius) continue;
                expose(level, citizen);
            }
        }
    }

    private Exposure() {}
}
