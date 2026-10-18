package com.malevolentgods.fourtwenty.compat.minecolonies.benefits;

import com.malevolentgods.fourtwenty.compat.minecolonies.*;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.CitizenDemand;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Reconciles native benefits from the single durable consumption checkpoint; never awards credit. */
public final class CitizenBenefits {
    public static final ResourceLocation HEALTH_ID = ResourceLocation.parse(DispensaryIds.HEALTH);
    private static final AttributeModifier HEALTH = new AttributeModifier(HEALTH_ID, 2, AttributeModifier.Operation.ADD_VALUE);

    public static void refresh(ICitizenData citizen) {
        if (!(citizen.getColony().getWorld() instanceof ServerLevel level)) return;
        var saved = DispensarySavedData.get(level);
        var state = saved.observe(citizen.getColony().getID(), citizen.getUUID(), ColonyIntegration.qualifies(citizen, saved),
                ColonyIntegration.rules(citizen, saved));
        refresh(citizen, state, saved.time());
    }
    public static void refresh(ICitizenData citizen, CitizenDemand state, long now) {
        var affinity = citizen.getColony().getWorld() instanceof ServerLevel level
                ? DispensarySavedData.get(level).affinity(citizen.getColony().getID(), citizen.getUUID())
                : com.malevolentgods.fourtwenty.compat.minecolonies.domain.Affinity.UNEXPOSED;
        var status = !affinity.exposed() ? CitizenDemand.Status.UNEXPOSED
                : affinity.onBreak(now) ? CitizenDemand.Status.TOLERANCE_BREAK : state.status(now);
        double weight = switch (status) {
            case SATISFIED -> DispensaryConfig.satisfactionWeight();
            case UNMET -> DispensaryConfig.unmetDemandWeight();
            case TOLERANCE_BREAK -> DispensaryConfig.toleranceBreakWeight();
            default -> 0;
        };
        var handler = citizen.getCitizenHappinessHandler();
        if (!(handler.getModifier(DispensaryIds.SATISFACTION) instanceof CannabisHappinessModifier old)
                || !old.matches(status, weight, affinity.preference(), affinity.step())) {
            // addModifier invalidates the native cache; dirty citizen data drives the existing view packet.
            handler.addModifier(new CannabisHappinessModifier(status, weight, affinity.preference(), affinity.step()));
            handler.getHappiness(citizen.getColony(), citizen);
            citizen.markDirty(0);
        }
        citizen.getEntity().ifPresent(entity -> applyHealth(entity, state.healthActive(now)));
    }
    /** Used after entity initialization and inside native bulk attribute removal, before its health clamp. */
    public static void restoreHealth(AbstractEntityCitizen entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity.getCitizenData() == null) return;
        var citizen = entity.getCitizenData();
        var saved = DispensarySavedData.get(level);
        applyHealth(entity, saved.citizen(citizen.getColony().getID(), citizen.getUUID()).healthActive(saved.time()));
    }
    private static void applyHealth(AbstractEntityCitizen entity, boolean active) {
        var attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        var existing = attribute.getModifier(HEALTH_ID);
        if (active && !HEALTH.equals(existing)) {
            if (existing != null) attribute.removeModifier(HEALTH_ID);
            // Native addHealthModifier heals proportionally. This entitlement increases capacity only.
            // Save the attribute cache too: vanilla loads attributes before Health and would otherwise
            // clamp a filled extra heart before colony initialization can restore the entitlement.
            // The saved demand deadline, not this cached modifier, remains authoritative on return.
            attribute.addPermanentModifier(HEALTH);
        } else if (!active && existing != null) {
            attribute.removeModifier(HEALTH_ID);
            if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
        }
    }
    private CitizenBenefits() {}
}
