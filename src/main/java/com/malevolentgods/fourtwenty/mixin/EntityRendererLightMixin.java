package com.malevolentgods.fourtwenty.mixin;

import com.malevolentgods.fourtwenty.client.DynamicLights;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mobs and players near a moving light are lit by it too. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererLightMixin {
    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true, require = 0)
    private void fourtwenty$movingLight(Entity entity, BlockPos pos, CallbackInfoReturnable<Integer> out) {
        out.setReturnValue(Math.max(out.getReturnValueI(), DynamicLights.at(pos)));
    }
}
