package com.malevolentgods.fourtwenty.util;

import com.malevolentgods.fourtwenty.domain.Strain;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    /**
     * Vanilla smoke supplies the upward drift. Others see it at the smoker's face; the smoker sees it rise off the
     * joint's ember, out in front and below the eyes (born at the face, it filled the top of their own view).
     */
    public static void puff(LivingEntity entity, int count, Strain strain) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Vec3 forward = Vec3.directionFromRotation(0, entity.getYHeadRot());
        Vec3 face = new Vec3(entity.getX() + forward.x * 0.25, entity.getEyeY() - 0.1, entity.getZ() + forward.z * 0.25);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 ember = new Vec3(entity.getX() + forward.x * 0.75 + right.x * 0.18, entity.getEyeY() - 0.45, entity.getZ() + forward.z * 0.75 + right.z * 0.18);
        DustParticleOptions dust = null;
        if (strain != Strain.LANDRACE) {
            int c = strain.color();
            dust = new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.1f);
        }
        for (ServerPlayer viewer : level.players()) {
            Vec3 at = viewer == entity ? ember : face;
            level.sendParticles(viewer, ParticleTypes.SMOKE, false, at.x, at.y, at.z, count, 0.06, 0.03, 0.06, 0.01);
            if (dust != null) level.sendParticles(viewer, dust, false, at.x, at.y + 0.05, at.z, Math.max(1, count), 0.1, 0.05, 0.1, 0.01);
        }
    }

    private JointSmoke() {}
}
