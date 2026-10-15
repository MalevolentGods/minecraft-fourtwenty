package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CitizenBenefits;
import com.minecolonies.core.colony.CitizenData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CitizenData.class, remap = false)
public abstract class CitizenAttributesMixin {
    @Inject(method = "initEntityValues", at = @At("RETURN"))
    private void fourtwenty$restore(CallbackInfo ci) {
        ((CitizenData)(Object)this).getEntity().ifPresent(CitizenBenefits::restoreHealth);
    }
}
