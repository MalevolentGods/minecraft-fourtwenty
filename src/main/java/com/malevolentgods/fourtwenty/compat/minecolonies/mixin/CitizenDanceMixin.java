package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.client.DanceAnimation;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The arms, head and legs of a dancing Builder (specs/014-midnight-machine T025, T034), added after the model has posed
 * her, so the arm she places with still swings: that arm sits the dance out while she's swinging it.
 */
@Mixin(HumanoidModel.class)
public abstract class CitizenDanceMixin {
    private static final float RAD = (float) (Math.PI / 180);

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void fourtwenty$dance(LivingEntity entity, float limb, float amount, float age, float yaw, float pitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractEntityCitizen citizen)) return;
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        var model = (HumanoidModel<?>) (Object) this;
        // The model never resets the head's roll itself (it sets only its pitch and yaw each frame), and one model
        // draws every citizen: adding to it built up frame after frame until her head lay on her shoulder, and stayed
        // there after the music stopped (2026-11-14). So it's set outright here, back to level for anyone not dancing.
        model.head.zRot = 0;
        model.hat.zRot = 0;
        var pose = DanceAnimation.current(citizen, partial);
        if (pose != null) fourtwenty$danceLimbs(citizen, model, pose.body(), amount, partial);
        float cruise = DanceAnimation.cruise(citizen);
        boolean swinging = citizen.getAttackAnim(partial) > 0;
        if (cruise > 0.01f) {
            // the cruise (spec 015 FR-009): no walking stride in the air; legs together, arms relaxed
            model.leftLeg.xRot = Mth.lerp(cruise, model.leftLeg.xRot, 0.1f);
            model.rightLeg.xRot = Mth.lerp(cruise, model.rightLeg.xRot, 0.1f);
            model.leftLeg.yRot = Mth.lerp(cruise, model.leftLeg.yRot, 0);
            model.rightLeg.yRot = Mth.lerp(cruise, model.rightLeg.yRot, 0);
            model.leftArm.xRot = Mth.lerp(cruise * 0.85f, model.leftArm.xRot, 0.12f);
            model.leftArm.zRot = Mth.lerp(cruise * 0.85f, model.leftArm.zRot, -0.12f);
            if (!swinging) {
                model.rightArm.xRot = Mth.lerp(cruise * 0.85f, model.rightArm.xRot, 0.12f);
                model.rightArm.zRot = Mth.lerp(cruise * 0.85f, model.rightArm.zRot, 0.12f);
            }
        } else if (!swinging && fourtwenty$holdsJoint(citizen)) {
            // a smoke break (FR-010): the joint up to her mouth for a puff every few seconds, then down again
            float cycle = (age % 70f) / 70f;
            float raise = cycle < 0.35f ? Mth.sin((float) Math.PI * cycle / 0.35f) : 0;
            model.rightArm.xRot = Mth.lerp(raise, model.rightArm.xRot, -1.95f);
            model.rightArm.yRot = Mth.lerp(raise, model.rightArm.yRot, -0.45f);
            model.rightArm.zRot = Mth.lerp(raise, model.rightArm.zRot, 0);
        }
        model.hat.copyFrom(model.head);
    }

    @Unique
    private static boolean fourtwenty$holdsJoint(AbstractEntityCitizen citizen) {
        var held = citizen.getMainHandItem();
        return !held.isEmpty() && BuiltInRegistries.ITEM.getKey(held.getItem()).equals(
                ResourceLocation.parse(com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem.JOINT.itemId));
    }

    @Unique
    private static void fourtwenty$danceLimbs(AbstractEntityCitizen citizen, HumanoidModel<?> model,
                                              com.malevolentgods.fourtwenty.domain.DanceMoves.Body body, float amount, float partial) {
        // arms: pitch raises forward and up, yaw turns in, roll takes each one out to her own side
        model.leftArm.xRot -= (float) body.leftArm().pitch() * RAD;
        model.leftArm.yRot += (float) body.leftArm().yaw() * RAD;
        model.leftArm.zRot -= (float) body.leftArm().roll() * RAD;
        if (citizen.getAttackAnim(partial) <= 0) {   // the arm she places with swings as MineColonies has it
            model.rightArm.xRot -= (float) body.rightArm().pitch() * RAD;
            model.rightArm.yRot -= (float) body.rightArm().yaw() * RAD;
            model.rightArm.zRot += (float) body.rightArm().roll() * RAD;
        }
        model.head.xRot += (float) body.head().pitch() * RAD;
        model.head.yRot += (float) body.head().yaw() * RAD;
        model.head.zRot = (float) body.head().roll() * RAD;
        if (amount < 0.1f) {   // legs only while she stands still: walking keeps its own stride
            model.leftLeg.xRot -= (float) body.leftLeg() * RAD;
            model.rightLeg.xRot -= (float) body.rightLeg() * RAD;
        }
    }
}
