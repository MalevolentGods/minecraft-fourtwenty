package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.service.SkillBonusHolder;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

/**
 * Temporary skill levels from a specialty bong. The colony tick sets the bonus from the saved buff
 * (so it survives restarts); it is never written into the citizen's own skill data.
 */
@Mixin(value = CitizenSkillHandler.class, remap = false)
public abstract class SkillBonusMixin implements SkillBonusHolder {
    @Unique private Map<Skill, Integer> fourtwenty$bonus = Map.of();

    @Override public Map<Skill, Integer> fourtwenty$skillBonus() { return fourtwenty$bonus; }
    @Override public void fourtwenty$skillBonus(Map<Skill, Integer> bonus) { fourtwenty$bonus = Map.copyOf(bonus); }

    @Inject(method = "getLevel", at = @At("RETURN"), cancellable = true)
    private void fourtwenty$addBonus(Skill skill, CallbackInfoReturnable<Integer> cir) {
        int bonus = fourtwenty$bonus.getOrDefault(skill, 0);
        if (bonus != 0) cir.setReturnValue(cir.getReturnValueI() + bonus);
    }
}
