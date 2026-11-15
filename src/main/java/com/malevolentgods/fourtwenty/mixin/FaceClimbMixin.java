package com.malevolentgods.fourtwenty.mixin;

import com.malevolentgods.fourtwenty.util.FaceClimbing;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Anyone pressing into a {@code #fourtwenty:climbable_faces} block (MineColonies racks) climbs it like a ladder, from outside. */
@Mixin(LivingEntity.class)
public abstract class FaceClimbMixin {
    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void fourtwenty$climbFaces(CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ() && FaceClimbing.climbing((LivingEntity) (Object) this)) callback.setReturnValue(true);
    }
}
