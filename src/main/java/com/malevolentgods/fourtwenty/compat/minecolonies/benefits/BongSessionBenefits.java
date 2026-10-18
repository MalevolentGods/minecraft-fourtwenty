package com.malevolentgods.fourtwenty.compat.minecolonies.benefits;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryConfig;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryIds;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.BongSessionRules;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Grants and expires the bong session bonus. Called by a burning bong once a second, and from the colony tick. */
public final class BongSessionBenefits {
    /** A burning bong at {@code pos}: colonists of the colony it stands in, within range, get the bonus. */
    public static void burning(ServerLevel level, BlockPos pos) {
        double weight = DispensaryConfig.bongSessionWeight();
        int radius = DispensaryConfig.bongSessionRadius();
        int duration = DispensaryConfig.bongSessionTicks();
        long now = DispensarySavedData.get(level).time();
        for (var colony : IColonyManager.getInstance().getColonies(level)) {
            if (!colony.isCoordInColony(level, pos)) continue;
            for (var citizen : colony.getCitizenManager().getCitizens()) {
                var entity = citizen.getEntity().orElse(null);
                if (entity == null || entity.distanceToSqr(pos.getCenter()) > (double) radius * radius) continue;
                com.malevolentgods.fourtwenty.compat.minecolonies.service.Exposure.bongSession(level, citizen);
                if (weight <= 0) continue;
                long expires = current(citizen) instanceof BongSessionHappinessModifier old && old.active() ? old.expires() : 0;
                if (BongSessionRules.shouldRefresh(expires, now, duration)) set(citizen, new BongSessionHappinessModifier(true, now + duration, weight));
            }
        }
    }

    /** Switch an expired bonus off; called for every citizen from the colony tick. */
    public static void refresh(ICitizenData citizen, long now) {
        if (current(citizen) instanceof BongSessionHappinessModifier old && old.active() && !BongSessionRules.active(old.expires(), now)) {
            set(citizen, new BongSessionHappinessModifier(false, old.expires(), old.getWeight()));
        }
    }

    private static Object current(ICitizenData citizen) {
        return citizen.getCitizenHappinessHandler().getModifier(DispensaryIds.BONG_SESSION);
    }

    private static void set(ICitizenData citizen, BongSessionHappinessModifier modifier) {
        // addModifier invalidates the native cache; dirty citizen data drives the existing view packet.
        var handler = citizen.getCitizenHappinessHandler();
        handler.addModifier(modifier);
        handler.getHappiness(citizen.getColony(), citizen);
        citizen.markDirty(0);
    }

    private BongSessionBenefits() {}
}
