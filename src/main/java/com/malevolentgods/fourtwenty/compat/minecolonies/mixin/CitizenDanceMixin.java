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
        var pose = DanceAnimation.current(citizen, partial);
        if (pose == null) return;
        var model = (HumanoidModel<?>) (Object) this;
        var body = pose.body();
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
        model.head.zRot += (float) body.head().roll() * RAD;
        if (amount < 0.1f) {   // legs only while she stands still: walking keeps its own stride
            model.leftLeg.xRot -= (float) body.leftLeg() * RAD;
            model.rightLeg.xRot -= (float) body.rightLeg() * RAD;
        }
        model.hat.copyFrom(model.head);
    }
}
