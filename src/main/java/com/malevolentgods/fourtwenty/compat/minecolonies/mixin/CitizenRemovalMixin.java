package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.DispensarySavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CitizenBenefits;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.CitizenDemand;
import com.malevolentgods.fourtwenty.compat.minecolonies.service.DispensaryCustomer;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.core.colony.managers.CitizenManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native removal covers death and departure; unloading only removes the entity and keeps this record. */
@Mixin(value = CitizenManager.class, remap = false)
public abstract class CitizenRemovalMixin {
    @Inject(method = "removeCivilian", at = @At("RETURN"))
    private void fourtwenty$forget(ICivilianData civilian, CallbackInfo ci) {
        if (!(civilian instanceof ICitizenData citizen) || !(citizen.getColony().getWorld() instanceof ServerLevel level)) return;
        citizen.getEntity().ifPresent(entity -> {
            var task = ((DispensaryCustomer) entity).fourtwenty$smokingTask();
            if (task != null) task.interrupt();
        });
        var saved = DispensarySavedData.get(level);
        saved.removeCitizen(citizen.getColony().getID(), citizen.getUUID());
        CitizenBenefits.refresh(citizen, CitizenDemand.empty(), saved.time());
    }
}
