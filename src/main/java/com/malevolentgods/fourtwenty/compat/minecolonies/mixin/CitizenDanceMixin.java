package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.malevolentgods.fourtwenty.compat.minecolonies.client.DanceAnimation;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The arms and head of a dancing Builder (specs/014-midnight-machine T025), added after the model has posed her, so
 * the arm she places with still swings: that arm sits the dance out while she's swinging it.
 */
@Mixin(HumanoidModel.class)
public abstract class CitizenDanceMixin {
    private static final float RAD = (float) (Math.PI / 180);

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void fourtwenty$dance(LivingEntity entity, float limb, float amount, float age, float yaw, float pitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractEntityCitizen citizen)) return;
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        var pose = DanceAnimation.current(citizen, partial);
        if (pose == null) return;
        var model = (HumanoidModel<?>) (Object) this;
        float raise = (float) pose.armPitch() * RAD, out = (float) pose.armRoll() * RAD;
        model.leftArm.xRot -= raise;
        model.leftArm.zRot -= out;
        if (citizen.getAttackAnim(partial) <= 0) {
            model.rightArm.xRot -= raise;
            model.rightArm.zRot -= out;
        }
        model.head.xRot += (float) pose.headNod() * RAD;
        model.hat.copyFrom(model.head);
    }
}
