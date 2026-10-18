package com.malevolentgods.fourtwenty.compat.minecolonies.service;

import com.malevolentgods.fourtwenty.compat.minecolonies.building.DispensaryBuilding;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.world.InteractionHand;
import java.util.UUID;

/**
 * Restaurant-style service: the Budtender walks to a customer waiting anywhere in the shop and
 * hands over a joint, instead of serving only whoever stands at the hut block. One per budtender AI.
 */
public final class CustomerService {
    /** Unreachable customers are skipped; they still self-serve after the wait deadline. */
    private static final int GIVE_UP_AFTER = 200;
    private UUID target;
    private UUID skip;
    private long giveUpAt;

    /** Returns true while serving this tick, so the caller skips the native work AI. */
    public boolean tick(EntityCitizen worker, DispensaryBuilding building, long now) {
        if (!JointService.live(building) || worker.getCitizenData().getWorkBuilding() != building
                || !CitizenSmokingTask.available(worker)) {
            target = null;
            return false;
        }
        EntityCitizen customer = target == null ? null : building.waitingCustomer(target, now);
        if (customer != null && now > giveUpAt) {
            skip = target;
            customer = null;
        }
        // Keep skipping an unreachable customer until they leave the queue (served by themselves or gone).
        if (skip != null && building.waitingCustomer(skip, now) == null) skip = null;
        if (customer == null) {
            customer = building.nextCustomer(now, skip);
            if (customer == null) {
                target = null;
                return false;
            }
            target = customer.getUUID();
            giveUpAt = now + GIVE_UP_AFTER;
        }
        if (!EntityNavigationUtils.walkToPos(worker, customer.blockPosition(), 2, true)) return true;
        if (building.serve(worker, customer)) worker.swing(InteractionHand.MAIN_HAND);
        target = null;
        return true;
    }
}
