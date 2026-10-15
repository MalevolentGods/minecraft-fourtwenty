package com.malevolentgods.fourtwenty.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared vanilla smoke presentation for joint users, independent of optional integrations. */
public final class JointSmoke {
    /** The initial puff is emitted by the caller; subsequent puffs follow elapsed use time. */
    public static void tick(LivingEntity entity, long elapsedTicks) {
        if (elapsedTicks > 0 && elapsedTicks % 8 == 0) puff(entity, 2);
    }

    /** Broadcast at the current face position; vanilla smoke supplies the upward drift. */
    public static void puff(LivingEntity entity, int count) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 forward = Vec3.directionFromRotation(0, entity.getYHeadRot()).scale(0.25);
        level.sendParticles(ParticleTypes.SMOKE,
                entity.getX() + forward.x, entity.getEyeY() - 0.1, entity.getZ() + forward.z,
                count, 0.08, 0.04, 0.08, 0.01);
    }

    private JointSmoke() {}
}
