package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.malevolentgods.fourtwenty.compat.minecolonies.builder.Album;
import com.malevolentgods.fourtwenty.compat.minecolonies.builder.BuilderConfig;
import com.malevolentgods.fourtwenty.domain.DanceRules;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Lifted Builder dancing to her music (specs/014-midnight-machine US3, T024). Only how she's drawn: her real place,
 * facing and hitbox stay exactly where MineColonies and her flight put them, so the dance can't change where she
 * builds or slow her down. Her whole body bobs, sways and spins here; {@code CitizenDanceMixin} adds the arms and head.
 */
public final class DanceAnimation {
    private DanceAnimation() {}

    private static final Set<Integer> PUSHED = new HashSet<>();
    /** Her waist, in blocks above her feet: what she leans and tilts round. */
    private static final double WAIST = 0.75;

    public static void install() {
        NeoForge.EVENT_BUS.addListener(DanceAnimation::pre);
        NeoForge.EVENT_BUS.addListener(DanceAnimation::post);
    }

    /** Her dance pose at this frame, or null when she isn't dancing (not playing). */
    @Nullable
    public static DanceRules.Pose current(AbstractEntityCitizen citizen, float partial) {
        int id = citizen.getId();
        if (!MidnightMachineClient.playing(id)) return null;
        var track = Album.track(MidnightMachineClient.track(id));
        long ms = MidnightMachineClient.positionMs(id, partial);
        if (ms >= track.lengthMs()) return null;   // the silence between tracks: a breather
        var beats = track.beats();
        boolean moving = citizen.getDeltaMovement().horizontalDistanceSqr() > 0.0004 || Math.abs(citizen.getX() - citizen.xo) + Math.abs(citizen.getZ() - citizen.zo) > 0.02;
        var tuning = DanceRules.Tuning.of(BuilderConfig.danceBob(), BuilderConfig.spinCooldownSeconds(), BuilderConfig.fallbackBpm());
        return DanceRules.pose(beats.bpm(), beats.beatsMs(), beats.accentsMs(), ms, moving, tuning, Album.routine(MidnightMachineClient.track(id)));
    }

    private static void pre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof AbstractEntityCitizen citizen)) return;
        var pose = current(citizen, event.getPartialTick());
        float cruise = cruise(citizen);
        if (pose == null && cruise < 0.01f) return;
        var stack = event.getPoseStack();
        stack.pushPose();
        double lean = cruise * CRUISE_RECLINE, tilt = 0;
        if (pose != null) {
            var body = pose.body();
            stack.translate(0, pose.bob() + body.bob(), 0);
            lean += body.lean() * (1 - cruise);
            tilt = body.tilt() * (1 - cruise);
        }
        // lean and tilt round her waist, in her own frame (the renderer turns her to face her way after this)
        if (lean != 0 || tilt != 0) {
            float facing = 180 - Mth.rotLerp(event.getPartialTick(), citizen.yBodyRotO, citizen.yBodyRot);
            stack.translate(0, WAIST, 0);
            stack.mulPose(Axis.YP.rotationDegrees(facing));
            stack.mulPose(Axis.XP.rotationDegrees((float) -lean));
            stack.mulPose(Axis.ZP.rotationDegrees((float) tilt));
            stack.mulPose(Axis.YP.rotationDegrees(-facing));
            stack.translate(0, -WAIST, 0);
        }
        // turns round her own middle: the renderer's origin is her feet, centred
        if (pose != null) stack.mulPose(Axis.YP.rotationDegrees((float) -(pose.spinDeg() + pose.swayDeg() + pose.body().turn())));
        PUSHED.add(citizen.getId());
    }

    /** How far back she reclines in the cruise, in degrees. */
    private static final double CRUISE_RECLINE = 12;
    /** How long the cruise takes to ease in or out, in seconds. */
    private static final double CRUISE_EASE = 0.3;

    private static final class Cruise {
        float weight;
        long at = System.nanoTime();
    }

    private static final java.util.Map<Integer, Cruise> CRUISES = new java.util.HashMap<>();

    /**
     * How much she's in her cruise pose (specs/015-lifted-body FR-009), 0..1, easing over CRUISE_EASE: a Lifted
     * Builder flying from place to place holds still (legs together, a slight recline) instead of walking in the air;
     * hovering to build or dance, the dance takes over. Read from her motion, no payload.
     */
    public static float cruise(AbstractEntityCitizen citizen) {
        boolean flying = MidnightMachineClient.lifted(citizen.getId()) && citizen.isNoGravity() && !citizen.onGround();
        double speed = Math.hypot(citizen.getX() - citizen.xo, citizen.getZ() - citizen.zo) + Math.abs(citizen.getY() - citizen.yo) * 0.5;
        float target = flying && speed > 0.04 ? 1 : 0;
        var c = CRUISES.get(citizen.getId());
        if (c == null) {
            if (target == 0) return 0;
            c = new Cruise();
            CRUISES.put(citizen.getId(), c);
        }
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - c.at) / 1e9);
        c.at = now;
        c.weight += (float) ((target - c.weight) * Math.min(1, dt / CRUISE_EASE));
        if (target == 0 && c.weight < 0.005f) {
            CRUISES.remove(citizen.getId());
            return 0;
        }
        return c.weight;
    }

    private static void post(RenderLivingEvent.Post<?, ?> event) {
        if (PUSHED.remove(event.getEntity().getId())) event.getPoseStack().popPose();
    }
}
