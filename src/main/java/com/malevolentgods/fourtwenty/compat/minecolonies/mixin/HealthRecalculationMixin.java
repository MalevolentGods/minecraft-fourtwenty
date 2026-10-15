package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CitizenBenefits;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.util.AttributeModifierUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AttributeModifierUtils.class, remap = false)
public abstract class HealthRecalculationMixin {
    @Inject(method = "removeAllHealthModifiers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;removeModifiers()V", shift = At.Shift.AFTER))
    private static void fourtwenty$retainEntitlement(LivingEntity entity, CallbackInfo ci) {
        if (entity instanceof AbstractEntityCitizen citizen) CitizenBenefits.restoreHealth(citizen);
    }
}
