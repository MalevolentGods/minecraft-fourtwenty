package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CannabisHappinessModifier;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenHappinessHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The pinned reader discards unknown IDs unless installed before server/client deserialization. */
@Mixin(value = CitizenHappinessHandler.class, remap = false)
public abstract class CitizenHappinessMixin {
    @Inject(method = "<init>()V", at = @At("RETURN"))
    private void fourtwenty$client(CallbackInfo ci) { fourtwenty$install(); }
    @Inject(method = "<init>(Lcom/minecolonies/api/colony/ICitizenData;)V", at = @At("RETURN"))
    private void fourtwenty$server(ICitizenData data, CallbackInfo ci) { fourtwenty$install(); }
    @org.spongepowered.asm.mixin.Unique
    private void fourtwenty$install() { ((CitizenHappinessHandler)(Object)this).addModifier(new CannabisHappinessModifier()); }
}
