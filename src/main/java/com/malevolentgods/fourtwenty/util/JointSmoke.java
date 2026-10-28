package com.malevolentgods.fourtwenty.util;

import com.malevolentgods.fourtwenty.domain.Strain;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared smoke for joints and pipes: grey smoke threaded with the strain's colour (plain smoke for Landrace). */
public final class JointSmoke {
    /** The initial puff is emitted by the caller; subsequent puffs follow elapsed use time. */
    public static void tick(LivingEntity entity, long elapsedTicks) { tick(entity, elapsedTicks, Strain.LANDRACE); }

    public static void tick(LivingEntity entity, long elapsedTicks, Strain strain) {
        if (elapsedTicks > 0 && elapsedTicks % 8 == 0) puff(entity, 2, strain);
    }

    public static void puff(LivingEntity entity, int count) { puff(entity, count, Strain.LANDRACE); }

    /** Broadcast at the current face position; vanilla smoke supplies the upward drift. */
    public static void puff(LivingEntity entity, int count, Strain strain) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 forward = Vec3.directionFromRotation(0, entity.getYHeadRot()).scale(0.25);
        double x = entity.getX() + forward.x, y = entity.getEyeY() - 0.1, z = entity.getZ() + forward.z;
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, count, 0.08, 0.04, 0.08, 0.01);
        if (strain != Strain.LANDRACE) {
            int c = strain.color();
            var dust = new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.1f);
            level.sendParticles(dust, x, y + 0.05, z, Math.max(1, count), 0.12, 0.06, 0.12, 0.01);
        }
    }

    private JointSmoke() {}
}
